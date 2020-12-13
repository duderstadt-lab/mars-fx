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
