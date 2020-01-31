package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import de.jensd.fx.glyphs.GlyphIcons;
import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TAG;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import javafx.scene.paint.ImagePattern;

import net.imagej.ops.Initializable;

import javafx.scene.shape.Rectangle;

import javafx.animation.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.Group;

@Plugin( type = DefaultWidget.class, name = "DefaultWidget" )
public class DefaultWidget extends AbstractDashboardWidget implements MarsDashboardWidget, SciJavaPlugin, Initializable {

	@Override
	public void initialize() {
		super.initialize();
		Image image = new Image("de/mpg/biochem/mars/fx/molecule/mars.jpg");
        //ImagePattern imagePattern = new ImagePattern(image);
        
        //Rectangle(double x, double y, double width, double height)
        //Rectangle rect = new Rectangle(0, 0, 1000, 250);
        
        //rect.setFill(imagePattern);
		
		//ISSUES - why is is not smooth...
		//why is it taking so much of the processor
		//How to make it nicely scale - or perhaps just fix it but with dimesions like 100 x 100 x
		//Use a rectangle for that??
		
		ImageView imageView1 = new ImageView(image);
		ImageView imageView2 = new ImageView(image);
		
		imageView1.setFitHeight(250);
		imageView2.setFitHeight(250);
		imageView2.setX(800);
        
		Group imageGroup = new Group(imageView1, imageView2);
		
        TranslateTransition animation = new TranslateTransition(
                Duration.seconds(2.5), imageGroup
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.setInterpolator(Interpolator.LINEAR);
        
        //Needs to be shifted by one whole image
        //Then it looks continuous...
        animation.setFromX(-800);
        animation.setToX(0);
        
        Pane pane = new Pane(imageGroup);

        animation.play();
		
		StackPane stack = new StackPane();
		stack.getChildren().add(pane);
		stack.setPrefSize(250,  250);

        BorderPane chartPane = new BorderPane();
        chartPane.setCenter(stack);
        setContent(getIcon(), chartPane);
        
        rootPane.setMinSize(250, 250);
        rootPane.setMaxSize(250, 250);
	}

	@Override
	protected void createIOMaps() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Node getIcon() {
		return (Node) OctIconFactory.get().createIcon(BEAKER, "1.2em");
	}

	@Override
	public void run() {
		
	}

	@Override
	public String getName() {
		return "DefaultWidget";
	}
}
