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

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerState;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;
import static bdv.viewer.Interpolation.NLINEAR;
import static bdv.viewer.ViewerStateChange.DISPLAY_MODE_CHANGED;
import static bdv.viewer.ViewerStateChange.INTERPOLATION_CHANGED;

public class NavigationPanel extends JPanel
{

	private final NavigationButton moleculeLocation, fullView, autoContrast, exportImagePlus, help;

	public NavigationPanel( final ViewerState state, final MarsBdvFrame<?> marsBdvFrame )
	{
		super( new MigLayout( "ins 0, fillx, filly", "[][][]", "top" ) );
		this.setBackground( Color.white );
		
		moleculeLocation = new NavigationButton(
				new ImageIcon( this.getClass().getResource( "Molecule.png" ) ),
				"Go to");
		
		fullView = new NavigationButton(
				new ImageIcon( this.getClass().getResource( "FullView.png" ) ),
				"Full view");
		
		autoContrast = new NavigationButton(
				new ImageIcon( this.getClass().getResource( "AutoContrast.png" ) ),
				"Contrast");
		
		exportImagePlus = new NavigationButton(
				new ImageIcon( this.getClass().getResource( "ExportImage.png" ) ),
				"Export");
		
		help = new NavigationButton(
				new ImageIcon( this.getClass().getResource( "Help.png" ) ),
				"Help");
		
		moleculeLocation.addActionListener( e -> {
				marsBdvFrame.updateView();
				marsBdvFrame.updateLocation();
			});
		
		fullView.addActionListener( e -> marsBdvFrame.setFullView());
		
		exportImagePlus.addActionListener( e -> {
			GenericDialog dialog = new GenericDialog("Export to ImagePlus");
			dialog.addNumericField("x0", -10, 2);
			dialog.addNumericField("y0", -10, 2);
			dialog.addNumericField("width", 20, 2);
			dialog.addNumericField("height", 60, 2);
	  		dialog.showDialog();
	  		
	  		if (dialog.wasCanceled())
	  			return;
	  		
	  		int x0 = (int)dialog.getNextNumber();
	  		int y0 = (int)dialog.getNextNumber();
	  		int width = (int)dialog.getNextNumber();
	  		int height = (int)dialog.getNextNumber();
	  		
	  		ImagePlus ip = marsBdvFrame.exportView(x0, y0, width, height);
	  		
	  		//Now show it!
	  		ip.show();
	  		
	  		IJ.run(ip, "Enhance Contrast...", "saturated=0.35");
		});
		
		help.addActionListener( e -> marsBdvFrame.showHelp(true));
		
		autoContrast.addActionListener( e -> MarsBdvFrame.initBrightness( 0.001, 0.999, marsBdvFrame.bdv.getViewerPanel().state(), marsBdvFrame.bdv.getConverterSetups() ));

		this.add( moleculeLocation );
		this.add( fullView );
		this.add( autoContrast );
		this.add( exportImagePlus );
		this.add( help );
	}
}
