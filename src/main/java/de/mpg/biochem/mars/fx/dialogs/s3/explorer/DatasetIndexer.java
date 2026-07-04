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
import java.util.List;
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
    }

    /** Progress/callback hooks so the UI can show status and stream results in. */
    public interface Listener {
        default void onStarted() {}
        default void onDatasetFound(DatasetEntry entry) {}
        default void onProgress(int discovered, String currentPrefix) {}
        default void onFinished(List<DatasetEntry> all) {}
        default void onError(Exception e) {}
    }

    private final S3Access s3;
    private volatile boolean cancelled = false;

    public DatasetIndexer(S3Access s3) {
        this.s3 = s3;
    }

    public void cancel() { this.cancelled = true; }

    /**
     * Runs the walk on the CURRENT thread. Callers should invoke this from a
     * background thread (see {@link #indexAsync}). Results are also delivered
     * incrementally via the listener.
     */
    public List<DatasetEntry> index(String bucket, Listener listener) {
        List<DatasetEntry> results = new ArrayList<>();
        try {
            listener.onStarted();
            walk(bucket, "", results, listener);
            if (!cancelled) listener.onFinished(results);
        } catch (Exception e) {
            listener.onError(e);
        }
        return results;
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
    private void walk(String bucket, String prefix, List<DatasetEntry> out, Listener listener) throws Exception {
        if (cancelled) return;
        listener.onProgress(out.size(), prefix);

        // --- files at this level ---
        for (String fileName : s3.listFiles(bucket, prefix)) {
            if (cancelled) return;
            String key = prefix + fileName;
            if (s3.isN5(fileName)) {
                addDataset(bucket, key, fileName, DatasetEntry.Type.N5, out, listener);
            } else if (s3.isArchive(fileName)) {
                addDataset(bucket, key, fileName, DatasetEntry.Type.ARCHIVE, out, listener);
            }
        }

        // --- folders at this level ---
        for (String folderName : s3.listFolders(bucket, prefix)) {
            if (cancelled) return;
            String key = prefix + folderName; // dataset key (no trailing slash)
            if (s3.isN5(folderName)) {
                addDataset(bucket, key, folderName, DatasetEntry.Type.N5, out, listener);
            } else if (s3.isArchive(folderName)) {
                addDataset(bucket, key, folderName, DatasetEntry.Type.ARCHIVE, out, listener);
            } else {
                walk(bucket, key + "/", out, listener); // ordinary folder — recurse
            }
        }
    }

    private void addDataset(String bucket, String key, String name,
                             DatasetEntry.Type type, List<DatasetEntry> out,
                             Listener listener) throws Exception {
        DatasetEntry e = new DatasetEntry(name, key, type);
        ObjectMeta meta = s3.meta(bucket, key);
        if (meta != null) {
            e.setSizeBytes(meta.size);
            e.setModifiedEpochMillis(meta.lastModified);
        }
        out.add(e);
        listener.onDatasetFound(e);
    }

}
