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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import de.mpg.biochem.mars.io.MoleculeArchiveIOFactory;
import de.mpg.biochem.mars.io.MoleculeArchiveSource;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsUtil;

/**
 * Merges Molecule Archives into a new archive, following the Merge Archives
 * and Merge Virtual Stores commands: all inputs must be the same archive type;
 * molecule records are copied, failing on duplicate molecule UIDs; metadata
 * records sharing a UID are combined when they hold different images (e.g.
 * positions of one acquisition) and fail when the same image appears twice;
 * global properties (tags, channels, parameters, columns, comments) are
 * combined; and the merge is logged in every metadata record.
 *
 * <p>Unlike those commands, inputs are opened through
 * {@link MoleculeArchiveSource}, so they may be .yama files or .yama.store
 * virtual stores, local or on S3, in any mix, and the output may be either. A
 * .yama.store output is written record by record, so virtual-store inputs are
 * never held in memory all at once; a .yama output is assembled in memory
 * (as it would be when opened anyway) and saved.
 */
final class ArchiveMerger {

    /** Progress callback; fraction is in [0, 1], or negative when unknown. */
    interface Progress {
        void update(String message, double fraction);
    }

    /** A problem that stops the merge, with a message meant for the user. */
    static final class MergeException extends Exception {
        MergeException(String message) { super(message); }
    }

    /** What was written. */
    static final class Result {
        final int archives;
        final int metadata;
        final int molecules;
        final int combinedMetadata; // metadata records combined from duplicates

        Result(int archives, int metadata, int molecules, int combinedMetadata) {
            this.archives = archives;
            this.metadata = metadata;
            this.molecules = molecules;
            this.combinedMetadata = combinedMetadata;
        }
    }

    private ArchiveMerger() {}

    /** Archive type (class name) of an archive, read from its properties. */
    static String archiveType(String location) throws IOException {
        return new MoleculeArchiveIOFactory().openSource(location).getArchiveType();
    }

    /** Display name of a location: its last path element. */
    static String nameOf(String location) {
        String l = location.replace('\\', '/');
        while (l.endsWith("/")) l = l.substring(0, l.length() - 1);
        return l.substring(l.lastIndexOf('/') + 1);
    }

