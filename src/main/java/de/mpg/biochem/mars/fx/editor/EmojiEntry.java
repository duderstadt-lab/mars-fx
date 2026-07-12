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

package de.mpg.biochem.mars.fx.editor;

import java.util.List;

/**
 * A single emoji known to both the flexmark-ext-emoji shortcode table and the
 * bundled Twemoji SVG set. shortcode/unicode/svg drive insertion, lookup, and
 * preview rendering; name/keywords/aliases drive picker search.
 */
public class EmojiEntry {

	private final String shortcode;
	private final String unicode;
	private final String name;
	private final String category;
	private final List<String> keywords;
	private final List<String> aliases;
	private final String svg;

	EmojiEntry(String shortcode, String unicode, String name, String category,
		List<String> keywords, List<String> aliases, String svg)
	{
		this.shortcode = shortcode;
		this.unicode = unicode;
		this.name = name;
		this.category = category;
		this.keywords = keywords;
		this.aliases = aliases;
		this.svg = svg;
	}

	public String getShortcode() {
		return shortcode;
	}

	public String getUnicode() {
		return unicode;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public List<String> getAliases() {
		return aliases;
	}

	public String getSvg() {
		return svg;
	}

	/** The text inserted into the editor, e.g. {@code :microscope:}. */
	public String getInsertText() {
		return ":" + shortcode + ":";
	}

	boolean matches(String query) {
		if (query.isEmpty()) return true;
		if (shortcode.contains(query)) return true;
		if (name.toLowerCase().contains(query)) return true;
		for (String keyword : keywords)
			if (keyword.contains(query)) return true;
		for (String alias : aliases)
			if (alias.toLowerCase().contains(query)) return true;
		return false;
	}

	boolean startsWith(String query) {
		if (query.isEmpty()) return true;
		if (shortcode.startsWith(query)) return true;
		for (String alias : aliases)
			if (alias.toLowerCase().startsWith(query)) return true;
		for (String keyword : keywords)
			if (keyword.startsWith(query)) return true;
		return false;
	}
}
