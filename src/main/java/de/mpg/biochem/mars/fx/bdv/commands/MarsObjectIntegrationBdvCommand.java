/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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
import de.mpg.biochem.mars.image.MarsImageUtils;
import de.mpg.biochem.mars.image.Peak;
import de.mpg.biochem.mars.image.PeakShape;
import de.mpg.biochem.mars.image.PeakTracker;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.object.MartianObject;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsUtil;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.Initializable;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.InteractiveCommand;
import org.scijava.command.Previewable;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.DoubleArray;
import org.scijava.widget.Button;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(type = Command.class, label = "Bdv Object Integrator")
public class MarsObjectIntegrationBdvCommand extends DynamicCommand implements Command,
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
	
	@Parameter(label = "Source", choices = { "a", "b", "c" },
		style = "group:Input", persist = false)
	private String source = "";

	@Parameter(label = "Background integration radius")
	private long radius = 10;

	@Parameter(label = "All Objects")
	private boolean integrateAll = false;

	@Parameter(label = "Threads", required = false, min = "1", max = "120")
	private int nThreads = Runtime.getRuntime().availableProcessors();

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private String imgIntegrationMessage = "Image Integration Boundaries (in pixels)";

	@Parameter(label = " X0")
	private int X0 = 0;

	@Parameter(label = " Y0")
	private int Y0 = 0;

	@Parameter(label = " width")
	private int width = 1024;

	@Parameter(label = " height")
	private int height = 1024;

	private AtomicInteger progressInteger = new AtomicInteger(0);

	private Set<String> sourceNotPresent = new HashSet<>();

	private Interval imgInterval;

	private String objectUID;
	
	@Override
	public void initialize() {
		final MutableModuleItem<String> channelItems = getInfo().getMutableInput(
			"source", String.class);
		channelItems.setChoices(marsBdvFrame.getSourceNames());
	}
	
	@Override
	public void run() {
		archive.getWindow().lock();
		//save the current settings to the PrefService
		//so they are reloaded the next time the command is opened.
		saveInputs();
		archive.getWindow().updateLockMessage((integrateAll) ? "Integrating objects" : "Integrating object");
		imgInterval = Intervals.createMinMax(X0, Y0, X0 + width - 1,
				Y0 + height - 1);

		if (integrateAll) {
			//Integrate all with one thread per object
			List<Runnable> tasks = new ArrayList<>();
			archive.molecules().forEach( object -> tasks.add(() -> integrateObject((MartianObject) object)));
			MarsUtil.threadPoolBuilder(statusService, logService, () -> {
				archive.getWindow().setProgress((double) progressInteger.get() / archive.getNumberOfMolecules());
			}, tasks, nThreads);
		} else {
			//Integrate just one object
			MartianObject object = (MartianObject) archive.get(objectUID);
			integrateObject(object);
		}

		LogBuilder builder = new LogBuilder();
		String log = LogBuilder.buildTitleBlock(getInfo().getLabel());
		addInputParameterLog(builder);
		log += builder.buildParameterList();

		String successMessage = log + "\n" + LogBuilder.endBlock(true);
		String failMessage = log + "Requested source " + source + " does not exist.\n" + LogBuilder.endBlock(false);

		if (integrateAll)
			archive.metadata().forEach(metadata -> {
				if (sourceNotPresent.contains(metadata.getUID()))
					metadata.logln(failMessage);
				else
					metadata.logln(successMessage);
			});
		else archive.getMetadata(marsBdvFrame.getMetadataUID()).logln(successMessage);
		archive.getWindow().unlock();
	}

	private void integrateObject(MartianObject object) {
		ConcurrentMap<Integer, Double> tToShapeSum = new ConcurrentHashMap<>();
		ConcurrentMap<Integer, Double> tToPixelMedianBackground = new ConcurrentHashMap<>();
		ConcurrentMap<Integer, Double> tToShapeIntensity = new ConcurrentHashMap<>();
		if (integrateAll) {
			for (int t : object.getShapeKeys()) integrateObjectInT(t, object, tToShapeSum, tToPixelMedianBackground, tToShapeIntensity);
		} else {
			List<Runnable> tasks = new ArrayList<>();
			for (int t : object.getShapeKeys()) tasks.add(() -> integrateObjectInT(t, object, tToShapeSum, tToPixelMedianBackground, tToShapeIntensity));
			MarsUtil.threadPoolBuilder(statusService, logService, () -> {
				archive.getWindow().setProgress((double) progressInteger.get() / object.getShapeKeys().size());
			}, tasks, nThreads);
		}
		//Add sum, median pixel background, and intensity (sum - medianPixelBG * numPixels...) to table
		object.getTable().rows().forEach(row -> {
			int theT = (int)row.getValue("T");
			row.setValue(source + "_Median_Background_Pixel", tToPixelMedianBackground.getOrDefault(theT, Double.NaN));
			row.setValue(source + "_Sum_Pixels", tToShapeSum.getOrDefault(theT, Double.NaN));
			row.setValue(source + "_Intensity", tToShapeIntensity.getOrDefault(theT, Double.NaN));
		});
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> void integrateObjectInT(
			int t, MartianObject object, ConcurrentMap<Integer, Double> tToShapeSum, ConcurrentMap<Integer, Double> tToPixelMedianBackground, ConcurrentMap<Integer, Double> tToShapeIntensity)
	{
		if (!marsBdvFrame.getSourceNames(object.getMetadataUID()).contains(source)) {
			sourceNotPresent.add(object.getMetadataUID());
			return;
		}
		Source<T> bdvSource = marsBdvFrame.getSource(object.getMetadataUID(), source);

		//Remove the Z dimension
		RandomAccessibleInterval<T> img = Views.hyperSlice(bdvSource.getSource(t, 0), 2, 0);

		//Find shape boundaries that define integration interval
		PeakShape shape = object.getShape(t);
		double xmin = Double.POSITIVE_INFINITY;
		double xmax = Double.NEGATIVE_INFINITY;
		for (double x: shape.x) {
			if (x < xmin) xmin = x;
			if (x > xmax) xmax = x;
		}

		double ymin = Double.POSITIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		for (double y: shape.y) {
			if (y < ymin) ymin = y;
			if (y > ymax) ymax = y;
		}

		Interval integrationInterval = Intervals.createMinMax((long) xmin - radius, (long) ymin - radius, (long) xmax + radius, (long) ymax + radius);

		//remove regions outside imgInterval. For example, on other half of dual view.
		Interval finalInterval = Intervals.intersect(imgInterval, integrationInterval);
		img = Views.interval(img, finalInterval);

		//Sum pixels inside shape
		final WritablePolygon2D polygon = GeomMasks.closedPolygon2D(shape.x, shape.y);
		final IterableRegion<BoolType> region = Masks.toIterableRegion(polygon);
		IterableInterval<T> neighborhood = Regions.sample(region, Views.extendMirrorDouble(img));
		final DoubleArray intensities = new DoubleArray();
		for ( final T pixel : neighborhood )
		{
			final double val = pixel.getRealDouble();
			if ( Double.isNaN( val ) )
				continue;
			intensities.addValue( val );
		}

		double sum;
		if ( intensities.isEmpty() ) sum = Double.NaN;
		else
		{
			sum = 0;
			for ( int i = 0; i < intensities.size(); i++ )
				sum += intensities.getArray()[ i ];
		}
		tToShapeSum.put(t, sum);

		//find median in pixel values in local background surrounding shape
		final WritableBox innerBox = GeomMasks.closedBox(new double[] { xmin, ymin }, new double[] { xmax, ymax });
		final WritableBox outerBox = GeomMasks.closedBox(new double[] { xmin - radius, ymin - radius}, new double[] { xmax + radius, ymax + radius });
		final IterableRegion<BoolType> backgroundRegion = Masks.toIterableRegion(outerBox.minus(innerBox));
		IterableInterval<T> backgroundNeighborhood = Regions.sample(backgroundRegion, Views.extendMirrorDouble(img));
		final DoubleArray backgroundPixels = new DoubleArray();
		for ( final T pixel : backgroundNeighborhood )
		{
			final double val = pixel.getRealDouble();
			if ( Double.isNaN( val ) )
				continue;
			backgroundPixels.addValue( val );
		}
		Util.quicksort( backgroundPixels.getArray(), 0, backgroundPixels.size() - 1 );

		final double medianBackgroundPixelValue = Double.valueOf( backgroundPixels.getArray()[ backgroundPixels.size() / 2 ] );
		tToPixelMedianBackground.put(t, medianBackgroundPixelValue);

		//Calculate background subtracted intensity
		tToShapeIntensity.put(t, Double.valueOf( sum - medianBackgroundPixelValue * intensities.size() ));

		progressInteger.incrementAndGet();
	}
	
	private void addInputParameterLog(LogBuilder builder) {
		builder.addParameter("Source", source);
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

	public void setSelectedObjectUID(String objectUID) {
		this.objectUID = objectUID;
	}

	public String getSelectedObjectUID() {
		return this.objectUID;
	}
}

