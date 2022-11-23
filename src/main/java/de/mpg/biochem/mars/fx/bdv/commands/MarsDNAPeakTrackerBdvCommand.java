package de.mpg.biochem.mars.fx.bdv.commands;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;

import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.KDTree;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.scijava.Initializable;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.command.Previewable;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DoubleColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOverlay;
import bdv.util.BdvOverlaySource;
import bdv.viewer.Source;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.image.DNASegment;
import de.mpg.biochem.mars.image.MarsImageUtils;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.metadata.MarsOMEMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.SingleMolecule;
import de.mpg.biochem.mars.molecule.SingleMoleculeArchive;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;

@Plugin(type = Command.class, label = "Bdv Peak Tracker")
public class MarsDNAPeakTrackerBdvCommand extends InteractiveCommand implements Command,
Initializable, Previewable
{

	/**
	 * SERVICES
	 */
	@Parameter
	private LogService logService;
	
	@Parameter
	private ConvertService convertService;
	
	@Parameter
	private OpService opService;
	
	@Parameter
	private UIService uiService;
	
	/**
	 * IMAGE
	 */
	private MarsBdvFrame marsBdvFrame;
	
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	/**
	 * INPUT SETTINGS
	 */
	
	@Parameter(visibility = ItemVisibility.MESSAGE,
		style = "groupLabel, tabbedPaneWidth:450", persist = false)
	private String inputGroup = "Input";
	
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "image, group:Input",
		persist = false)
	private String inputFigure = "ImageInput.png";
	
	@Parameter(label = "Source", choices = { "a", "b", "c" },
		style = "group:Input", persist = false)
	private String source = "";
		
	@Parameter(label = "Select region", style = "group:Input, align:center",
		description = "Select region to search for DNAs",
		callback = "setSelectionRegion", persist = false)
	private Button regionSelectionButton;
	
	/**
	 * FINDER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String findGroup = "Find";
	
	@Parameter(label = "DoG filter", style = "group:Find")
	private boolean useDogFilter = true;
	
	@Parameter(label = "DoG radius", style = "group:Find")
	private double dogFilterRadius = 2;
	
	@Parameter(label = "Threshold", style = "group:Find")
	private double threshold = 50;
	
	@Parameter(label = "Peak separation", style = "group:Find")
	private int minimumDistance = 4;
	
	/**
	 * FITTER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String fitGroup = "Fit";
	
	@Parameter(label = "Radius", style = "group:Fit")
	private int fitRadius = 3;
	
	@Parameter(label = "R-squared", style = NumberWidget.SLIDER_STYLE +
		", group:Fit", min = "0.00", max = "1.00", stepSize = "0.01")
	private double RsquaredMin = 0;
	
	/**
	 * TRACKER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String trackGroup = "Track";
	
	@Parameter(label = "Max ΔX", style = "group:Track")
	private double maxDifferenceX = 1;
	
	@Parameter(label = "Max ΔY", style = "group:Track")
	private double maxDifferenceY = 1;
	
	@Parameter(label = "Max ΔT", style = "group:Track")
	private int maxDifferenceT = 1;
	
	@Parameter(label = "Minimum length", style = "group:Track")
	private int minTrajectoryLength = 100;
	
	/**
	 * INTEGRATION SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String integrateGroup = "Integrate";
	
	@Parameter(label = "Integrate", style = "group:Integrate")
	private boolean integrate = false;
	
	@Parameter(label = "Inner radius", style = "group:Integrate")
	private int integrationInnerRadius = 3;
	
	@Parameter(label = "Outer radius", style = "group:Integrate")
	private int integrationOuterRadius = 12;
	
	/**
	 * OUTPUT SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String outputGroup = "Output";
	
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "group:Output")
	private final String excludeTitle =
		"List of time points to exclude (T0, T1-T2, ...)";
	
	@Parameter(label = "Exclude", style = "group:Output", required = false)
	private String excludeTimePointList = "";
	
	@Parameter(label = "Replace all tracks", style = "group:Output")
	private boolean replaceAllTracks = false;
	
	@Parameter(label = "DNA length in bps", style = "group:Search Parameters")
	private int DNALength = 21236;
	
	@Parameter(label = "Search radius around DNA",
			style = "group:Search Parameters")
	private double radius;
	
	/**
	 * Global Settings
	 */

	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "previewChanged")
	private boolean preview = false;
	
	@Parameter(label = "Add tracks to DNA molecule records",
			description = "Add tracks to DNA molecule",
			callback = "addTracksToArchive", persist = false)
	private Button addTracksButton;
	
	//@Parameter(label = "Timeout (s)", style = "group:Preview")
	//private int previewTimeout = 10;
	
	private Interval interval;
	private PeakPreviewOverlay previewOverlay;
	private BdvOverlaySource<?> overlaySource;
	private boolean activeOverlay = false;
	private boolean selectionInProgress = false;
	
	@Override
	public void initialize() {
		final MutableModuleItem<String> channelItems = getInfo().getMutableInput(
			"source", String.class);
		channelItems.setChoices(marsBdvFrame.getSourceNames());
		
		if (marsBdvFrame != null) {
			final Interval viewInterval = marsBdvFrame.currentViewImageCoordinates();
			long viewWidth = (long) (viewInterval.max(0) - viewInterval.min(0));
			long viewHeight = (long) (viewInterval.max(1) - viewInterval.min(1));
			interval = Intervals.createMinMax( (long) (viewInterval.min(0) + viewWidth*0.2 ), (long) (viewInterval.min(1) + viewHeight*0.2 ), 0, 
								                         (long) (viewInterval.min(0) + viewWidth*0.8 ), (long) (viewInterval.min(1) + viewHeight*0.8 ), 0);
		}
	}
	
	@Override
	public void run() {
		//command is run interactively using buttons.
	}
	
	protected void addTracksToArchive() {
		//save the current settings to the PrefService 
		//so they are reloaded the next time the command is opened.
		saveInputs();
		if (archive != null) {
			List<int[]> excludeTimePoints = new ArrayList<int[]>();
			if (excludeTimePointList.length() > 0) {
				try {
					final String[] excludeArray = excludeTimePointList.split(",");
					for (int i = 0; i < excludeArray.length; i++) {
						String[] endPoints = excludeArray[i].split("-");
						int start = Integer.valueOf(endPoints[0].trim());
						int end = (endPoints.length > 1) ? Integer.valueOf(endPoints[1]
							.trim()) : start;
		
						excludeTimePoints.add(new int[] { start, end });
					}
				}
				catch (NumberFormatException e) {
					logService.info(
						"NumberFormatException encountered when parsing exclude list. Tracking all time points.");
					excludeTimePoints = new ArrayList<int[]>();
				}
			}
			
			long[] dims = marsBdvFrame.getSourceDimensions(source);
		
			long timepoints = dims[dims.length - 1];
			
			ConcurrentMap<Integer, List<Peak>> peaks = new ConcurrentHashMap<Integer, List<Peak>>();
			
			List<Integer> processTimePoints = new ArrayList<Integer>();
			List<Runnable> tasks = new ArrayList<Runnable>();
			for (int t = 0; t < timepoints; t++) {
				boolean processedTimePoint = true;
				for (int index = 0; index < excludeTimePoints.size(); index++)
					if (excludeTimePoints.get(index)[0] <= t && t <= excludeTimePoints.get(
						index)[1])
					{
						processedTimePoint = false;
						break;
					}

				if (processedTimePoint) {
					List<Peak> peaksInT = findPeaksInT(t, useDogFilter, integrate);
					if (peaksInT.size() > 0) {
						processTimePoints.add(t);
						final int theT = t;
						
						tasks.add(() -> peaks.put(theT, peaksInT));
					}
				}
			}

			try {
				ExecutorService threadPool = Executors.newFixedThreadPool(1);
				tasks.forEach(task -> threadPool.submit(task));
				threadPool.shutdown();
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
			catch (InterruptedException exc) {
				exc.printStackTrace();
			}
			
			PeakTracker tracker = new PeakTracker(maxDifferenceX, maxDifferenceY, maxDifferenceT,
				minimumDistance, minTrajectoryLength, true, logService, 1);
			
			//Need to make a temporary SingleMoleculeArchive for the tracking results
			SingleMoleculeArchive tracksArchive = new SingleMoleculeArchive("tracking results container");
			
			
			MarsOMEMetadata metadata = (MarsOMEMetadata) archive.getMetadata(marsBdvFrame.getMetadataUID());
			//We set the metadata to point at the current metadata record. This will allow for setting dt correcting.
			tracksArchive.putMetadata(metadata);
			
			//We are assuming the channel in N5 is the same as the channel from the original metadata hmmm.... 
			tracker.track(peaks, tracksArchive, metadata.getBdvSource(source).getChannel(), processTimePoints, 1);
			
			if (tracksArchive.getNumberOfMolecules() == 0) {
				logService.info("MarsDNAPeakTrackerBdvCommand: No tracks found for current settings.");
				logService.info("DnaMolecule " + marsBdvFrame.getSelectedMolecule().getUID() + " left unchanged.");
				return;
			}
			
			RadiusNeighborSearchOnKDTree<MoleculePosition> archive1PositionSearcher = getMoleculeSearcher(tracksArchive);
			
			archive.getWindow().lock();
			
			Molecule dnaMolecule = marsBdvFrame.getSelectedMolecule();
			MarsTable dnaMoleculeTable = (replaceAllTracks) ? new MarsTable() : dnaMolecule.getTable();
			
			DNASegment dnaSegment = new DNASegment(dnaMolecule.getParameter("Dna_Top_X1"), dnaMolecule.getParameter("Dna_Top_Y1"),
																						 dnaMolecule.getParameter("Dna_Bottom_X2"), dnaMolecule.getParameter("Dna_Bottom_Y2"));
			
			List<SingleMolecule> moleculesOnDNA = findMoleculesOnDna(
				archive1PositionSearcher, tracksArchive, dnaSegment);
			
			if (moleculesOnDNA.size() != 0) addTracksToDnaMoleculeTable(dnaMoleculeTable, moleculesOnDNA, source, dnaSegment);
	
			dnaMolecule.setParameter("Number_" + source, moleculesOnDNA.size());
			dnaMolecule.setTable(dnaMoleculeTable);
			dnaMolecule.setNotes("Tracks added on " + new java.util.Date() + " by the MarsDNAPeakTrackerBdvCommand");
			//The molecule is already in the archive but this updates indexes and could work in virtual mode.
			archive.put(dnaMolecule);
			
			LogBuilder builder = new LogBuilder();
			String log = LogBuilder.buildTitleBlock(getInfo().getLabel());
			builder.addParameter("DnaMolecule UID", dnaMolecule.getUID());
			builder.addParameter("Tracks added", moleculesOnDNA.size());
			addInputParameterLog(builder);
			log += builder.buildParameterList();
			log += "\n" + LogBuilder.endBlock();
			archive.getMetadata(marsBdvFrame.getMetadataUID()).logln(log);
			
			archive.getWindow().unlock();
		}
	}
	
	private void addInputParameterLog(LogBuilder builder) {
		builder.addParameter("Metadata UID", marsBdvFrame.getMetadataUID());
		builder.addParameter("Region", "(" + interval.min(0) + ", " + interval.min(1) + ") to (" + interval.max(0) + ", " + interval.max(1) + ")");
		builder.addParameter("Source", source);
		builder.addParameter("Use DoG filter", String.valueOf(useDogFilter));
		builder.addParameter("DoG filter radius", String.valueOf(dogFilterRadius));
		builder.addParameter("Threshold", String.valueOf(threshold));
		builder.addParameter("Minimum distance", String.valueOf(minimumDistance));
		builder.addParameter("Fit radius", String.valueOf(fitRadius));
		builder.addParameter("Minimum R-squared", String.valueOf(RsquaredMin));
		builder.addParameter("Max difference X", String.valueOf(maxDifferenceX));
		builder.addParameter("Max difference Y", String.valueOf(maxDifferenceY));
		builder.addParameter("Max difference T", String.valueOf(maxDifferenceT));
		builder.addParameter("Minimum track length", String.valueOf(
			minTrajectoryLength));
		builder.addParameter("Integrate", String.valueOf(integrate));
		builder.addParameter("Integration inner radius", String.valueOf(
			integrationInnerRadius));
		builder.addParameter("Integration outer radius", String.valueOf(
			integrationOuterRadius));
		builder.addParameter("Exclude time points", excludeTimePointList);
		builder.addParameter("Replace all tracks", replaceAllTracks);
	}
	
	private void addTracksToDnaMoleculeTable(MarsTable mergedTable, List<SingleMolecule> moleculesOnDNA,
		String name, DNASegment dnaSegment)
	{
		//If we are not removing all tracks, we just need to remove all tracks for the current channel
		if (mergedTable.getColumnCount() > 0) {
			List<String> oldTrackColumnNames = new ArrayList<>();
			mergedTable.stream().filter(col -> col.getHeader().startsWith(name + "_")).forEach(col -> oldTrackColumnNames.add(col.getHeader()));
			for (String header : oldTrackColumnNames)
				mergedTable.removeColumn(header);
		}
		
		int rows = mergedTable.getRowCount();
		for (SingleMolecule molecule : moleculesOnDNA)
			if(molecule.getTable().getRowCount() > rows)
				rows = molecule.getTable().getRowCount();
		
	  // We need to make sure to fill the table with Double.NaN values
		// this will over write the scijava default value of 0.0.
		if (mergedTable.getRowCount() < rows)
		{
			for (int row = mergedTable.getRowCount(); row < rows; row++)
			{
				mergedTable.appendRow();
				for (int col = 0; col < mergedTable.getColumnCount(); col++)
					mergedTable.setValue(col, row, Double.NaN);
			}
		}
		
		int index = 1;
		for (SingleMolecule molecule : moleculesOnDNA) {
			MarsTable table = molecule.getTable().clone();
			
			if (rows > table.getRowCount())
			{
				for (int row = table.getRowCount(); row < mergedTable
					.getRowCount(); row++)
				{
					table.appendRow();
					for (int col = 0; col < table.getColumnCount(); col++)
						table.setValue(col, row, Double.NaN);
				}
			}

			// Distance from the DNA top END
			DoubleColumn dnaPositionColumn = new DoubleColumn(name + "_" + index +
				"_Position_on_DNA");
			for (int row = 0; row < rows; row++) {
				if (row < table.getRowCount())
					dnaPositionColumn.add(dnaSegment.getPositionOnDNA(table.getValue(
						Peak.X, row), table.getValue(Peak.Y, row), DNALength));
				else
					dnaPositionColumn.add(Double.NaN);
			}
			
			for (int col = 0; col < table.getColumnCount(); col++) {
				table.get(col).setHeader(name + "_" + index + "_" + table.get(col)
					.getHeader());
				mergedTable.add(table.get(col));
			}
			
			mergedTable.add(dnaPositionColumn);

			index++;
		}
	}
	
	private ArrayList<SingleMolecule> findMoleculesOnDna(
		RadiusNeighborSearchOnKDTree<MoleculePosition> archivePositionSearcher,
		SingleMoleculeArchive tracksArchive, DNASegment dnaSegment)
	{
		ArrayList<SingleMolecule> moleculesLocated =
			new ArrayList<SingleMolecule>();

		archivePositionSearcher.search(dnaSegment, radius + dnaSegment.getLength() / 2,
			false);

		// build DNA fit
		double x1 = dnaSegment.getX1();
		double y1 = dnaSegment.getY1();

		double x2 = dnaSegment.getX2();
		double y2 = dnaSegment.getY2();

		for (int j = 0; j < archivePositionSearcher.numNeighbors(); j++) {
			MoleculePosition moleculePosition = archivePositionSearcher.getSampler(j)
				.get();

			double distance;

			// Before we add the the molecules we need to constrain positions to just
			// within radius of DNA....
			if (moleculePosition.getY() < y1) {
				// the molecules is above the DNA
				distance = Math.sqrt((moleculePosition.getX() - x1) * (moleculePosition
					.getX() - x1) + (moleculePosition.getY() - y1) * (moleculePosition
						.getY() - y1));
			}
			else if (moleculePosition.getY() > y2) {
				distance = Math.sqrt((moleculePosition.getX() - x2) * (moleculePosition
					.getX() - x2) + (moleculePosition.getY() - y2) * (moleculePosition
						.getY() - y2));
			}
			else {
				// find the x center position of the DNA for the molecule y position.

				// If there is no intercept just take top x1.
				double DNAx;
				if (x1 == x2) DNAx = x1;
				else {
					SimpleRegression linearFit = new SimpleRegression(true);
					linearFit.addData(x1, y1);
					linearFit.addData(x2, y2);
					DNAx = (moleculePosition.getY() - linearFit.getIntercept()) / linearFit.getSlope();
				}

				distance = Math.abs(moleculePosition.getX() - DNAx);
			}

			// other conditions
			if (distance < radius) {
				moleculesLocated.add(tracksArchive.get(moleculePosition.getUID()));
			}
		}

		return moleculesLocated;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<Peak> findPeaksInT(
		int t, boolean useDogFilter, boolean integrate)
	{
		Source<T> bdvSource = marsBdvFrame.getSource(source);

		//Remove the Z dimension
		RandomAccessibleInterval<T> img = Views.hyperSlice(bdvSource.getSource(t, 0), 2, 0);
		
		//We need to transform the interval to the correct position in the img...
		//Then we need to transform the results back...
		final AffineTransform3D bdvSourceTransform = new AffineTransform3D();
		bdvSource.getSourceTransform(t, 0, bdvSourceTransform);
		
		Interval transformedInterval = getTransformedInterval(interval, bdvSourceTransform);
		RandomAccessibleInterval<T> imgView = Views.interval(img, Intervals.createMinMax(transformedInterval.min(0), transformedInterval.min(1), transformedInterval.max(0), transformedInterval.max(1))); 
		
		RandomAccessibleInterval<FloatType> filteredImg = (useDogFilter) ? MarsImageUtils.dogFilter(imgView, dogFilterRadius, 1) : null;

		List<Peak> peaks = new ArrayList<Peak>();
		
		if (useDogFilter) peaks = MarsImageUtils.findPeaks(filteredImg, filteredImg, t, threshold, minimumDistance, false);
		else peaks = MarsImageUtils.findPeaks(imgView, imgView, t, threshold, minimumDistance, false);

		peaks = MarsImageUtils.fitPeaks(imgView, imgView, peaks, fitRadius,
			dogFilterRadius, false, RsquaredMin);
		peaks = MarsImageUtils.removeNearestNeighbors(peaks, minimumDistance);

		if (integrate) MarsImageUtils.integratePeaks(imgView, imgView, peaks,
			integrationInnerRadius, integrationOuterRadius);
		
		//Now we transform from the original image coordinates to the BDV view coordinates.
		for (Peak peak : peaks) {
			double[] source = new double[] { peak.getX(), peak.getY(), 0 };
			double[] target = new double[3];
			bdvSourceTransform.apply(source, target);
			peak.setX(target[0]);
			peak.setY(target[1]);
		}

		return peaks;
	}
	
	private static Interval getTransformedInterval(Interval inter, AffineTransform3D transform) {
		double[] minInterval = new double[] {inter.min(0), inter.min(1), 0};
		double[] transformedMinInterval = new double[3];
		transform.applyInverse(transformedMinInterval, minInterval);
		
		double[] maxInterval = new double[] {inter.max(0), inter.max(1), 0};
		double[] transformedMaxInterval = new double[3];
		transform.applyInverse(transformedMaxInterval, maxInterval);
		
		return Intervals.createMinMax( (long) transformedMinInterval[0], (long) transformedMinInterval[1], 0, 
																	 (long) transformedMaxInterval[0], (long) transformedMaxInterval[1], 0);
	}
	
	@Override
	public void preview() {
		if (preview) {
			if (previewOverlay == null) previewOverlay = new PeakPreviewOverlay();
			if (!activeOverlay) {
				overlaySource = BdvFunctions.showOverlay(previewOverlay, "Peak-Preview", Bdv
								                    .options().addTo(marsBdvFrame.getBdvHandle()));
				activeOverlay = true;
			}
			
			//HACK Find the frame and add window listener to make sure 
			//the preview is removed when the window is closed.
		  for (Window window : Window.getWindows())
				if (window instanceof JDialog && ((JDialog) window).getTitle()
					.equals(getInfo().getLabel())) window.addWindowListener(new java.awt.event.WindowAdapter() {
						    @Override
						    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
						        cancel();
						    }
						});
		}
	}
	
	@Override
	public void cancel() {
		if (activeOverlay && overlaySource != null) overlaySource.removeFromBdv();
		if (activeOverlay && previewOverlay != null) marsBdvFrame.getBdvHandle().getViewerPanel().getDisplay().overlays().remove( previewOverlay );
		activeOverlay = false;
	}
	
	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		if (!preview) cancel();
	}

	protected void setSelectionRegion() {
		if (selectionInProgress) return;
		final Interval viewInterval = marsBdvFrame.currentViewImageCoordinates();
		long viewWidth = (long) (viewInterval.max(0) - viewInterval.min(0));
		long viewHeight = (long) (viewInterval.max(1) - viewInterval.min(1));

		final AffineTransform3D imageTransform = new AffineTransform3D();
		//We must give the dialog the image transform. This should be no transform 
		//because we always build the view around one channel that is not transformed.
		imageTransform.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
		
		ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
		final Interval initialInterval = Intervals.createMinMax( (long) (viewInterval.min(0) + viewWidth*0.2 ), (long) (viewInterval.min(1) + viewHeight*0.2 ), 0, 
																	                           (long) (viewInterval.min(0) + viewWidth*0.8 ), (long) (viewInterval.min(1) + viewHeight*0.8 ), 0);
		MarsMetadata metadata = archive.getMetadata(marsBdvFrame.getMetadataUID());
		final Interval rangeInterval = Intervals.createMinMax( 0, 0, 0, metadata.getImage(0).getSizeX(), metadata.getImage(0).getSizeY(), 0 );
		backgroundThread.submit(() -> {
			selectionInProgress = true;
			final TransformedBoxSelectionDialog.Result result = BdvFunctions.selectBox(
				  marsBdvFrame.getBdvHandle(),
					imageTransform,
					initialInterval,
					rangeInterval,
					BoxSelectionOptions.options()
							.title( "Select region" ));
			selectionInProgress = false;
			if (result.isValid()) interval = result.getInterval();
		});
		backgroundThread.shutdown();
	}
	
	private RadiusNeighborSearchOnKDTree<MoleculePosition> getMoleculeSearcher(
		SingleMoleculeArchive archive)
	{
		ArrayList<MoleculePosition> moleculePositionList =
			new ArrayList<MoleculePosition>();

		archive.molecules().forEach(molecule -> moleculePositionList.add(
			new MoleculePosition(molecule.getUID(), molecule.getTable().median(
				Peak.X), molecule.getTable().median(Peak.Y))));

		KDTree<MoleculePosition> moleculesTree = new KDTree<MoleculePosition>(
			moleculePositionList, moleculePositionList);

		return new RadiusNeighborSearchOnKDTree<MoleculePosition>(moleculesTree);
	}
	
	public class PeakPreviewOverlay extends BdvOverlay {
		private Color intersectionFillColor = new Color( 0x88994499, true );
		//https://github.com/bigdataviewer/bigdataviewer-core/blob/9d54b96f2b789ccd21e828db17cd41944bd18704/src/main/java/bdv/tools/boundingbox/TransformedBoxOverlay.java
		
		public PeakPreviewOverlay() {}

		@Override
		protected void draw(Graphics2D g) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);
			
			Interval selection = selectionToViewTransform(transform);
			g.setPaint( intersectionFillColor );
			g.fillRect((int) selection.min(0), (int) selection.min(1), (int)(selection.max(0) - selection.min(0)), (int)(selection.max(1) - selection.min(1)));
			
		  List<Peak> peaks = findPeaksInT(info.getTimePointIndex(), true, true);
		  
			if (peaks.size() > 0) {
				g.setColor(getColor());
				g.setStroke(new BasicStroke(2));
				
				for (Peak peak : peaks) {
					final double vx = transform.get(0, 0);
					final double vy = transform.get(1, 0);
					final double transformScale = Math.sqrt(vx * vx + vy * vy);

					final double[] globalCoords = new double[] { peak.getX(), peak.getY() };
					final double[] viewerCoords = new double[2];
					transform.apply(globalCoords, viewerCoords);
					
					//TODO Should we add an input for the preview radius? instead of hard coding to 5?
					final double rad = 5 * transformScale;

					final double arad = Math.sqrt(rad * rad);
					g.drawOval((int) (viewerCoords[0] - arad), (int) (viewerCoords[1] - arad),
						(int) (2 * arad), (int) (2 * arad));
				}
			}
		}
		
		private Interval selectionToViewTransform(AffineTransform2D transform) {
			final double[] globalCoords = new double[] {interval.min(0), interval.min(1)};
			final double[] viewerCoords = new double[2];
			transform.apply(globalCoords, viewerCoords);

			int x1 = (int) Math.round(viewerCoords[0]);
			int y1 = (int) Math.round(viewerCoords[1]);

			final double[] globalCoords2 = new double[] { interval.max(0), interval.max(1) };
			final double[] viewerCoords2 = new double[2];
			transform.apply(globalCoords2, viewerCoords2);

			int x2 = (int) Math.round(viewerCoords2[0]);
			int y2 = (int) Math.round(viewerCoords2[1]);
			
			return Intervals.createMinMax( (long) x1, (long) y1, 
        														 (long) x2, (long) y2);
		}

		private Color getColor() {
			int alpha = (int) info.getDisplayRangeMax();

			if (alpha > 255 || alpha < 0) alpha = 255;

			final int r = ARGBType.red(info.getColor().get());
			final int g = ARGBType.green(info.getColor().get());
			final int b = ARGBType.blue(info.getColor().get());
			return new Color(r, g, b, alpha);
		}
	}
	
	public void setMarsBdvFrame(MarsBdvFrame marsBdvFrame) {
		this.marsBdvFrame = marsBdvFrame;
	}
	
	public MarsBdvFrame getMarsBdvFrame() {
		return marsBdvFrame;
	}
	
	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
	}
	
	public void setUseDogFiler(boolean useDogFilter) {
		this.useDogFilter = useDogFilter;
	}
	
	public void setDogFilterRadius(double dogFilterRadius) {
		this.dogFilterRadius = dogFilterRadius;
	}
	
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
	
	public double getThreshold() {
		return threshold;
	}
	
	public void setMinimumDistance(int minimumDistance) {
		this.minimumDistance = minimumDistance;
	}
	
	public int getMinimumDistance() {
		return minimumDistance;
	}
	
	public void setFitRadius(int fitRadius) {
		this.fitRadius = fitRadius;
	}
	
	public int getFitRadius() {
		return fitRadius;
	}
	
	public void setMinimumRsquared(double Rsquared) {
		this.RsquaredMin = Rsquared;
	}
	
	public double getMinimumRsquared() {
		return RsquaredMin;
	}
	
	public void setMaxDifferenceX(double PeakTracker_maxDifferenceX) {
		this.maxDifferenceX = PeakTracker_maxDifferenceX;
	}
	
	public double getMaxDifferenceX() {
		return maxDifferenceX;
	}
	
	public void setMaxDifferenceY(double maxDifferenceY) {
		this.maxDifferenceY = maxDifferenceY;
	}
	
	public double getMaxDifferenceY() {
		return maxDifferenceY;
	}
	
	public void setMaxDifferenceT(int maxDifferenceT) {
		this.maxDifferenceT = maxDifferenceT;
	}
	
	public int getMaxDifferenceT() {
		return maxDifferenceT;
	}
	
	public void setMinimumTrackLength(int minTrajectoryLength) {
		this.minTrajectoryLength = minTrajectoryLength;
	}
	
	public int getMinimumTrackLength() {
		return minTrajectoryLength;
	}
	
	public void setIntegrate(boolean integrate) {
		this.integrate = integrate;
	}
	
	public boolean getIntegrate() {
		return integrate;
	}
	
	public void setIntegrationInnerRadius(int integrationInnerRadius) {
		this.integrationInnerRadius = integrationInnerRadius;
	}
	
	public int getIntegrationInnerRadius() {
		return integrationInnerRadius;
	}
	
	public void setIntegrationOuterRadius(int integrationOuterRadius) {
		this.integrationOuterRadius = integrationOuterRadius;
	}
	
	public int getIntegrationOuterRadius() {
		return integrationOuterRadius;
	}
	
	public void setExcludedTimePointsList(String excludeTimePointList) {
		this.excludeTimePointList = excludeTimePointList;
	}
	
	public String getExcludedTimePointsList() {
		return this.excludeTimePointList;
	}
	
	class MoleculePosition implements RealLocalizable {

		private String UID;

		private double x, y;

		public MoleculePosition(String UID, double x, double y) {
			this.UID = UID;
			this.x = x;
			this.y = y;
		}

		public String getUID() {
			return UID;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		// Override from RealLocalizable interface.. so peaks can be passed to
		// KDTree and other imglib2 functions.
		@Override
		public int numDimensions() {
			// We make no effort to think beyond 2 dimensions !
			return 2;
		}

		@Override
		public double getDoublePosition(int arg0) {
			if (arg0 == 0) {
				return x;
			}
			else if (arg0 == 1) {
				return y;
			}
			else {
				return -1;
			}
		}

		@Override
		public float getFloatPosition(int arg0) {
			if (arg0 == 0) {
				return (float) x;
			}
			else if (arg0 == 1) {
				return (float) y;
			}
			else {
				return -1;
			}
		}

		@Override
		public void localize(float[] arg0) {
			arg0[0] = (float) x;
			arg0[1] = (float) y;
		}

		@Override
		public void localize(double[] arg0) {
			arg0[0] = x;
			arg0[1] = y;
		}
	}
}

