/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import bdv.SpimSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import bdv.viewer.render.MipmapOrdering.Level;
import bdv.viewer.render.MipmapOrdering.MipmapHints;
import bdv.viewer.render.Prefetcher;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import mpicbg.spim.data.SpimDataException;
import de.mpg.biochem.mars.molecule.*;
import ij.ImagePlus;
import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import mpicbg.spim.data.registration.*;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import bdv.img.cache.VolatileCachedCellImg;

public class MarsBdvFrame< T extends NumericType< T > & NativeType< T > > {
	
	private final JFrame frame;
	
	//private double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };
	private double[] screenScales = new double[] { 1 };
	private AffineTransform3D[] screenScaleTransforms;
	protected ARGBScreenImage[][] screenImages;
	
	private JTextField scaleField;
	private JCheckBox autoUpdate;
	private JCheckBox cacheTimepoints;
	
	private HashMap<String, ArrayList<SpimDataMinimal>> bdvSources;
	
	private String metaUID;
	
	private BdvHandlePanel bdv;
	
	private String xParameter, yParameter;
	private MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	protected Molecule molecule;
	
	protected AffineTransform3D viewerTransform;
	
	public MarsBdvFrame(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive, Molecule molecule, String xParameter, String yParameter) {
		this.archive = archive;
		this.molecule = molecule;
		this.xParameter = xParameter;
		this.yParameter = yParameter;
		
		bdvSources = new HashMap<String, ArrayList<SpimDataMinimal>>();
		
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		
		frame = new JFrame( archive.getName() + " Bdv" );
		
		JPanel buttonPane = new JPanel();
		
		JButton reload = new JButton("Reload");
		reload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});
		buttonPane.add(reload);
		
		JButton resetView = new JButton("Reset view");
		resetView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetView();
			}
		});
		buttonPane.add(resetView);
		
		JButton goTo = new JButton("Go to molecule");
		goTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Molecule molecule = molPane.getMolecule();
				if (molecule != null) {			
					MarsImageMetadata meta = archive.getImageMetadata(molecule.getImageMetadataUID());
					if (!metaUID.equals(meta.getUID())) {
						metaUID = meta.getUID();
						createView(meta);
					}
					if (molecule.hasParameter(xParameter) && molecule.hasParameter(yParameter))
						goTo(molecule.getParameter(xParameter), molecule.getParameter(yParameter));
				 }
			}
		});
		buttonPane.add(goTo);
		
		JPanel optionsPane = new JPanel();
		
		autoUpdate = new JCheckBox("Auto update", true);
		cacheTimepoints = new JCheckBox("Cache timepoints", false);
		
		optionsPane.add(new JLabel("Zoom "));
		
		scaleField = new JTextField(6);
		scaleField.setText("10");
		Dimension dimScaleField = new Dimension(100, 20);
		scaleField.setMinimumSize(dimScaleField);
		
		optionsPane.add(scaleField);
		optionsPane.add(autoUpdate);
		
		//START Cache testing
		optionsPane.add(cacheTimepoints);
		Bdv.options().screenScales(screenScales);
		Bdv.options().targetRenderNanos(1_000_000_000);
		screenScaleTransforms = new AffineTransform3D[ screenScales.length ];
		screenImages = new ARGBScreenImage[ screenScales.length ][ 3 ];
		Bdv.options().numRenderingThreads(8);
		//END Cache testing
		
		bdv = new BdvHandlePanel( frame, Bdv.options().is2D() );
		frame.add( bdv.getViewerPanel(), BorderLayout.CENTER );
		
		JPanel panel = new JPanel(new GridLayout(2, 1));
		
		panel.add(buttonPane);
		panel.add(optionsPane);
		
		frame.add(panel, BorderLayout.SOUTH);
		frame.setPreferredSize( new Dimension( 800, 600 ) );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		
		load();
	}
	
	protected synchronized boolean checkResize() {
		
		final int componentW = bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth();
		final int componentH = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight();
		if ( screenImages[ 0 ][ 0 ] == null || screenImages[ 0 ][ 0 ].dimension( 0 ) != ( int ) ( componentW * screenScales[ 0 ] ) || screenImages[ 0 ][ 0 ].dimension( 1 ) != ( int ) ( componentH  * screenScales[ 0 ] ) )
		{
			for ( int i = 0; i < screenScales.length; ++i )
			{
				final double screenToViewerScale = screenScales[ i ];
				final int w = ( int ) ( screenToViewerScale * componentW );
				final int h = ( int ) ( screenToViewerScale * componentH );
				screenImages[ i ][ 0 ] = new ARGBScreenImage( w, h );
				final AffineTransform3D scale = new AffineTransform3D();
				final double xScale = ( double ) w / componentW;
				final double yScale = ( double ) h / componentH;
				scale.set( xScale, 0, 0 );
				scale.set( yScale, 1, 1 );
				scale.set( 0.5 * xScale - 0.5, 0, 3 );
				scale.set( 0.5 * yScale - 0.5, 1, 3 );
				screenScaleTransforms[ i ] = scale;
			}

			return true;
		}
		return false;
	}
	
	public void load() {
		MarsImageMetadata meta = archive.getImageMetadata(molecule.getImageMetadataUID());
		metaUID = meta.getUID();
		createView(meta);
		if (molecule.hasParameter(xParameter) && molecule.hasParameter(yParameter)) 
			goTo(molecule.getParameter(xParameter), molecule.getParameter(yParameter));
	}
	
	public void setMolecule(Molecule molecule) {
		if (molecule != null && autoUpdate.isSelected()) {	
			this.molecule = molecule;
			MarsImageMetadata meta = archive.getImageMetadata(molecule.getImageMetadataUID());
			if (!metaUID.equals(meta.getUID())) {
				metaUID = meta.getUID();
				createView(meta);
			}
			if (molecule.hasParameter(xParameter) && molecule.hasParameter(yParameter))
				goTo(molecule.getParameter(xParameter), molecule.getParameter(yParameter));
		 }
	}
	
	private void createView(MarsImageMetadata meta) {
		if (bdv != null) {
			frame.setVisible( false );
			frame.remove(bdv.getViewerPanel());
		}
		bdv = new BdvHandlePanel( frame, Bdv.options().is2D() );		
		frame.add( bdv.getViewerPanel(), BorderLayout.CENTER );
		frame.setVisible( true );
		
		if (!bdvSources.containsKey(meta.getUID()))
			bdvSources.put(meta.getUID(), loadNewSources(meta));
		
		for (SpimDataMinimal spimData : bdvSources.get(meta.getUID()))
			BdvFunctions.show( spimData, Bdv.options().addTo( bdv ) );
		
		InitializeViewerState.initBrightness( 0.001, 0.999, bdv.getViewerPanel(), bdv.getSetupAssignments() );
	}
	
	public ImagePlus exportView(int x0, int y0, int width, int height) {
		int numChannels = bdvSources.get(metaUID).size();
		
		int TOP_left_x0 = (int)molecule.getParameter(xParameter) + x0;
		int TOP_left_y0 = (int)molecule.getParameter(yParameter) + y0;
		
		ImagePlus[] images = new ImagePlus[numChannels];
		
		for ( int i = 0; i < numChannels; i++ ) {
			ArrayList< RandomAccessibleInterval< T > > raiList = new ArrayList< RandomAccessibleInterval< T > >(); 
			SpimDataMinimal bdvSource = bdvSources.get(metaUID).get(i);
			
			for ( int t = 0; t < bdvSource.getSequenceDescription().getTimePoints().size(); t++ ) {
				//SpimDataMinimal, setup, name
				SpimSource<T> spimS = new SpimSource<T>( bdvSource, 0, "source" );

				//t, level, interpolation
				final RealRandomAccessible< T > raiRaw = ( RealRandomAccessible< T > )spimS.getInterpolatedSource( t, 0, Interpolation.NLINEAR );
				
				//retrieve transform
				AffineTransform3D affine = bdvSource.getViewRegistrations().getViewRegistration(t, 0).getModel();
				final AffineRandomAccessible< T, AffineGet > rai = RealViews.affine( raiRaw, affine );
				
				RandomAccessibleInterval< T > view = Views.interval( Views.raster( rai ), new long[] { TOP_left_x0, TOP_left_y0, 0 }, new long[]{ TOP_left_x0 + width, TOP_left_y0 + height, 0 } );
				
				raiList.add( view );
			}
			RandomAccessibleInterval< T > raiStack = Views.stack( raiList );
			images[i] = ImageJFunctions.wrap( raiStack, "channel " + i );
		}
		if (numChannels == 1)
			return images[0];
		
		//image arrays, boolean keep original.
		ImagePlus ip = ij.plugin.RGBStackMerge.mergeChannels(images, false);
		ip.setTitle("molecule " + molecule.getUID());
		
		return ip;
	}
	
	private ArrayList<SpimDataMinimal> loadNewSources(MarsImageMetadata meta) {
		ArrayList<SpimDataMinimal> spimArray = new ArrayList<SpimDataMinimal>();
		for (MarsBdvSource source : meta.getBdvSources()) {
			SpimDataMinimal spimData;
			try {
				spimData = new XmlIoSpimDataMinimal().load( source.getPathToXml() );
				
				//Add transforms to spimData...
				Map< ViewId, ViewRegistration > registrations = spimData.getViewRegistrations().getViewRegistrations();
				
				boolean driftCorrect = false;
				if (meta.getDataTable().hasColumn(source.getXDriftColumn()) && meta.getDataTable().hasColumn(source.getYDriftColumn())) {
					driftCorrect = true;
					System.out.println("found drift columns for " + source.getName());
				}
					
				for (ViewId id : registrations.keySet()) {
					if (driftCorrect) {
						double dX = meta.getDataTable().getValue(source.getXDriftColumn(), id.getTimePointId());
						double dY = meta.getDataTable().getValue(source.getYDriftColumn(), id.getTimePointId());
						registrations.get(id).getModel().set(source.getAffineTransform3D(dX, dY));
					} else
						registrations.get(id).getModel().set(source.getAffineTransform3D());
				}
				
				spimArray.add(spimData);
			} catch (SpimDataException e) {
				e.printStackTrace();
			}
		}
		
		return spimArray;
	}
	
	public void setPositionParameters(String xParameter, String yParameter) {
		this.xParameter = xParameter;
		this.yParameter = yParameter;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
		this.archive = archive;
	}

	public void goTo(double x, double y) {
		resetView();
		
		Dimension dim = bdv.getBdvHandle().getViewerPanel().getDisplay().getSize();
		viewerTransform = bdv.getViewerPanel().getDisplay().getTransformEventHandler().getTransform();
		AffineTransform3D affine = viewerTransform;
		
		double[] source = new double[3];
		source[0] = 0;
		source[1] = 0;
		source[2] = 0;
		double[] target = new double[3];
		target[0] = 0;
		target[1] = 0;
		target[2] = 0;
		
		viewerTransform.apply(source, target);
		
		affine.set( affine.get( 0, 3 ) - target[0], 0, 3 );
		affine.set( affine.get( 1, 3 ) - target[1], 1, 3 );

		double scale = Double.valueOf(scaleField.getText());
		
		//check it was set correctly?
		
		// scale
		affine.scale( scale );
		
		source[0] = x;
		source[1] = y;
		source[2] = 0;
		
		affine.apply(source, target);

		affine.set( affine.get( 0, 3 ) - target[0] + dim.getWidth()/2, 0, 3 );
		affine.set( affine.get( 1, 3 ) - target[1] + dim.getHeight()/2, 1, 3 );
		
		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affine );

		if (cacheTimepoints.isSelected()) {
			ViewerState viewerState = bdv.getBdvHandle().getViewerPanel().getState();
			
			//For the moment, we assume all sources have the same number of timepoints
			int timepoints = bdvSources.get(metaUID).get( 0 ).getSequenceDescription().getTimePoints().size();
			
			//Pre-cache timepoints for all sources
			checkResize();
			final List< SourceState< ? > > sourceStates = viewerState.getSources();
			
			CacheHints hints = new CacheHints( LoadingStrategy.VOLATILE, 0, false );
				
			for (int sourceIndex=0;sourceIndex<sourceStates.size();sourceIndex++) {
				
				final Source< ? > spimSource = sourceStates.get( sourceIndex ).getSpimSource();
				
				final ARGBScreenImage screenImage = screenImages[ 0 ][ 0 ];
				
				for ( int t = 0; t < timepoints; t++ ) {
						prefetch( viewerState, t, spimSource, affine, 0, hints, screenImage );
				}
			}
		}
	}
	
	
	//Added timepoint input..
	private static < T > void prefetch(
			final ViewerState viewerState,
			final int timepoint,
			final Source< T > source,
			final AffineTransform3D screenScaleTransform,
			final int mipmapIndex,
			final CacheHints prefetchCacheHints,
			final Dimensions screenInterval )
	{
		final RandomAccessibleInterval< T > img = source.getSource( timepoint, mipmapIndex );
		
		final VolatileCachedCellImg< ?, ? > cellImg = ( VolatileCachedCellImg< ?, ? > ) img;

		CacheHints hints = prefetchCacheHints;
		if ( hints == null )
		{
			final CacheHints d = cellImg.getDefaultCacheHints();
			hints = new CacheHints( LoadingStrategy.VOLATILE, d.getQueuePriority(), false );
		}
		cellImg.setCacheHints( hints );
		final int[] cellDimensions = new int[ 3 ];
		cellImg.getCellGrid().cellDimensions( cellDimensions );
		final long[] dimensions = new long[ 3 ];
		cellImg.dimensions( dimensions );
		final RandomAccess< ? > cellsRandomAccess = cellImg.getCells().randomAccess();

		final Interpolation interpolation = viewerState.getInterpolation();

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		viewerState.getViewerTransform( sourceToScreen );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
		sourceToScreen.concatenate( sourceTransform );
		sourceToScreen.preConcatenate( screenScaleTransform );

		Prefetcher.fetchCells( sourceToScreen, cellDimensions, dimensions, screenInterval, interpolation, cellsRandomAccess );
	}
	
	public void resetView() {
		ViewerPanel viewer = bdv.getBdvHandle().getViewerPanel();
		Dimension dim = viewer.getDisplay().getSize();
		viewerTransform = initTransform( (int)dim.getWidth(), (int)dim.getHeight(), false, viewer.getState() );
		viewer.setCurrentViewerTransform(viewerTransform);
	}

	/**
	 * Get a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * at z = 0
	 * <li>centered and scaled such that the full <em>dim_x</em> by
	 * <em>dim_y</em> is visible.
	 * </ul>
	 *
	 * @param viewerWidth
	 *            width of the viewer display
	 * @param viewerHeight
	 *            height of the viewer display
	 * @param state
	 *            the {@link ViewerState} containing at least one source.
	 * @return proposed initial viewer transform.
	 */
	public static AffineTransform3D initTransform( final int viewerWidth, final int viewerHeight, final boolean zoomedIn, final ViewerState state ) {
		final int cX = viewerWidth / 2;
		final int cY = viewerHeight / 2;

		final Source< ? > source = state.getSources().get( state.getCurrentSource() ).getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		if ( !source.isPresent( timepoint ) )
			return new AffineTransform3D();

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, 0, sourceTransform );

		final Interval sourceInterval = source.getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		//final double sZ0 = sourceInterval.min( 2 );
		//final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;
		final double sZ = 0;//( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		if ( zoomedIn )
			scale = Math.max( scaleX, scaleY );
		else
			scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );
		return viewerTransform;
	}
}
