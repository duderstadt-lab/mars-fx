/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2022 Karl Duderstadt
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
 * Copyright (c) 2017 Karl Tauber <karl at jformdesigner dot com>
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

import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.HARD_LINE_BREAK_BACKSLASH;
import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.HARD_LINE_BREAK_SPACES;
import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.LINE_BREAK;
import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.PROTECTED_SPACE;
import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.PROTECTED_TAB;
import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.SOFT_LINE_BREAK;
import static de.mpg.biochem.mars.fx.addons.SmartFormatAddon.SPECIAL_INDENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;

import org.fxmisc.richtext.MultiChangeBuilder;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.misc.Pair;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import de.mpg.biochem.mars.fx.addons.SmartFormatAddon;
import de.mpg.biochem.mars.fx.options.Options;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;

/**
 * Smart Markdown text formatting methods.
 *
 * @author Karl Tauber
 * 
 * Small update for compatability with flexmark 0.62.2
 * 
 * @author Karl Duderstadt
 */
class SmartFormat
{
	private static final ServiceLoader<SmartFormatAddon> addons = ServiceLoader.load(SmartFormatAddon.class);

	private final MarkdownEditorPane editor;
	private final MarkdownTextArea textArea;

	SmartFormat(MarkdownEditorPane editor, MarkdownTextArea textArea) {
		this.editor = editor;
		this.textArea = textArea;
	}

	void format(KeyEvent e) {
		format(e.isAltDown(), null);
	}

	void format(boolean formatSelectionOnly, String oldMarkdown) {
		Node markdownAST = editor.getMarkdownAST();
		if (markdownAST == null)
			return;

		// find paragraphs in old markdown
		HashSet<BasedSequence> oldParagraphs = new HashSet<>();
		if (oldMarkdown != null) {
			Node oldMarkdownAST = editor.parseMarkdown(oldMarkdown);
			NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
				@Override
				 public void processNode(Node node, boolean withChildren, BiConsumer<Node, Visitor<Node>> processor) {
				     if (node instanceof Paragraph || node instanceof HtmlBlock) {
						oldParagraphs.add(node.getChars());
					} else if (withChildren)
						processChildren(node, processor);
				 }
			};
			visitor.visit(oldMarkdownAST);
		}

		IndexRange selectedLinesRange = formatSelectionOnly ? editor.getSmartEdit().getSelectedLinesRange(false) : null;
		IndexRange selection = textArea.getSelection();
		int wrapLength = Options.getWrapLineLength();

		// find and format paragraphs
		List<Pair<Block, String>> formattedParagraphs = formatParagraphs(markdownAST, wrapLength, selectedLinesRange, oldParagraphs);
		if (formattedParagraphs.isEmpty())
			return;

		// replace text of formatted paragraphs
		MultiChangeBuilder<?, ?, ?> multiChange = textArea.createMultiChange(formattedParagraphs.size());
		for (Pair<Block, String> pair : formattedParagraphs) {
			Block paragraph = pair.getFirst();
			String newText = pair.getSecond();

			int startOffset = paragraph.getStartOffset();
			int endOffset = paragraph.getEndOffset();

			multiChange.replaceText(startOffset, endOffset, newText);
		}
		SmartEdit.commitMultiChange(textArea, multiChange);

