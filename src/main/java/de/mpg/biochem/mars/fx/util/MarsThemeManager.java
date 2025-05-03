package de.mpg.biochem.mars.fx.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.prefs.Preferences;

/**
 * Centralized theme manager for handling application-wide theme switching
 * between light and dark modes.
 */
public class MarsThemeManager {

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
        String css = darkMode ? DARK_THEME : LIGHT_THEME;
        scene.getStylesheets().clear();

        if (darkMode) scene.getStylesheets().add("de/mpg/biochem/mars/fx/dark-theme.css");
        else scene.getStylesheets().add("de/mpg/biochem/mars/fx/light-theme.css");

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

        if (darkMode) root.getStylesheets().add("de/mpg/biochem/mars/fx/dark-theme.css");
        else root.getStylesheets().add("de/mpg/biochem/mars/fx/light-theme.css");

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

        // Apply to all stages
        for (Stage stage : javafx.stage.Window.getWindows().stream()
                .filter(window -> window instanceof Stage)
                .map(window -> (Stage) window)
                .toList()) {

            if (stage.getScene() != null) {
                applyTheme(stage.getScene(), isDarkTheme);
            }
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
