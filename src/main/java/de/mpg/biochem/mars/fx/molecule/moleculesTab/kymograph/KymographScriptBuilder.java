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

package de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph;

import java.util.List;
import java.util.stream.Collectors;

import de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph.KymographSettings.ChannelSettings;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.kymograph.KymographSettings.Mode;

/**
 * Turns {@link KymographSettings} into a Groovy script that uses
 * mars-kymograph. The same script is run by the kymograph tab and can be added
 * to the Comments as a groovy-image-widget fenced code block, so the GUI and
 * the scripts never drift apart.
 */
public class KymographScriptBuilder {

	// ImageFormatter.setScale is only available in newer mars-kymograph versions.
	private static final boolean SUPPORTS_SCALE = hasScale();

	private static boolean hasScale() {
		try {
			Class.forName(KymographArchiveType.KYMOGRAPH_PACKAGE + ".ImageFormatter")
				.getMethod("setScale", double.class);
			return true;
		}
		catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
			return false;
		}
	}

	public static final String IMAGE_OUTPUT = "imgsrc";
	public static final String DISPLAY_RANGES_OUTPUT = "displayRanges";
	public static final String SOURCE_IMAGE_OUTPUT = "sourceImage";
	public static final String SOURCE_IMAGE_INPUT = "cachedImage";
	public static final String WIDGET_KEYWORD = "groovy-image-widget";

	private static final String INDENT = "        ";

	private final KymographSettings settings;
	private final KymographArchiveType archiveType;
	private final String UID;
	private boolean reportDisplayRanges = false;
	private boolean outputSourceImage = false;
	private boolean useSourceImage = false;
	private String svgPath = null;

	public KymographScriptBuilder(KymographSettings settings,
		KymographArchiveType archiveType, String UID)
	{
		this.settings = settings;
		this.archiveType = archiveType;
		this.UID = UID;
	}

	/**
	 * Add an output with the pixel range and the automatic display range of each
	 * channel, so the GUI can show them and offer them as starting points.
	 *
	 * @param reportDisplayRanges Whether to add the output.
	 * @return This builder.
	 */
	public KymographScriptBuilder reportDisplayRanges(
		boolean reportDisplayRanges)
	{
		this.reportDisplayRanges = reportDisplayRanges;
		return this;
	}

	/**
	 * Add an output with the image built from the source, before formatting,
	 * so it can be formatted again without reading the source.
	 *
	 * @param outputSourceImage Whether to add the output.
	 * @return This builder.
	 */
	public KymographScriptBuilder outputSourceImage(boolean outputSourceImage) {
		this.outputSourceImage = outputSourceImage;
		return this;
	}

	/**
	 * Format an image provided as an input instead of building it from the
	 * source. The image is the source image output by an earlier script with
	 * the same {@link #sourceKey()}.
	 *
	 * @param useSourceImage Whether to take the image as an input.
	 * @return This builder.
	 */
	public KymographScriptBuilder useSourceImage(boolean useSourceImage) {
		this.useSourceImage = useSourceImage;
		return this;
	}

	/**
	 * The part of the script that builds the image from the source. Scripts with
	 * the same key build the same image, so an image can be reused when only
	 * the formatting changed.
	 *
	 * @return The source part of the script.
	 */
	public String sourceKey() {
		StringBuilder sb = new StringBuilder();
		appendSource(sb);
		return sb.toString();
	}

	/**
	 * Also save the figure as an SVG file.
	 *
	 * @param svgPath The file path or null.
	 * @return This builder.
	 */
	public KymographScriptBuilder saveAsSVG(String svgPath) {
		this.svgPath = svgPath;
		return this;
	}

	public boolean isKymograph() {
		return settings.mode == Mode.KYMOGRAPH && archiveType.supportsKymograph();
	}

	public String build() {
		StringBuilder sb = new StringBuilder();
		sb.append("#@ Context scijavaContext\n");
		sb.append("#@ MoleculeArchive archive\n");
		if (useSourceImage) sb.append("#@ Object " + SOURCE_IMAGE_INPUT + "\n");
		sb.append("#@OUTPUT String " + IMAGE_OUTPUT + "\n");
		if (reportDisplayRanges) sb.append("#@OUTPUT String " +
			DISPLAY_RANGES_OUTPUT + "\n");
		if (outputSourceImage) sb.append("#@OUTPUT Object " + SOURCE_IMAGE_OUTPUT +
			"\n");
		sb.append("\n");

		String pkg = KymographArchiveType.KYMOGRAPH_PACKAGE;
		if (useSourceImage) {
			// Nothing to import for building the image.
		}
		else if (isKymograph()) {
			sb.append("import " + pkg + ".DnaArchiveKymographBuilder\n");
		}
		else {
			sb.append("import " + pkg + "." + archiveType.getRegionBuilder() + "\n");
			sb.append("import " + pkg + ".MontageBuilder\n");
		}
		sb.append("import " + pkg + ".ImageFormatter\n\n");

		if (useSourceImage) {
			appendTimeRange(sb);
			sb.append("def image = " + SOURCE_IMAGE_INPUT + "\n\n");
		}
		else appendSource(sb);

		appendFormatter(sb);

		sb.append(IMAGE_OUTPUT + " = formatter.getHtmlEncodedImage()\n");
		if (reportDisplayRanges) appendDisplayRanges(sb);
		if (outputSourceImage) sb.append(SOURCE_IMAGE_OUTPUT + " = image\n");
		if (svgPath != null) sb.append("formatter.saveAsSVG(" + quote(svgPath) +
			")\n");
		return sb.toString();
	}

	/**
	 * Build the script wrapped in a fenced code block for the Comments.
	 *
	 * @return Markdown with the fenced code block.
	 */
	public String buildFencedCodeBlock() {
		return "```" + WIDGET_KEYWORD + "\n" + build() + "```\n";
	}

	private void appendTimeRange(StringBuilder sb) {
		sb.append("def UID = " + quote(UID) + "\n");
		sb.append("def omeImage = archive.getMetadata(archive.get(UID).getMetadataUID()).getImage(0)\n");
		sb.append("def minT = " + (settings.minT < 0 ? "0" : settings.minT) + "\n");
		sb.append("def maxT = " + (settings.maxT < 0 ? "omeImage.getSizeT() - 1"
			: "Math.min(" + settings.maxT + ", omeImage.getSizeT() - 1)") + "\n\n");
	}

	private void appendSource(StringBuilder sb) {
		appendTimeRange(sb);
		if (isKymograph()) appendKymograph(sb);
		else appendMontage(sb);
	}

	private void appendKymograph(StringBuilder sb) {
		sb.append("def image = new DnaArchiveKymographBuilder(scijavaContext, archive)\n");
		line(sb, ".setMolecule(UID)");
		line(sb, ".setProjectionWidth(" + settings.projectionWidth + ")");
		line(sb, ".setMinT(minT)");
		line(sb, ".setMaxT(maxT)");
		appendFrameReduction(sb);
		switch (settings.filter) {
			case MEDIAN:
				line(sb, ".medianFilter(" + (int) settings.filterSize + ")");
				break;
			case GAUSSIAN:
				line(sb, ".gaussianFilter(" + number(settings.filterSize) + ")");
				break;
			case TOPHAT:
				line(sb, ".tophatFilter(" + (int) settings.filterSize + ")");
				break;
			default:
				break;
		}
		if (settings.interpolation > 1) line(sb, ".interpolation(" + number(
			settings.interpolation) + ")");
		if (settings.verticalReflection) line(sb, ".setVerticalReflection(true)");
		line(sb, ".threads(Runtime.getRuntime().availableProcessors())");
		line(sb, ".build()");
		sb.append("\n");
	}

	private void appendMontage(StringBuilder sb) {
		sb.append("def region = new " + archiveType.getRegionBuilder() +
			"(scijavaContext, archive)\n");
		line(sb, ".setMolecule(UID)");
		if (archiveType == KymographArchiveType.SINGLE_MOLECULE) {
			line(sb, ".setRadius(" + settings.spotRadius + ")");
			line(sb, ".setLocationSource(" + quote(settings.xLocationSource) + ", " +
				quote(settings.yLocationSource) + ")");
			if (settings.locationFromParameters) line(sb, ".useParameters(true)");
		}
		else {
			line(sb, ".setBorderWidth(" + settings.borderWidth + ")");
			line(sb, ".setBorderHeight(" + settings.borderHeight + ")");
		}
		line(sb, ".build()");
		sb.append("\n");

		sb.append("def image = new MontageBuilder(scijavaContext)\n");
		line(sb, ".setSource(region)");
		line(sb, ".setMinT(minT)");
		line(sb, ".setMaxT(maxT)");
		line(sb, ".setSpacing(" + settings.spacing + ")");
		line(sb, ".setHorizontalLayout(" + settings.horizontalLayout + ")");
		if (settings.columns > 0) line(sb, ".setColumns(" + settings.columns + ")");
		appendFrameReduction(sb);
		String channel = settings.filterChannel > 0 ? ", " +
			settings.filterChannel : "";
		switch (settings.filter) {
			case MEDIAN:
				line(sb, ".medianFilter(" + (int) settings.filterSize + channel + ")");
				break;
			case GAUSSIAN:
				line(sb, ".gaussianFilter(" + number(settings.filterSize) + channel +
					")");
				break;
			case TOPHAT:
				line(sb, ".tophatFilter(" + (int) settings.filterSize + channel + ")");
				break;
			default:
				break;
		}
		if (settings.interpolation > 1) line(sb, ".interpolation(" + number(
			settings.interpolation) + ")");
		if (settings.verticalReflection) line(sb, ".setVerticalReflection(true)");
		if (settings.horizontalReflection) line(sb,
			".setHorizontalReflection(true)");
		line(sb, ".threads(Runtime.getRuntime().availableProcessors())");
		line(sb, ".build()");
		sb.append("\n");
	}

	private void appendFrameReduction(StringBuilder sb) {
		switch (settings.frameReduction) {
			case SKIP:
				line(sb, ".skipFrames(" + settings.frameReductionFactor + ")");
				break;
			case AVERAGE:
				line(sb, ".averageFrames(" + settings.frameReductionFactor + ")");
				break;
			case SUM:
				line(sb, ".sumFrames(" + settings.frameReductionFactor + ")");
				break;
			default:
				break;
		}
	}

	private void appendFormatter(StringBuilder sb) {
		if (settings.showXAxis && settings.autoXAxisRange) {
			switch (settings.timeUnit) {
				case FRAMES:
					sb.append("def xMin = minT\n");
					sb.append("def xMax = maxT\n");
					break;
				case MINUTES:
					sb.append("def xMin = omeImage.getPlane(0, 0, minT).getDeltaTinSeconds() / 60\n");
					sb.append("def xMax = omeImage.getPlane(0, 0, maxT).getDeltaTinSeconds() / 60\n");
					break;
				default:
					sb.append("def xMin = omeImage.getPlane(0, 0, minT).getDeltaTinSeconds()\n");
					sb.append("def xMax = omeImage.getPlane(0, 0, maxT).getDeltaTinSeconds()\n");
					break;
			}
			sb.append("\n");
		}

		sb.append("def formatter = new ImageFormatter(scijavaContext, image)\n");
		line(sb, ".setRescale(" + settings.rescale + ")");
		// Keep the axes and text the same size relative to the figure when the
		// image is enlarged. Interpolation enlarges montages in both directions,
		// but kymographs only along the DNA, so it is left out for kymographs.
		double scale = (isKymograph() ? 1 : Math.max(1, settings.interpolation)) *
			settings.rescale;
		if (scale != 1 && SUPPORTS_SCALE) line(sb, ".setScale(" + number(scale) +
			")");
		line(sb, ".setTitle(" + (settings.title == null ? "UID" : quote(
			settings.title)) + ")");

		if (settings.showXAxis) {
			line(sb, ".setXAxisLabel(" + quote(settings.xAxisLabel) + ")");
			if (settings.autoXAxisRange) line(sb, ".setXAxisRange(xMin, xMax)");
			else line(sb, ".setXAxisRange(" + number(settings.xAxisMin) + ", " +
				number(settings.xAxisMax) + ")");
			if (settings.xStepSize > 0) line(sb, ".setXStepSize(" + number(
				settings.xStepSize) + ")");
			line(sb, ".setXAxisPrecision(" + settings.xAxisPrecision + ")");
		}
		else line(sb, ".hideXAxis()");

		if (settings.showYAxis) {
			line(sb, ".setYAxisLabel(" + quote(settings.yAxisLabel) + ")");
			if (settings.autoYAxisRange) line(sb, ".setYAxisRange(0, image.dimension(1)" +
				(settings.interpolation > 1 ? " / " + number(settings.interpolation)
					: "") + ")");
			else line(sb, ".setYAxisRange(" + number(settings.yAxisMin) + ", " +
				number(settings.yAxisMax) + ")");
			if (settings.yStepSize > 0) line(sb, ".setYStepSize(" + number(
				settings.yStepSize) + ")");
			line(sb, ".setYAxisPrecision(" + settings.yAxisPrecision + ")");
		}
		else line(sb, ".hideYAxis()");

		for (ChannelSettings channel : settings.channels) {
			line(sb, ".setLUT(" + channel.channel + ", " + quote(channel.lut) + ")");
			if (!channel.autoDisplayRange) {
				line(sb, ".setDisplayRangeMin(" + channel.channel + ", " + number(
					channel.displayRangeMin) + ")");
				line(sb, ".setDisplayRangeMax(" + channel.channel + ", " + number(
					channel.displayRangeMax) + ")");
			}
		}

		List<String> visible = settings.channels.stream().filter(c -> c.visible)
			.map(c -> String.valueOf(c.channel)).collect(Collectors.toList());
		if (!visible.isEmpty() && visible.size() < settings.channels.size()) line(
			sb, ".onlyShowChannels(" + String.join(", ", visible) + ")");

		if (settings.channelStack) line(sb, ".showChannelStack(" +
			settings.channelStackSpacing + ")");

		switch (settings.orientation) {
			case LEFT_VERTICAL:
				line(sb, ".leftVerticalOrientation()");
				break;
			case RIGHT_VERTICAL:
				line(sb, ".rightVerticalOrientation()");
				break;
			default:
				break;
		}

		line(sb, ".setAxisFontSize(" + settings.axisFontSize + ")");
		line(sb, ".setLabelFontSize(" + settings.labelFontSize + ")");
		line(sb, ".setTitleFontSize(" + settings.titleFontSize + ")");
		line(sb, ".setAxisLineWidth(" + number(settings.axisLineWidth) + ")");
		line(sb, ".setTickLineWidth(" + number(settings.tickLineWidth) + ")");
		line(sb, ".setTickLength(" + settings.tickLength + ")");
		if (settings.darkTheme) line(sb, ".setDarkTheme(true)");
		sb.append("formatter.build()\n\n");
	}

	/**
	 * Report "channel:pixel min:pixel max:auto min:auto max" for each channel.
	 * The automatic range is calculated the same way as the ImageFormatter,
	 * which doesn't report it reliably.
	 */
	private void appendDisplayRanges(StringBuilder sb) {
		sb.append("\ndef cAxis = image.dimensionIndex(net.imagej.axis.Axes.CHANNEL)\n");
		sb.append("def nChannels = cAxis < 0 ? 1 : image.dimension(cAxis)\n");
		sb.append(DISPLAY_RANGES_OUTPUT + " = (1..nChannels).collect { c ->\n");
		sb.append(INDENT + "def channelImage = net.imglib2.img.display.imagej.ImageJFunctions.wrap(\n");
		sb.append(INDENT + INDENT + "cAxis < 0 ? image : net.imglib2.view.Views.hyperSlice(image, cAxis, c - 1), \"C\" + c)\n");
		sb.append(INDENT + "def ip = channelImage.getProcessor()\n");
		sb.append(INDENT + "def stats = channelImage.getStackSize() > 1 ? new ij.process.StackStatistics(channelImage) : ip.getStats()\n");
		sb.append(INDENT + "new ij.plugin.ContrastEnhancer().stretchHistogram(ip, 0.35, stats)\n");
		sb.append(INDENT + "\"${c}:${stats.min}:${stats.max}:${ip.getMin()}:${ip.getMax()}\"\n");
		sb.append("}.join(';')\n");
	}

	private static void line(StringBuilder sb, String call) {
		sb.append(INDENT).append(call).append("\n");
	}

	static String quote(String value) {
		if (value == null) value = "";
		return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n",
			"\\n") + "'";
	}

	static String number(double value) {
		if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(
			value) < 1e15) return String.valueOf((long) value);
		return String.valueOf(value);
	}
}
