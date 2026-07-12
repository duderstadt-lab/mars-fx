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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Walks a local directory tree finding Molecule Archives and N5 datasets, the
 * local-filesystem counterpart of {@link DatasetIndexer}. Same classification
 * rules: a {@code .n5} directory or a {@code .yama} / {@code .yama.store} /
 * {@code .yama.json} entry is a dataset (and is NOT descended into); ordinary
 * folders are recursed. Runs its walk on a background thread via
 * {@link #indexAsync}.
 */
public class DatasetLocalIndexer {

    public interface Listener {
        default void onProgress(int discovered, String currentPath) {}
        void onFinished(List<DatasetEntry> all);
        void onError(Exception ex);
    }

    private volatile boolean cancelled = false;

    public void cancel() { this.cancelled = true; }

    public void indexAsync(File root, Listener listener) {
        Thread t = new Thread(() -> {
            try {
                List<DatasetEntry> out = new ArrayList<>();
                if (root != null && root.isDirectory())
                    walk(root, root, out, listener);
                if (!cancelled) listener.onFinished(out);
            } catch (Exception e) {
                listener.onError(e);
            }
        }, "DatasetLocalIndexer");
        t.setDaemon(true);
        t.start();
    }

    // Recursive descent. Paths recorded on entries are RELATIVE to the indexed
    // root (so cards show a tidy relative path); the entry name is the folder's
    // own name. Datasets are not descended into.
    private void walk(File root, File dir, List<DatasetEntry> out, Listener listener) {
        if (cancelled) return;
        listener.onProgress(out.size(), dir.getAbsolutePath());

        File[] children = dir.listFiles();
        if (children == null) return;

        for (File f : children) {
            if (cancelled) return;
            String name = f.getName();

            if (isN5(name) && f.isDirectory()) {
                add(root, f, DatasetEntry.Type.N5, out, listener);
            } else if (isArchive(name)) {
                // .yama / .yama.json (file) or .yama.store (directory)
                add(root, f, DatasetEntry.Type.ARCHIVE, out, listener);
            } else if (f.isDirectory()) {
                walk(root, f, out, listener); // ordinary folder — recurse
            }
        }
    }

    private void add(File root, File f, DatasetEntry.Type type,
                     List<DatasetEntry> out, Listener listener) {
        String rel = relativePath(root, f);
        DatasetEntry e = new DatasetEntry(f.getName(), rel, type);
        e.setSizeBytes(sizeOf(f));
        e.setModifiedEpochMillis(f.lastModified());
        out.add(e);
    }

    private static boolean isN5(String name) {
        return name.toLowerCase().endsWith(".n5");
    }

    private static boolean isArchive(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".yama") || n.endsWith(".yama.json") || n.endsWith(".yama.store");
    }

    private static String relativePath(File root, File f) {
        String rootPath = root.getAbsolutePath();
        String fPath = f.getAbsolutePath();
        if (fPath.startsWith(rootPath)) {
            String rel = fPath.substring(rootPath.length());
            while (rel.startsWith(File.separator)) rel = rel.substring(1);
            return rel.isEmpty() ? f.getName() : rel;
        }
        return fPath;
    }

    // Size: file length, or recursive sum for a directory-style dataset (.n5 /
    // .yama.store). Bounded best-effort — returns what it can.
    private static long sizeOf(File f) {
        if (f.isFile()) return f.length();
        long total = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) total += sizeOf(k);
        return total;
    }
}
