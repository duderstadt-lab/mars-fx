/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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
/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.mpg.biochem.mars.fx.options;

import java.util.List;
import java.util.prefs.Preferences;

import de.mpg.biochem.mars.fx.util.PrefsBooleanProperty;
import de.mpg.biochem.mars.fx.util.PrefsEnumProperty;
import de.mpg.biochem.mars.fx.util.PrefsIntegerProperty;
import de.mpg.biochem.mars.fx.util.PrefsStringProperty;
import de.mpg.biochem.mars.fx.util.PrefsStringsProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.text.Font;

/**
 * Options
 *
 * @author Karl Tauber
 */
public class Options {

	public static final String[] DEF_FONT_FAMILIES = { "Consolas",
		"DejaVu Sans Mono", "Lucida Sans Typewriter", "Lucida Console", };

	public static final int DEF_FONT_SIZE = 12;
	public static final int MIN_FONT_SIZE = 8;
	public static final int MAX_FONT_SIZE = 36;
	public static final String DEF_MARKDOWN_FILE_EXTENSIONS =
		"*.md,*.markdown,*.txt";

	public enum RendererType {
			CommonMark, FlexMark
	};

	public static final int DEF_WRAP_LINE_LENGTH = 80;
	public static final int MIN_WRAP_LINE_LENGTH = 10;

	private static Preferences globalOptions;
	private static Preferences options;

	public static void load(Preferences globalOptions) {
		Options.globalOptions = globalOptions;

		// options = getProjectOptions(ProjectManager.getActiveProject());

		options = globalOptions;

		fontFamily.init(options, "fontFamily", null, value -> safeFontFamily(
			value));
		fontSize.init(options, "fontSize", DEF_FONT_SIZE);
		lineSeparator.init(options, "lineSeparator", null);
		encoding.init(options, "encoding", null);
		markdownFileExtensions.init(options, "markdownFileExtensions",
			DEF_MARKDOWN_FILE_EXTENSIONS);
		markdownExtensions.init(options, "markdownExtensions");
		// markdownRenderer.init(options, "markdownRenderer",
		// RendererType.FlexMark);
		markdownRenderer.set(RendererType.FlexMark);
		showLineNo.init(options, "showLineNo", false);
		showWhitespace.init(options, "showWhitespace", false);
		showImagesEmbedded.init(options, "showImagesEmbedded", false);

		emphasisMarker.init(options, "emphasisMarker", "_");
		strongEmphasisMarker.init(options, "strongEmphasisMarker", "**");
		bulletListMarker.init(options, "bulletListMarker", "-");

		wrapLineLength.init(options, "wrapLineLength", DEF_WRAP_LINE_LENGTH);
		formatOnSave.init(options, "formatOnSave", false);
		formatOnlyModifiedParagraphs.init(options, "formatOnlyModifiedParagraphs",
			false);

		additionalCSS.init(options, "additionalCSS", null);

		// listen to active project
		// ProjectManager.activeProjectProperty().addListener((observer, oldProject,
		// newProject) -> {
		// set(getProjectOptions(newProject));
		// });
	}

	private static void set(Preferences options) {
		if (Options.options == options) return;

		Options.options = options;

		fontFamily.setPreferences(options);
		fontSize.setPreferences(options);
		lineSeparator.setPreferences(options);
		encoding.setPreferences(options);
		markdownFileExtensions.setPreferences(options);
		//markdownExtensions.setPreferences(options);

		//Make sure full set of markdown extensions are active.
		markdownExtensions.set(new String[]{"abbreviation", "anchorlink", "aside", "autolink",
				"definition", "gfm-strikethrough", "gfm-tables", "gfm-tasklist", "gitlab",
				"toc", "yaml-front-matter"});

		markdownRenderer.setPreferences(options);
		showLineNo.setPreferences(options);
		showWhitespace.setPreferences(options);
		showImagesEmbedded.setPreferences(options);

		emphasisMarker.setPreferences(options);
		strongEmphasisMarker.setPreferences(options);
		bulletListMarker.setPreferences(options);

		wrapLineLength.setPreferences(options);
		formatOnSave.setPreferences(options);
		formatOnlyModifiedParagraphs.setPreferences(options);

		additionalCSS.setPreferences(options);
	}

