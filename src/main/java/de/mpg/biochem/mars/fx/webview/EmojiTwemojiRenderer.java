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

package de.mpg.biochem.mars.fx.webview;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vladsch.flexmark.ext.emoji.Emoji;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;

import de.mpg.biochem.mars.fx.editor.EmojiData;
import de.mpg.biochem.mars.fx.editor.EmojiEntry;

/**
 * Renders flexmark-ext-emoji's {@link Emoji} AST node (created when it recognizes
 * a {@code :shortcode:} against its own shortcode table) as a bundled Twemoji
 * {@code <img>}, using the exact same {@link EmojiData} table the picker and
 * autocomplete use -- so a shortcode only ever autocompletes/renders if it is in
 * both flexmark-ext-emoji's table AND has a bundled SVG. Registered directly on
 * the {@code HtmlRenderer.Builder} (see {@link FlexmarkPreviewRenderer}) rather
 * than relying on EmojiExtension's own renderer, since that renderer's file
 * naming (emoji-cheat-sheet/GitHub asset names, always ".png") doesn't line up
 * with Twemoji's codepoint-based ".svg" filenames.
 */
class EmojiTwemojiRenderer implements NodeRenderer {

	// getResource() per file, like WebViewPreview's prism component lookups; cache
	// misses too so repeated unknown shortcodes don't repeatedly hit the classloader.
	private static final Map<String, URL> URL_CACHE = new HashMap<>();

	EmojiTwemojiRenderer(DataHolder options) {}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		Set<NodeRenderingHandler<?>> set = new HashSet<>();
		set.add(new NodeRenderingHandler<>(Emoji.class, this::render));
		return set;
	}

	private void render(Emoji node, NodeRendererContext context, HtmlWriter html) {
		String shortcode = node.getText().toString();
		EmojiEntry entry = EmojiData.getInstance().lookup(shortcode);
		URL svgUrl = (entry != null) ? resolveSvgUrl(entry.getSvg()) : null;

		if (svgUrl == null) {
			// unknown to our bundled set (rare -- see EmojiData) -- fall back to the
			// literal shortcode text rather than a broken image
			html.text(":" + shortcode + ":");
			return;
		}

		// Twemoji SVGs carry no intrinsic width/height (viewBox only), so without an
		// explicit size the WebView renders them at their default/native size --
		// easily many times the text size. Size and baseline-align like inline text
		// (GitHub's convention), except when the emoji is the only content of its
		// paragraph ("just an emoji" reactions/notes), which render noticeably
		// bigger -- the same convention Slack/GitHub/Discord use.
		//
		// display:inline-block is required: markdownpad-github(-dark).css sets a
		// blanket "img { display: block; }" (to keep dropped-in images from
		// overflowing), which -- with no override -- forces even an inline emoji
		// onto its own line. The inline style="" attribute on the element wins over
		// that stylesheet rule by specificity.
		String style = isSoloInParagraph(node)
			? "display:inline-block;height:2.2em;width:2.2em;vertical-align:-0.4em;"
			: "display:inline-block;height:1.2em;width:1.2em;vertical-align:-0.2em;";

		html.attr("src", svgUrl.toExternalForm()).attr("alt", ":" + shortcode +
			":").attr("class", "mars-emoji").attr("style", style).withAttr()
			.tagVoid("img");
	}

	/** True if {@code node}'s paragraph contains only emoji and/or whitespace text. */
	private static boolean isSoloInParagraph(Emoji node) {
		Node parent = node.getParent();
		if (parent == null) return false;
		for (Node child = parent.getFirstChild(); child != null; child = child
			.getNext())
		{
			if (child instanceof Emoji) continue;
			if (!child.getChars().toString().trim().isEmpty()) return false;
		}
		return true;
	}

	private static URL resolveSvgUrl(String filename) {
		return URL_CACHE.computeIfAbsent(filename, f -> EmojiTwemojiRenderer.class
			.getResource("emoji-svg/" + f));
	}
}
