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
package de.mpg.biochem.mars.fx.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

/**
 * Centralized theme manager for handling application-wide theme switching
 * between light and dark modes.
 */
public class MarsThemeManager {

    /**
     * The scenes mars-fx has themed, so a theme switch reaches those and only
     * those.
     * <p>
     * Fiji is one JVM shared with other JavaFX plugins, and a scene's stylesheet
     * list belongs to whoever created the window. Switching theme used to walk
     * Window.getWindows() and clear the stylesheets of every Stage in the
     * process, which wiped the styling of any window mars-fx did not create and
     * left mars-fx's own CSS on a scene built against a different base theme.
     * Weak references so a closed window is not held alive by this set.
     */
    private static final Set<Scene> THEMED_SCENES =
            Collections.newSetFromMap(new WeakHashMap<>());

    // CSS file paths - use actual paths relative to resources folder
    private static final String LIGHT_THEME = "/de/mpg/biochem/mars/fx/styles/master-light.css";
    private static final String DARK_THEME = "/de/mpg/biochem/mars/fx/styles/master-dark.css";

    // Preference key for saving theme selection
    private static final String DARK_MODE_PREF_KEY = "mars.darkMode";
    // Current theme state
    private static boolean isDarkTheme = true;

    /**
     * Initialize theme manager and set theme based on saved preferences.
     * Should be called early in the application startup process.
     */
    public static void initialize() {
        // Load preference
        Preferences prefs = Preferences.userNodeForPackage(MarsThemeManager.class);
        isDarkTheme = prefs.getBoolean(DARK_MODE_PREF_KEY, true);
    }

    /**
     * Apply theme to a Scene.
     *
     * @param scene The scene to apply the theme to
     */
    public static void applyTheme(Scene scene) {
        applyTheme(scene, isDarkTheme);
    }

    /**
     * Apply theme to a Scene with specific dark mode setting.
     *
     * @param scene The scene to apply the theme to
     * @param darkMode Whether to use dark mode
     */
    public static void applyTheme(Scene scene, boolean darkMode) {
        // Theming a scene is what marks it as ours to re-theme later.
        THEMED_SCENES.add(scene);
        String css = darkMode ? DARK_THEME : LIGHT_THEME;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(MarsThemeManager.class.getResource(css).toExternalForm());
    }

    /**
     * Apply theme to a Parent (useful for components loaded from FXML).
     *
     * @param root The parent node to apply the theme to
     */
    public static void applyTheme(Parent root) {
        applyTheme(root, isDarkTheme);
    }

    /**
     * Apply theme to a Parent with specific dark mode setting.
     *
     * @param root The parent node to apply the theme to
     * @param darkMode Whether to use dark mode
     */
    public static void applyTheme(Parent root, boolean darkMode) {
        String css = darkMode ? DARK_THEME : LIGHT_THEME;
        root.getStylesheets().clear();
        root.getStylesheets().add(MarsThemeManager.class.getResource(css).toExternalForm());
    }

    /**
     * Apply theme to a Stage.
     *
     * @param stage The stage to apply the theme to
     */
    public static void applyTheme(Stage stage) {
        applyTheme(stage.getScene());
    }

    /**
     * Toggle between light and dark themes for the entire application.
     * This will iterate through all open stages and apply the new theme.
     *
     * @return The new dark mode state
     */
    public static boolean toggleTheme() {
        isDarkTheme = !isDarkTheme;

        // Save preference
        Preferences prefs = Preferences.userNodeForPackage(MarsThemeManager.class);
        prefs.putBoolean(DARK_MODE_PREF_KEY, isDarkTheme);

        // Apply to the scenes mars-fx themed, and not to windows belonging to
        // other plugins sharing this JVM.
        for (Scene scene : new java.util.ArrayList<>(THEMED_SCENES)) {
            applyTheme(scene, isDarkTheme);
        }

        return isDarkTheme;
    }

    /**
     * Check if dark theme is currently active.
     *
     * @return true if dark theme is active, false otherwise
     */
    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    /**
     * Set the theme explicitly (useful for settings screens).
     *
     * @param darkMode Whether to use dark mode
     */
    public static void setDarkTheme(boolean darkMode) {
        if (darkMode != isDarkTheme) {
            toggleTheme();
        }
    }
}