	/**
	 * Check whether font family is null or invalid (family not available on
	 * system) and search for an available family.
	 */
	private static String safeFontFamily(String fontFamily) {
		List<String> fontFamilies = Font.getFamilies();
		if (fontFamily != null && fontFamilies.contains(fontFamily))
			return fontFamily;

		for (String family : DEF_FONT_FAMILIES) {
			if (fontFamilies.contains(family)) return family;
		}
		return "Monospaced";
	}

	// 'fontFamily' property
	private static final PrefsStringProperty fontFamily =
		new PrefsStringProperty();

	public static String getFontFamily() {
		return fontFamily.get();
	}

	public static void setFontFamily(String fontFamily) {
		Options.fontFamily.set(fontFamily);
	}

	public static StringProperty fontFamilyProperty() {
		return fontFamily;
	}

	// 'fontSize' property
	private static final PrefsIntegerProperty fontSize =
		new PrefsIntegerProperty();

	public static int getFontSize() {
		return fontSize.get();
	}

	public static void setFontSize(int fontSize) {
		Options.fontSize.set(Math.min(Math.max(fontSize, MIN_FONT_SIZE),
			MAX_FONT_SIZE));
	}

	public static IntegerProperty fontSizeProperty() {
		return fontSize;
	}

	// 'lineSeparator' property
	private static final PrefsStringProperty lineSeparator =
		new PrefsStringProperty();

	public static String getLineSeparator() {
		return lineSeparator.get();
	}

	public static void setLineSeparator(String lineSeparator) {
		Options.lineSeparator.set(lineSeparator);
	}

	public static StringProperty lineSeparatorProperty() {
		return lineSeparator;
	}

	// 'encoding' property
	private static final PrefsStringProperty encoding = new PrefsStringProperty();

	public static String getEncoding() {
		return encoding.get();
	}

	public static void setEncoding(String encoding) {
		Options.encoding.set(encoding);
	}

	public static StringProperty encodingProperty() {
		return encoding;
	}

	// 'markdownFileExtensions' property
	private static final PrefsStringProperty markdownFileExtensions =
		new PrefsStringProperty();

	public static String getMarkdownFileExtensions() {
		return markdownFileExtensions.get();
	}

	public static void setMarkdownFileExtensions(String markdownFileExtensions) {
		Options.markdownFileExtensions.set(markdownFileExtensions);
	}

	public static StringProperty markdownFileExtensionsProperty() {
		return markdownFileExtensions;
	}

	// 'markdownExtensions' property
	private static final PrefsStringsProperty markdownExtensions =
		new PrefsStringsProperty();

	public static String[] getMarkdownExtensions() {
		return markdownExtensions.get();
	}

	public static void setMarkdownExtensions(String[] markdownExtensions) {
		Options.markdownExtensions.set(markdownExtensions);
	}

	public static ObjectProperty<String[]> markdownExtensionsProperty() {
		return markdownExtensions;
	}

	// 'markdownRenderer' property
	private static final PrefsEnumProperty<RendererType> markdownRenderer =
		new PrefsEnumProperty<>();

	public static RendererType getMarkdownRenderer() {
		return markdownRenderer.get();
	}

	public static void setMarkdownRenderer(RendererType markdownRenderer) {
		Options.markdownRenderer.set(markdownRenderer);
	}

	public static ObjectProperty<RendererType> markdownRendererProperty() {
		return markdownRenderer;
	}

	// 'showLineNo' property
	private static final PrefsBooleanProperty showLineNo =
		new PrefsBooleanProperty();

	public static boolean isShowLineNo() {
		return showLineNo.get();
	}

	public static void setShowLineNo(boolean showLineNo) {
		Options.showLineNo.set(showLineNo);
	}

	public static BooleanProperty showLineNoProperty() {
		return showLineNo;
	}

	// 'showWhitespace' property
	private static final PrefsBooleanProperty showWhitespace =
		new PrefsBooleanProperty();

