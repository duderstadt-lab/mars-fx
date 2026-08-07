/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpg.biochem.mars.fx.dialogs.s3.explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Walks an S3 bucket and discovers datasets — Molecule Archives (.yama /
 * .yama.store) and N5 containers (.n5) — producing a list of
 * {@link DatasetEntry}.
 *
 * <p><b>Integration point.</b> This class deliberately talks to your existing
 * {@code MarsS3Browser} through the small {@link S3Access} interface below,
 * rather than importing it directly, so this file compiles in isolation and you
 * can wire it to the real browser in one place. In the command/window you'll
 * pass a lambda adapter, e.g.:
 *
 * <pre>{@code
 * MarsS3Browser browser = new MarsS3Browser(endpoint, accessKey, secretKey);
 * DatasetIndexer.S3Access access = new DatasetIndexer.S3Access() {
 *     public List<S3Object> list(String bucket, String prefix) {
 *         // adapt MarsS3Browser.listFiles(bucket, prefix) to S3Object records
 *     }
 *     public boolean isArchive(String key) { return browser.isArchive(key); }
 * };
 * }</pre>
 *
 * Adjust the method names in the adapter to match whatever {@code listFiles}
 * signature you settled on in mars-minio (the summary notes it lists objects at
 * a prefix level and that {@code isArchive()} detects .yama/.yama.store).
 */
public class DatasetIndexer {

    /** Optional per-object metadata (size / last-modified) for a dataset. */
    public static final class ObjectMeta {
        public final long size;          // bytes; -1 if unknown
        public final Long lastModified;  // epoch millis, or null
        public ObjectMeta(long size, Long lastModified) {
            this.size = size;
            this.lastModified = lastModified;
        }
    }

    /** Combined listFolders+listFiles result for one prefix — see {@link S3Access#listChildren}. */
    public static final class Children {
        public final List<String> folders;
        public final List<String> files;
        public Children(List<String> folders, List<String> files) {
            this.folders = folders;
            this.files = files;
        }
    }

    /**
     * The slice of {@code MarsS3Browser} this indexer relies on. These map
     * one-to-one onto existing MarsS3Browser methods, so the adapter in the
     * window is just method references — no new browser code required.
     */
    public interface S3Access {
        /** Immediate child folder names under {@code prefix} (MarsS3Browser.listFolders). */
        List<String> listFolders(String bucket, String prefix) throws Exception;
        /** Immediate child file names under {@code prefix} (MarsS3Browser.listFiles). */
        List<String> listFiles(String bucket, String prefix) throws Exception;
        /** True if the name is an N5 container (MarsS3Browser.isN5). */
        boolean isN5(String name) throws Exception;
        /** True if the name is a Molecule Archive (MarsS3Browser.isArchive). */
        boolean isArchive(String name) throws Exception;

        /**
         * Optional: last-modified / size for a dataset at {@code key}. MarsS3Browser
         * doesn't expose this today, so the default returns null (timestamps simply
         * won't show on cards). If you later add a metadata getter, override this.
         */
        default ObjectMeta meta(String bucket, String key) throws Exception {
            return null;
        }

        /**
         * Folders and files under {@code prefix} in one call. The walk needs both
         * at every prefix; {@code listFolders}+{@code listFiles} are each their
         * own identical (same bucket/prefix/delimiter) request, so calling both
         * pays for two round trips where one suffices — {@code MarsS3Browser}
         * exposes {@code listChildren} for exactly this. Default falls back to
         * calling both separately, for adapters that haven't overridden it.
         */
        default Children listChildren(String bucket, String prefix) throws Exception {
            return new Children(listFolders(bucket, prefix), listFiles(bucket, prefix));
        }
    }

    /** Progress/callback hooks so the UI can show status and stream results in. */
    public interface Listener {
        default void onStarted() {}
        default void onDatasetFound(DatasetEntry entry) {}
        default void onProgress(int discovered, String currentPrefix) {}
        default void onFinished(List<DatasetEntry> all) {}
        default void onError(Exception e) {}
    }

    // Bucket-tree walks are latency-bound (each listChildren call is a network
    // round trip), not CPU-bound, so a pool wider than the CPU count is
    // appropriate — this bounds how many concurrent requests hit the S3/MinIO
    // endpoint at once. Tuned against a bucket with ~11k ordinary folders for
    // 1348 datasets: walk time scaled cleanly with this value once
    // MarsS3Browser's HTTP client connection pool was also raised past its ~50
    // default (see buildClient in mars-minio) — that pool, not this one, was the
    // hidden ceiling once this got past ~48. If a much larger/shared MinIO
    // server starts showing degraded latency or errors under this load, lower it.
    private static final int PARALLELISM = 150;

    private final S3Access s3;
    private volatile boolean cancelled = false;

    // Paths already in the cached index — metadata fetch is skipped for these
    // (incremental reindex). Empty set = full reindex (fetch everything).
    private java.util.Set<String> knownPaths = java.util.Collections.emptySet();

    public DatasetIndexer(S3Access s3) {
        this.s3 = s3;
    }

    public void cancel() { this.cancelled = true; }

    /**
     * Restrict metadata fetching to datasets NOT in this set. Discovered datasets
     * whose path is already known are emitted "bare" (name/path/type only) so the
     * caller can substitute its cached entry — avoiding a metadata round-trip per
     * existing dataset. Pass an empty set (the default) for a full reindex.
     */
    public void setKnownPaths(java.util.Set<String> knownPaths) {
        this.knownPaths = knownPaths == null ? java.util.Collections.emptySet() : knownPaths;
    }

