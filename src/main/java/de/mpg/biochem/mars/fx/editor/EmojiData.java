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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads emoji-metadata.json (generated from flexmark-ext-emoji's own shortcode
 * table, filtered to the shortcodes that resolve to a bundled Twemoji SVG) and
 * serves lookup/search for the emoji picker and the {@code :}-autocomplete.
 * Loaded once and cached; both {@link EmojiPicker} and the autocomplete popup
 * share this single instance so there is exactly one source of truth for which
 * shortcodes are offered and how they resolve.
 */
public final class EmojiData {

	private static final String METADATA_RESOURCE = "emoji/emoji-metadata.json";

	private static EmojiData instance;

	private final List<EmojiEntry> entries;
	private final Map<String, EmojiEntry> byShortcode;
	private final List<String> categoriesInOrder;

	public static synchronized EmojiData getInstance() {
		if (instance == null) instance = new EmojiData();
		return instance;
	}

	private EmojiData() {
		List<EmojiEntry> loaded = new ArrayList<>();
		Map<String, EmojiEntry> byCode = new LinkedHashMap<>();
		TreeSet<String> categories = new TreeSet<>();

		try (InputStream is = EmojiData.class.getResourceAsStream(
			METADATA_RESOURCE))
		{
			if (is == null) throw new IOException("Resource not found: " +
				METADATA_RESOURCE);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(is);
			for (JsonNode node : root) {
				String shortcode = node.path("shortcode").asText(null);
				String svg = node.path("svg").asText(null);
				if (shortcode == null || svg == null) continue;

				List<String> keywords = toStringList(node.path("keywords"));
				List<String> aliases = toStringList(node.path("aliases"));

				EmojiEntry entry = new EmojiEntry(shortcode, node.path("unicode")
					.asText(""), node.path("name").asText(shortcode), node.path(
						"category").asText("Other"), keywords, aliases, svg);

				loaded.add(entry);
				byCode.put(shortcode, entry);
				for (String alias : aliases)
					byCode.putIfAbsent(alias, entry);
				categories.add(entry.getCategory());
			}
		}
		catch (IOException e) {
			throw new IllegalStateException(
				"Failed to load emoji metadata from " + METADATA_RESOURCE, e);
		}

		this.entries = Collections.unmodifiableList(loaded);
		this.byShortcode = Collections.unmodifiableMap(byCode);
		this.categoriesInOrder = Collections.unmodifiableList(new ArrayList<>(
			categories));
	}

	private static List<String> toStringList(JsonNode arrayNode) {
		List<String> list = new ArrayList<>();
		if (arrayNode.isArray()) for (JsonNode n : arrayNode)
			list.add(n.asText());
		return list;
	}

	/** All known emoji, in the order loaded (grouped by category, then shortcode). */
	public List<EmojiEntry> all() {
		return entries;
	}

	/** Category names, sorted alphabetically. */
	public List<String> categories() {
		return categoriesInOrder;
	}

	public List<EmojiEntry> byCategory(String category) {
		List<EmojiEntry> result = new ArrayList<>();
		for (EmojiEntry entry : entries)
			if (entry.getCategory().equals(category)) result.add(entry);
		return result;
	}

	/** Look up by shortcode or alias, without the leading/trailing colons. */
	public EmojiEntry lookup(String shortcodeOrAlias) {
		return byShortcode.get(shortcodeOrAlias);
	}

	/** Case-insensitive substring search across shortcode, name, keywords, and aliases. */
	public List<EmojiEntry> search(String query) {
		String q = query.trim().toLowerCase();
		List<EmojiEntry> result = new ArrayList<>();
		for (EmojiEntry entry : entries)
			if (entry.matches(q)) result.add(entry);
		return result;
	}

	/**
	 * Search for {@code :}-autocomplete: prefix matches only, so a query of "mic"
	 * offers "microscope" but not "comic". Ranked with shortcode-prefix matches
	 * first, then keyword/alias-prefix matches.
	 */
	public List<EmojiEntry> searchPrefix(String query, int limit) {
		String q = query.trim().toLowerCase();
		List<EmojiEntry> primary = new ArrayList<>();
		List<EmojiEntry> secondary = new ArrayList<>();
		for (EmojiEntry entry : entries) {
			if (entry.getShortcode().startsWith(q)) primary.add(entry);
			else if (entry.startsWith(q)) secondary.add(entry);
			if (primary.size() >= limit) break;
		}
		List<EmojiEntry> result = new ArrayList<>(primary);
		for (EmojiEntry entry : secondary) {
			if (result.size() >= limit) break;
			if (!result.contains(entry)) result.add(entry);
		}
		return result;
	}
}
