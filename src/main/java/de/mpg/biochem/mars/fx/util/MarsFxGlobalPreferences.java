package de.mpg.biochem.mars.fx.util;

import java.util.prefs.Preferences;

import javafx.application.Application;

public class MarsFxGlobalPreferences {
	
	private static Application app;
	
	public static void showDocument(String uri) {
		app.getHostServices().showDocument(uri);
	}

	static private Preferences getPrefsRoot() {
		return Preferences.userRoot().node("markdownwriterfx");
	}

	static Preferences getOptions() {
		return getPrefsRoot().node("options");
	}

	public static Preferences getState() {
		return getPrefsRoot().node("state");
	}

}
