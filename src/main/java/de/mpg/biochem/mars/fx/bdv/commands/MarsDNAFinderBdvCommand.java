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

package de.mpg.biochem.mars.fx.bdv.commands;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JDialog;

import net.imagej.ops.OpService;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
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
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOverlay;
import bdv.util.BdvOverlaySource;
import bdv.viewer.Source;
import de.mpg.biochem.mars.fx.bdv.MarsBdvFrame;
import de.mpg.biochem.mars.fx.molecule.AbstractMoleculeArchiveFxFrame;
import de.mpg.biochem.mars.image.DNAFinder;
import de.mpg.biochem.mars.image.DNASegment;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;
import javafx.application.Platform;

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
@Plugin(type = Command.class, label = "Bdv DNA Finder")
public class MarsDNAFinderBdvCommand extends InteractiveCommand implements Command,
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
	private ConvertService convertService;

	private MarsBdvFrame marsBdvFrame;
	
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
	 * Global Settings
	 */

	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "previewChanged")
	private boolean preview = false;
	
	@Parameter(label = "Create DNA molecule records",
			description = "Creates records for the DNA molecules",
			callback = "createDNAmoleculeRecords", persist = false)
	private Button addDNAsButton;

	//@Parameter(label = "Label", style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE +
	//	", group:Preview", choices = { "Median intensity", "Variance intensity" })
	//private String previewLabelType;

	//@Parameter(label = "Timeout (s)", style = "group:Preview")
	//private int previewTimeout = 10;
	
	private Interval interval;
	private DnaMoleculePreviewOverlay previewOverlay;
	private BdvOverlaySource<?> overlaySource;
	private boolean selectionInProgress = false;
	private boolean activeOverlay = false;
	private List<DNASegment> segments;

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
		//DNA finding is triggered when the timepoint is changed in the overlay.
		//the overlay is added when preview is on.
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T> & NativeType<T>> List<DNASegment> findDNAsInT(int t)
	{
		Source<T> bdvSource = marsBdvFrame.getSource(source);
		
		//Remove the Z dimension
		RandomAccessibleInterval<T> img = Views.hyperSlice(bdvSource.getSource(t, 0), 2, 0);

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
		
		final AffineTransform3D bdvSourceTransform = new AffineTransform3D();
		bdvSource.getSourceTransform(t, 0, bdvSourceTransform);
		
		Interval transformedInterval = getTransformedInterval(interval, bdvSourceTransform);
		RandomAccessibleInterval<T> imgView = Views.interval(img, Intervals.createMinMax(transformedInterval.min(0), transformedInterval.min(1), transformedInterval.max(0), transformedInterval.max(1))); 
		
		List<DNASegment> dnas = dnaFinder.findDNAs(imgView, imgView, t, 1);
		
	  //Now we transform from the original image coordinates to the BDV view coordinates.
		for (DNASegment dna : dnas) {
			double[] source = new double[] { dna.getX1(), dna.getY1(), 0 };
			double[] target = new double[3];
			bdvSourceTransform.apply(source, target);
			dna.setX1(target[0]);
			dna.setY1(target[1]);
			
			source[0] = dna.getX2();
			source[1] = dna.getY2();
			bdvSourceTransform.apply(source, target);
			dna.setX2(target[0]);
			dna.setY2(target[1]);
		}
		
		return dnas;
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
			if (previewOverlay == null) previewOverlay = new DnaMoleculePreviewOverlay();
			if (!activeOverlay) {
				overlaySource = BdvFunctions.showOverlay(previewOverlay, "DNA-Preview", Bdv
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
		
		//TODO make sure the currently selected Metadata is the one whose size is retrieved in the future.. This assumes all metadata records would be for images of similar sizes...
		final Interval rangeInterval = Intervals.createMinMax( 0, 0, 0, archive.getMetadata(0).getImage(0).getSizeX(), archive.getMetadata(0).getImage(0).getSizeY(), 0 );
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
	
	protected void createDNAmoleculeRecords() {
		//save the current settings to the PrefService 
		//so they are reloaded the next time the command is opened.
		saveInputs();
		if (archive != null) {
			archive.getWindow().lock();
			List<String> uids = new ArrayList<>();
			for (DNASegment segment : segments) {
				//Add an empty table...
				MarsTable table = new MarsTable("table");
				
				//Build molecule record with DNA location
				Molecule molecule = archive.createMolecule(MarsMath.getUUID58(), table);
				molecule.setMetadataUID(marsBdvFrame.getMetadataUID());
				molecule.setImage(archive.getMetadata(marsBdvFrame.getMetadataUID()).getImage(0).getImageID());
				molecule.setParameter("Dna_Top_X1", segment.getX1());
				molecule.setParameter("Dna_Top_Y1", segment.getY1());
				molecule.setParameter("Dna_Bottom_X2", segment.getX2());
				molecule.setParameter("Dna_Bottom_Y2", segment.getY2());
				
				molecule.addTag("Bdv DNA Finder");
				molecule.setNotes("DnaMolecule created on " + new java.util.Date() + " by the MarsDNAFinderBdvCommand");
				//add to archive
				archive.put(molecule);
				//should add something to the archive log ... logService.info("Added DnaMolecule record " + molecule.getUID());
				uids.add(molecule.getUID());
			}
			
			LogBuilder builder = new LogBuilder();
			String log = LogBuilder.buildTitleBlock(getInfo().getLabel());
			
			String uidList = uids.get(0);
			for (int i=1; i<uids.size(); i++)
				uidList = uidList + ", " + uids.get(i);
			
			builder.addParameter("Created DnaMolecules", uidList);
			addInputParameterLog(builder);
			log += builder.buildParameterList();
			log += "\n" + LogBuilder.endBlock();
			archive.getMetadata(marsBdvFrame.getMetadataUID()).logln(log);
			
			archive.getWindow().unlock();
			final String lastUID = uids.get(uids.size() - 1);
			Platform.runLater(() -> ((AbstractMoleculeArchiveFxFrame) archive.getWindow()).getMoleculesTab().setSelectedMolecule(lastUID));
			marsBdvFrame.setMolecule(archive.get(lastUID));
		}
	}
	
	private void addInputParameterLog(LogBuilder builder) {
		builder.addParameter("Metadata UID", marsBdvFrame.getMetadataUID());
		builder.addParameter("Region", "(" + interval.min(0) + ", " + interval.min(1) + ") to (" + interval.max(0) + ", " + interval.max(1) + ")");
		builder.addParameter("Source", source);
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
	}
	
	public class DnaMoleculePreviewOverlay extends BdvOverlay {
		private Color intersectionFillColor = new Color( 0x88994499, true );
		//https://github.com/bigdataviewer/bigdataviewer-core/blob/9d54b96f2b789ccd21e828db17cd41944bd18704/src/main/java/bdv/tools/boundingbox/TransformedBoxOverlay.java
		
		public DnaMoleculePreviewOverlay() {}

		@Override
		protected void draw(Graphics2D g) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);
			
			Interval selection = selectionToViewTransform(transform);
			g.setPaint( intersectionFillColor );
			g.fillRect((int) selection.min(0), (int) selection.min(1), (int)(selection.max(0) - selection.min(0)), (int)(selection.max(1) - selection.min(1)));
			
		  segments = findDNAsInT(info.getTimePointIndex());
		  
			if (segments.size() > 0) {
				for (DNASegment segment : segments) {
					if (Double.isNaN(segment.getX1()) || Double.isNaN(segment.getY1()) || Double.isNaN(segment
						.getX2()) || Double.isNaN(segment.getY2())) return;
	
					final double[] globalCoords = new double[] { segment.getX1(), segment.getY1() };
					final double[] viewerCoords = new double[2];
					transform.apply(globalCoords, viewerCoords);
	
					int xSource = (int) Math.round(viewerCoords[0]);
					int ySource = (int) Math.round(viewerCoords[1]);
	
					final double[] globalCoords2 = new double[] { segment
						.getX2(), segment.getY2() };
					final double[] viewerCoords2 = new double[2];
					transform.apply(globalCoords2, viewerCoords2);
	
					int xTarget = (int) Math.round(viewerCoords2[0]);
					int yTarget = (int) Math.round(viewerCoords2[1]);
	
					g.setColor(getColor());
					g.setStroke(new BasicStroke(2));
					g.drawLine(xSource, ySource, xTarget, yTarget);
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
}
