package de.mpg.biochem.mars.fx.bdv.commands;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imagej.ops.Initializable;
import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.Regions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.command.Previewable;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
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
import de.mpg.biochem.mars.image.MarsImageUtils;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import ij.gui.Roi;

@Plugin(type = Command.class, label = "Bdv Peak Tracker")
public class MarsPeakTrackerBdvCommand extends InteractiveCommand implements Command,
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
		
	@Parameter(visibility = ItemVisibility.MESSAGE,
		style = "group:Input, align:center", persist = false)
	private String region = "";
		
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
	
	@Parameter(label = "Pixel length", style = "group:Output")
	private double pixelLength = 1;
	
	@Parameter(label = "Pixel units", style = "group:Output", choices = { "pixel",
		"µm", "nm" })
	private String pixelUnits = "pixel";
	
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "group:Output")
	private final String excludeTitle =
		"List of time points to exclude (T0, T1-T2, ...)";
	
	@Parameter(label = "Exclude", style = "group:Output", required = false)
	private String excludeTimePointList = "";
	
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
	
	/**
	 * Map from T to label peak lists
	 */
	private List<ConcurrentMap<Integer, List<Peak>>> peakLabelsStack;
	
	private PeakTracker tracker;
	
	private Interval interval;
	private PeakPreviewOverlay previewOverlay;
	private BdvOverlaySource<?> overlaySource;
	private boolean activeOverlay = false;
	
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
	
	}
	
	protected void addTracksToArchive() {
		if (archive != null) {
			archive.getWindow().lock();
			
			/*
			for (DNASegment segment : segments) {
				//Build table with timepoint index
				MarsTable table = new MarsTable("table");
				DoubleColumn col = new DoubleColumn("T");
				for (double t=0; t<marsBdvFrame.getNumberTimePoints(); t++) col.add(t);
				table.add(col);
				
				//Build molecule record with DNA location
				Molecule molecule = archive.createMolecule(MarsMath.getUUID58(), table);
				molecule.setMetadataUID(marsBdvFrame.getMetadataUID());
				molecule.setImage(archive.getMetadata(marsBdvFrame.getMetadataUID()).getImage(0).getImageID());
				molecule.setParameter("Dna_Top_X1", segment.getX1());
				molecule.setParameter("Dna_Top_Y1", segment.getY1());
				molecule.setParameter("Dna_Bottom_X2", segment.getX2());
				molecule.setParameter("Dna_Bottom_Y2", segment.getY2());
				//add to archive
				archive.put(molecule);
				logService.info("Added DnaMolecule record " + molecule.getUID());
			}
			*/
			
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
		
			int frameCount = 100;
			
			List<Integer> processTimePoints = new ArrayList<Integer>();
			List<Runnable> tasks = new ArrayList<Runnable>();
			for (int t = 0; t < frameCount; t++) {
				boolean processedTimePoint = true;
				for (int index = 0; index < excludeTimePoints.size(); index++)
					if (excludeTimePoints.get(index)[0] <= t && t <= excludeTimePoints.get(
						index)[1])
					{
						processedTimePoint = false;
						break;
					}
		
				
			}
		
			tracker = new PeakTracker(maxDifferenceX, maxDifferenceY, maxDifferenceT,
				minimumDistance, minTrajectoryLength, true, logService, pixelLength);
			
			archive.getWindow().unlock();
		}
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
		
		double[] minInterval = new double[] {interval.min(0), interval.min(1), 0};
		double[] transformedMinInterval = new double[3];
		bdvSourceTransform.applyInverse(transformedMinInterval, minInterval);
		
		double[] maxInterval = new double[] {interval.max(0), interval.max(1), 0};
		double[] transformedMaxInterval = new double[3];
		bdvSourceTransform.applyInverse(transformedMaxInterval, maxInterval);
		
		Interval transformedInterval = Intervals.createMinMax( (long) transformedMinInterval[0], (long) transformedMinInterval[1], 0, 
																											     (long) transformedMaxInterval[0], (long) transformedMaxInterval[1], 0);

		RandomAccessibleInterval<FloatType> filteredImg = null;
		if (useDogFilter) filteredImg = MarsImageUtils.dogFilter(img,
			dogFilterRadius, 1);
	
		List<Peak> peaks = new ArrayList<Peak>();

		RealMask roiMask = convertService.convert(new Roi(new Rectangle((int) transformedInterval.min(0), (int) transformedInterval.min(1), 
			(int)(transformedInterval.max(0) - transformedInterval.min(0)), (int)(transformedInterval.max(1) - transformedInterval.min(1)))), RealMask.class);
		IterableRegion<BoolType> iterableROI = MarsImageUtils.toIterableRegion(
			roiMask, img);

		if (useDogFilter) peaks = MarsImageUtils.findPeaks(filteredImg, Regions
			.sample(iterableROI, filteredImg), t, threshold, minimumDistance, false);
		else peaks = MarsImageUtils.findPeaks(img, Regions.sample(iterableROI,
			img), t, threshold, minimumDistance, false);

		peaks = MarsImageUtils.fitPeaks(img, img, peaks, fitRadius,
			dogFilterRadius, false, RsquaredMin);
		peaks = MarsImageUtils.removeNearestNeighbors(peaks, minimumDistance);

		if (integrate) MarsImageUtils.integratePeaks(img, img, peaks,
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
	
	@Override
	public void preview() {
		if (preview) {
			if (previewOverlay == null) previewOverlay = new PeakPreviewOverlay();
			if (!activeOverlay) {
				overlaySource = BdvFunctions.showOverlay(previewOverlay, "Peak-Preview", Bdv
								                    .options().addTo(marsBdvFrame.getBdvHandle()));
				activeOverlay = true;
			}
		}
	}
	
	@Override
	public void cancel() {
		if (overlaySource != null) overlaySource.removeFromBdv();
		if (previewOverlay != null) marsBdvFrame.getBdvHandle().getViewerPanel().getDisplay().overlays().remove( previewOverlay );
		activeOverlay = false;
	}
	
	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		if (!preview) cancel();
	}
	
	protected void setSelectionRegion() {
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
		
		//TODO make sure the currently selected Metadata is the one whose size is retrieved in the future.. This assumes all metadata records would be for images of similar sizes...
		final Interval rangeInterval = Intervals.createMinMax( 0, 0, 0, archive.getMetadata(0).getImage(0).getSizeX(), archive.getMetadata(0).getImage(0).getSizeY(), 0 );
		backgroundThread.submit(() -> {
			final TransformedBoxSelectionDialog.Result result = BdvFunctions.selectBox(
				  marsBdvFrame.getBdvHandle(),
					imageTransform,
					initialInterval,
					rangeInterval,
					BoxSelectionOptions.options()
							.title( "Select region" ));
			if (result.isValid()) {
				interval = result.getInterval();
			}
		});
		backgroundThread.shutdown();
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
	
	public void setPixelLength(double pixelLength) {
		this.pixelLength = pixelLength;
	}
	
	public double getPixelLength() {
		return this.pixelLength;
	}
	
	public void setPixelUnits(String pixelUnits) {
		this.pixelUnits = pixelUnits;
	}
	
	public String getPixelUnits() {
		return this.pixelUnits;
	}
	
	public void setExcludedTimePointsList(String excludeTimePointList) {
		this.excludeTimePointList = excludeTimePointList;
	}
	
	public String getExcludedTimePointsList() {
		return this.excludeTimePointList;
	}
}
