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
package de.mpg.biochem.mars.fx.bdv.commands;

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
import de.mpg.biochem.mars.molecule.*;
import de.mpg.biochem.mars.object.MartianObject;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import de.mpg.biochem.mars.image.MarsImageUtils;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakShape;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.metadata.MarsOMEMetadata;
import de.mpg.biochem.mars.metadata.MarsOMEUtils;
import de.mpg.biochem.mars.molecule.MoleculeArchiveService;
import de.mpg.biochem.mars.object.ObjectArchive;
import de.mpg.biochem.mars.table.MarsTableService;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;
import de.mpg.biochem.mars.util.MarsUtil;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import io.scif.Metadata;
import io.scif.img.SCIFIOImgPlus;
import io.scif.ome.OMEMetadata;
import io.scif.ome.services.OMEXMLService;
import io.scif.services.FormatService;
import io.scif.services.TranslatorService;
import loci.common.services.ServiceException;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import ome.units.quantity.Length;
import ome.xml.meta.OMEXMLMetadata;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.UnitsLength;
import ome.xml.model.enums.handlers.UnitsLengthEnumHandler;
import ome.xml.model.primitives.PositiveInteger;
import org.decimal4j.util.DoubleRounder;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Previewable;
import org.scijava.convert.ConvertService;
import org.scijava.event.EventService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

