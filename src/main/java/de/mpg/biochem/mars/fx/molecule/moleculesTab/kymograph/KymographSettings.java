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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * All settings used to generate a kymograph or montage. The fields map
 * directly onto the mars-kymograph builders and the ImageFormatter. Settings
 * are stored as json with each figure so they can be restored when a molecule
 * is selected again.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KymographSettings {

	public static final int SETTINGS_VERSION = 1;

	private static final ObjectMapper MAPPER = new ObjectMapper().configure(
		DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public enum Mode {
			KYMOGRAPH, MONTAGE
	}

	public enum FrameReduction {
			NONE, SKIP, AVERAGE, SUM
	}

	public enum Filter {
			NONE, MEDIAN, GAUSSIAN, TOPHAT
	}

	public enum TimeUnit {
			SECONDS, MINUTES, FRAMES
	}

	public enum Orientation {
			HORIZONTAL, LEFT_VERTICAL, RIGHT_VERTICAL
	}

	public int settingsVersion = SETTINGS_VERSION;

	public Mode mode = Mode.KYMOGRAPH;

	// Source
	public int projectionWidth = 10;
	public int borderWidth = 2;
	public int borderHeight = 2;
	public int spotRadius = 5;
	public String xLocationSource = "x";
	public String yLocationSource = "y";
	public boolean locationFromParameters = false;

	// Frames, -1 means first or last frame
	public int minT = -1;
	public int maxT = -1;
	public FrameReduction frameReduction = FrameReduction.NONE;
	public int frameReductionFactor = 2;

	// Processing
	public Filter filter = Filter.NONE;
	public double filterSize = 2;
	// Montage only, 0 means all channels
	public int filterChannel = 0;
	public double interpolation = 1;
	public boolean verticalReflection = false;
	public boolean horizontalReflection = false;

	// Montage layout
	public int spacing = 0;
	public boolean horizontalLayout = true;
	// -1 means a single row or column
	public int columns = -1;

	// Channels
	public List<ChannelSettings> channels = new ArrayList<>();

	// Axes and labels, a null title means use the molecule UID
	public String title = null;
	public String xAxisLabel = "Time (s)";
	public boolean autoXAxisRange = true;
	public TimeUnit timeUnit = TimeUnit.SECONDS;
	public double xAxisMin = 0;
	public double xAxisMax = 100;
	public String yAxisLabel = "";
	public boolean autoYAxisRange = true;
	public double yAxisMin = 0;
	public double yAxisMax = 100;
	// 0 means automatic
	public double xStepSize = 0;
	public double yStepSize = 0;
	public int xAxisPrecision = 0;
	public int yAxisPrecision = 0;
	public boolean showXAxis = true;
	public boolean showYAxis = true;
	public Orientation orientation = Orientation.HORIZONTAL;

	// Style
	public int rescale = 3;
	public boolean darkTheme = false;
	public boolean channelStack = false;
	public int channelStackSpacing = 25;
	public int axisFontSize = 16;
	public int labelFontSize = 16;
	public int titleFontSize = 16;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChannelSettings {

		// Channel index starting from 1 as used by the ImageFormatter
		public int channel = 1;
		public boolean visible = true;
		public String lut = "Grays";
		public boolean autoDisplayRange = true;
		public double displayRangeMin = 0;
		public double displayRangeMax = 65535;

		public ChannelSettings() {}

		public ChannelSettings(int channel, String lut) {
			this.channel = channel;
			this.lut = lut;
		}
	}

	/**
	 * Get the settings for the channel provided, creating them if needed.
	 *
	 * @param channel The channel index starting from 1.
	 * @return The channel settings.
	 */
	public ChannelSettings getChannel(int channel) {
		for (ChannelSettings channelSettings : channels)
			if (channelSettings.channel == channel) return channelSettings;
		ChannelSettings channelSettings = new ChannelSettings(channel,
			defaultLut(channel));
		channels.add(channelSettings);
		return channelSettings;
	}

	/**
	 * Make sure there are settings for exactly the number of channels provided.
	 *
	 * @param numberOfChannels The number of channels.
	 */
	public void setNumberOfChannels(int numberOfChannels) {
		channels.removeIf(c -> c.channel > numberOfChannels);
		for (int c = 1; c <= numberOfChannels; c++)
			getChannel(c);
		channels.sort((a, b) -> Integer.compare(a.channel, b.channel));
	}

	private static String defaultLut(int channel) {
		switch (channel) {
			case 1:
				return "Grays";
			case 2:
				return "Magenta";
			case 3:
				return "Green";
			default:
				return "Cyan";
		}
	}

	public String toJson() {
		try {
			return MAPPER.writeValueAsString(this);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static KymographSettings fromJson(String json) {
		if (json == null || json.isEmpty()) return new KymographSettings();
		try {
			return MAPPER.readValue(json, KymographSettings.class);
		}
		catch (IOException e) {
			return new KymographSettings();
		}
	}

	public KymographSettings copy() {
		return fromJson(toJson());
	}
}
