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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * One indexed dataset in the explorer — either a Molecule Archive (.yama /
 * .yama.store) or an N5 container (.n5) discovered in an S3 bucket.
 *
 * <p>The "indexed" fields (name, path, type, timestamps) come from the S3
 * listing and are refreshed on every re-index. The "user" fields (tags,
 * comments, chosen icon-seed override) are supplied by the user and are
 * persisted separately so a re-index never destroys them — they are merged
 * back in by {@link DatasetIndexStore} keyed on {@link #getPath()}.
 */
public class DatasetEntry {

    public enum Type { ARCHIVE, N5 }

    // --- Indexed (server-derived) fields ---
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty path = new SimpleStringProperty();
    private Type type;
    private long sizeBytes = -1;
    private Long createdEpochMillis;   // may be null if S3 doesn't provide it
    private Long modifiedEpochMillis;  // from S3 object last-modified

    // --- User-supplied, persisted fields ---
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final StringProperty commentsMarkdown = new SimpleStringProperty(""); // serialized rich text
    private String iconSeedOverride; // null => use path as seed

    public DatasetEntry() {}

    public DatasetEntry(String name, String path, Type type) {
        this.name.set(name);
        this.path.set(path);
        this.type = type;
    }

    // The seed that drives the identicon. Defaults to the full path so that two
    // datasets with the same display name in different prefixes still differ.
    public String iconSeed() {
        return (iconSeedOverride != null && !iconSeedOverride.isEmpty())
            ? iconSeedOverride : getPath();
    }

    // --- name ---
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    // --- path ---
    public String getPath() { return path.get(); }
    public void setPath(String v) { path.set(v); }
    public StringProperty pathProperty() { return path; }

    // --- type ---
    public Type getType() { return type; }
    public void setType(Type t) { this.type = t; }
    public boolean isArchive() { return type == Type.ARCHIVE; }
    public boolean isN5() { return type == Type.N5; }

    // --- size ---
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long v) { this.sizeBytes = v; }

    // --- timestamps ---
    public Long getCreatedEpochMillis() { return createdEpochMillis; }
    public void setCreatedEpochMillis(Long v) { this.createdEpochMillis = v; }
    public Long getModifiedEpochMillis() { return modifiedEpochMillis; }
    public void setModifiedEpochMillis(Long v) { this.modifiedEpochMillis = v; }

    // --- tags ---
    public ObservableList<String> getTags() { return tags; }
    public void setTags(List<String> newTags) {
        Set<String> dedup = new LinkedHashSet<>(newTags == null ? List.of() : newTags);
        tags.setAll(dedup);
    }
    public void addTag(String tag) {
        if (tag == null) return;
        String t = tag.trim();
        if (!t.isEmpty() && !tags.contains(t)) tags.add(t);
    }
    public void removeTag(String tag) { tags.remove(tag); }

    // --- comments ---
    public String getCommentsMarkdown() { return commentsMarkdown.get(); }
    public void setCommentsMarkdown(String v) { commentsMarkdown.set(v == null ? "" : v); }
    public StringProperty commentsMarkdownProperty() { return commentsMarkdown; }

    // --- icon seed override ---
    public String getIconSeedOverride() { return iconSeedOverride; }
    public void setIconSeedOverride(String v) { this.iconSeedOverride = v; }

    /** Snapshot of the user-editable state, for merge-on-reindex. */
    public UserData userData() {
        UserData u = new UserData();
        u.path = getPath();
        u.tags = new ArrayList<>(tags);
        u.commentsMarkdown = getCommentsMarkdown();
        u.iconSeedOverride = iconSeedOverride;
        return u;
    }

    public void applyUserData(UserData u) {
        if (u == null) return;
        setTags(u.tags);
        setCommentsMarkdown(u.commentsMarkdown);
        setIconSeedOverride(u.iconSeedOverride);
    }

    /** Plain serializable holder for the persisted user-side fields. */
    public static class UserData {
        public String path;
        public List<String> tags = new ArrayList<>();
        public String commentsMarkdown = "";
        public String iconSeedOverride;
    }
}
