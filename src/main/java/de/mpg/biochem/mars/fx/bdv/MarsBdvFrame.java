/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2023 Karl Duderstadt
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

import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD;
import static bdv.ui.BdvDefaultCards.DEFAULT_VIEWERMODES_CARD;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import bdv.SpimSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.HelpDialog;
import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvHandlePanel;
import bdv.util.Bounds;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.ConverterSetups;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import de.mpg.biochem.mars.fx.dialogs.RoverErrorDialog;
import de.mpg.biochem.mars.fx.molecule.AbstractMoleculeArchiveFxFrame;
import de.mpg.biochem.mars.n5.*;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import ij.IJ;
import javafx.application.Platform;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.ViewId;

public class MarsBdvFrame<T extends NumericType<T> & NativeType<T>> extends
	AbstractJsonConvertibleRecord
{

	@Parameter
	protected MarsBdvCardService marsBdvCardService;

	@Parameter
	protected UIService uiService;

	protected final JFrame frame;

	protected int numTimePoints = 1;

	protected final HelpDialog helpDialog;

	protected final SharedQueue sharedQueue;

	protected Map<String, List<Source<T>>> bdvSources;
	protected Map<String, List<Source<T>>> bdvSourcesForExport;
	protected Map<String, Map<String, long[]>> bdvSourceDimensions;
	protected Map<String, SourceDisplaySettings> displaySettings;
	protected Map<String, N5Reader> n5Readers;
	protected List<MarsBdvCard> cards;

	protected final boolean useVolatile;

	protected boolean windowStateLoaded = false;

	protected BdvHandlePanel bdv;
	
	@Parameter
	protected Context context;

	protected MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	protected MarsMetadata metadataSelection;
	protected Molecule molecule;
	protected String activeMetaUID = "";

	protected LocationCard locationCard;

	protected AffineTransform3D viewerTransform;

	public MarsBdvFrame(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive,
		Molecule molecule, MarsMetadata metadataSelection, boolean useVolatile, final Context context)
	{
		this(archive, molecule, metadataSelection, useVolatile, new ArrayList<MarsBdvCard>(), context);
	}

	public MarsBdvFrame(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive,
		Molecule molecule, MarsMetadata metadataSelection, boolean useVolatile, List<MarsBdvCard> cards,
		final Context context)
	{
		super();
		context.inject(this);

		this.archive = archive;
		this.molecule = molecule;
		this.metadataSelection = metadataSelection;
		this.useVolatile = useVolatile;
		this.cards = cards;

		frame = new JFrame(archive.getName() + " Bdv");
		helpDialog = new HelpDialog(frame);
		sharedQueue = new SharedQueue(Math.max(1, Runtime.getRuntime()
			.availableProcessors() / 2));

		System.setProperty("apple.laf.useScreenMenuBar", "true");

		bdvSources = new HashMap<String, List<Source<T>>>();
		bdvSourcesForExport = new HashMap<String, List<Source<T>>>();
		bdvSourceDimensions = new HashMap<String, Map<String, long[]>>();
		n5Readers = new HashMap<String, N5Reader>();
		displaySettings = new HashMap<String, SourceDisplaySettings>();

		bdv = new BdvHandlePanel(frame, Bdv.options().is2D());
		bdv.getBdvHandle().getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
		bdv.getBdvHandle().getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);

		bdv.getBdvHandle().getCardPanel().addCard("Display", "Display",
			new NavigationPanel(bdv.getViewerPanel().state(), this), true);

		locationCard = new LocationCard();
		locationCard.setArchive(archive);
		locationCard.initialize();
		bdv.getBdvHandle().getCardPanel().addCard("Location", "Location",
			locationCard.getPanel(), true);

		// Add custom cards
		for (MarsBdvCard card : cards) {
			card.setBdvFrame(this);
			bdv.getBdvHandle().getCardPanel().addCard(card.getName(), card.getName(),
				card.getPanel(), true);
		}

		frame.add(bdv.getSplitPanel(), BorderLayout.CENTER);

		frame.setPreferredSize(new Dimension(800, 600));
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
          releaseMemory();
      }
		});
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		initializeView();

		frame.setVisible(true);
	}

	public MarsBdvFrame(JsonParser jParser,
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive,
		Molecule molecule, MarsMetadata metadataSelection, boolean useVolatile, final Context context)
		throws IOException
	{
		context.inject(this);
		this.archive = archive;
		this.molecule = molecule;
		this.metadataSelection = metadataSelection;
		this.useVolatile = useVolatile;
		this.cards = new ArrayList<MarsBdvCard>();

		frame = new JFrame(archive.getName() + " Bdv");
		helpDialog = new HelpDialog(frame);
		sharedQueue = new SharedQueue(Math.max(1, Runtime.getRuntime()
			.availableProcessors() / 2));

		System.setProperty("apple.laf.useScreenMenuBar", "true");

		bdvSources = new HashMap<String, List<Source<T>>>();
		bdvSourcesForExport = new HashMap<String, List<Source<T>>>();
		bdvSourceDimensions = new HashMap<String, Map<String, long[]>>();
		n5Readers = new HashMap<String, N5Reader>();
		displaySettings = new HashMap<String, SourceDisplaySettings>();

		bdv = new BdvHandlePanel(frame, Bdv.options().is2D());
		bdv.getBdvHandle().getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
		bdv.getBdvHandle().getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);

		bdv.getBdvHandle().getCardPanel().addCard("Display", "Display",
			new NavigationPanel(bdv.getViewerPanel().state(), this), true);

		if (jParser != null) fromJSON(jParser);

		if (locationCard == null) {
			locationCard = new LocationCard();
			locationCard.setArchive(archive);
			locationCard.initialize();
		}
		bdv.getBdvHandle().getCardPanel().addCard("Location", "Location",
			locationCard.getPanel(), true);

		// Add custom cards
		for (MarsBdvCard card : cards) {
			card.setBdvFrame(this);
			bdv.getBdvHandle().getCardPanel().addCard(card.getName(), card.getName(),
				card.getPanel(), true);
		}

		frame.add(bdv.getSplitPanel(), BorderLayout.CENTER);

		if (!windowStateLoaded) frame.setPreferredSize(new Dimension(800, 600));
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
      	releaseMemory();
      }
		});
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		initializeView();

		frame.setVisible(true);
	}

	public void initializeView() {
		MarsMetadata metadata;
		if (locationCard.roverMoleculeSync() && molecule != null)
			metadata = archive.getMetadata(molecule.getMetadataUID());
		else if (locationCard.roverMetadataSync() &&  metadataSelection != null)
			metadata = metadataSelection;
		else if (metadataSelection != null)
			metadata = metadataSelection;
		else if (molecule != null) 
			metadata = archive.getMetadata(molecule.getMetadataUID());
		else 
			return;
		
		createView(metadata);
		applySourceDisplaySettings();

		if (metadata.getImage(0) != null && metadata.getImage(0).getSizeX() != -1 && metadata
			.getImage(0).getSizeY() != -1) goTo(metadata.getImage(0).getSizeX() / 2,
				metadata.getImage(0).getSizeY() / 2);
		else goTo(0, 0);
		
		if (locationCard.roverMoleculeSync() && molecule != null) setMolecule(molecule);
	}

	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
		if (locationCard.roverMoleculeSync()) {
			updateView();
			updateLocation();
		}
	}
	
	public void setMetadata(MarsMetadata metadataSelection) {
		this.metadataSelection = metadataSelection;
		if (locationCard.roverMetadataSync()) {
			updateView();
		}
	}

	public void updateView() {
		saveSourceDisplaySettings();
		if (locationCard.roverMoleculeSync() && molecule != null) {
			if (!activeMetaUID.equals(molecule.getMetadataUID())) {
				createView(archive.getMetadata(molecule.getMetadataUID()));

				// createView removes overlays.. this deactivates ensure they are
				// readded below.
				locationCard.setActive(false);
				for (MarsBdvCard card : cards)
					card.setActive(false);
			}

			locationCard.setMolecule(molecule);
			if (locationCard.showLocationOverlay() && !locationCard.isActive()) {
				BdvFunctions.showOverlay(locationCard.getBdvOverlay(), "Location", Bdv
					.options().addTo(bdv));
				locationCard.setActive(true);
			}

			for (MarsBdvCard card : cards) {
				card.setMolecule(molecule);
				if (!card.isActive()) {
					BdvFunctions.showOverlay(card.getBdvOverlay(), card.getName(), Bdv
						.options().addTo(bdv));
					card.setActive(true);
				}
			}
		} else if (locationCard.roverMetadataSync() && metadataSelection != null) {
			if (!activeMetaUID.equals(metadataSelection.getUID())) {
				createView(metadataSelection);

				// createView removes overlays.. this deactivates ensure they are
				// readded below.
				locationCard.setActive(false);
				for (MarsBdvCard card : cards)
					card.setActive(false);
			}

			locationCard.setMetadata(metadataSelection);
			if (locationCard.showLocationOverlay() && !locationCard.isActive()) {
				BdvFunctions.showOverlay(locationCard.getBdvOverlay(), "Location", Bdv
					.options().addTo(bdv));
				locationCard.setActive(true);
			}

			for (MarsBdvCard card : cards) {
				card.setMolecule(molecule);
				if (!card.isActive()) {
					BdvFunctions.showOverlay(card.getBdvOverlay(), card.getName(), Bdv
						.options().addTo(bdv));
					card.setActive(true);
				}
			}
		}
		applySourceDisplaySettings();
	}

	public void updateLocation() {
		double x = getXLocation();
		double y = getYLocation();

		if (!Double.isNaN(x) && !Double.isNaN(y)) goTo(x, y);
	}

	private double getXLocation() {
		if (molecule != null) {
			if (locationCard.useParameters() && molecule.hasParameter(locationCard
				.getXLocationSource())) return molecule.getParameter(locationCard
					.getXLocationSource());
			else if (molecule.getTable().hasColumn(locationCard.getXLocationSource()))
				return molecule.getTable().mean(locationCard.getXLocationSource());
		}

		return Double.NaN;
	}

	private double getYLocation() {
		if (molecule != null) {
			if (locationCard.useParameters() && molecule.hasParameter(locationCard
				.getYLocationSource())) return molecule.getParameter(locationCard
					.getYLocationSource());
			else if (molecule.getTable().hasColumn(locationCard.getYLocationSource()))
				return molecule.getTable().mean(locationCard.getYLocationSource());
		}

		return Double.NaN;
	}

	private void createView(MarsMetadata meta) {
		activeMetaUID = meta.getUID();
		if (!bdvSources.containsKey(meta.getUID())) {
			try {
				List<Source<T>> sources = new ArrayList<Source<T>>();
				List<Source<T>> exportSources = new ArrayList<Source<T>>();

				for (MarsBdvSource marsSource : meta.getBdvSources()) {
					if (marsSource.isN5()) {
						if (useVolatile) {
							sources.add(loadN5VolatileSource(marsSource, meta));
							exportSources.add(loadN5Source(marsSource, meta));
						}
						else {
							Source<T> source = loadN5Source(marsSource, meta);
							sources.add(source);
							exportSources.add(source);
						}
					}
					else {
						Source<T> source = loadAsSpimDataMinimal(marsSource, meta);
						sources.add(source);
						exportSources.add(source);
					}
				}

				bdvSources.put(meta.getUID(), sources);
				bdvSourcesForExport.put(meta.getUID(), exportSources);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<SourceAndConverter<?>> sourcesAndConverters =
			new ArrayList<SourceAndConverter<?>>();
		for (SourceAndConverter<?> sourceAndConverter : bdv.getViewerPanel().state()
			.getSources())
			sourcesAndConverters.add(sourceAndConverter);

		for (SourceAndConverter<?> sourceAndConverter : sourcesAndConverters)
			bdv.getViewerPanel().state().removeSource(sourceAndConverter);

		sourcesAndConverters.clear();

		for (Source<T> source : bdvSources.get(meta.getUID()))
			BdvFunctions.show(source, numTimePoints, Bdv.options().addTo(bdv));

		initBrightness(0.001, 0.999, bdv.getViewerPanel().state(), bdv
			.getConverterSetups());
	}

	public void setFullView() {
		updateView();
		ViewerPanel viewer = bdv.getViewerPanel();
		Dimension dim = viewer.getDisplay().getSize();
		viewerTransform = initTransform((int) dim.getWidth(), (int) dim.getHeight(),
			false, viewer.state());
		viewer.state().setViewerTransform(viewerTransform);
	}

	public void exportView() {
		double moleculeXCenter = getXLocation();
		double moleculeYCenter = getYLocation();
		
		final Interval viewInterval = currentViewImageCoordinates();
		long viewWidth = (long) (viewInterval.max(0) - viewInterval.min(0));
		long viewHeight = (long) (viewInterval.max(1) - viewInterval.min(1));

		final AffineTransform3D imageTransform = new AffineTransform3D();
		//We must give the dialog the image transform. This should be no transform 
		//because we always build the view around one channel that is not transformed.
		imageTransform.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
		
		ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
		
		
		final Interval initialInterval = (locationCard.roverMoleculeSync() && !Double.isNaN(moleculeXCenter) && !Double.isNaN(moleculeYCenter)) ? 
					Intervals.createMinMax( (long) (moleculeXCenter - viewWidth*0.6/2), (long) (moleculeYCenter - viewHeight*0.6/2), 0, 
																	(long) (moleculeXCenter + viewWidth*0.6/2), (long) (moleculeYCenter + viewHeight*0.6/2), 0) :
					Intervals.createMinMax( (long) (viewInterval.min(0) + viewWidth*0.2 ), (long) (viewInterval.min(1) + viewHeight*0.2 ), 0, 
																	(long) (viewInterval.min(0) + viewWidth*0.8 ), (long) (viewInterval.min(1) + viewHeight*0.8 ), 0);
		
		final Interval rangeInterval = Intervals.createMinMax( 0, 0, 0, archive.getMetadata(activeMetaUID).getImage(0).getSizeX(), archive.getMetadata(activeMetaUID).getImage(0).getSizeY(), 0 );
		backgroundThread.submit(() -> {
			final TransformedBoxSelectionDialog.Result result = BdvFunctions.selectBox(
				  bdv,
					imageTransform,
					initialInterval,
					rangeInterval,
					BoxSelectionOptions.options()
							.title( "Select region" )
							.selectTimepointRange()
							.initialTimepointRange( 0, numTimePoints - 1));
			if (result.isValid()) exportView(result.getInterval(), result.getMinTimepoint(), result.getMaxTimepoint());
		});
		backgroundThread.shutdown();
	}

	public void exportView(Interval interval, int minTimePoint, int maxTimePoint) {
		int numSources = bdvSourcesForExport.get(activeMetaUID).size();

		if (bdvSourcesForExport.get(activeMetaUID).stream().map(source -> source.getType()
			.getClass()).distinct().count() > 1)
		{
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					RoverErrorDialog alert = new RoverErrorDialog(
						((AbstractMoleculeArchiveFxFrame) archive.getWindow()).getNode()
							.getScene().getWindow(),
						"Could not create composite view because the sources are not all the same type (uint16, float32, ...).");
					alert.show();
				}
			});

			return;
		}

		List<RandomAccessibleInterval<T>> finalImages =
			new ArrayList<RandomAccessibleInterval<T>>();
		for (int i = 0; i < numSources; i++) {
			ArrayList<RandomAccessibleInterval<T>> raiList =
				new ArrayList<RandomAccessibleInterval<T>>();
			Source<T> bdvSource = bdvSourcesForExport.get(activeMetaUID).get(i);

			for (int t = minTimePoint; t <= maxTimePoint; t++) {

				// t, level, interpolation
				final RealRandomAccessible<T> raiRaw =
					(RealRandomAccessible<T>) bdvSource.getInterpolatedSource(t, 0,
						Interpolation.NLINEAR);

				// retrieve transform
				AffineTransform3D affine = new AffineTransform3D();
				bdvSource.getSourceTransform(t, 0, affine);
				final AffineRandomAccessible<T, AffineGet> rai = RealViews.affine(
					raiRaw, affine);
				RandomAccessibleInterval<T> view = Views.interval(Views.raster(rai),
					new long[] { interval.min(0), interval.min(1), 0 }, new long[] { interval.max(0), interval.max(1), 0 });

				raiList.add(Views.hyperSlice(view, 2, 0));
			}

			if (numSources > 1) finalImages.add(Views.addDimension(Views.stack(
				raiList), 0, 0));
			else finalImages.add(Views.stack(raiList));
		}

		String title = (molecule != null) ? "molecule " + molecule.getUID()
			: "BDV export (" + interval.min(0) + ", " + interval.min(1) + ", " + interval.max(0) + ", " + interval.max(1) + ")";

		AxisType[] axInfo = (numSources > 1) ? new AxisType[] { Axes.X, Axes.Y,
			Axes.TIME, Axes.CHANNEL } : new AxisType[] { Axes.X, Axes.Y, Axes.TIME };

		final RandomAccessibleInterval<T> rai = (numSources > 1) ? Views
			.concatenate(3, finalImages) : finalImages.get(0);

		final Img<T> img = Util.getSuitableImgFactory(rai, Util.getTypeFromInterval(
			rai)).create(rai);
		LoopBuilder.setImages(img, rai).multiThreaded().forEachPixel(Type::set);

		final ImgPlus<T> imgPlus = new ImgPlus<T>(img, title, axInfo);

		// Make sure these are run on the Swing EDT
		SwingUtilities.invokeLater(() -> {
			uiService.show(imgPlus);
			IJ.run(IJ.getImage(), "Enhance Contrast...", "saturated=0.35");
		});
	}

	/**
	 * The possible java array size is JVM dependent an actually slightly below
	 * Integer.MAX_VALUE. This is the same MAX_ARRAY_SIZE as used for example in
	 * ArrayList in OpenJDK8.
	 */
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	public static <T extends NativeType<T>> ImgFactory<T>
		getArrayOrCellImgFactory(final Dimensions targetSize,
			final int targetCellSize, final T type)
	{
		Fraction entitiesPerPixel = type.getEntitiesPerPixel();
		final long numElements = Intervals.numElements(targetSize);
		final long numEntities = entitiesPerPixel.mulCeil(numElements);
		if (numElements <= Integer.MAX_VALUE && numEntities <= MAX_ARRAY_SIZE)
			return new ArrayImgFactory<>(type);
		final int maxCellSize = (int) Math.pow(Math.min(MAX_ARRAY_SIZE /
			entitiesPerPixel.getRatio(), Integer.MAX_VALUE), 1.0 / targetSize
				.numDimensions());
		final int cellSize = Math.min(targetCellSize, maxCellSize);
		return new CellImgFactory<>(type, cellSize);
	}

	private Source<T> loadN5Source(MarsBdvSource source, MarsMetadata meta)
		throws IOException
	{
		N5Reader reader;
		if (n5Readers.containsKey(source.getPath())) {
			reader = n5Readers.get(source.getPath());
		}
		else {
			reader = new MarsN5ViewerReaderFun().apply(source.getPath());
			n5Readers.put(source.getPath(), reader);
		}

		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval wholeImage = N5Utils.open(reader, source
			.getN5Dataset());

		// wholeImage should be XYT or XYCT. If XYCT, we hyperSlice to get one
		// channel.
		// XYZCT should also be supported
		int dims = wholeImage.numDimensions();
		long[] dimensions = new long[dims];
		wholeImage.dimensions(dimensions);
		
		if (!bdvSourceDimensions.containsKey(meta.getUID())) {
			Map<String, long[]> dimensionsMap = new HashMap<String, long[]>();
			bdvSourceDimensions.put(meta.getUID(), dimensionsMap);
		}
		bdvSourceDimensions.get(meta.getUID()).put(source.getName(), dimensions);
		
		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval image = (dims > 3) ? Views.hyperSlice(
			wholeImage, wholeImage.numDimensions() - 2, source.getChannel())
			: wholeImage;

		int tSize = (int) image.dimension(image.numDimensions() - 1);

		if (tSize > numTimePoints) numTimePoints = tSize;
			
		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[1];
		images[0] = image;

		if (source.getSingleTimePointMode()) {
			AffineTransform3D[] transforms = new AffineTransform3D[tSize];

			// We don't drift correct single time point overlays
			// Drift should be corrected against them
			for (int t = 0; t < tSize; t++)
				transforms[t] = source.getAffineTransform3D();

			int singleTimePoint = source.getSingleTimePoint();
			@SuppressWarnings("unchecked")
			final MarsSingleTimePointN5Source<T> n5Source =
				new MarsSingleTimePointN5Source<>((T) Util.getTypeFromInterval(image),
					source.getName(), images, transforms, singleTimePoint);

			return n5Source;
		}
		else {
			AffineTransform3D[] transforms = new AffineTransform3D[tSize];

			for (int t = 0; t < tSize; t++) {
				if (source.getCorrectDrift()) {
					double dX = meta.getPlane(0, 0, 0, t).getXDrift();
					double dY = meta.getPlane(0, 0, 0, t).getYDrift();
					transforms[t] = source.getAffineTransform3D(dX, dY);
				}
				else transforms[t] = source.getAffineTransform3D();
			}

			@SuppressWarnings("unchecked")
			final MarsN5Source<T> n5Source = new MarsN5Source<>((T) Util
				.getTypeFromInterval(image), source.getName(), images, transforms);

			return n5Source;
		}

	}

	private Source<T> loadN5VolatileSource(MarsBdvSource source,
		MarsMetadata meta) throws IOException
	{
		N5Reader reader;
		if (n5Readers.containsKey(source.getPath())) {
			reader = n5Readers.get(source.getPath());
		}
		else {
			reader = new MarsN5ViewerReaderFun().apply(source.getPath());
			n5Readers.put(source.getPath(), reader);
		}

		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval wholeImage = N5Utils.openVolatile(reader,
			source.getN5Dataset());

		// wholeImage should be XYT or XYCT. If XYCT, we hyperSlice to get one
		// channel.
		// XYZCT should also be supported
		int dims = wholeImage.numDimensions();
		long[] dimensions = new long[dims];
		wholeImage.dimensions(dimensions);
		
		if (!bdvSourceDimensions.containsKey(meta.getUID())) {
			Map<String, long[]> dimensionsMap = new HashMap<String, long[]>();
			bdvSourceDimensions.put(meta.getUID(), dimensionsMap);
		}
		bdvSourceDimensions.get(meta.getUID()).put(source.getName(), dimensions);

		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval image = (dims > 3) ? Views.hyperSlice(
			wholeImage, wholeImage.numDimensions() - 2, source.getChannel())
			: wholeImage;

		int tSize = (int) image.dimension(image.numDimensions() - 1);

		if (tSize > numTimePoints) numTimePoints = tSize;

		@SuppressWarnings("rawtypes")
		final RandomAccessibleInterval[] images = new RandomAccessibleInterval[1];
		images[0] = image;

		if (source.getSingleTimePointMode()) {
			AffineTransform3D[] transforms = new AffineTransform3D[tSize];

			// We don't drift correct single time point overlays
			// Drift should be corrected against them
			for (int t = 0; t < tSize; t++)
				transforms[t] = source.getAffineTransform3D();

			int singleTimePoint = source.getSingleTimePoint();
			@SuppressWarnings("unchecked")
			final MarsSingleTimePointN5Source<T> n5Source =
				new MarsSingleTimePointN5Source<>((T) Util.getTypeFromInterval(image),
					source.getName(), images, transforms, singleTimePoint);

			return (Source) n5Source.asVolatile(sharedQueue);
		}
		else {
			AffineTransform3D[] transforms = new AffineTransform3D[tSize];

			for (int t = 0; t < tSize; t++) {
				if (source.getCorrectDrift()) {
					double dX = meta.getPlane(0, 0, 0, t).getXDrift();
					double dY = meta.getPlane(0, 0, 0, t).getYDrift();
					transforms[t] = source.getAffineTransform3D(dX, dY);
				}
				else transforms[t] = source.getAffineTransform3D();
			}

			@SuppressWarnings("unchecked")
			final MarsN5Source<T> n5Source = new MarsN5Source<>((T) Util
				.getTypeFromInterval(image), source.getName(), images, transforms);

			return (Source) n5Source.asVolatile(sharedQueue);
		}
	}

	private Source<T> loadAsSpimDataMinimal(MarsBdvSource source,
		MarsMetadata meta)
	{
		SpimDataMinimal spimData;
		try {
			spimData = new XmlIoSpimDataMinimal().load(source.getPath());

			// Add transforms to spimData...
			Map<ViewId, ViewRegistration> registrations = spimData
				.getViewRegistrations().getViewRegistrations();

			for (ViewId id : registrations.keySet()) {
				if (source.getCorrectDrift()) {
					double dX = meta.getPlane(0, 0, 0, id.getTimePointId()).getXDrift();
					double dY = meta.getPlane(0, 0, 0, id.getTimePointId()).getYDrift();
					registrations.get(id).getModel().set(source.getAffineTransform3D(dX,
						dY));
				}
				else registrations.get(id).getModel().set(source
					.getAffineTransform3D());
			}

			if (spimData.getSequenceDescription().getTimePoints()
				.size() > numTimePoints) numTimePoints = spimData
					.getSequenceDescription().getTimePoints().size();
			
			Source<T> spimSource = new SpimSource<T>(spimData, 0, source.getName());
			
			//There must be a better way to get the dimensions from a spimSource...
			RandomAccessibleInterval<T> img = spimSource.getSource(0, 0);
			
			int dims = img.numDimensions();
			long[] dimensions = new long[dims + 1];
			img.dimensions(dimensions);
			dimensions[dimensions.length - 1] = spimData.getSequenceDescription().getTimePoints().size();
			
			if (!bdvSourceDimensions.containsKey(meta.getUID())) {
				Map<String, long[]> dimensionsMap = new HashMap<String, long[]>();
				bdvSourceDimensions.put(meta.getUID(), dimensionsMap);
			}
			bdvSourceDimensions.get(meta.getUID()).put(source.getName(), dimensions);
			
			return spimSource;
		}
		catch (SpimDataException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void showHelp(boolean showHelp) {
		helpDialog.setVisible(showHelp);
	}

	public JFrame getFrame() {
		return frame;
	}

	public BdvHandle getBdvHandle() {
		return bdv;
	}
	
	public Molecule getSelectedMolecule() {
		return molecule;
	}

	public List<String> getSourceNames() {
		return bdvSourcesForExport.get(activeMetaUID).stream().map(source -> source.getName()).collect(Collectors.toList());
	}
	
	public Source<T> getSource(String name) {
		return bdvSourcesForExport.get(activeMetaUID).stream().filter(source -> source.getName().equals(name)).findFirst().get();
	}
	
	public long[] getSourceDimensions(String name) {
		return bdvSourceDimensions.get(activeMetaUID).get(name);
	}
	
	public int getMaximumTimePoints() {
		return numTimePoints;
	}
	
	public String getMetadataUID() {
		return activeMetaUID;
	}
	
	public void goTo(double x, double y) {
		setFullView();

		Dimension dim = bdv.getViewerPanel().getDisplay().getSize();
		viewerTransform = bdv.getViewerPanel().state().getViewerTransform();
		AffineTransform3D affine = viewerTransform;

		double[] source = new double[] {0, 0, 0};
		double[] target = new double[] {0, 0, 0};


		viewerTransform.apply(source, target);

		affine.set(affine.get(0, 3) - target[0], 0, 3);
		affine.set(affine.get(1, 3) - target[1], 1, 3);

		double scale = locationCard.getMagnification();

		// scale
		affine.scale(scale);

		source[0] = x;
		source[1] = y;
		source[2] = 0;

		affine.apply(source, target);

		affine.set(affine.get(0, 3) - target[0] + dim.getWidth() / 2, 0, 3);
		affine.set(affine.get(1, 3) - target[1] + dim.getHeight() / 2, 1, 3);

		bdv.getViewerPanel().state().setViewerTransform(affine);
	}
	
	public final Interval currentViewImageCoordinates() {
		Dimension dim = bdv.getViewerPanel().getDisplay().getSize();
		AffineTransform3D affine = bdv.getViewerPanel().state().getViewerTransform();
		
		double[] source = new double[]{0, 0, 0};
		double[] target = new double[]{0, 0, 0};

		affine.applyInverse(source, target);

		long x0 = (long) source[0];
		long y0 = (long) source[1];
		
		source[0] = 0;
		source[1] = 0;
		source[2] = 0;
		target[0] = dim.getWidth();
		target[1] = dim.getHeight();
		target[2] = 0;
		
		affine.applyInverse(source, target);
		
		long x1 = (long) source[0];
		long y1 = (long) source[1];

		return Intervals.createMinMax(x0, y0, x1, y1);
	}

	@Override
	protected void createIOMaps() {

		setJsonField("window", jGenerator -> {
			jGenerator.writeObjectFieldStart("window");
			jGenerator.writeNumberField("x", frame.getX());
			jGenerator.writeNumberField("y", frame.getY());
			jGenerator.writeNumberField("width", frame.getWidth());
			jGenerator.writeNumberField("height", frame.getHeight());
			jGenerator.writeEndObject();
		}, jParser -> {
			Rectangle rect = new Rectangle(0, 0, 800, 600);
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
				if ("x".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.x = jParser.getIntValue();
				}
				if ("y".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.y = jParser.getIntValue();
				}
				if ("width".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.width = jParser.getIntValue();
				}
				if ("height".equals(jParser.getCurrentName())) {
					jParser.nextToken();
					rect.height = jParser.getIntValue();
				}
			}

			frame.setBounds(rect);
			frame.setPreferredSize(new Dimension(rect.width, rect.height));

			windowStateLoaded = true;
		});

		setJsonField("cards", jGenerator -> {
			jGenerator.writeArrayFieldStart("cards");

			// Save location card settings
			jGenerator.writeStartObject();
			jGenerator.writeStringField("name", locationCard.getName());
			jGenerator.writeFieldName("settings");
			locationCard.toJSON(jGenerator);
			jGenerator.writeEndObject();

			// Then we save custom card settings
			for (MarsBdvCard card : cards) {
				jGenerator.writeStartObject();
				jGenerator.writeStringField("name", card.getName());
				jGenerator.writeFieldName("settings");
				card.toJSON(jGenerator);
				jGenerator.writeEndObject();
			}
			jGenerator.writeEndArray();
		}, jParser -> {
			while (jParser.nextToken() != JsonToken.END_ARRAY) {
				while (jParser.nextToken() != JsonToken.END_OBJECT) {
					MarsBdvCard card = null;

					if ("name".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						card = marsBdvCardService.createCard(jParser.getText());

						card.setArchive(archive);
						card.initialize();

						if (card instanceof LocationCard) locationCard =
							(LocationCard) card;
						else cards.add(card);
					}

					jParser.nextToken();

					if ("settings".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						if (card != null) card.fromJSON(jParser);
					}
				}
			}
		});

		setJsonField("sources", jGenerator -> {
			saveSourceDisplaySettings();
			jGenerator.writeArrayFieldStart("sources");
			for (String name : displaySettings.keySet())
				displaySettings.get(name).toJSON(jGenerator);
			jGenerator.writeEndArray();
		}, jParser -> {
			while (jParser.nextToken() != JsonToken.END_ARRAY) {
				SourceDisplaySettings settings = new SourceDisplaySettings(jParser);
				displaySettings.put(settings.name, settings);
			}
		});
	}

	private void saveSourceDisplaySettings() {
		for (SourceAndConverter<?> source : bdv.getViewerPanel().state()
			.getSources())
		{
			SourceDisplaySettings settings = new SourceDisplaySettings(source
				.getSpimSource().getName(), bdv.getConverterSetups().getConverterSetup(
					source));
			displaySettings.put(source.getSpimSource().getName(), settings);
		}
	}

	private void applySourceDisplaySettings() {
		for (SourceAndConverter<?> source : bdv.getViewerPanel().state()
			.getSources())
		{
			ConverterSetup converterSetup = bdv.getConverterSetups()
				.getConverterSetup(source);

			if (!displaySettings.containsKey(source.getSpimSource().getName()))
				continue;

			SourceDisplaySettings settings = displaySettings.get(source
				.getSpimSource().getName());
			converterSetup.setColor(new ARGBType(settings.color));
			converterSetup.setDisplayRange(settings.min, settings.max);
		}
	}
	
	public void releaseMemory() {
 		archive = null;
 		if (locationCard != null)
 			locationCard.setArchive(null);
 		for (MarsBdvCard card : cards)
 			card.setArchive(null);
 		if (molecule != null) molecule.setParent(null);
 		molecule = null;
 	}

	private class SourceDisplaySettings extends AbstractJsonConvertibleRecord {

		public String name;
		public int color;
		public double min, max;

		public SourceDisplaySettings(JsonParser jParser) throws IOException {
			super();
			fromJSON(jParser);
		}

		public SourceDisplaySettings(String name, ConverterSetup converterSetup) {
			this.name = name;
			this.color = converterSetup.getColor().get();
			this.min = converterSetup.getDisplayRangeMin();
			this.max = converterSetup.getDisplayRangeMax();
		}

		@Override
		protected void createIOMaps() {

			setJsonField("name", jGenerator -> jGenerator.writeStringField("name",
				name), jParser -> name = jParser.getText());

			setJsonField("color", jGenerator -> jGenerator.writeNumberField("color",
				color), jParser -> color = jParser.getIntValue());

			setJsonField("min", jGenerator -> jGenerator.writeNumberField("min", min),
				jParser -> min = jParser.getIntValue());

			setJsonField("max", jGenerator -> jGenerator.writeNumberField("max", max),
				jParser -> max = jParser.getIntValue());

		}
	}

	/**
	 * Get a "good" initial viewer transform. The viewer transform is chosen such
	 * that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane, at z = 0
	 * <li>centered and scaled such that the full <em>dim_x</em> by <em>dim_y</em>
	 * is visible.
	 * </ul>
	 *
	 * @param viewerWidth width of the viewer display
	 * @param viewerHeight height of the viewer display
	 * @param zoomedIn True if zoomed in.
	 * @param state the {@link ViewerState} containing at least one source.
	 * @return proposed initial viewer transform.
	 */
	public static AffineTransform3D initTransform(final int viewerWidth,
		final int viewerHeight, final boolean zoomedIn, final ViewerState state)
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		final double cX = viewerWidth / 2.0;
		final double cY = viewerHeight / 2.0;

		final SourceAndConverter<?> current = state.getCurrentSource();
		if (current == null) return viewerTransform;
		final Source<?> source = current.getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		if (!source.isPresent(timepoint)) return viewerTransform;

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform(timepoint, 0, sourceTransform);

		final Interval sourceInterval = source.getSource(timepoint, 0);
		final double sX0 = sourceInterval.min(0);
		final double sX1 = sourceInterval.max(0);
		final double sY0 = sourceInterval.min(1);
		final double sY1 = sourceInterval.max(1);
		// final double sZ0 = sourceInterval.min( 2 );
		// final double sZ1 = sourceInterval.max( 2 );
		final double sX = (sX0 + sX1 + 1) / 2;
		final double sY = (sY0 + sY1 + 1) / 2;
		final double sZ = 0;// ( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[3][4];

		// rotation
		final double[] qSource = new double[4];
		final double[] qViewer = new double[4];
		Affine3DHelpers.extractApproximateRotationAffine(sourceTransform, qSource,
			2);
		LinAlgHelpers.quaternionInvert(qSource, qViewer);
		LinAlgHelpers.quaternionToR(qViewer, m);

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[3];
		final double[] translation = new double[3];
		sourceTransform.apply(centerSource, centerGlobal);
		LinAlgHelpers.quaternionApply(qViewer, centerGlobal, translation);
		LinAlgHelpers.scale(translation, -1, translation);
		LinAlgHelpers.setCol(3, translation, m);

		viewerTransform.set(m);

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[3];
		final double[] pScreen = new double[3];
		sourceTransform.apply(pSource, pGlobal);
		viewerTransform.apply(pGlobal, pScreen);
		final double scaleX = cX / pScreen[0];
		final double scaleY = cY / pScreen[1];
		final double scale;
		if (zoomedIn) scale = Math.max(scaleX, scaleY);
		else scale = Math.min(scaleX, scaleY);
		viewerTransform.scale(scale);

		// window center offset
		viewerTransform.set(viewerTransform.get(0, 3) + cX, 0, 3);
		viewerTransform.set(viewerTransform.get(1, 3) + cY, 1, 3);
		return viewerTransform;
	}

	public static void initBrightness(final double cumulativeMinCutoff,
		final double cumulativeMaxCutoff, final ViewerState state,
		final ConverterSetups converterSetups)
	{
		final SourceAndConverter<?> current = state.getCurrentSource();
		if (current == null) return;
		final Source<?> source = current.getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		final Bounds bounds = estimateSourceRange(source, timepoint,
			cumulativeMinCutoff, cumulativeMaxCutoff);
		for (SourceAndConverter<?> s : state.getSources()) {
			if (s.getSpimSource().getName().equals("Track") || s.getSpimSource()
				.getName().equals("Location")) continue;

			final ConverterSetup setup = converterSetups.getConverterSetup(s);
			setup.setDisplayRange(bounds.getMinBound(), bounds.getMaxBound());
		}
	}

	/**
	 * @param source the source.
	 * @param timepoint time point.
	 * @param cumulativeMinCutoff fraction of pixels that are allowed to be
	 *          saturated at the lower end of the range.
	 * @param cumulativeMaxCutoff fraction of pixels that are allowed to be
	 *          saturated at the upper end of the range.
	 * @return The bounds.
	 */
	public static Bounds estimateSourceRange(final Source<?> source,
		final int timepoint, final double cumulativeMinCutoff,
		final double cumulativeMaxCutoff)
	{
		final Object type = source.getType();

		if ((type instanceof UnsignedShortType ||
			type instanceof VolatileUnsignedShortType) && source.isPresent(timepoint))
		{
			@SuppressWarnings("unchecked")
			final RandomAccessibleInterval<UnsignedShortType> img =
				(RandomAccessibleInterval<UnsignedShortType>) source.getSource(
					timepoint, source.getNumMipmapLevels() - 1);
			final long z = (img.min(2) + img.max(2) + 1) / 2;

			final int numBins = 6535;
			final Histogram1d<?> histogram = new Histogram1d<>(Views.hyperSlice(img,
				2, z), new Real1dBinMapper<>(0, 65535, numBins, false));
			final DiscreteFrequencyDistribution dfd = histogram.dfd();
			final long[] bin = new long[] { 0 };
			double cumulative = 0;
			int i = 0;
			for (; i < numBins && cumulative < cumulativeMinCutoff; ++i) {
				bin[0] = i;
				cumulative += dfd.relativeFrequency(bin);
			}
			final int min = i * 65535 / numBins;
			for (; i < numBins && cumulative < cumulativeMaxCutoff; ++i) {
				bin[0] = i;
				cumulative += dfd.relativeFrequency(bin);
			}
			final int max = i * 65535 / numBins;
			return new Bounds(min, max);
		}
		else if (type instanceof UnsignedByteType) return new Bounds(0, 255);
		else {
			return new Bounds(0, 65535);
		}
	}
}