	public static boolean isShowWhitespace() {
		return showWhitespace.get();
	}

	public static void setShowWhitespace(boolean showWhitespace) {
		Options.showWhitespace.set(showWhitespace);
	}

	public static BooleanProperty showWhitespaceProperty() {
		return showWhitespace;
	}

	// 'showImagesEmbedded' property
	private static final PrefsBooleanProperty showImagesEmbedded =
		new PrefsBooleanProperty();

	public static boolean isShowImagesEmbedded() {
		return showImagesEmbedded.get();
	}

	public static void setShowImagesEmbedded(boolean showImagesEmbedded) {
		Options.showImagesEmbedded.set(showImagesEmbedded);
	}

	public static BooleanProperty showImagesEmbeddedProperty() {
		return showImagesEmbedded;
	}

	// 'emphasisMarker' property
	private static final PrefsStringProperty emphasisMarker =
		new PrefsStringProperty();

	public static String getEmphasisMarker() {
		return emphasisMarker.get();
	}

	public static void setEmphasisMarker(String emphasisMarker) {
		Options.emphasisMarker.set(emphasisMarker);
	}

	public static StringProperty emphasisMarkerProperty() {
		return emphasisMarker;
	}

	// 'strongEmphasisMarker' property
	private static final PrefsStringProperty strongEmphasisMarker =
		new PrefsStringProperty();

	public static String getStrongEmphasisMarker() {
		return strongEmphasisMarker.get();
	}

	public static void setStrongEmphasisMarker(String strongEmphasisMarker) {
		Options.strongEmphasisMarker.set(strongEmphasisMarker);
	}

	public static StringProperty strongEmphasisMarkerProperty() {
		return strongEmphasisMarker;
	}

	// 'bulletListMarker' property
	private static final PrefsStringProperty bulletListMarker =
		new PrefsStringProperty();

	public static String getBulletListMarker() {
		return bulletListMarker.get();
	}

	public static void setBulletListMarker(String bulletListMarker) {
		Options.bulletListMarker.set(bulletListMarker);
	}

	public static StringProperty bulletListMarkerProperty() {
		return bulletListMarker;
	}

	// 'wrapLineLength' property
	private static final PrefsIntegerProperty wrapLineLength =
		new PrefsIntegerProperty();

	public static int getWrapLineLength() {
		return wrapLineLength.get();
	}

	public static void setWrapLineLength(int wrapLineLength) {
		Options.wrapLineLength.set(Math.max(wrapLineLength, MIN_WRAP_LINE_LENGTH));
	}

	public static IntegerProperty wrapLineLengthProperty() {
		return wrapLineLength;
	}

	// 'formatOnSave' property
	private static final PrefsBooleanProperty formatOnSave =
		new PrefsBooleanProperty();

	public static boolean isFormatOnSave() {
		return formatOnSave.get();
	}

	public static void setFormatOnSave(boolean formatOnSave) {
		Options.formatOnSave.set(formatOnSave);
	}

	public static BooleanProperty formatOnSaveProperty() {
		return formatOnSave;
	}

	// 'formatOnlyModifiedParagraphs' property
	private static final PrefsBooleanProperty formatOnlyModifiedParagraphs =
		new PrefsBooleanProperty();

	public static boolean isFormatOnlyModifiedParagraphs() {
		return formatOnlyModifiedParagraphs.get();
	}

	public static void setFormatOnlyModifiedParagraphs(
		boolean formatOnlyModifiedParagraphs)
	{
		Options.formatOnlyModifiedParagraphs.set(formatOnlyModifiedParagraphs);
	}

	public static BooleanProperty formatOnlyModifiedParagraphsProperty() {
		return formatOnlyModifiedParagraphs;
	}

	// 'additionalCSS' property
	private static final PrefsStringProperty additionalCSS =
		new PrefsStringProperty();

	public static String getAdditionalCSS() {
		return additionalCSS.get();
	}

	public static void setAdditionalCSS(String additionalCSS) {
		Options.additionalCSS.set(additionalCSS);
	}

	public static StringProperty additionalCSSProperty() {
		return additionalCSS;
	}
}
