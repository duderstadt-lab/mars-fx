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

package de.mpg.biochem.mars.fx.editor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;

import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.TwoDimensional.Bias;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlBlockBase;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.HtmlInlineBase;
import com.vladsch.flexmark.ast.HtmlInlineComment;
import com.vladsch.flexmark.ast.HtmlInnerBlock;
import com.vladsch.flexmark.ast.HtmlInnerBlockComment;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.MailLink;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import de.mpg.biochem.mars.fx.addons.MarkdownSyntaxHighlighterAddon;
import de.mpg.biochem.mars.fx.syntaxhighlighter.SyntaxHighlighter;
import de.mpg.biochem.mars.fx.util.Range;
import javafx.application.Platform;

/**
 * Markdown syntax highlighter. Uses flexmark-java AST.
 *
 * @author Karl Tauber
 */
class MarkdownSyntaxHighlighter {

	/*private*/ enum StyleClass {

			// headers
			h1, h2, h3, h4, h5, h6,

			// inlines
			strong, em, del, a, img, code, br,

			// blocks
			pre, blockquote, aside,

			// lists
			ul, ol, li, liopen, liopentask, dl, dt, dd,

			// tables
			table, thead, tbody, tr, th, td,

			// misc
			html, reference, abbrdef, abbr,

			_c1, _c2, _c3, _c4, _c5, _c6, _c7, _c8, _c9, _c10, _c11, _c12, _c13, _c14,
			_c15, _c16, _c17, _c18, _c19, _c20;

		private static final HashMap<String, StyleClass> customMap =
			new HashMap<>();
		private static int nextCustom = 1;
		private String cssClass;
		private String cssClass2;

		static StyleClass custom(String cssClass) {
			return custom(cssClass, null);
		}

		static StyleClass custom(String cssClass, String cssClass2) {
			StyleClass styleClass = customMap.get(cssClass);
			if (styleClass != null) return styleClass;

			styleClass = StyleClass.valueOf("_c" + nextCustom++);
			styleClass.cssClass = cssClass;
			styleClass.cssClass2 = cssClass2;
			customMap.put(cssClass, styleClass);
			return styleClass;
		}

		String cssClass() {
			return (cssClass != null) ? cssClass : name();
		}
	};

	private static final HashMap<Long, Collection<String>> styleClassesCache =
		new HashMap<>();
	private static final HashMap<Class<? extends Node>, StyleClass> node2style =
		new HashMap<>();
	private static final HashMap<Class<? extends Node>, StyleClass> node2lineStyle =
		new HashMap<>();

	static {
		// inlines
		node2style.put(StrongEmphasis.class, StyleClass.strong);
		node2style.put(Emphasis.class, StyleClass.em);
		node2style.put(Strikethrough.class, StyleClass.del);
		node2style.put(Link.class, StyleClass.a);
		node2style.put(LinkRef.class, StyleClass.a);
		node2style.put(AutoLink.class, StyleClass.a);
		node2style.put(MailLink.class, StyleClass.a);
		node2style.put(Image.class, StyleClass.img);
		node2style.put(ImageRef.class, StyleClass.img);
		node2style.put(Code.class, StyleClass.code);
		node2style.put(HardLineBreak.class, StyleClass.br);

		// blocks
		node2lineStyle.put(FencedCodeBlock.class, StyleClass.pre);
		node2lineStyle.put(IndentedCodeBlock.class, StyleClass.pre);
		node2style.put(IndentedCodeBlock.class, StyleClass.pre);
		node2style.put(BlockQuote.class, StyleClass.blockquote);

		// lists
		node2style.put(BulletList.class, StyleClass.ul);
		node2style.put(OrderedList.class, StyleClass.ol);
		node2style.put(BulletListItem.class, StyleClass.li);
		node2style.put(OrderedListItem.class, StyleClass.li);

		// tables
		node2lineStyle.put(TableBlock.class, StyleClass.table);
		node2style.put(TableHead.class, StyleClass.thead);
		node2style.put(TableBody.class, StyleClass.tbody);
		node2style.put(TableRow.class, StyleClass.tr);

		// misc
		node2style.put(Reference.class, StyleClass.reference);
	}

	private static final ServiceLoader<MarkdownSyntaxHighlighterAddon> addons =
		ServiceLoader.load(MarkdownSyntaxHighlighterAddon.class);

	private final MarkdownTextArea textArea;
	private ArrayList<StyleRange> styleRanges;
	private ArrayList<StyleRange> lineStyleRanges;

	static void highlight(MarkdownTextArea textArea, Node astRoot,
		List<ExtraStyledRanges> extraStyledRanges)
	{
		assert Platform.isFxApplicationThread();

		assert textArea.getText().length() == textArea.getLength();
		new MarkdownSyntaxHighlighter(textArea).highlight(astRoot,
			extraStyledRanges);
	}

