/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2024 Karl Duderstadt
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fxmisc.richtext.model.Paragraph;

import de.mpg.biochem.mars.fx.editor.ParagraphOverlayGraphicFactory.OverlayFactory;
import de.mpg.biochem.mars.fx.util.Range;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.text.Text;

/**
 * Shows whitespace characters.
 *
 * @author Karl Tauber
 */
class WhitespaceOverlayFactory extends OverlayFactory {

	private static final String SPACE = "\u00B7";
	private static final String TAB = "\u00BB";
	private static final String EOL = "\u00B6";

	@Override
	public List<Node> createOverlayNodes(int paragraphIndex) {
		Paragraph<?, ?, Collection<String>> par = getTextArea().getParagraph(
			paragraphIndex);

		ArrayList<Node> nodes = new ArrayList<>();
		String text = par.getText();
		int textLength = text.length();
		for (int i = 0; i < textLength; i++) {
			char ch = text.charAt(i);
			if (ch != ' ' && ch != '\t') continue;

			nodes.add(createTextNode((ch == ' ') ? SPACE : TAB, par.getStyleOfChar(i),
				i, i + 1));
		}

		nodes.add(createTextNode(EOL, par.getStyleAtPosition(textLength),
			textLength - 1, textLength));

		return nodes;
	}

	private Text createTextNode(String text, Collection<String> styleClasses,
		int start, int end)
	{
		Text t = new Text(text);
		t.setTextOrigin(VPos.TOP);
		t.getStyleClass().add("text");
		t.setOpacity(0.3);
		t.getStyleClass().addAll(styleClasses);
		t.setUserData(new Range(start, end));
		return t;
	}

	@Override
	void layoutOverlayNodes(int paragraphIndex, List<Node> nodes) {
		Insets insets = getInsets();
		double leftInsets = insets.getLeft();
		double topInsets = insets.getTop();

		// all paragraphs except last one have line separators
		boolean showEOL = (paragraphIndex < getTextArea().getParagraphs().size() -
			1);
		Node eolNode = nodes.get(nodes.size() - 1);
		if (eolNode.isVisible() != showEOL) eolNode.setVisible(showEOL);

		for (Node node : nodes) {
			Range range = (Range) node.getUserData();
			Rectangle2D bounds = getBounds(range.start, range.end);
			node.setLayoutX(leftInsets + (node == eolNode ? bounds.getMaxX() : bounds
				.getMinX()));
			node.setLayoutY(topInsets + bounds.getMinY());
		}
	}
}
