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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Persists the dataset index to a user-chosen local folder.
 *
 * <p>Two files are written per (endpoint, bucket) pair:
 * <ul>
 *   <li>{@code index-&lt;bucketKey&gt;.json} — the last discovered dataset list
 *       (server-derived fields). Lets the explorer open instantly on next
 *       launch without re-listing the whole bucket.</li>
 *   <li>{@code userdata-&lt;bucketKey&gt;.json} — the user-supplied tags, comments,
 *       and icon overrides, keyed by dataset path. Kept separate so a re-index
 *       (which rewrites the index file) never clobbers user annotations.</li>
 * </ul>
 *
 * <p>Uses Jackson streaming (already on the mars classpath via mars-core) so we
 * add no new dependency. If you'd rather use the object mapper, swap the manual
 * read/write for {@code mapper.writeValue(...)} — the on-disk shape is simple
 * enough that either works.
 */
public class DatasetIndexStore {

    private static final JsonFactory JSON = new JsonFactory();

    private final Path root;

    public DatasetIndexStore(String localFolder) {
        this.root = Paths.get(localFolder);
    }

    public void ensureFolder() throws IOException {
        Files.createDirectories(root);
    }

    // A filesystem-safe key for an endpoint+bucket combination.
    private String bucketKey(String endpoint, String bucket) {
        String raw = (endpoint == null ? "" : endpoint) + "__" + (bucket == null ? "" : bucket);
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path indexFile(String endpoint, String bucket) {
        return root.resolve("index-" + bucketKey(endpoint, bucket) + ".json");
    }

    private Path userDataFile(String endpoint, String bucket) {
        return root.resolve("userdata-" + bucketKey(endpoint, bucket) + ".json");
    }

    // ---------------------------------------------------------------------
    // Index (server-derived) read/write
    // ---------------------------------------------------------------------

    public void writeIndex(String endpoint, String bucket, List<DatasetEntry> entries) throws IOException {
        ensureFolder();
        Path file = indexFile(endpoint, bucket);
        try (JsonGenerator g = JSON.createGenerator(Files.newOutputStream(file), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {
            g.writeStartArray();
            for (DatasetEntry e : entries) {
                g.writeStartObject();
                g.writeStringField("name", e.getName());
                g.writeStringField("path", e.getPath());
                g.writeStringField("type", e.getType().name());
                g.writeNumberField("size", e.getSizeBytes());
                if (e.getCreatedEpochMillis() != null) g.writeNumberField("created", e.getCreatedEpochMillis());
                if (e.getModifiedEpochMillis() != null) g.writeNumberField("modified", e.getModifiedEpochMillis());
                g.writeEndObject();
            }
            g.writeEndArray();
        }
    }

    public List<DatasetEntry> readIndex(String endpoint, String bucket) throws IOException {
        Path file = indexFile(endpoint, bucket);
        List<DatasetEntry> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try (JsonParser p = JSON.createParser(Files.newInputStream(file))) {
            if (p.nextToken() != JsonToken.START_ARRAY) return out;
            while (p.nextToken() == JsonToken.START_OBJECT) {
                DatasetEntry e = new DatasetEntry();
                String type = "ARCHIVE";
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String field = p.getCurrentName();
                    p.nextToken();
                    switch (field) {
                        case "name": e.setName(p.getText()); break;
                        case "path": e.setPath(p.getText()); break;
                        case "type": type = p.getText(); break;
                        case "size": e.setSizeBytes(p.getLongValue()); break;
                        case "created": e.setCreatedEpochMillis(p.getLongValue()); break;
                        case "modified": e.setModifiedEpochMillis(p.getLongValue()); break;
                        default: p.skipChildren();
                    }
                }
                e.setType(DatasetEntry.Type.valueOf(type));
                out.add(e);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // User data (tags / comments / icon override) read/write
    // ---------------------------------------------------------------------

    public void writeUserData(String endpoint, String bucket, List<DatasetEntry> entries) throws IOException {
        ensureFolder();
        Path file = userDataFile(endpoint, bucket);
        try (JsonGenerator g = JSON.createGenerator(Files.newOutputStream(file), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {
            g.writeStartArray();
            for (DatasetEntry e : entries) {
                DatasetEntry.UserData u = e.userData();
                // Skip entries with no user annotations at all, to keep the file small.
                if (u.tags.isEmpty() && (u.commentsMarkdown == null || u.commentsMarkdown.isEmpty())
                        && u.iconSeedOverride == null) continue;
                g.writeStartObject();
                g.writeStringField("path", u.path);
                g.writeArrayFieldStart("tags");
                for (String t : u.tags) g.writeString(t);
                g.writeEndArray();
                g.writeStringField("comments", u.commentsMarkdown == null ? "" : u.commentsMarkdown);
                if (u.iconSeedOverride != null) g.writeStringField("iconSeed", u.iconSeedOverride);
                g.writeEndObject();
            }
            g.writeEndArray();
        }
    }

    public Map<String, DatasetEntry.UserData> readUserData(String endpoint, String bucket) throws IOException {
        Path file = userDataFile(endpoint, bucket);
        Map<String, DatasetEntry.UserData> out = new HashMap<>();
        if (!Files.exists(file)) return out;
        try (JsonParser p = JSON.createParser(Files.newInputStream(file))) {
            if (p.nextToken() != JsonToken.START_ARRAY) return out;
            while (p.nextToken() == JsonToken.START_OBJECT) {
                DatasetEntry.UserData u = new DatasetEntry.UserData();
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String field = p.getCurrentName();
                    p.nextToken();
                    switch (field) {
                        case "path": u.path = p.getText(); break;
                        case "comments": u.commentsMarkdown = p.getText(); break;
                        case "iconSeed": u.iconSeedOverride = p.getText(); break;
                        case "tags":
                            List<String> tags = new ArrayList<>();
                            while (p.nextToken() != JsonToken.END_ARRAY) tags.add(p.getText());
                            u.tags = tags;
                            break;
                        default: p.skipChildren();
                    }
                }
                if (u.path != null) out.put(u.path, u);
            }
        }
        return out;
    }

    /** Merge persisted user data into freshly-indexed entries, keyed by path. */
    public void mergeUserData(String endpoint, String bucket, List<DatasetEntry> freshEntries) throws IOException {
        Map<String, DatasetEntry.UserData> user = readUserData(endpoint, bucket);
        for (DatasetEntry e : freshEntries) {
            DatasetEntry.UserData u = user.get(e.getPath());
            if (u != null) e.applyUserData(u);
        }
    }
}
