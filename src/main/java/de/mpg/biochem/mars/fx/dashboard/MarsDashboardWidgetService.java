/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.dashboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.prefs.PrefService;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import de.mpg.biochem.mars.fx.molecule.dashboardTab.MoleculeArchiveDashboardWidget;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.dashboard.MoleculeDashboardWidget;
import de.mpg.biochem.mars.fx.table.dashboard.MarsTableDashboardWidget;
import net.imagej.ImageJService;

@Plugin(type = Service.class)
public class MarsDashboardWidgetService extends AbstractPTService<MarsDashboardWidget> implements ImageJService {
	@Parameter
	private PluginService plugins;

	@Parameter
	private CommandService commands;

	@Parameter
	private PrefService prefService;

	/** Map of each Widget name to its corresponding plugin metadata. */
	private HashMap<String, PluginInfo<MarsDashboardWidget>> widgets = new HashMap<>();

	/**
	 * Gets the list of available widgets. The names on this list can be passed to
	 * {@link #createWidget(String)} to create instances of that widget.
	 * 
	 * @return The set of widget names.
	 */
	public Set<String> getWidgetNames() {
		return widgets.keySet();
	}

	public Set<String> getWidgetNames(Class<?> clazz) {
		HashSet<String> widgetsOfType = new HashSet<String>();

		for (String name : widgets.keySet()) {
			final PluginInfo<MarsDashboardWidget> info = widgets.get(name);

			if (info.getPluginType().equals(clazz))
				widgetsOfType.add(name);
		}
		return widgetsOfType;
	}

	public MarsDashboardWidget createWidget(final String name) {
		final PluginInfo<MarsDashboardWidget> info = widgets.get(name);

		if (info == null) {
			throw new IllegalArgumentException("No widgets of that name");
		}

		final MarsDashboardWidget widget = plugins.createInstance(info);

		return widget;
	}

	public Class<? extends MarsDashboardWidget> getWidgetClass(final String name) {
		final PluginInfo<MarsDashboardWidget> info = widgets.get(name);

		if (info == null) {
			throw new IllegalArgumentException("No widgets of that name");
		}

		return info.getPluginClass();
	}

	public void setDefaultScriptingLanguage(String language) {
		prefService.put(MarsDashboardWidgetService.class, "DefaultScriptingLanguage", language);
	}

	public String getDefaultScriptingLanguage() {
		if (prefService.get(MarsDashboardWidgetService.class, "DefaultScriptingLanguage") != null)
			return prefService.get(MarsDashboardWidgetService.class, "DefaultScriptingLanguage");
		else
			return "Python";
	}

	@Override
	public void initialize() {
		for (final PluginInfo<MarsDashboardWidget> info : getPlugins()) {
			String name = info.getName();
			if (name == null || name.isEmpty()) {
				name = info.getClassName();
			}

			// Add the plugin to the list of known widgets.
			widgets.put(name, info);
		}
	}

	@Override
	public Class<MarsDashboardWidget> getPluginType() {
		return MarsDashboardWidget.class;
	}
}