@Plugin(type = Command.class, label = "Bdv Object Tracker")
public class MarsObjectTrackerBdvCommand extends InteractiveCommand implements Command,
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
		description = "Select region to search for Objects",
		callback = "setSelectionRegion", persist = false)
	private Button regionSelectionButton;
	
	/**
	 * FINDER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String findGroup = "Find";

	@Parameter(label = "Use median filter", style = "group:Find")
	private boolean useMedianFilter = false;

	@Parameter(label = "Median filter radius", style = "group:Find")
	private long medianFilterRadius = 2;

	@Parameter(label = "Use local otsu", style = "group:Find")
	private boolean useLocalOstu = true;

	@Parameter(label = "Otsu radius", style = "group:Find")
	private long otsuRadius = 50;

	@Parameter(label = "Minimum object center separation", style = "group:Find")
	private int minimumDistance = 4;

	@Parameter(label = "Use area filter", style = "group:Find")
	private boolean useAreaFilter = true;

	@Parameter(label = "Minimum area", style = "group:Find")
	private double minArea = 1;
	
	/**
	 * FITTER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String contourGroup = "Contour";

	@Parameter(label = "Linear interpolation factor", style = "group:Contour")
	private double interpolationFactor = 1;
	
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
	 * OUTPUT SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String outputGroup = "Output";
	
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "group:Output")
	private final String excludeTitle =
		"List of time points to exclude (T0, T1-T2, ...)";
	
	@Parameter(label = "Exclude", style = "group:Output", required = false)
	private String excludeTimePointList = "";

	@Parameter(label = "Verbose", style = "group:Output")
	private boolean verbose = false;

	@Parameter(label = "Threads", required = false, min = "1", max = "120",
			style = "group:Output")
	private int nThreads = 1;
	
	/**
	 * Global Settings
	 */

	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "previewChanged")
	private boolean preview = false;
	
	@Parameter(label = "Add Object records",
			description = "Add Object records to archive",
			callback = "addObjectsToArchive", persist = false)
	private Button addObjectsButton;

	@Parameter(label = "Timeout (s)", style = "group:Preview")
	private int previewTimeout = 10;
	
	private Interval interval;
	private ObjectPreviewOverlay previewOverlay;
	private BdvOverlaySource<?> overlaySource;
	private boolean activeOverlay = false;
	private boolean selectionInProgress = false;

	//This maps from t to list of labels or shapes for the that time point.
	private ConcurrentMap<Integer, List<Peak>> objectLabels;
	
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
	
	protected void addObjectsToArchive() {
		archive.getWindow().lock();
		//save the current settings to the PrefService 
		//so they are reloaded the next time the command is opened.
		saveInputs();
		if (archive != null) {
			List<int[]> excludeTimePoints = new ArrayList<int[]>();
			int totalExcludeTimePoints = 0;
			if (excludeTimePointList.length() > 0) {
				try {
					final String[] excludeArray = excludeTimePointList.split(",");
					for (int i = 0; i < excludeArray.length; i++) {
						String[] endPoints = excludeArray[i].split("-");
						int start = Integer.valueOf(endPoints[0].trim());
						int end = (endPoints.length > 1) ? Integer.valueOf(endPoints[1]
							.trim()) : start;
						totalExcludeTimePoints += end - start;
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

			objectLabels = new ConcurrentHashMap<>();

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
					processTimePoints.add(t);
					final int theT = t;
					tasks.add(() -> {
						List<Peak> objectsInT = findObjectsInT(theT);
						if (!objectsInT.isEmpty()) {
							processTimePoints.add(theT);
							objectLabels.put(theT, objectsInT);
						}
					});
				}
			}

			archive.getWindow().updateLockMessage("Segmenting objects");
			try {
				ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
				tasks.forEach(task -> threadPool.submit(task));
				threadPool.shutdown();
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
			catch (InterruptedException exc) {
				exc.printStackTrace();
			}

			PeakTracker tracker = new PeakTracker(maxDifferenceX, maxDifferenceY, maxDifferenceT,
				minimumDistance, minTrajectoryLength, true, logService, 1);

			int numMolecules = archive.getNumberOfMolecules();

			archive.getWindow().lock();

			archive.getWindow().updateLockMessage("Tracking objects");
			//We are assuming the channel in N5 is the same as the channel from the original metadata hmmm....
			tracker.track(objectLabels, archive, archive.getMetadata(marsBdvFrame.getMetadataUID()).getBdvSource(source).getChannel(), processTimePoints, 1);

			//if (archive.getNumberOfMolecules() == numMolecules) {
			//	logService.info("MarsObjectTrackerBdvCommand: No new objects tracked for current settings.");
			//	archive.getWindow().unlock();
			//	return;
			//}
			
			LogBuilder builder = new LogBuilder();
			String log = LogBuilder.buildTitleBlock(getInfo().getLabel());
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
		builder.addParameter("Use median filter", String.valueOf(useMedianFilter));
		builder.addParameter("Median filter radius", String.valueOf(
				medianFilterRadius));
		builder.addParameter("Use local otsu", String.valueOf(useLocalOstu));
		builder.addParameter("Local otsu radius", String.valueOf(otsuRadius));
		builder.addParameter("Interpolation factor", String.valueOf(
				interpolationFactor));
		builder.addParameter("Use area filter", String.valueOf(useAreaFilter));
		builder.addParameter("Minimum area", String.valueOf(minArea));
		builder.addParameter("Minimum distance", String.valueOf(minimumDistance));
		builder.addParameter("Verbose output", String.valueOf(verbose));
		builder.addParameter("Max difference X", String.valueOf(maxDifferenceX));
		builder.addParameter("Max difference Y", String.valueOf(maxDifferenceY));
		builder.addParameter("Max difference T", String.valueOf(maxDifferenceT));
		builder.addParameter("Minimum track length", String.valueOf(
			minTrajectoryLength));
		builder.addParameter("Exclude time points", excludeTimePointList);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<Peak> findObjectsInT(
		int t)
	{
		Source<T> bdvSource = marsBdvFrame.getSource(source);

		//Remove the Z dimension
		RandomAccessibleInterval<T> rawImg = Views.hyperSlice(bdvSource.getSource(t, 0), 2, 0);

		List<Peak> objectLabelLists = new ArrayList<>();

		double[] scaleFactors = new double[]{interpolationFactor,
				interpolationFactor};
		NLinearInterpolatorFactory<T> interpolator =
				new NLinearInterpolatorFactory<>();

		//We need to transform the interval to the correct position in the img...
		//Then we need to transform the results back...
		final AffineTransform3D bdvSourceTransform = new AffineTransform3D();
		bdvSource.getSourceTransform(t, 0, bdvSourceTransform);

		Interval transformedInterval = getTransformedInterval(interval, bdvSourceTransform);
		RandomAccessibleInterval<T> imgInterval = Views.interval(rawImg, Intervals.createMinMax(transformedInterval.min(0), transformedInterval.min(1), transformedInterval.max(0), transformedInterval.max(1)));

		RandomAccessibleInterval<T> imgView;
		if (useMedianFilter) {
			RandomAccessibleInterval<T> tempImg = (RandomAccessibleInterval<T>) opService.run("copy.rai", imgInterval);
			tempImg = Views.translate(tempImg, -imgInterval.min(0), -imgInterval.min(1));
			imgView = (RandomAccessibleInterval<T>) opService.run("create.img", tempImg);
			//There seems to be a bug where intervals that are not at 0, 0 are shifted by the median radius.
			//HACK : to overcome this issue we make a copy of the rai shifted to the origin
			//then shift back afterward.
			opService.filter().median((IterableInterval<T>) imgView, tempImg,
					new HyperSphereShape(medianFilterRadius));
			imgView = Views.translate(imgView, imgInterval.min(0), imgInterval.min(1));
		}
		else imgView = imgInterval;

		Interval newInterval = Intervals.createMinMax(Math.round(interval.min(0) *
						interpolationFactor), Math.round(interval.min(1) * interpolationFactor),
				Math.round(interval.max(0) * interpolationFactor), Math.round(interval
						.max(1) * interpolationFactor));

		IntervalView<T> scaledImg = Views.interval(Views.raster(RealViews
				.affineReal(Views.interpolate(Views.extendMirrorSingle(imgView),
						interpolator), new Scale(scaleFactors))), newInterval);

		final RandomAccessibleInterval<BitType> binaryImg =
				(RandomAccessibleInterval<BitType>) opService.run("create.img",
						scaledImg, new BitType());

		if (useLocalOstu) {
			opService.run("threshold.otsu", binaryImg, scaledImg,
					new HyperSphereShape(this.otsuRadius),
					new OutOfBoundsMirrorFactory<T, RandomAccessibleInterval<T>>(
							Boundary.SINGLE));
		} else {
			opService.run("threshold.otsu", binaryImg, scaledImg);
		}

		final RandomAccessibleInterval<UnsignedShortType> indexImg =
				(RandomAccessibleInterval<UnsignedShortType>) opService.run(
						"create.img", binaryImg, new UnsignedShortType());
		final ImgLabeling<Integer, UnsignedShortType> labeling =
				new ImgLabeling<>(indexImg);

		opService.run("labeling.cca", labeling, binaryImg,
				StructuringElement.FOUR_CONNECTED);

		List<Peak> objects = new ArrayList<>();

		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
		for (LabelRegion<Integer> region : regions) {
			Polygon2D poly = opService.geom().contour(region, true);
			float[] xPoints = new float[poly.numVertices()];
			float[] yPoints = new float[poly.numVertices()];
			for (int i = 0; i < poly.numVertices(); i++) {
				RealLocalizable p = poly.vertex(i);
				xPoints[i] = p.getFloatPosition(0);
				yPoints[i] = p.getFloatPosition(1);
			}
			PolygonRoi r = new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
			r = new PolygonRoi(r.getInterpolatedPolygon(1, false), Roi.POLYGON);
			r = smoothPolygonRoi(r);
			r = new PolygonRoi(r.getInterpolatedPolygon(Math.min(2, r
					.getNCoordinates() * 0.1), false), Roi.POLYGON);

			double[] xs = new double[r.getFloatPolygon().xpoints.length];
			double[] ys = new double[r.getFloatPolygon().ypoints.length];
			for (int i = 0; i < xs.length; i++) {
				xs[i] = r.getFloatPolygon().xpoints[i] / interpolationFactor;
				ys[i] = r.getFloatPolygon().ypoints[i] / interpolationFactor;
			}

			Peak peak = PeakShape.createPeak(xs, ys);
			final double area = peak.getShape().area();
			peak.setProperty(Peak.AREA, area);

			final double perimeter = peak.getShape().perimeter();
			peak.setProperty(Peak.PERIMETER, perimeter);

			final double circularity = peak.getShape().circularity();
			peak.setProperty(Peak.CIRCULARITY, circularity);

			if (useAreaFilter) {
				if (area > minArea) objects.add(peak);
			} else objects.add(peak);
		}

		objects = MarsImageUtils.removeNearestNeighbors(objects, minimumDistance);

		// Set the T for the Peaks
		objects.forEach(p -> p.setT(t));

		return objects;
	}

	private PolygonRoi smoothPolygonRoi(PolygonRoi r) {
		FloatPolygon poly = r.getFloatPolygon();
		FloatPolygon poly2 = new FloatPolygon();
		int nPoints = poly.npoints;
		for (int i = 0; i < nPoints; i += 2) {
			int iMinus = (i + nPoints - 1) % nPoints;
			int iPlus = (i + 1) % nPoints;
			poly2.addPoint((poly.xpoints[iMinus] + poly.xpoints[iPlus] +
					poly.xpoints[i]) / 3, (poly.ypoints[iMinus] + poly.ypoints[iPlus] +
					poly.ypoints[i]) / 3);
		}
		return new PolygonRoi(poly2, Roi.POLYGON);
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
			if (previewOverlay == null) previewOverlay = new ObjectPreviewOverlay();
			if (!activeOverlay) {
				overlaySource = BdvFunctions.showOverlay(previewOverlay, "Object-Preview", Bdv
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
	
	public class ObjectPreviewOverlay extends BdvOverlay {
		private Color intersectionFillColor = new Color( 0x88994499, true );
		//https://github.com/bigdataviewer/bigdataviewer-core/blob/9d54b96f2b789ccd21e828db17cd41944bd18704/src/main/java/bdv/tools/boundingbox/TransformedBoxOverlay.java
		
		public ObjectPreviewOverlay() {}

		@Override
		protected void draw(Graphics2D g) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);
			
			Interval selection = selectionToViewTransform(transform);
			g.setPaint( intersectionFillColor );
			g.fillRect((int) selection.min(0), (int) selection.min(1), (int)(selection.max(0) - selection.min(0)), (int)(selection.max(1) - selection.min(1)));
			
		  	List<Peak> peaks = findObjectsInT(info.getTimePointIndex());
		  
			if (!peaks.isEmpty()) {
				g.setColor(getColor());
				g.setStroke(new BasicStroke(2));

				for (Peak peak : peaks) {
					PeakShape shape = peak.getShape();

					boolean sourceInitialized = false;
					int xSource = 0;
					int ySource = 0;
					int x1 = 0;
					int y1 = 0;
					for (int pIndex = 0; pIndex < shape.x.length; pIndex++) {
						double x = shape.x[pIndex];
						double y = shape.y[pIndex];

						if (Double.isNaN(x) || Double.isNaN(y)) continue;

						final double[] globalCoords = new double[] { x, y };
						final double[] viewerCoords = new double[2];
						transform.apply(globalCoords, viewerCoords);

						int xTarget = (int) Math.round(viewerCoords[0]);
						int yTarget = (int) Math.round(viewerCoords[1]);

						if (sourceInitialized) g.drawLine(xSource, ySource, xTarget, yTarget);
						else {
							x1 = xTarget;
							y1 = yTarget;
						}

						xSource = xTarget;
						ySource = yTarget;
						sourceInitialized = true;
					}

					if (x1 != xSource || y1 != ySource) g.drawLine(xSource, ySource, x1,
							y1);
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
	
	public void setMinimumDistance(int minimumDistance) {
		this.minimumDistance = minimumDistance;
	}
	
	public int getMinimumDistance() {
		return minimumDistance;
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
	
	public void setExcludedTimePointsList(String excludeTimePointList) {
		this.excludeTimePointList = excludeTimePointList;
	}
	
	public String getExcludedTimePointsList() {
		return this.excludeTimePointList;
	}

	public void setUseLocalOstu(boolean useLocalOstu) {
		this.useLocalOstu = useLocalOstu;
	}

	public boolean getUseLocalOstu() {
		return useLocalOstu;
	}

	public void setLocalOtsuRadius(int otsuRadius) {
		this.otsuRadius = otsuRadius;
	}

	public double getLocalOtsuRadius() {
		return otsuRadius;
	}

	public void setInterpolationFactor(double interpolationFactor) {
		this.interpolationFactor = interpolationFactor;
	}

	public double getInterpolationFactor() {
		return this.interpolationFactor;
	}

	public void setUseAreaFilter(boolean useAreaFilter) {
		this.useAreaFilter = useAreaFilter;
	}

	public boolean getUseAreaFilter() {
		return this.useAreaFilter;
	}

	public void setMinimumArea(double minArea) {
		this.minArea = minArea;
	}

	public double getMinimumArea() {
		return this.minArea;
	}

	public void setVerboseOutput(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean getVerboseOutput() {
		return verbose;
	}

}

