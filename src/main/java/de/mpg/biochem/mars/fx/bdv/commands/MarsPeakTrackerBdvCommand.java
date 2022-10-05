package de.mpg.biochem.mars.fx.bdv.commands;

import io.scif.ome.services.OMEXMLService;
import io.scif.services.FormatService;
import io.scif.services.TranslatorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.imagej.ops.Initializable;
import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.decimal4j.util.DoubleRounder;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import bdv.util.Bdv;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.table.MarsTableService;
import de.mpg.biochem.mars.util.LogBuilder;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.process.FloatPolygon;

public class MarsPeakTrackerBdvCommand extends DynamicCommand implements Command,
Initializable
{

	/**
	 * SERVICES
	 */
	@Parameter
	private LogService logService;
	
	@Parameter
	private StatusService statusService;
	
	@Parameter
	private TranslatorService translatorService;
	
	@Parameter
	private OMEXMLService omexmlService;
	
	@Parameter
	private FormatService formatService;
	
	@Parameter
	private MarsTableService resultsTableService;
	
	@Parameter
	private ConvertService convertService;
	
	@Parameter
	private OpService opService;
	
	@Parameter
	private MoleculeArchiveService moleculeArchiveService;
	
	@Parameter
	private UIService uiService;
	
	@Parameter
	private DisplayService displayService;
	
	@Parameter
	private PlatformService platformService;
	
	/**
	 * IMAGE
	 */
	@Parameter(label = "Image to search for peaks")
	private Bdv bdvHandle;
	
	@Parameter(label = "Molecule Archive")
	private MoleculeArchive archive;
	
	/**
	 * INPUT SETTINGS
	 */
	
	@Parameter(visibility = ItemVisibility.MESSAGE,
		style = "groupLabel, tabbedPaneWidth:450", persist = false)
	private String inputGroup = "Input";
	
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "image, group:Input",
		persist = false)
	private String inputFigure = "ImageInput.png";
	
	@Parameter(visibility = ItemVisibility.MESSAGE,
		style = "group:Input, align:center", persist = false)
	private String imageName = "?";
	
	@Parameter(visibility = ItemVisibility.MESSAGE,
			style = "group:Input, align:center", persist = false)
	private String region = "[roi]";
	
	@Parameter(label = "Source", choices = { "a", "b", "c" },
		style = "group:Input", persist = false)
	private String source = "";
	
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
	 * THREADS
	 */
	
	@Parameter(label = "Threads", required = false, min = "1", max = "120",
		style = "group:Output")
	private int nThreads = Runtime.getRuntime().availableProcessors();
	
	/**
	 * PREVIEW SETTINGS
	 */
	
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String previewGroup = "Preview";
	
	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "previewChanged", style = "group:Preview")
	private boolean preview = false;
	
	@Parameter(label = "Roi", style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE +
		", group:Preview", choices = { "circle", "point" })
	private String previewRoiType;
	
	@Parameter(label = "T", min = "0", style = NumberWidget.SCROLL_BAR_STYLE +
		", group:Preview", persist = false)
	private int previewT;
	
	@Parameter(label = "Timeout (s)", style = "group:Preview")
	private int previewTimeout = 10;
	
	/**
	 * Map from T to label peak lists
	 */
	private List<ConcurrentMap<Integer, List<Peak>>> peakLabelsStack;
	
	private PeakTracker tracker;
	
	@Override
	public void initialize() {
		//Need to get a list of sources...
	/*
		final MutableModuleItem<String> channelItems = getInfo().getMutableInput(
			"source", String.class);
		long channelCount = dataset.getChannels();
		ArrayList<String> channels = new ArrayList<String>();
		for (int ch = 0; ch < channelCount; ch++)
			channels.add(String.valueOf(ch));
		channelItems.setChoices(channels);
	
*/
	}
	
	@Override
	public void run() {
		//get the source that was chosen..
		
		// Build log
		LogBuilder builder = new LogBuilder();
		String log = LogBuilder.buildTitleBlock("Peak Tracker");
		addInputParameterLog(builder);
		log += builder.buildParameterList();
		logService.info(log);
	
		double starttime = System.currentTimeMillis();
		logService.info("Finding and Fitting Peaks...");
		// build list of timepoints to process...
	
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
	
		logService.info("Time: " + DoubleRounder.round((System.currentTimeMillis() -
			starttime) / 60000, 2) + " minutes.");
	
		tracker = new PeakTracker(maxDifferenceX, maxDifferenceY, maxDifferenceT,
			minimumDistance, minTrajectoryLength, true, logService, pixelLength);

		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		statusService.showProgress(1, 1);
	
		logService.info("Finished in " + DoubleRounder.round((System
			.currentTimeMillis() - starttime) / 60000, 2) + " minutes.");
		if (archive.getNumberOfMolecules() == 0) {
			logService.info(
				"No molecules found. Maybe there is a problem with your settings");
			archive = null;
			logService.info(LogBuilder.endBlock(false));
			uiService.showDialog(
				"No molecules found. Maybe there is a problem with your settings. " +
					"Make sure molecules are detected using preview. If not try lowering the " +
					"detection threshold.", MessageType.ERROR_MESSAGE,
				OptionType.DEFAULT_OPTION);
		}
		else {
			logService.info(LogBuilder.endBlock(true));
	
			log += "\n" + LogBuilder.endBlock(true);
			archive.logln(log);
			archive.logln("   ");
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<List<Peak>> findPeaksInT(
		int t, boolean useDogFilter, boolean integrate, int numThreads)
	{
		
		List<List<Peak>> labelPeakLists = new ArrayList<List<Peak>>();
		//RandomAccessibleInterval<T> img = MarsImageUtils.get2DHyperSlice((ImgPlus<T>) dataset.getImgPlus(), 0,
		//		channel, t);
	/*
		RandomAccessibleInterval<FloatType> filteredImg = null;
		if (useDogFilter) filteredImg = MarsImageUtils.dogFilter(img,
			dogFilterRadius, numThreads);
	
		List<List<Peak>> labelPeakLists = new ArrayList<List<Peak>>();
		for (int i = 0; i < processingRois.length; i++) {
			List<Peak> peaks = new ArrayList<Peak>();
	
			RealMask roiMask = convertService.convert(processingRois[i],
				RealMask.class);
			IterableRegion<BoolType> iterableROI = MarsImageUtils.toIterableRegion(
				roiMask, img);
	
			if (useDogFilter) peaks = MarsImageUtils.findPeaks(filteredImg, Regions
				.sample(iterableROI, filteredImg), t, threshold, minimumDistance);
			else peaks = MarsImageUtils.findPeaks(img, Regions.sample(iterableROI,
				img), t, threshold, minimumDistance, findNegativePeaks);
	
			peaks = MarsImageUtils.fitPeaks(img, img, peaks, fitRadius,
				dogFilterRadius, findNegativePeaks, RsquaredMin);
			peaks = MarsImageUtils.removeNearestNeighbors(peaks, minimumDistance);
	
			if (integrate) MarsImageUtils.integratePeaks(img, img, peaks,
				integrationInnerRadius, integrationOuterRadius);
	
			labelPeakLists.add(peaks);
		}
	*/
		return labelPeakLists;
	}
	
	@Override
	public void preview() {
		if (preview) {	
			ExecutorService es = Executors.newSingleThreadExecutor();
			try {
				es.submit(() -> {
	
					List<List<Peak>> labelPeakLists = findPeaksInT(previewT, useDogFilter, false, Runtime.getRuntime().availableProcessors());
	
					if (Thread.currentThread().isInterrupted()) return;
	
					int peakCount = 0;
					Overlay overlay = new Overlay();
					if (previewRoiType.equals("point")) {
						FloatPolygon poly = new FloatPolygon();
						for (List<Peak> labelPeaks : labelPeakLists)
							for (Peak p : labelPeaks) {
								poly.addPoint(p.getDoublePosition(0), p.getDoublePosition(1));
								peakCount++;
	
								if (Thread.currentThread().isInterrupted()) return;
							}
	
						PointRoi peakRoi = new PointRoi(poly);
	
						overlay.add(peakRoi);
					}
					else {
						for (List<Peak> labelPeaks : labelPeakLists)
							for (Peak p : labelPeaks) {
								// The pixel origin for OvalRois is at the upper left corner
								// !!!!
								// The pixel origin for PointRois is at the center !!!
								final OvalRoi ovalRoi = new OvalRoi(p.getDoublePosition(0) +
									0.5 - integrationInnerRadius, p.getDoublePosition(1) + 0.5 -
										integrationInnerRadius, integrationInnerRadius * 2,
									integrationInnerRadius * 2);
								// ovalRoi.setStrokeColor(Color.CYAN.darker());
								overlay.add(ovalRoi);
								peakCount++;
								if (Thread.currentThread().isInterrupted()) return;
							}
					}
					if (Thread.currentThread().isInterrupted()) return;
	
					final String countString = "count: " + peakCount;
					final MutableModuleItem<String> preFrameCount = getInfo()
						.getMutableInput("tPeakCount", String.class);
					preFrameCount.setValue(this, countString);
				}).get(previewTimeout, TimeUnit.SECONDS);
			}
			catch (TimeoutException e1) {
				es.shutdownNow();
				uiService.showDialog(
					"Preview took too long. Try a smaller region, a higher threshold, or try again with a longer delay before preview timeout.",
					MessageType.ERROR_MESSAGE, OptionType.DEFAULT_OPTION);
				cancel();
			}
			catch (InterruptedException | ExecutionException e2) {
				es.shutdownNow();
				cancel();
			}
			es.shutdownNow();
		}
	}
	
	@Override
	public void cancel() {
		//??
	}
	
	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		if (!preview) cancel();
	}
	
	private void addInputParameterLog(LogBuilder builder) {
		//builder.addParameter("Dataset name", dataset.getName());
	
		builder.addParameter("Region", region);
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
		builder.addParameter("Pixel length", String.valueOf(this.pixelLength));
		builder.addParameter("Pixel units", this.pixelUnits);
		builder.addParameter("Exclude time points", excludeTimePointList);
		builder.addParameter("Thread count", nThreads);
	}
	
	// Getters and Setters
	public MoleculeArchive getArchive() {
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
	
	public void setThreads(int nThreads) {
		this.nThreads = nThreads;
	}
	
	public int getThreads() {
		return this.nThreads;
	}
}