	private MarkdownSyntaxHighlighter(MarkdownTextArea textArea) {
		this.textArea = textArea;
	}

	private void highlight(Node astRoot,
		List<ExtraStyledRanges> extraStyledRanges)
	{
		addonsAddStylesheets();

		styleRanges = new ArrayList<>();
		lineStyleRanges = new ArrayList<>();

		// visit all nodes
		NodeVisitor visitor = new NodeVisitor(new VisitHandler<>(
			com.vladsch.flexmark.ast.Paragraph.class, this::visit),
			new VisitHandler<>(Heading.class, this::visit), new VisitHandler<>(
				BulletListItem.class, this::visit), new VisitHandler<>(
					OrderedListItem.class, this::visit), new VisitHandler<>(
						TableCell.class, this::visit), new VisitHandler<>(
							FencedCodeBlock.class, this::visit), new VisitHandler<>(
								HtmlBlock.class, this::visit), new VisitHandler<>(
									HtmlCommentBlock.class, this::visit), new VisitHandler<>(
										HtmlInnerBlock.class, this::visit), new VisitHandler<>(
											HtmlInnerBlockComment.class, this::visit),
			new VisitHandler<>(HtmlInline.class, this::visit), new VisitHandler<>(
				HtmlInlineComment.class, this::visit), new VisitHandler<>(
					HtmlEntity.class, this::visit))
		{

			@Override
			public void processNode(Node node, boolean withChildren,
				BiConsumer<Node, Visitor<Node>> processor)
			{
				Class<? extends Node> nodeClass = node.getClass();

				StyleClass style = node2style.get(nodeClass);
				if (style != null) setStyleClass(node, style);

				StyleClass lineStyle = node2lineStyle.get(nodeClass);
				if (lineStyle != null) setLineStyleClass(node, lineStyle);

				VisitHandler<?> handler = getHandler(nodeClass);
				if (handler != null) processor.accept(node, handler);

				processChildren(node, processor);
			}
		};
		visitor.visit(astRoot);

		// add extra styled ranges
		if (extraStyledRanges != null) {
			long extraStyleBits = 1L << StyleClass.values().length;
			for (ExtraStyledRanges extraStyledRange : extraStyledRanges) {
				for (Range extraRange : extraStyledRange.ranges) {
					addStyledRange(styleRanges, extraRange.start, extraRange.end,
						extraStyleBits);
				}
				extraStyleBits <<= 1;
			}

			// need to clear cache
			styleClassesCache.clear();
		}

		// set text styles
		StyleSpansBuilder<Collection<String>> spansBuilder =
			new StyleSpansBuilder<>();
		int textLength = textArea.getLength();
		if (textLength > 0) {
			int spanStart = 0;
			for (StyleRange range : styleRanges) {
				if (range.begin > spanStart) spansBuilder.add(Collections.emptyList(),
					range.begin - spanStart);
				spansBuilder.add(toStyleClasses(range.styleBits, extraStyledRanges),
					range.end - range.begin);
				spanStart = range.end;
			}
			if (spanStart < textLength) spansBuilder.add(Collections.emptyList(),
				textLength - spanStart);
		}
		else spansBuilder.add(Collections.emptyList(), 0);
		textArea.setStyleSpans(0, spansBuilder.create());

		// set line styles
		int start = 0;
		for (StyleRange range : lineStyleRanges) {
			if (range.begin > start) setParagraphStyle(start, range.begin, Collections
				.emptyList());
			setParagraphStyle(range.begin, range.end, toStyleClasses(range.styleBits,
				null));
			start = range.end;
		}
		int lineCount = textArea.getParagraphs().size();
		if (start < lineCount) setParagraphStyle(start, lineCount, Collections
			.emptyList());
	}

	private void setParagraphStyle(int start, int end, Collection<String> ps) {
		for (int i = start; i < end; i++) {
			Paragraph<?, ?, ?> paragraph = textArea.getParagraph(i);
			if (ps != paragraph.getParagraphStyle()) setParagraphStyle(paragraph, i,
				ps);
		}
	}

	private void setParagraphStyle(Paragraph<?, ?, ?> paragraph,
		int paragraphIndex, Collection<String> paragraphStyle)
	{
		if (paragraphStyleField != null) {
			// because StyledTextArea.setParagraphStyle() is very very slow,
			// especially if invoked many times, we (try to) go the "short way"
			try {
				paragraphStyleField.set(paragraph, paragraphStyle);
				return;
			}
			catch (Exception ex) {
				// ignore
			}
		}

		textArea.setParagraphStyle(paragraphIndex, paragraphStyle);
	}

	private static Field paragraphStyleField;
	static {
		try {
			paragraphStyleField = Paragraph.class.getDeclaredField("paragraphStyle");
			paragraphStyleField.setAccessible(true);
		}
		catch (Exception e) {
			// ignore
		}
	}