    /**
     * Merge the archives at {@code inputs} into a new archive at {@code dest}
     * (a .yama.store when {@code store}, else a .yama file). Runs on the
     * calling thread — call it off the FX thread.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static Result merge(List<String> inputs, String dest, boolean store,
                        Progress progress) throws Exception
    {
        if (inputs.size() < 2) throw new MergeException("Select at least two archives to merge.");
        final String name = nameOf(dest);
        final MoleculeArchiveIOFactory factory = new MoleculeArchiveIOFactory();

        // --- Destination must be new and not one of (or inside) the inputs ---
        String destKey = normalize(dest);
        for (String in : inputs) {
            String inKey = normalize(in);
            if (destKey.equals(inKey) || destKey.startsWith(inKey + "/"))
                throw new MergeException("The destination can't be one of the archives "
                        + "being merged, or inside one.");
        }
        if (ArchiveDestinationPane.exists(dest))
            throw new MergeException(dest + " already exists. Choose another name or folder.");

        // --- Same archive type everywhere (checked before loading anything) ---
        progress.update("Checking archive types …", -1);
        List<MoleculeArchiveSource> sources = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for (String in : inputs) {
            MoleculeArchiveSource source = factory.openSource(in);
            String type;
            try {
                type = source.getArchiveType();
            }
            catch (IOException e) {
                throw new MergeException("Could not read " + nameOf(in) + ": " + e.getMessage());
            }
            sources.add(source);
            types.add(type);
        }
        checkSameType(inputs, types);

        Class<?> archiveClass = loadClass(types.get(0));
        Constructor<?> fromSource = archiveClass.getConstructor(MoleculeArchiveSource.class);
        Constructor<?> empty = archiveClass.getConstructor(String.class);

        // --- Open the archives (virtual stores lazily, .yama files in full) ---
        List<MoleculeArchive> archives = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            progress.update("Opening " + nameOf(inputs.get(i)) + " (" + (i + 1) + " of "
                    + inputs.size() + ") …", 0.2 * i / inputs.size());
            try {
                archives.add((MoleculeArchive) fromSource.newInstance(sources.get(i)));
            }
            catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw new MergeException("Could not open " + nameOf(inputs.get(i)) + ": "
                        + cause.getMessage());
            }
        }

        // --- Molecule UIDs must be unique across the inputs ---
        progress.update("Checking for duplicate records …", 0.2);
        Map<String, String> moleculeOwner = new HashMap<>();
        int moleculeCount = 0;
        for (int i = 0; i < archives.size(); i++) {
            String archiveName = nameOf(inputs.get(i));
            for (Object uid : archives.get(i).getMoleculeUIDs()) {
                String previous = moleculeOwner.put((String) uid, archiveName);
                if (previous != null) throw new MergeException("Molecule " + uid
                        + " is in both " + previous + " and " + archiveName
                        + ".\nAre you merging copies of the same archive? Please resolve "
                        + "the conflict before merging.");
                moleculeCount++;
            }
        }

        // --- Metadata: combine records sharing a UID if their images differ ---
        Map<String, MarsMetadata> metadata = new LinkedHashMap<>();
        Map<String, String> metadataOwner = new HashMap<>();
        int combined = 0;
        for (int i = 0; i < archives.size(); i++) {
            String archiveName = nameOf(inputs.get(i));
            for (Object uidObject : archives.get(i).getMetadataUIDs()) {
                String uid = (String) uidObject;
                MarsMetadata record = (MarsMetadata) archives.get(i).getMetadata(uid);
                MarsMetadata existing = metadata.get(uid);
                if (existing == null) {
                    metadata.put(uid, record);
                    metadataOwner.put(uid, archiveName);
                    continue;
                }
                Set<Integer> imageIDs = new HashSet<>();
                for (int im = 0; im < existing.getImageCount(); im++)
                    imageIDs.add(existing.getImage(im).getImageID());
                for (int im = 0; im < record.getImageCount(); im++) {
                    int imageID = record.getImage(im).getImageID();
                    if (imageIDs.contains(imageID)) throw new MergeException(
                            "Metadata record " + uid + " image " + imageID + " is in both "
                                    + metadataOwner.get(uid) + " and " + archiveName
                                    + ".\nAre you merging copies of the same dataset? "
                                    + "Please resolve the conflict before merging.");
                }
                existing.merge(record);
                combined++;
            }
        }

        // --- Log the merge in every metadata record, as the commands do ---
        List<String> names = new ArrayList<>();
        for (String in : inputs) names.add(nameOf(in));
        LogBuilder builder = new LogBuilder();
        String log = LogBuilder.buildTitleBlock("Merge Archives (Dataset Explorer)");
        builder.addParameter("Archive type", types.get(0));
        builder.addParameter("Destination", dest);
        log += builder.buildParameterList();
        log += "Merged " + inputs.size() + " archives into the output archive " + name + "\n";
        log += "Including: " + String.join(", ", names) + "\n";
        log += "In total " + metadata.size() + " metadata records were merged.\n";
        log += "In total " + moleculeCount + " molecules were merged.\n";
        log += LogBuilder.endBlock(true);
        for (MarsMetadata record : metadata.values()) record.logln(log);

        StringBuilder comments = new StringBuilder();
        for (int i = 0; i < archives.size(); i++) {
            String c = archives.get(i).properties().getComments();
            if (c == null || c.trim().isEmpty()) continue;
            comments.append("Comments from Merged Archive ").append(names.get(i))
                    .append(":\n").append(c).append("\n");
        }

        // --- Write ---
        final int total = moleculeCount;
        final AtomicInteger written = new AtomicInteger();
        if (!store) {
            // A .yama is one document, so build the archive in memory and save it.
            MoleculeArchive out = (MoleculeArchive) empty.newInstance(name);
            for (MarsMetadata record : metadata.values()) out.putMetadata(record);
            for (MoleculeArchive archive : archives) {
                for (Object uid : archive.getMoleculeUIDs()) {
                    out.put((Molecule) archive.get((String) uid));
                    reportMolecules(progress, written.incrementAndGet(), total);
                }
            }
            out.properties().setComments(comments.toString());
            progress.update("Saving " + name + " …", 0.97);
            if (ArchiveDestinationPane.isCloud(dest)) out.saveAs(dest);
            else {
                File f = new File(dest);
                if (f.getParentFile() != null) f.getParentFile().mkdirs();
                out.saveAs(f);
            }
        }
        else {
            // A .yama.store is written record by record, with its index and
            // properties built along the way — like Merge Virtual Stores.
            MoleculeArchiveSource out = factory.openSource(dest);
            out.initializeLocation();
            SmileFactory smile = new SmileFactory();

            MoleculeArchive template = (MoleculeArchive) empty.newInstance(name);
            MoleculeArchiveIndex index = template.createIndex();
            MoleculeArchiveProperties properties = template.createProperties();
            properties.setParent(template);

            progress.update("Writing metadata …", 0.25);
            for (MarsMetadata record : metadata.values()) {
                index.addMetadata(record);
                properties.addMetadataProperties(record);
                MarsUtil.writeJsonRecord(record, out.getMetadataOutputStream(record.getUID()), smile);
            }

            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            try {
                for (MoleculeArchive archive : archives) {
                    List<String> uids = new ArrayList<>(archive.getMoleculeUIDs());
                    pool.submit(() -> uids.parallelStream().forEach(uid -> {
                        Molecule molecule = (Molecule) archive.get(uid);
                        synchronized (properties) {
                            properties.addMoleculeProperties(molecule);
                        }
                        synchronized (index) {
                            index.addMolecule(molecule);
                        }
                        try {
                            MarsUtil.writeJsonRecord(molecule,
                                    out.getMoleculeOutputStream(uid), smile);
                        }
                        catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        reportMolecules(progress, written.incrementAndGet(), total);
                    })).get();
                }
            }
            catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof UncheckedIOException) throw ((UncheckedIOException) cause).getCause();
                throw e;
            }
            finally {
                pool.shutdown();
            }

            progress.update("Writing index and properties …", 0.97);
            properties.setNumberOfMolecules(total);
            properties.setNumberOfMetadatas(metadata.size());
            properties.setComments(comments.toString());
            MarsUtil.writeJsonRecord(index, out.getIndexesOutputStream(), smile);
            MarsUtil.writeJsonRecord(properties, out.getPropertiesOutputStream(), smile);
        }

        progress.update("Done.", 1);
        return new Result(inputs.size(), metadata.size(), total, combined);
    }

    /** Fail with the type of every input when they don't all match. */
    static void checkSameType(List<String> inputs, List<String> types) throws MergeException {
        boolean same = true;
        for (String t : types) if (!t.equals(types.get(0))) same = false;
        if (same) return;
        StringBuilder msg = new StringBuilder(
                "All archives must be the same type to merge. Found:\n");
        for (int i = 0; i < inputs.size(); i++)
            msg.append("\n").append(nameOf(inputs.get(i))).append(" — ")
                    .append(simpleName(types.get(i)));
        throw new MergeException(msg.toString());
    }

    static String simpleName(String className) {
        return className == null ? "unknown" : className.substring(className.lastIndexOf('.') + 1);
    }

    private static Class<?> loadClass(String className) throws MergeException {
        for (ClassLoader cl : new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                ArchiveMerger.class.getClassLoader() }) {
            if (cl == null) continue;
            try {
                return Class.forName(className, true, cl);
            }
            catch (ClassNotFoundException | LinkageError ignore) {}
        }
        throw new MergeException(simpleName(className) + " is not installed — enable the "
                + "update site that provides it.");
    }

    private static void reportMolecules(Progress progress, int done, int total) {
        // Report every ~1% so a large merge doesn't flood the FX thread.
        int step = Math.max(1, total / 100);
        if (done % step == 0 || done == total)
            progress.update("Copying molecules " + done + " of " + total + " …",
                    0.25 + 0.7 * done / Math.max(1, total));
    }

    private static String normalize(String location) {
        String l = ArchiveDestinationPane.isCloud(location) ? location
                : new File(location).getAbsolutePath();
        l = l.replace('\\', '/');
        while (l.endsWith("/")) l = l.substring(0, l.length() - 1);
        return l;
    }
}
