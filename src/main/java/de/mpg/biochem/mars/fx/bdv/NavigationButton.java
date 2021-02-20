/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.bdv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;

class NavigationButton extends JPanel
{
	private final JButton button;
	private final String text;

	private final JLabel label;
	
	public NavigationButton(
			final Icon icon,
			final String text)
	{
		super( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );

		button = new JButton( icon );
		setLook( button );

		this.setBackground( Color.white );
		
		this.add( button, "growx, center, wrap" );
		
		this.text = text;

		label = new JLabel( text );
		setFont( label );

		this.add( label, "center" );
	}

	public void addActionListener( final ActionListener l )
	{
		button.addActionListener( l );
	}

	public void removeActionListener( final ActionListener l )
	{
		button.removeActionListener( l );
	}

	private void setLook( final JButton button )
	{
		button.setMaximumSize( new Dimension( button.getIcon().getIconWidth(), button.getIcon().getIconHeight() ) );
		button.setBackground( Color.white );
		button.setBorderPainted( false );
		button.setFocusPainted( false );
		button.setContentAreaFilled( false );
	}
	
	private void setFont( final JLabel label )
	{
		label.setFont( new Font( Font.MONOSPACED, Font.BOLD, 9 ) );
	}
}