	private Collection<String> toStyleClasses(long bits,
		List<ExtraStyledRanges> extraStyledRanges)
	{
		if (bits == 0) return Collections.emptyList();

		Collection<String> styleClasses = styleClassesCache.get(bits);
		if (styleClasses != null) return styleClasses;

		styleClasses = new ArrayList<>(1);
		for (StyleClass styleClass : StyleClass.values()) {
			if ((bits & (1L << styleClass.ordinal())) != 0) {
				styleClasses.add(styleClass.cssClass());
				if (styleClass.cssClass2 != null) styleClasses.add(
					styleClass.cssClass2);
			}
		}
		if (extraStyledRanges != null) {
			long extraStyleBits = 1L << StyleClass.values().length;
			for (ExtraStyledRanges extraStyledRange : extraStyledRanges) {
				if ((bits & extraStyleBits) != 0) styleClasses.add(
					extraStyledRange.styleClass);
				extraStyleBits <<= 1;
			}
		}
		styleClassesCache.put(bits, styleClasses);
		return styleClasses;
	}

	private void visit(com.vladsch.flexmark.ast.Paragraph node) {
		addonsHighlightNode(node);
	}

	private void visit(Heading node) {
		StyleClass styleClass;
		switch (node.getLevel()) {
			case 1:
				styleClass = StyleClass.h1;
				break;
			case 2:
				styleClass = StyleClass.h2;
				break;
			case 3:
				styleClass = StyleClass.h3;
				break;
			case 4:
				styleClass = StyleClass.h4;
				break;
			case 5:
				styleClass = StyleClass.h5;
				break;
			case 6:
				styleClass = StyleClass.h6;
				break;
			default:
				return;
		}
		setStyleClass(node, styleClass);
	}

	private void visit(ListItem node) {
		setStyleClass(node.getOpeningMarker(), StyleClass.liopen);
	}

	private void visit(TableCell node) {
		setStyleClass(node, node.isHeader() ? StyleClass.th : StyleClass.td);
	}

	private void visit(FencedCodeBlock node) {
		String language = node.getInfo().toString();
		if (highlightSequence(node.getContentChars(), language)) {
			setStyleClass(node.getOpeningFence(), StyleClass.pre);
			setStyleClass(node.getInfo(), StyleClass.pre);
			setStyleClass(node.getClosingFence(), StyleClass.pre);
		}
		else setStyleClass(node, StyleClass.pre);
	}

	private void visit(HtmlBlockBase node) {
		setLineStyleClass(node, StyleClass.html);
		highlightSequence(node.getChars(), "html");
	}

	private void visit(HtmlInlineBase node) {
		setStyleClass(node, StyleClass.html);
		highlightSequence(node.getChars(), "html");
	}

	private void visit(HtmlEntity node) {
		setStyleClass(node, StyleClass.html);
		setStyleClass(node, StyleClass.custom("entity", "token"));
	}

	private boolean highlightSequence(BasedSequence sequence, String language) {
		SyntaxHighlighter.HighlightConsumer highlighter =
			new SyntaxHighlighter.HighlightConsumer()
			{

				private int index = sequence.getStartOffset();

				@Override
				public void accept(int length, String style) {
					if (style != null) addStyledRange(styleRanges, index, index + length,
						StyleClass.custom(style, "token"));
					index += length;
				}
			};
		String text = sequence.baseSubSequence(sequence.getStartOffset(), sequence
			.getEndOffset()).toString();
		return SyntaxHighlighter.highlight(text, language, highlighter);
	}

	private void setStyleClass(Node node, StyleClass styleClass) {
		setStyleClass(node.getChars(), styleClass);
	}

	private void setStyleClass(BasedSequence sequence, StyleClass styleClass) {
		int start = sequence.getStartOffset();
		int end = sequence.getEndOffset();

		addStyledRange(styleRanges, start, end, styleClass);
	}

	private void setLineStyleClass(Node node, StyleClass styleClass) {
		int start = textArea.offsetToPosition(node.getStartOffset(), Bias.Forward)
			.getMajor();
		int end = textArea.offsetToPosition(node.getEndOffset() - 1, Bias.Forward)
			.getMajor() + 1;

		addStyledRange(lineStyleRanges, start, end, styleClass);
	}

	/**
	 * Adds a style range to styleRanges. Makes sure that the ranges are sorted by
	 * begin index and that there are no overlapping ranges. In case the added
	 * range overlaps, existing ranges are split.
	 *
	 * @param begin the beginning index, inclusive
	 * @param end the ending index, exclusive
	 */
	/*private*/ static void addStyledRange(ArrayList<StyleRange> styleRanges,
		int begin, int end, StyleClass styleClass)
	{
		long styleBits = 1L << styleClass.ordinal();
		addStyledRange(styleRanges, begin, end, styleBits);
	}

