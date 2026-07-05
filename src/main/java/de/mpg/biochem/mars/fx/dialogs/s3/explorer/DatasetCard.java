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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A single rectangular dataset card:
 *
 * <pre>
 *  +-----------------------------------------------------+
 *  | [tag] [tag] [tag]                                   |  <- tag row (top)
 *  |          +----------+                               |
 *  |  ICON    | Dataset Name           (centered)        |
 *  |  (fills  |                                          |
 *  |   left)  | s3://bucket/path/to/dataset  (small)     |  <- path (bottom)
 *  |          | created … · modified …       (small)     |
 *  +-----------------------------------------------------+
 * </pre>
 *
 * Styling is done with inline styles + style classes so you can move the look
 * into your mars-fx CSS/theme later. The card reads its colors from CSS classes
 * ({@code dataset-card}, {@code dataset-card-selected}) so light/dark theming is
 * handled by your existing {@code MarsThemeManager} stylesheet, while the
 * identicon image itself is regenerated per-theme by the caller.
 */
public class DatasetCard extends BorderPane {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final DatasetEntry entry;
    private final ImageView iconView = new ImageView();
    private final FlowPane tagRow = new FlowPane();
    private boolean selected = false;

    public DatasetCard(DatasetEntry entry, Image icon, double iconSize) {
        this(entry, icon, iconSize, false);
    }

    public DatasetCard(DatasetEntry entry, Image icon, double iconSize, boolean darkMode) {
        this.entry = entry;
        getStyleClass().add("dataset-card");
        setMinHeight(iconSize + 24);
        setPrefHeight(iconSize + 24);
        setPadding(new Insets(8));

        // Base look — kept inline so the component is self-contained; override in CSS.
        applyBaseStyle();

        // --- Icon (left, fills the left side) ---
        iconView.setImage(icon);
        iconView.setFitWidth(iconSize);
        iconView.setFitHeight(iconSize);
        iconView.setPreserveRatio(true);
        StackPane iconHolder = new StackPane(iconView);
        iconHolder.setMinWidth(iconSize);
        iconHolder.setPrefWidth(iconSize);
        iconHolder.setMaxWidth(iconSize);
        iconHolder.setMaxHeight(iconSize);
        // Theme-appropriate CONSTANT backing: a light square in light theme, a dark
        // square in dark theme — matching what each identicon variant is designed to
        // sit on. Crucially this is a fixed color, NOT the card's selection color, so
        // the icon looks identical selected or not and stands out against the
        // selection highlight. The identicon's own light/dark shape variant is chosen
        // by the caller (theme), so the two always pair correctly.
        String iconBg = darkMode ? "#3a3d42" : "white";
        iconHolder.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 6;");
        BorderPane.setMargin(iconHolder, new Insets(0, 10, 0, 0));

        // --- Center: name + path + dates ---
        Label nameLabel = new Label(entry.getName());
        nameLabel.getStyleClass().add("dataset-card-name");
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        Label typeBadge = new Label(entry.isArchive() ? "YAMA" : "N5");
        typeBadge.getStyleClass().add("dataset-card-type");
        typeBadge.setStyle("-fx-font-size: 10px; -fx-padding: 1 6 1 6; "
                + "-fx-background-radius: 8; -fx-background-color: -fx-accent; -fx-text-fill: white;");

        HBox nameLine = new HBox(8, nameLabel, typeBadge);
        nameLine.setAlignment(Pos.CENTER_LEFT);

        Label pathLabel = new Label(displayPath());
        pathLabel.getStyleClass().add("dataset-card-path");
        pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-inner-color; -fx-opacity: 0.7;");
        pathLabel.setWrapText(true);

        Label dateLabel = new Label(dateLine());
        dateLabel.getStyleClass().add("dataset-card-dates");
        dateLabel.setStyle("-fx-font-size: 10px; -fx-opacity: 0.6;");

        VBox center = new VBox(4);
        center.setAlignment(Pos.CENTER_LEFT);
        Region spacerTop = new Region();
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerTop, Priority.ALWAYS);
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);
        // name vertically centered, path + dates pinned toward the bottom
        center.getChildren().addAll(spacerTop, nameLine, spacerBottom, pathLabel, dateLabel);
        center.setPadding(new Insets(0, 4, 0, 0));

        // --- Tag row: overlaid at the BOTTOM-RIGHT, floating above the content
        //     via a StackPane, clear of the icon on the left. ---
        tagRow.setHgap(4);
        tagRow.setVgap(4);
        tagRow.setAlignment(Pos.BOTTOM_RIGHT);
        tagRow.setMaxHeight(Region.USE_PREF_SIZE);
        tagRow.setMouseTransparent(true); // clicks pass through to the card
        StackPane.setAlignment(tagRow, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(tagRow, new Insets(0, 4, 4, 0));
        rebuildTagChips();

        BorderPane inner = new BorderPane();
        inner.setLeft(iconHolder);
        BorderPane.setAlignment(iconHolder, Pos.CENTER);
        inner.setCenter(center);

        StackPane overlay = new StackPane(inner, tagRow);
        setCenter(overlay);
    }

    private void applyBaseStyle() {
        // 1px border so cards are clearly separable; -fx-* look-ups resolve against
        // whatever theme (light/dark) is active via your global stylesheet.
        setStyle(baseStyle(false));
    }

    private String baseStyle(boolean sel) {
        // Selection changes ONLY the border COLOR, not its width — the width stays
        // constant at 2px so content never reflows/shifts when a card is selected.
        // Unselected uses the theme's standard control-border gray (-fx-box-border),
        // which is a muted gray in both light and dark themes rather than a bright
        // white line; selected uses the accent color.
        String border = sel ? "-fx-accent" : "-fx-box-border";
        return "-fx-background-color: -fx-control-inner-background;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 8; -fx-background-radius: 8;";
    }

    public void setSelected(boolean sel) {
        this.selected = sel;
        setStyle(baseStyle(sel));
        if (sel) {
            if (!getStyleClass().contains("dataset-card-selected"))
                getStyleClass().add("dataset-card-selected");
        } else {
            getStyleClass().remove("dataset-card-selected");
        }
    }

    public boolean isSelectedCard() { return selected; }

    public DatasetEntry getEntry() { return entry; }

    public void setIcon(Image icon) { iconView.setImage(icon); }

    /** Rebuild the tag chips (top-right overlay) — call after tags change. */
    public void rebuildTagChips() {
        tagRow.getChildren().clear();
        for (String tag : entry.getTags()) {
            Label chip = new Label(tag);
            chip.getStyleClass().add("dataset-tag-chip");
            // Solid accent with white text so chips stay legible floating over the
            // card content, and look consistent in light and dark themes.
            chip.setStyle("-fx-font-size: 13px; -fx-padding: 2 9 2 9;"
                    + "-fx-background-radius: 8; -fx-border-radius: 8;"
                    + "-fx-background-color: -fx-accent; -fx-text-fill: white;");
            tagRow.getChildren().add(chip);
        }
    }

    private String displayPath() {
        return entry.getPath();
    }

    private String dateLine() {
        StringBuilder sb = new StringBuilder();
        if (entry.getCreatedEpochMillis() != null) {
            sb.append("created ").append(DATE_FMT.format(Instant.ofEpochMilli(entry.getCreatedEpochMillis())));
        }
        if (entry.getModifiedEpochMillis() != null) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append("modified ").append(DATE_FMT.format(Instant.ofEpochMilli(entry.getModifiedEpochMillis())));
        }
        return sb.toString();
    }
}
