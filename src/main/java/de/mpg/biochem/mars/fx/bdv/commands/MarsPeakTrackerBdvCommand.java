/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.Initializable;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOverlay;
import bdv.util.BdvOverlaySource;
import bdv.viewer.Source;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.fx.util.MarsN5ImageMetadata;
import de.mpg.biochem.mars.image.MarsImageUtils;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.molecule.SingleMoleculeArchive;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsUtil;

/**
 * Peak tracker for single-molecule archives, run from the Peak Tracker card in
 * the BDV view. It finds, fits and tracks peaks in one BDV source of the
 * metadata record being viewed — the same steps as the Peak Tracker command
 * on an image — and adds every track as a new molecule record in the archive.
 * Settings are previewed live on the current time point.
 *
 * <p>Records created from N5/Zarr data without an OME image get one from the
 * source's dimensions before tracking, since the tracker reads image sizes and
 * plane times from it.
 */
@Plugin(type = Command.class, label = "Single-Molecule Peak Tracker")
public class MarsPeakTrackerBdvCommand extends InteractiveCommand implements
	Command, Initializable, Previewable
{

	private static final String WHOLE_IMAGE = "Whole image";
	private static final String SELECTED_REGION = "Selected region";

	/**
	 * SERVICES
	 */
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;

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

	@Parameter(label = "Region", choices = { WHOLE_IMAGE, SELECTED_REGION },
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE + ", group:Input")
	private String region = WHOLE_IMAGE;

	@Parameter(label = "Select region", style = "group:Input, align:center",
		description = "Select the region to search for peaks",
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

	@Parameter(label = "All sources", style = "group:Integrate")
	private boolean integrateAllSources = false;

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

	@Parameter(label = "Verbose", style = "group:Output",
		description = "Write every fit parameter to the molecule tables")
	private boolean verbose = false;

	@Parameter(label = "Replace existing", style = "group:Output",
		description = "Remove the molecules already tracked in this record and " +
			"source before adding the new ones")
	private boolean replaceExisting = false;

	@Parameter(label = "Threads", required = false, min = "1", max = "120",
		style = "group:Output")
	private int nThreads = Math.max(1, Runtime.getRuntime()
		.availableProcessors() - 2);

	/**
	 * Global Settings
	 */
	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "previewChanged")
	private boolean preview = false;

	@Parameter(label = "Add molecules to archive",
		description = "Track peaks in the current record and add each track as " +
			"a molecule", callback = "addMoleculesToArchive", persist = false)
	private Button addMoleculesButton;

	private Interval interval;
	private PeakPreviewOverlay previewOverlay;
	private BdvOverlaySource<?> overlaySource;
	private boolean activeOverlay = false;
	private boolean selectionInProgress = false;
	private final AtomicInteger progressInteger = new AtomicInteger(0);

	// The preview is computed off the paint thread: finding and fitting every
	// peak of a dense frame (or loading its chunks from S3) inside BDV's paint
	// call froze the viewer. Results are cached by time point and settings, and
	// requests for frames already scrolled past are dropped.
	private ExecutorService previewExecutor;
	private volatile String requestedPreviewKey;
	private volatile String previewKey;
	private volatile List<Peak> previewPeaks = new ArrayList<>();

	@Override
	public void initialize() {
		if (marsBdvFrame == null) return;
		final MutableModuleItem<String> sourceItems = getInfo().getMutableInput(
			"source", String.class);
		sourceItems.setChoices(marsBdvFrame.getSourceNames());

		final Interval viewInterval = marsBdvFrame.currentViewImageCoordinates();
		long viewWidth = viewInterval.max(0) - viewInterval.min(0);
		long viewHeight = viewInterval.max(1) - viewInterval.min(1);
		interval = Intervals.createMinMax((long) (viewInterval.min(0) + viewWidth *
			0.2), (long) (viewInterval.min(1) + viewHeight * 0.2), 0,
			(long) (viewInterval.min(0) + viewWidth * 0.8), (long) (viewInterval
				.min(1) + viewHeight * 0.8), 0);
	}

	@Override
	public void run() {
		// command is run interactively using buttons.
	}

	protected void addMoleculesToArchive() {
		if (archive == null || marsBdvFrame == null) return;
		if (!(((Object) archive) instanceof SingleMoleculeArchive)) {
			logService.error(getInfo().getLabel() +
				": molecules can only be added to a SingleMoleculeArchive.");
			return;
		}
		// Save the current settings so they are reloaded next time.
		saveInputs();

		final String metaUID = marsBdvFrame.getMetadataUID();
		archive.getWindow().lock();
		try {
			// The tracker reads image sizes and plane times from the record (and
			// from the first record in the archive), so records built from N5 data
			// without an OME image get one from their source dimensions.
			MarsMetadata metadata = archive.getMetadata(metaUID);
			int imagesAdded = 0;
			if (MarsN5ImageMetadata.ensureImage(metadata, marsBdvFrame
				.getSourceDimensions(source), source))
			{
				archive.putMetadata(metadata);
				imagesAdded++;
			}
			for (String uid : archive.getMetadataUIDs()) {
				if (uid.equals(metaUID)) continue;
				MarsMetadata other = archive.getMetadata(uid);
				if (MarsN5ImageMetadata.ensureImage(other)) {
					archive.putMetadata(other);
					imagesAdded++;
				}
			}
			if (metadata.getImageCount() == 0) {
				logService.error(getInfo().getLabel() + ": metadata record " +
					metaUID + " has no image and its size could not be read from " +
					"source " + source + ".");
				return;
			}

			List<int[]> excludeTimePoints = parseExcludeList();
			long[] dims = marsBdvFrame.getSourceDimensions(source);
			int timepoints = MarsN5ImageMetadata.sizesXYZCT(dims)[4];

			List<Integer> processTimePoints = new ArrayList<>();
			for (int t = 0; t < timepoints; t++)
				if (!excluded(t, excludeTimePoints)) processTimePoints.add(t);

			ConcurrentMap<Integer, List<Peak>> peaks = new ConcurrentHashMap<>();
			List<Runnable> tasks = new ArrayList<>();
			progressInteger.set(0);
			for (int t : processTimePoints) {
				tasks.add(() -> {
					List<Peak> peaksInT = findPeaksInT(t, integrate,
						integrateAllSources);
					if (!peaksInT.isEmpty()) peaks.put(t, peaksInT);
					progressInteger.incrementAndGet();
				});
			}

			archive.getWindow().updateLockMessage("Finding peaks in " + source);
			MarsUtil.threadPoolBuilder(statusService, logService, () -> archive
				.getWindow().setProgress((double) progressInteger.get() /
					processTimePoints.size()), tasks, nThreads);

			int channel = metadata.getBdvSource(source).getChannel();
			int removed = 0;
			if (replaceExisting) {
				for (String uid : archive.getMoleculeUIDs())
					if (metaUID.equals(archive.getMetadataUIDforMolecule(uid)) && archive
						.getChannel(uid) == channel)
					{
						archive.remove(uid);
						removed++;
					}
			}

			int before = archive.getNumberOfMolecules();
			archive.getWindow().updateLockMessage("Tracking peaks");
			// Other sources' intensities are only written as columns in verbose mode.
			boolean verboseTables = verbose || (integrate && integrateAllSources);
			PeakTracker tracker = new PeakTracker(maxDifferenceX, maxDifferenceY,
				maxDifferenceT, minimumDistance, minTrajectoryLength, verboseTables,
				logService, 1);
			if (!peaks.isEmpty()) tracker.track(peaks, archive, metaUID, channel,
				processTimePoints, nThreads);
			int added = archive.getNumberOfMolecules() - before;

			LogBuilder builder = new LogBuilder();
			String log = LogBuilder.buildTitleBlock(getInfo().getLabel());
			addInputParameterLog(builder);
			builder.addParameter("Molecules added", added);
			if (replaceExisting) builder.addParameter("Molecules replaced", removed);
			if (imagesAdded > 0) builder.addParameter(
				"Records given an image from N5 dimensions", imagesAdded);
			log += builder.buildParameterList();
			log += "\n" + LogBuilder.endBlock();
			archive.getMetadata(metaUID).logln(log);
			logService.info(log);
		}
		finally {
			archive.getWindow().unlock();
		}
	}

	private List<int[]> parseExcludeList() {
		List<int[]> excludeTimePoints = new ArrayList<>();
		if (excludeTimePointList == null || excludeTimePointList.trim().isEmpty())
			return excludeTimePoints;
		try {
			for (String range : excludeTimePointList.split(",")) {
				String[] endPoints = range.split("-");
				int start = Integer.parseInt(endPoints[0].trim());
				int end = (endPoints.length > 1) ? Integer.parseInt(endPoints[1].trim())
					: start;
				excludeTimePoints.add(new int[] { start, end });
			}
		}
		catch (NumberFormatException e) {
			logService.info("NumberFormatException encountered when parsing " +
				"exclude list. Tracking all time points.");
			return new ArrayList<>();
		}
		return excludeTimePoints;
	}

	private static boolean excluded(int t, List<int[]> excludeTimePoints) {
		for (int[] range : excludeTimePoints)
			if (range[0] <= t && t <= range[1]) return true;
		return false;
	}

	private void addInputParameterLog(LogBuilder builder) {
		builder.addParameter("Metadata UID", marsBdvFrame.getMetadataUID());
		builder.addParameter("Source", source);
		builder.addParameter("Region", region.equals(WHOLE_IMAGE) ? WHOLE_IMAGE
			: "(" + interval.min(0) + ", " + interval.min(1) + ") to (" + interval
				.max(0) + ", " + interval.max(1) + ")");
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
		builder.addParameter("Integrate all sources", String.valueOf(
			integrateAllSources));
		builder.addParameter("Exclude time points", excludeTimePointList);
		builder.addParameter("Verbose", String.valueOf(verbose));
		builder.addParameter("Replace existing", String.valueOf(replaceExisting));
	}

	/**
	 * Find, fit and optionally integrate the peaks of the selected source at time
	 * point t. Positions are returned in global (BDV) coordinates.
	 */
	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<Peak> findPeaksInT(
		int t, boolean integrate, boolean integrateAllSources)
	{
		Source<T> bdvSource = marsBdvFrame.getSource(source);

		// Remove the Z dimension
		RandomAccessibleInterval<T> img = Views.hyperSlice(bdvSource.getSource(t,
			0), 2, 0);

		final AffineTransform3D bdvSourceTransform = new AffineTransform3D();
		bdvSource.getSourceTransform(t, 0, bdvSourceTransform);

		RandomAccessibleInterval<T> imgView = img;
		if (region.equals(SELECTED_REGION)) {
			// The selection is in global coordinates; map it into the source.
			Interval localInterval = BdvSourceIntervals.globalToSource(interval,
				bdvSourceTransform, img);
			if (Intervals.isEmpty(localInterval)) return new ArrayList<>();
			imgView = Views.interval(img, localInterval);
		}

		RandomAccessibleInterval<FloatType> filteredImg = (useDogFilter)
			? MarsImageUtils.dogFilter(imgView, dogFilterRadius, 1) : null;

		List<Peak> peaks;
		if (useDogFilter) peaks = MarsImageUtils.findPeaks(filteredImg, filteredImg,
			t, threshold, minimumDistance, false);
		else peaks = MarsImageUtils.findPeaks(imgView, imgView, t, threshold,
			minimumDistance, false);

		peaks = MarsImageUtils.fitPeaks(imgView, imgView, peaks, fitRadius,
			dogFilterRadius, false, RsquaredMin);
		peaks = MarsImageUtils.removeNearestNeighbors(peaks, minimumDistance);

		if (integrate) MarsImageUtils.integratePeaks(imgView, imgView, peaks,
			integrationInnerRadius, integrationOuterRadius);

		if (integrate && integrateAllSources) {
			for (String otherSourceName : (List<String>) marsBdvFrame
				.getSourceNames())
			{
				if (otherSourceName.equals(source)) continue;
				integrateOtherSource(otherSourceName, t, peaks, bdvSourceTransform);
			}
		}

		// Transform from source pixel coordinates to global (BDV) coordinates.
		for (Peak peak : peaks) {
			double[] local = new double[] { peak.getX(), peak.getY(), 0 };
			double[] global = new double[3];
			bdvSourceTransform.apply(local, global);
			peak.setX(global[0]);
			peak.setY(global[1]);
		}

		return peaks;
	}

	/**
	 * Integrates the peaks (found in the primary source) in another BDV source,
	 * mapping their positions through global coordinates into that source. The
	 * intensity is stored on each peak as "&lt;otherSourceName&gt;_Intensity".
	 */
	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> void integrateOtherSource(
		String otherSourceName, int t, List<Peak> peaks,
		AffineTransform3D primaryTransform)
	{
		Source<T> otherBdvSource = marsBdvFrame.getSource(otherSourceName);
		RandomAccessibleInterval<T> otherImg = Views.hyperSlice(otherBdvSource
			.getSource(t, 0), 2, 0);

		final AffineTransform3D otherTransform = new AffineTransform3D();
		otherBdvSource.getSourceTransform(t, 0, otherTransform);

		List<Peak> otherSourcePeaks = new ArrayList<>();
		for (Peak peak : peaks) {
			double[] local = new double[] { peak.getX(), peak.getY(), 0 };
			double[] global = new double[3];
			primaryTransform.apply(local, global);
			double[] otherLocal = new double[3];
			otherTransform.applyInverse(otherLocal, global);
			otherSourcePeaks.add(new Peak(otherLocal[0], otherLocal[1]));
		}

		MarsImageUtils.integratePeaks(otherImg, otherImg, otherSourcePeaks,
			integrationInnerRadius, integrationOuterRadius);

		String intensityProperty = otherSourceName + "_Intensity";
		for (int i = 0; i < peaks.size(); i++)
			peaks.get(i).setProperty(intensityProperty, otherSourcePeaks.get(i)
				.getIntensity());
	}

	@Override
	public void preview() {
		if (preview) {
			if (previewOverlay == null) previewOverlay = new PeakPreviewOverlay();
			if (!activeOverlay) {
				overlaySource = BdvFunctions.showOverlay(previewOverlay,
					"Peak-Preview", Bdv.options().addTo(marsBdvFrame.getBdvHandle()));
				activeOverlay = true;
			}

			// HACK Find the dialog and remove the preview when it is closed.
			for (Window window : Window.getWindows())
				if (window instanceof JDialog && ((JDialog) window).getTitle().equals(
					getInfo().getLabel())) window.addWindowListener(
						new java.awt.event.WindowAdapter()
						{

							@Override
							public void windowClosing(java.awt.event.WindowEvent e) {
								cancel();
							}
						});
		}
	}

	/**
	 * Peaks to draw for time point t. Returns the cached result when it matches
	 * the current settings; otherwise schedules a computation and returns the
	 * previous result for the same frame (or nothing) until it's ready.
	 */
	private List<Peak> previewPeaks(int t) {
		final String key = t + "|" + previewSettings();
		if (key.equals(previewKey)) return previewPeaks;
		if (!key.equals(requestedPreviewKey)) {
			requestedPreviewKey = key;
			if (previewExecutor == null || previewExecutor.isShutdown())
				previewExecutor = Executors.newSingleThreadExecutor(r -> {
					Thread thread = new Thread(r, "PeakTrackerPreview");
					thread.setDaemon(true);
					return thread;
				});
			previewExecutor.submit(() -> {
				// Superseded while queued (e.g. scrolled on) — skip it.
				if (!key.equals(requestedPreviewKey)) return;
				List<Peak> peaks;
				try {
					peaks = findPeaksInT(t, false, false);
				}
				catch (RuntimeException e) {
					peaks = new ArrayList<>();
				}
				if (!key.equals(requestedPreviewKey)) return;
				previewPeaks = peaks;
				previewKey = key;
				marsBdvFrame.getBdvHandle().getViewerPanel().getDisplay().repaint();
			});
		}
		String shownKey = previewKey;
		return shownKey != null && shownKey.startsWith(t + "|") ? previewPeaks
			: new ArrayList<>();
	}

	/** Everything the preview depends on besides the time point. */
	private String previewSettings() {
		return source + "|" + region + "|" + (interval == null ? "" : interval
			.min(0) + "," + interval.min(1) + "," + interval.max(0) + "," + interval
				.max(1)) + "|" + useDogFilter + "|" + dogFilterRadius + "|" +
			threshold + "|" + minimumDistance + "|" + fitRadius + "|" + RsquaredMin;
	}

	@Override
	public void cancel() {
		if (previewExecutor != null) previewExecutor.shutdownNow();
		requestedPreviewKey = null;
		previewKey = null;
		if (activeOverlay && overlaySource != null) overlaySource.removeFromBdv();
		if (activeOverlay && previewOverlay != null) marsBdvFrame.getBdvHandle()
			.getViewerPanel().getDisplay().overlays().remove(previewOverlay);
		activeOverlay = false;
	}

	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		if (!preview) cancel();
	}

	protected void setSelectionRegion() {
		if (selectionInProgress) return;
		final Interval viewInterval = marsBdvFrame.currentViewImageCoordinates();
		long viewWidth = viewInterval.max(0) - viewInterval.min(0);
		long viewHeight = viewInterval.max(1) - viewInterval.min(1);

		// No image transform: the view is built around an untransformed source.
		final AffineTransform3D imageTransform = new AffineTransform3D();
		imageTransform.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

		final Interval initialInterval = Intervals.createMinMax((long) (viewInterval
			.min(0) + viewWidth * 0.2), (long) (viewInterval.min(1) + viewHeight *
				0.2), 0, (long) (viewInterval.min(0) + viewWidth * 0.8),
			(long) (viewInterval.min(1) + viewHeight * 0.8), 0);
		// Bound the selection by the source, which works without an OME image.
		long[] dims = marsBdvFrame.getSourceDimensions(source);
		final Interval rangeInterval = Intervals.createMinMax(0, 0, 0, dims[0],
			dims[1], 0);

		ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
		backgroundThread.submit(() -> {
			selectionInProgress = true;
			final TransformedBoxSelectionDialog.Result result = BdvFunctions
				.selectBox(marsBdvFrame.getBdvHandle(), imageTransform,
					initialInterval, rangeInterval, BoxSelectionOptions.options().title(
						"Select region"));
			selectionInProgress = false;
			if (result.isValid()) {
				interval = result.getInterval();
				region = SELECTED_REGION;
			}
		});
		backgroundThread.shutdown();
	}

	public class PeakPreviewOverlay extends BdvOverlay {

		private final Color intersectionFillColor = new Color(0x88994499, true);

		@Override
		protected void draw(Graphics2D g) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);

			if (region.equals(SELECTED_REGION)) {
				Interval selection = selectionToViewTransform(transform);
				g.setPaint(intersectionFillColor);
				g.fillRect((int) selection.min(0), (int) selection.min(1),
					(int) (selection.max(0) - selection.min(0)), (int) (selection.max(
						1) - selection.min(1)));
			}

			List<Peak> peaks = previewPeaks(info.getTimePointIndex());

			if (peaks.size() > 0) {
				g.setColor(getColor());
				g.setStroke(new BasicStroke(2));

				final double vx = transform.get(0, 0);
				final double vy = transform.get(1, 0);
				final double transformScale = Math.sqrt(vx * vx + vy * vy);
				final double radius = 5 * transformScale;

				for (Peak peak : peaks) {
					final double[] globalCoords = new double[] { peak.getX(), peak
						.getY() };
					final double[] viewerCoords = new double[2];
					transform.apply(globalCoords, viewerCoords);
					g.drawOval((int) (viewerCoords[0] - radius), (int) (viewerCoords[1] -
						radius), (int) (2 * radius), (int) (2 * radius));
				}
			}
		}

		private Interval selectionToViewTransform(AffineTransform2D transform) {
			final double[] min = new double[2];
			transform.apply(new double[] { interval.min(0), interval.min(1) }, min);
			final double[] max = new double[2];
			transform.apply(new double[] { interval.max(0), interval.max(1) }, max);
			return Intervals.createMinMax(Math.round(min[0]), Math.round(min[1]),
				Math.round(max[0]), Math.round(max[1]));
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

	public void setArchive(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		this.archive = archive;
	}

	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> getArchive() {
		return archive;
	}
}
