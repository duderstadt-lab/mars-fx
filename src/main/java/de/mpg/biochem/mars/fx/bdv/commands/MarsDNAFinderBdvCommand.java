/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2022 Karl Duderstadt
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.imagej.ops.Initializable;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;

import org.decimal4j.util.DoubleRounder;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Previewable;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import bdv.viewer.Source;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.image.DNAFinder;
import de.mpg.biochem.mars.image.DNASegment;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTableService;
import de.mpg.biochem.mars.util.LogBuilder;
import ij.gui.Line;
import ij.gui.Overlay;

/**
 * Finds the location of vertically aligned DNA molecules within the specified
 * length range. Calculates the vertical gradient of the image and applies a DoG
 * filter. Then a search for pairs of positive and negative peaks is conducted.
 * Vertically aligned pairs with the range provided considered DNA molecules.
 * The ends can be fit with subpixel accuracy. Output can be the number of
 * molecules or a list of positions provided as a table. Alternatively, line
 * Rois can be added to the RoiManager, which can be used to create
 * DnaMoleculeArchives. Thresholds for the intensity and variance in intensity
 * can be applied to further filter the DNA molecule located.
 *
 * @author Karl Duderstadt
 */
public class MarsDNAFinderBdvCommand extends DynamicCommand implements Command,
	Initializable, Previewable
{

	/**
	 * SERVICES
	 */
	@Parameter
	private LogService logService;

	@Parameter
	private OpService opService;

	@Parameter
	private UIService uiService;

	@Parameter
	private StatusService statusService;

	@Parameter
	private MarsTableService marsTableService;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private PlatformService platformService;

	/**
	 * IMAGE
	 */
	@Parameter
	protected MarsBdvFrame marsBdvFrame;
	
	@Parameter(label = "Molecule Archive")
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	/**
	 * INPUT SETTINGS
	 */

	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel",
		persist = false)
	private String inputGroup = "Input";

	@Parameter(visibility = ItemVisibility.MESSAGE,
		style = "group:Input, align:center", persist = false)
	private String inputDetails = "Images with Y-axis aligned DNA molecules";

	@Parameter(visibility = ItemVisibility.MESSAGE, style = "image, group:Input",
		persist = false)
	private String inputFigure = "DNAImageInput.png";

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

	@Parameter(label = "Gaussian Smoothing Sigma", style = "group:Find")
	private double gaussSigma = 2;

	@Parameter(label = "DoG filter", style = "group:Find")
	private boolean useDogFilter = true;

	@Parameter(label = "DoG radius", style = "group:Find")
	private double dogFilterRadius = 2;

	@Parameter(label = "Threshold", style = "group:Find")
	private double threshold = 50;

	@Parameter(label = "DNA separation (pixels)", style = "group:Find")
	private int minimumDistance = 6;

	@Parameter(label = "DNA length (pixels)", style = "group:Find")
	private int optimalDNALength = 38;

	@Parameter(label = "DNA end search Y (pixels)", style = "group:Find")
	private int yDNAEndSearchRadius = 6;

	@Parameter(label = "DNA end search X (pixels)", style = "group:Find")
	private int xDNAEndSearchRadius = 5;

	@Parameter(label = "Filter by median intensity", style = "group:Find")
	private boolean medianIntensityFilter = false;

	@Parameter(label = "Median DNA intensity lower bound", style = "group:Find")
	private int medianIntensityLowerBound = 0;

	@Parameter(label = "Filter by intensity variance", style = "group:Find")
	private boolean varianceFilter = false;

	@Parameter(label = "DNA intensity variance upper bound", style = "group:Find")
	private int varianceUpperBound = 1_000_000;

	/**
	 * FITTER SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String fitGroup = "Fit";

	@Parameter(label = "Fit ends", style = "group:Fit")
	private boolean fit = false;

	@Parameter(label = "2nd order", style = "group:Fit")
	private boolean fitSecondOrder = false;

	@Parameter(label = "Radius", style = "group:Fit")
	private int fitRadius = 4;

	/**
	 * OUTPUT SETTINGS
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE, style = "groupLabel")
	private String outputGroup = "Output";

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

	@Parameter(label = "Label", style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE +
		", group:Preview", choices = { "Median intensity", "Variance intensity" })
	private String previewLabelType;

	@Parameter(label = "T", min = "0", max = "100", style = NumberWidget.SCROLL_BAR_STYLE +
		", group:Preview", persist = false)
	private int theT;

	@Parameter(label = "Timeout (s)", style = "group:Preview")
	private int previewTimeout = 10;

	// A map with peak lists for each slice for an image stack
	private ConcurrentMap<Integer, List<DNASegment>> dnaStack;

	@Override
	public void initialize() {
		final MutableModuleItem<String> channelItems = getInfo().getMutableInput(
			"source", String.class);
		channelItems.setChoices(marsBdvFrame.getSourceNames());

		//final MutableModuleItem<Integer> preFrame = getInfo().getMutableInput(
		//	"theT", Integer.class);
		//preFrame.setValue(this, marsBdvFrame.getBdvHandle().getViewerPanel());
		//preFrame.setMaximumValue();
	}

	@Override
	public void run() {

		// Build log
		LogBuilder builder = new LogBuilder();
		String log = LogBuilder.buildTitleBlock("DNA Finder");
		addInputParameterLog(builder);
		log += builder.buildParameterList();
		logService.info(log);

		// Used to store dna list for multiframe search
		dnaStack = new ConcurrentHashMap<>();

		double starttime = System.currentTimeMillis();
		logService.info("Finding DNAs...");
		
		//dnaStack.put(theT, findDNAsInT(Integer.valueOf(channel), theT, rois, nThreads));

		logService.info("Time: " + DoubleRounder.round((System.currentTimeMillis() -
			starttime) / 60000, 2) + " minutes.");

		logService.info("Finished in " + DoubleRounder.round((System
			.currentTimeMillis() - starttime) / 60000, 2) + " minutes.");
		logService.info(LogBuilder.endBlock(true));
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<DNASegment> findDNAsInT(int t, int numTheads)
	{

		Source<T> bdvSource = marsBdvFrame.getSource(source);
		
		RandomAccessibleInterval<T> img = bdvSource.getSource(theT, 0);

		DNAFinder<T> dnaFinder = new DNAFinder<>(opService);
		dnaFinder.setGaussianSigma(gaussSigma);
		dnaFinder.setOptimalDNALength(optimalDNALength);
		dnaFinder.setMinimumDistance(minimumDistance);
		dnaFinder.setXDNAEndSearchRadius(xDNAEndSearchRadius);
		dnaFinder.setYDNAEndSearchRadius(yDNAEndSearchRadius);
		dnaFinder.setUseDogFiler(useDogFilter);
		dnaFinder.setDogFilterRadius(dogFilterRadius);
		dnaFinder.setThreshold(threshold);
		dnaFinder.setFilterByMedianIntensity(medianIntensityFilter);
		dnaFinder.setMedianIntensityLowerBound(medianIntensityLowerBound);
		dnaFinder.setFilterByVariance(varianceFilter);
		dnaFinder.setVarianceUpperBound(varianceUpperBound);
		dnaFinder.setFit(fit);
		dnaFinder.setFitSecondOrder(fitSecondOrder);
		dnaFinder.setFitRadius(fitRadius);

		List<IterableRegion<BoolType>> regionList =
			new ArrayList<IterableRegion<BoolType>>();
		/*		for (int i = 0; i < processingRois.length; i++) {
			// Convert from Roi to IterableInterval
			RealMask roiMask = convertService.convert(processingRois[i],
				RealMask.class);
			IterableRegion<BoolType> iterableROI = MarsImageUtils.toIterableRegion(
				roiMask, img);
			regionList.add(iterableROI);
		}
*/
		return dnaFinder.findDNAs(img, regionList, t, numTheads);
	}

	@Override
	public void preview() {
		if (preview) {
			ExecutorService es = Executors.newSingleThreadExecutor();
			try {
				es.submit(() -> {

					List<DNASegment> segments = findDNAsInT(theT, Runtime.getRuntime().availableProcessors());

					if (Thread.currentThread().isInterrupted()) return;

					Overlay overlay = new Overlay();
					if (segments.size() > 0) {
						for (DNASegment segment : segments) {
							Line line = new Line(segment.getX1(), segment.getY1(), segment
								.getX2(), segment.getY2());

							double value = Double.NaN;
							if (previewLabelType.equals("Variance intensity")) value = segment
								.getVariance();
							else if (previewLabelType.equals("Median intensity")) value =
								segment.getMedianIntensity();

							if (Double.isNaN(value)) line.setName("");
							if (value > 1_000_000) line.setName(DoubleRounder.round(value /
								1_000_000, 2) + " m");
							else if (value > 1000) line.setName(DoubleRounder.round(value /
								1000, 2) + " k");
							else line.setName((int) value + "");

							overlay.add(line);
							if (Thread.currentThread().isInterrupted()) return;
						}
						overlay.drawLabels(true);
						overlay.drawNames(true);
						overlay.setLabelColor(new Color(255, 255, 255));
					}

					final String countString = "count: " + segments.size();
					final MutableModuleItem<String> preFrameCount = getInfo()
						.getMutableInput("tDNACount", String.class);
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
		//
	}

	/** Called when the {@link #preview} parameter value changes. */
	protected void previewChanged() {
		// When preview box is unchecked, reset the Roi back to how it was before...
		if (!preview) cancel();
	}

	private void addInputParameterLog(LogBuilder builder) {
		
		//builder.addParameter("Dataset name", dataset.getName());
		//builder.addParameter("Channel", channel);
		builder.addParameter("Gaussian smoothing sigma", String.valueOf(
			this.gaussSigma));
		builder.addParameter("DoG filter", String.valueOf(useDogFilter));
		builder.addParameter("DoG radius", String.valueOf(dogFilterRadius));
		builder.addParameter("Threshold", String.valueOf(threshold));
		builder.addParameter("Minimum distance", String.valueOf(minimumDistance));
		builder.addParameter("Optimal DNA length", String.valueOf(
			optimalDNALength));
		builder.addParameter("DNA end search radius Y", String.valueOf(
			yDNAEndSearchRadius));
		builder.addParameter("DNA end search radius X", String.valueOf(
			xDNAEndSearchRadius));
		builder.addParameter("Filter by median intensity", String.valueOf(
			medianIntensityFilter));
		builder.addParameter("Median intensity lower bound", String.valueOf(
			medianIntensityLowerBound));
		builder.addParameter("Filter by variance", String.valueOf(varianceFilter));
		builder.addParameter("Intensity variance upper bound", String.valueOf(
			varianceUpperBound));
		builder.addParameter("Fit peaks", String.valueOf(fit));
		builder.addParameter("Fit radius", String.valueOf(fitRadius));
		builder.addParameter("Fit 2nd order", String.valueOf(fitSecondOrder));
		builder.addParameter("Thread count", nThreads);
	}

	public void setT(int theT) {
		this.theT = theT;
	}

	public int getT() {
		return theT;
	}

	public void setGaussianSigma(double gaussSigma) {
		this.gaussSigma = gaussSigma;
	}

	public double getGaussianSigma() {
		return gaussSigma;
	}

	public void setUseDogFiler(boolean useDogFilter) {
		this.useDogFilter = useDogFilter;
	}

	public void setDogFilterRadius(double dogFilterRadius) {
		this.dogFilterRadius = dogFilterRadius;
	}

	public void setThreshold(double threshold) {
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

	public void setOptimalDNALength(int optimalDNALength) {
		this.optimalDNALength = optimalDNALength;
	}

	public int getOptimalDNALength() {
		return optimalDNALength;
	}

	public void setYDNAEndSearchRadius(int yDNAEndSearchRadius) {
		this.yDNAEndSearchRadius = yDNAEndSearchRadius;
	}

	public int getYDNAEndSearchRadius() {
		return yDNAEndSearchRadius;
	}

	public void setXDNAEndSearchRadius(int xDNAEndSearchRadius) {
		this.xDNAEndSearchRadius = xDNAEndSearchRadius;
	}

	public int getXDNAEndSearchRadius() {
		return xDNAEndSearchRadius;
	}

	public void setFilterByVariance(boolean varianceFilter) {
		this.varianceFilter = varianceFilter;
	}

	public boolean getFilterByVariance() {
		return this.varianceFilter;
	}

	public double getVarianceUpperBound() {
		return varianceUpperBound;
	}

	public void setVarianceUpperBound(int varianceUpperBound) {
		this.varianceUpperBound = varianceUpperBound;
	}

	public void setFilterByMedianIntensity(boolean medianIntensityFilter) {
		this.medianIntensityFilter = medianIntensityFilter;
	}

	public boolean getFilterByMedianIntensity() {
		return medianIntensityFilter;
	}

	public void setMedianIntensityLowerBound(int medianIntensityLowerBound) {
		this.medianIntensityLowerBound = medianIntensityLowerBound;
	}

	public int getMedianIntensityLowerBound() {
		return medianIntensityLowerBound;
	}

	public void setFit(boolean fit) {
		this.fit = fit;
	}

	public boolean getFit() {
		return fit;
	}

	public void setFitSecondOrder(boolean fitSecondOrder) {
		this.fitSecondOrder = fitSecondOrder;
	}

	public boolean getFitSecondOrder() {
		return fitSecondOrder;
	}

	public void setFitRadius(int fitRadius) {
		this.fitRadius = fitRadius;
	}

	public int getFitRadius() {
		return fitRadius;
	}

	public void setThreads(int nThreads) {
		this.nThreads = nThreads;
	}

	public int getThreads() {
		return this.nThreads;
	}
}