	private static void addStyledRange(ArrayList<StyleRange> styleRanges,
		int begin, int end, long styleBits)
	{
		final int lastIndex = styleRanges.size() - 1;

		// check whether list is empty
		if (styleRanges.isEmpty()) {
			styleRanges.add(new StyleRange(begin, end, styleBits));
			return;
		}

		// check whether new range is after last range
		final StyleRange lastRange = styleRanges.get(lastIndex);
		if (begin >= lastRange.end) {
			styleRanges.add(new StyleRange(begin, end, styleBits));
			return;
		}

		// walk existing ranges from last to first
		for (int i = lastIndex; i >= 0; i--) {
			StyleRange range = styleRanges.get(i);
			if (end <= range.begin) {
				// new range is before existing range (no overlapping) --> nothing yet
				// to do
				continue;
			}

			if (begin >= range.end) {
				// existing range is before new range (no overlapping)

				if (begin < styleRanges.get(i + 1).begin) {
					// new range starts after this range (may overlap next range) --> add
					int end2 = Math.min(end, styleRanges.get(i + 1).begin);
					styleRanges.add(i + 1, new StyleRange(begin, end2, styleBits));
				}

				break; // done
			}

			if (end > range.end) {
				// new range ends after this range (may overlap next range) --> add
				int end2 = (i == lastIndex) ? end : Math.min(end, styleRanges.get(i +
					1).begin);
				if (end2 > range.end) styleRanges.add(i + 1, new StyleRange(range.end,
					end2, styleBits));
			}

			if (begin < range.end && end > range.begin) {
				// the new range overlaps the existing range somewhere

				if (begin <= range.begin && end >= range.end) {
					// new range completely overlaps existing range --> merge style bits
					styleRanges.set(i, new StyleRange(range.begin, range.end,
						range.styleBits | styleBits));
				}
				else if (begin <= range.begin && end < range.end) {
					// new range overlaps at the begin with existing range --> split range
					styleRanges.set(i, new StyleRange(range.begin, end, range.styleBits |
						styleBits));
					styleRanges.add(i + 1, new StyleRange(end, range.end,
						range.styleBits));
				}
				else if (begin > range.begin && end >= range.end) {
					// new range overlaps at the end with existing range --> split range
					styleRanges.set(i, new StyleRange(range.begin, begin,
						range.styleBits));
					styleRanges.add(i + 1, new StyleRange(begin, range.end,
						range.styleBits | styleBits));
				}
				else if (begin > range.begin && end < range.end) {
					// new range is in existing range --> split range
					styleRanges.set(i, new StyleRange(range.begin, begin,
						range.styleBits));
					styleRanges.add(i + 1, new StyleRange(begin, end, range.styleBits |
						styleBits));
					styleRanges.add(i + 2, new StyleRange(end, range.end,
						range.styleBits));
				}
			}
		}

		// check whether new range starts before first range
		if (begin < styleRanges.get(0).begin) {
			// add new range (part) before first range
			int end2 = Math.min(end, styleRanges.get(0).begin);
			styleRanges.add(0, new StyleRange(begin, end2, styleBits));
		}
	}

	// ---- addons -------------------------------------------------------------

	private void addonsAddStylesheets() {
		for (MarkdownSyntaxHighlighterAddon addon : addons) {
			for (String stylesheet : addon.getStylesheets()) {
				if (!textArea.getStylesheets().contains(stylesheet)) textArea
					.getStylesheets().add(stylesheet);
			}
		}
	}

	private void addonsHighlightNode(com.vladsch.flexmark.ast.Paragraph node) {
		int startOffset = node.getStartOffset();
		addonsHighlightText(node.getChars().toString(), (begin, end, style) -> {
			addStyledRange(styleRanges, startOffset + begin, startOffset + end,
				StyleClass.custom(style, "token"));
		});
	}

	private void addonsHighlightText(String text,
		MarkdownSyntaxHighlighterAddon.Highlighter highlighter)
	{
		for (MarkdownSyntaxHighlighterAddon addon : addons)
			addon.highlight(text, highlighter);
	}

	// ---- class StyleRange ---------------------------------------------------

	/*private*/ static class StyleRange {

		final int begin; // inclusive
		final int end; // exclusive
		final long styleBits; // 1 << StyleClass.ordinal()

		StyleRange(int begin, int end, long styleBits) {
			this.begin = begin;
			this.end = end;
			this.styleBits = styleBits;
		}
	}

	// ---- class ExtraStyledRanges --------------------------------------------

	static class ExtraStyledRanges {

		final String styleClass;
		final List<Range> ranges;

		ExtraStyledRanges(String styleClass, List<Range> ranges) {
			this.styleClass = styleClass;
			this.ranges = ranges;
		}
	}
}
