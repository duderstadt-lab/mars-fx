package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import java.util.HashMap;
import java.util.Set;

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import net.imagej.ImageJService;

@Plugin( type = Service.class )
public class MarsDashboardWidgetService extends AbstractPTService<MarsDashboardWidget> implements ImageJService {
	@Parameter
	private PluginService plugins;

	@Parameter
	private CommandService commands;
	
	/** Map of each Widget name to its corresponding plugin metadata. */
	private HashMap<String, PluginInfo<MarsDashboardWidget>> widgets = new HashMap<>();
	
	/**
	 * Gets the list of available animals. The names on this list can be passed to
	 * {@link #createAnimal(String)} to create instances of that animal.
	 */
	public Set<String> getWidgetNames() {
		return widgets.keySet();
	}

	//hmm.... I guess achive and Dashboard would need to be parameter inputs for this mechanism to work
	//then the initialize method would need to be called?
	
	/** Creates an animal of the given name. */
	public MarsDashboardWidget createWidget(final String name) {
		final PluginInfo<MarsDashboardWidget> info = widgets.get(name);

		if (info == null) {
			throw new IllegalArgumentException("No widgets of that name");
		}

		// Next, we use the plugin service to create an animal of that kind.
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