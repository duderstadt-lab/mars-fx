package de.mpg.biochem.mars.gui.view;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.net.URL;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public class MoleculeArchiveFrameController {

    @FXML
    private JFXTabPane tabContainer;

    @FXML
    private Tab propertiesTab;

    @FXML
    private AnchorPane propertiesContainer;

    @FXML
    private Tab imageMetaDataTab;

    @FXML
    private AnchorPane imageMetaDataContainer;

    @FXML
    private Tab moleculesTab;
    
    @FXML
    private AnchorPane moleculesContainer;


    private double tabWidth = 90.0;
    public static int lastSelectedTabIndex = 0;

    /// Life cycle

    @FXML
    public void initialize() {
        configureView();
    }

    /// Private

    private void configureView() {
        tabContainer.setTabMinWidth(tabWidth);
        tabContainer.setTabMaxWidth(tabWidth);
        tabContainer.setTabMinHeight(tabWidth);
        tabContainer.setTabMaxHeight(tabWidth);
        tabContainer.setRotateGraphic(true);

        EventHandler<Event> replaceBackgroundColorHandler = event -> {
            lastSelectedTabIndex = tabContainer.getSelectionModel().getSelectedIndex();

            Tab currentTab = (Tab) event.getTarget();
            if (currentTab.isSelected()) {
                currentTab.setStyle("-fx-background-color: -fx-focus-color;");
            } else {
                currentTab.setStyle("-fx-background-color: -fx-accent;");
            }
        };

        configureTab(propertiesTab, "Properties", DASHBOARD, propertiesContainer, getClass().getResource("MAProperties.fxml"), replaceBackgroundColorHandler);
        configureTab(imageMetaDataTab, "ImageMetaData", IMAGE, imageMetaDataContainer, getClass().getResource("MAImageMetaData.fxml"), replaceBackgroundColorHandler);
        configureTab(moleculesTab, "Molecules", COG, moleculesContainer, getClass().getResource("MAProperties.fxml"), replaceBackgroundColorHandler);

        propertiesTab.setStyle("-fx-background-color: -fx-focus-color;");
    }

    private void configureTab(Tab tab, String title, GlyphIcons icon, AnchorPane containerPane, URL resourceURL, EventHandler<Event> onSelectionChangedEvent) {

        Label label = new Label(title);
        label.setMaxWidth(tabWidth - 20);
        label.setPadding(new Insets(5, 0, 0, 0));
        label.setStyle("-fx-text-fill: black; -fx-font-size: 8pt; -fx-font-weight: normal;");
        label.setTextAlignment(TextAlignment.CENTER);

        BorderPane tabPane = new BorderPane();
        tabPane.setRotate(90.0);
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(FontAwesomeIconFactory.get().createIcon(icon, "1.2em"));
        //tabPane.setBottom(label);

        tab.setText("");
        tab.setGraphic(tabPane);

        tab.setOnSelectionChanged(onSelectionChangedEvent);

        if (containerPane != null && resourceURL != null) {
            try {
                Parent contentView = FXMLLoader.load(resourceURL);
                containerPane.getChildren().add(contentView);
                AnchorPane.setTopAnchor(contentView, 0.0);
                AnchorPane.setBottomAnchor(contentView, 0.0);
                AnchorPane.setRightAnchor(contentView, 0.0);
                AnchorPane.setLeftAnchor(contentView, 0.0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}