		// make sure that selection is not out of bounds if text becomes shorter
		SmartEdit.selectRange(textArea, Math.min(selection.getStart(), textArea.getLength()), Math.min(selection.getEnd(), textArea.getLength()));
	}

	/*private*/ List<Pair<Block, String>> formatParagraphs(Node markdownAST, int wrapLength, IndexRange selection, HashSet<BasedSequence> oldParagraphs) {
		ArrayList<Pair<Block, String>> formattedParagraphs = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {			
			@Override
			public void processNode(Node node, boolean withChildren, BiConsumer<Node, Visitor<Node>> processor) {
				if (node instanceof Paragraph || node instanceof HtmlBlock) {
					if (selection != null && !isNodeSelected(node, selection))
						return;

					if (oldParagraphs != null && oldParagraphs.contains(node.getChars()))
						return; // ignore unmodified paragraphs

					String newText = (node instanceof Paragraph)
						? formatParagraph((Paragraph) node, wrapLength)
						: formatHtmlBlock((HtmlBlock) node, wrapLength);

					// append trailing line separator (if necessary)
					if (node.getChars().endsWith("\n"))
						newText += "\n";

					if (!node.getChars().equals(newText, false))
						formattedParagraphs.add(new Pair<>((Block) node, newText));
				} else if (withChildren)
					processChildren(node, processor);
			}
		};
		visitor.visit(markdownAST);
		return formattedParagraphs;
	}

	private boolean isNodeSelected(Node node, IndexRange selection) {
		return node.getStartOffset() <= selection.getEnd() && node.getEndOffset() > selection.getStart();
	}

	private String formatParagraph(Paragraph paragraph, int wrapLength) {
		String firstindent = paragraphIndent(paragraph);
		String indent = "";
		Block block = paragraph.getParent();
		if (block instanceof ListItem) {
			char[] chars = new char[firstindent.length()];
			Arrays.fill(chars, ' ');
			indent = new String(chars);
		} else if (block instanceof BlockQuote)
			indent = firstindent;

		// collect the paragraph text
		StringBuilder buf = new StringBuilder(paragraph.getTextLength());
		collectFormattableText(buf, paragraph);
		String text = buf.toString();

		// let addons protect text
		for (SmartFormatAddon addon : addons)
			text = addon.protect(text);

		// format the paragraph text
		text = formatText(text, wrapLength, indent, firstindent.length());

		// let addons unprotect text
		for (SmartFormatAddon hook : addons)
			text = hook.unprotect(text);

		return text;
	}

	/**
	 * Returns the indent of the paragraph, which is the characters between
	 * the start of the line and the first character of the paragraph.
	 */
	private String paragraphIndent(Paragraph paragraph) {
		int paraStartOffset = paragraph.getStartOffset();
		int paraLineStartOffset = paragraph.getDocument().getChars().startOfLine(paraStartOffset);
		return (paraStartOffset > paraLineStartOffset)
			? paragraph.getDocument().getChars().subSequence(paraLineStartOffset, paraStartOffset).toString()
			: "";
	}

	/**
	 * Collects the text of a single paragraph.
	 *
	 * Replaces:
	 *   - tabs with spaces
	 *   - newlines with spaces (may occur in Code nodes)
	 *   - soft line breaks with spaces
	 *   - hard line breaks with special marker characters
	 *   - spaces and tabs in special nodes, that should not formatted, with marker characters
	 */
	private void collectFormattableText(StringBuilder buf, Node node) {
		for (Node n = node.getFirstChild(); n != null; n = n.getNext()) {
			if (n instanceof Text) {
				buf.append(n.getChars().toString().replace('\t', ' ').replace('\n', ' '));
			} else if (n instanceof DelimitedNode) {
				// italic, bold and code
				buf.append(((DelimitedNode) n).getOpeningMarker());
				collectFormattableText(buf, n);
				buf.append(((DelimitedNode) n).getClosingMarker());
			} else if (n instanceof SoftLineBreak) {
				buf.append(' ');
			} else if (n instanceof HardLineBreak) {
				buf.append(' ').append(n.getChars().startsWith("\\")
					? HARD_LINE_BREAK_BACKSLASH : HARD_LINE_BREAK_SPACES).append(' ');
			} else {
				// other text that should be not wrapped or formatted
				buf.append(protectWhitespace(n.getChars().toString()));
			}
		}
	}

	private String formatHtmlBlock(HtmlBlock htmlBlock, int wrapLength) {
		String text = htmlBlock.getChars().toString();
		String[] lines = text.split("\n");
		for (int i = 0; i < lines.length; i++)
			lines[i] = formatText(lines[i], wrapLength, "", 0);
		return String.join("\n", lines);
	}

	/**
	 * Formats the given text by merging multiple spaces into one space
	 * and wrapping lines.
	 */
	private String formatText(String text, int wrapLength, String indent, int firstIndent) {
		String[] words = text.split(" +");

		StringBuilder buf = new StringBuilder(text.length());
		int lineLength = firstIndent;
		boolean firstWord = true;
		boolean specialFirstLine = false;
		int specialIndent = 0;
		for (String word : words) {
			if (word.startsWith(SPECIAL_INDENT)) {
				specialIndent = Integer.parseInt(word.substring(1));
				continue;
			}

			if (word.startsWith(LINE_BREAK)) {
				// hard line break ("two spaces" or "backslash") or soft line break
				buf.append(word.equals(HARD_LINE_BREAK_SPACES) ? "  \n"
						: (word.equals(HARD_LINE_BREAK_BACKSLASH) ? "\\\n" : "\n"));
				lineLength = 0;
				firstWord = true;
				specialFirstLine = word.equals(SOFT_LINE_BREAK);
				continue;
			}

			if (!firstWord &&
				lineLength > indent.length() &&
				lineLength + 1 + word.length() > wrapLength &&
				allowWrapBeforeWord(word))
			{
				// wrap
				buf.append('\n');
				lineLength = 0;
				firstWord = true;
			} else if (!firstWord && lineLength > indent.length()) {
				// add space before word
				buf.append(' ');
				lineLength++;
			}

			// indent
			if (lineLength == 0) {
				int indentSize = 0;

				if (specialIndent > 0) {
					if (!specialFirstLine)
						indentSize += specialIndent;
					else if (indent.length() == 0)
						indentSize += firstIndent;
				}

				buf.append(indent);
				for (int i = 0; i < indentSize; i++)
					buf.append(' ');
				lineLength += indent.length() + indentSize;
			}

			// add word
			buf.append(word);
			lineLength += word.length();
			firstWord = false;
			specialFirstLine = false;
		}

		return unprotectWhitespace(buf.toString());
	}

	private boolean allowWrapBeforeWord(String word) {
		// avoid wrapping before '>' because this would create a blockquote
		if (word.startsWith(">"))
			return false;

		// avoid wrapping before list markers
		if (word.equals("-") || word.equals("+") || word.equals("*"))
			return false;

		// avoid wrapping before numbered list markers
		if (Character.isDigit(word.charAt(0)) && word.matches("[0-9]+\\."))
			return false;

		return true;
	}

	private String protectWhitespace(String s) {
		return s.replace(' ', PROTECTED_SPACE).replace('\t', PROTECTED_TAB);
	}

	private String unprotectWhitespace(String s) {
		return s.replace(PROTECTED_SPACE, ' ').replace(PROTECTED_TAB, '\t');
	}
}