    /**
     * Runs the walk on the CURRENT thread, fanning sibling subtree walks out
     * across a bounded worker pool ({@link #PARALLELISM}) so latency from one
     * {@code listFolders}/{@code listFiles} round trip overlaps with others
     * instead of paying for the whole tree serially. Callers should invoke this
     * from a background thread (see {@link #indexAsync}). Results are also
     * delivered incrementally via the listener.
     */
    public List<DatasetEntry> index(String bucket, Listener listener) {
        List<DatasetEntry> out = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(PARALLELISM);
        try {
            listener.onStarted();
            walkAsync(bucket, "", out, listener, pool).join();
            if (!cancelled) listener.onFinished(new ArrayList<>(out));
        } catch (CompletionException ce) {
            Throwable cause = unwrap(ce);
            listener.onError(cause instanceof Exception ? (Exception) cause : new Exception(cause));
        } catch (Exception e) {
            listener.onError(e);
        } finally {
            pool.shutdown();
        }
        return new ArrayList<>(out);
    }

    /** Convenience: run {@link #index} on a daemon background thread. */
    public Thread indexAsync(String bucket, Listener listener) {
        Thread t = new Thread(() -> index(bucket, listener), "DatasetIndexer");
        t.setDaemon(true);
        t.start();
        return t;
    }

    // Recursive descent using MarsS3Browser's folder/file name listings.
    //
    // At each prefix level:
    //   - files (regular objects): a single-file .yama / .yama.json is an archive dataset.
    //   - folders (common prefixes): classify by NAME —
    //        .n5        => N5 dataset          (don't descend)
    //        .yama.store => archive dataset    (don't descend)
    //        otherwise  => ordinary folder     (recurse)
    //   - a .n5 that appears as a file-name (rare) is also caught.
    //
    // Keys are assembled as prefix + name (+ "/" for folders) since MarsS3Browser
    // returns only the last path segment.
    //
    // Concurrency: each level's listChildren round trip runs on the pool, and
    // ordinary-folder children recurse via thenCompose rather than a blocking
    // join — so no pool thread ever blocks waiting on another pool thread
    // (which would risk exhausting a bounded pool on deep trees). The only
    // blocking join() is the single one in index(), on the caller's own
    // background thread, outside the pool.
    private CompletableFuture<Void> walkAsync(String bucket, String prefix, List<DatasetEntry> out,
                                               Listener listener, ExecutorService pool) {
        if (cancelled) return CompletableFuture.completedFuture(null);
        listener.onProgress(out.size(), prefix);

        CompletableFuture<Children> childrenF = CompletableFuture.supplyAsync(
                () -> listOrThrow(() -> s3.listChildren(bucket, prefix)), pool);

        return childrenF.thenApply(children -> {
            List<String> files = children.files;
            List<String> folders = children.folders;
            List<CompletableFuture<Void>> subtasks = new ArrayList<>();

            for (String fileName : files) {
                if (cancelled) break;
                String key = prefix + fileName;
                subtasks.add(CompletableFuture.runAsync(
                        () -> classifyAndAdd(bucket, key, fileName, out, listener), pool));
            }

            for (String folderName : folders) {
                if (cancelled) break;
                String key = prefix + folderName; // dataset key (no trailing slash)
                subtasks.add(CompletableFuture.supplyAsync(
                        () -> classify(folderName), pool)
                        .thenCompose(type -> {
                            if (cancelled) return CompletableFuture.<Void>completedFuture(null);
                            if (type != null) {
                                return CompletableFuture.runAsync(
                                        () -> addDataset(bucket, key, folderName, type, out, listener), pool);
                            }
                            return walkAsync(bucket, key + "/", out, listener, pool); // ordinary folder — recurse
                        }));
            }

            return subtasks;
        }).thenCompose(subtasks -> CompletableFuture.allOf(subtasks.toArray(new CompletableFuture[0])));
    }

    private interface ThrowingSupplier<T> { T get() throws Exception; }

    private static <T> T listOrThrow(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    /** Classifies a file-level name and adds it as a dataset if it's an archive/N5 leaf. */
    private void classifyAndAdd(String bucket, String key, String fileName,
                                 List<DatasetEntry> out, Listener listener) {
        DatasetEntry.Type type = classify(fileName);
        if (type != null) addDataset(bucket, key, fileName, type, out, listener);
    }

    /** Classifies a folder-level name; null means "ordinary folder — recurse". */
    private DatasetEntry.Type classify(String name) {
        try {
            if (s3.isN5(name)) return DatasetEntry.Type.N5;
            if (s3.isArchive(name)) return DatasetEntry.Type.ARCHIVE;
            return null;
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private void addDataset(String bucket, String key, String name,
                             DatasetEntry.Type type, List<DatasetEntry> out,
                             Listener listener) {
        DatasetEntry e = new DatasetEntry(name, key, type);
        try {
            // Incremental: skip the (expensive) metadata round-trip for datasets
            // we've already indexed. The caller substitutes the cached entry by
            // path. Runs on the pool, same as the discovery calls, so new-dataset
            // metadata fetches also overlap instead of serializing.
            if (!knownPaths.contains(key)) {
                ObjectMeta meta = s3.meta(bucket, key);
                if (meta != null) {
                    e.setSizeBytes(meta.size);
                    e.setModifiedEpochMillis(meta.lastModified);
                }
            }
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
        out.add(e);
        listener.onDatasetFound(e);
    }

    private static Throwable unwrap(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        return t;
    }

}
