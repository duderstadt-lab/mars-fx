package de.mpg.biochem.mars.fx.util;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

//import javafx.scene.effect.InnerShadow;
//import javafx.scene.effect.BlurType;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class MarsAnimation extends BorderPane {
	
	public MarsAnimation() {
		Image image = new Image("de/mpg/biochem/mars/fx/molecule/mars.jpg");
        //ImagePattern imagePattern = new ImagePattern(image);
        
        //rect.setFill(imagePattern);
		
		//ISSUES - why is it not smooth...
		//why is it taking so much of the processor
		//How to make it nicely scale - or perhaps just fix it but with dimesions like 100 x 100 x
		//Use a rectangle for that??
		
		ImageView imageView1 = new ImageView(image);
		ImageView imageView2 = new ImageView(image);
		
		imageView1.setFitHeight(200);
		imageView2.setFitHeight(200);
		imageView2.setX(800);
        
		Group imageGroup = new Group(imageView1, imageView2);
		
        TranslateTransition animation = new TranslateTransition(
                Duration.seconds(3.5), imageGroup
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.setInterpolator(Interpolator.LINEAR);
        
        //Needs to be shifted by one whole image
        //Then it looks continuous...
        animation.setFromX(-800);
        animation.setToX(0);
        
        //InnerShadow innerShadow2 = new InnerShadow(BlurType.THREE_PASS_BOX, Color.valueOf("black"), 50, 0, -70, 0);
		
        RadialGradient shadePaint = new RadialGradient(
                0, 0.3, 0, 0, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(1, Color.BLACK),
                new Stop(0, Color.TRANSPARENT)
        );
        
		Circle c1 = new Circle(100, 100, 100);
		c1.setFill(shadePaint);
	
        
        Pane pane = new Pane(imageGroup);
        pane.setClip(new Circle(100, 100, 100));
        
        StackPane stack = new StackPane();
        
        stack.getChildren().add(pane);
        stack.setRotate(25.2);
        stack.setPrefSize(200,  200);
        
        stack.getChildren().add(c1);
		
		//InnerShadow innerShadow1 = new InnerShadow(BlurType.THREE_PASS_BOX, Color.valueOf("rgba(255,255,255,.2)"), 12, -2, 10, 0);
		//setEffect(innerShadow1);

		setCenter(stack);
		
		animation.play();
	}
}
