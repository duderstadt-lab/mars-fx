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

import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.BlurType;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import javafx.scene.effect.DropShadow;

public class MarsAnimation extends BorderPane {
	
	private TranslateTransition animation;
	
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
		
		imageView1.setFitHeight(100);
		imageView1.setFitWidth(200);
		imageView2.setFitHeight(100);
		imageView2.setFitWidth(200);
		imageView2.setX(200);
        
		Group imageGroup = new Group(imageView1, imageView2);
		
        animation = new TranslateTransition(
                Duration.seconds(2.5), imageGroup
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.setInterpolator(Interpolator.LINEAR);
        
        //Needs to be shifted by one whole image
        //Then it looks continuous...
        animation.setFromX(-200);
        animation.setToX(0);
        
        //InnerShadow(BlurType blurType, Color color, double radius, double choke, double offsetX, double offsetY)
		InnerShadow atmosphere = new InnerShadow(BlurType.THREE_PASS_BOX, Color.valueOf("rgba(255,255,255,.2)"), 10, -1, 5, 0); 
		InnerShadow innerShadow = new InnerShadow(BlurType.GAUSSIAN, Color.valueOf("black"), 25, 0, -35, 10);	
		innerShadow.setInput(atmosphere);
		
		//DropShadow(BlurType blurType, Color color, double radius, double spread, double offsetX, double offsetY)
		DropShadow dropShadow = new DropShadow(BlurType.THREE_PASS_BOX, Color.valueOf("#c07158"), 1, -1, -1, 0);
        dropShadow.setInput(innerShadow);
        
        Pane pane = new Pane(imageGroup);
        pane.setClip(new Circle(50, 50, 50));
        
        StackPane stack = new StackPane();
        
        stack.getChildren().add(pane);
        stack.setRotate(25.2);
        stack.setPrefSize(100,  100);
        stack.setEffect(dropShadow);

        //radial-gradient(circle at 30% 50%, rgba(255,255,255,.2) 0%, rgba(255,255,255,0) 65%);
        
        //RadialGradient( focusAngle, focusDistance, centerX, centerY, radius, proportional,CycleMethod cycleMethod, Stop... stops)
        RadialGradient shadePaint = new RadialGradient(
                0, 0, 0.3, 0.65, 0.25, true, CycleMethod.NO_CYCLE,
                new Stop(1, Color.valueOf("rgba(100,100,100,0.3)")),
                new Stop(0, Color.valueOf("rgba(100,100,100,0.1)"))
        );
    	
    	Circle c0 = new Circle(50, 50, 50);
    	c0.setFill(shadePaint);
    	stack.getChildren().add(c0);
    	/*
    	RadialGradient darkside = new RadialGradient(
                0, 0, 0.15, 0.5, 0.6, true, CycleMethod.NO_CYCLE,
                new Stop(1, Color.valueOf("rgba(0,0,0,0.4)")),
                new Stop(0, Color.valueOf("rgba(0,0,0,0.1)"))
        );
    	
    	Circle c1 = new Circle(50, 50, 50);
    	c1.setFill(darkside);
    	stack.getChildren().add(c1);
    	 */
		setCenter(stack);
		
		animation.play();
	}
	
	public void play() {
		animation.play();
	}
	
	public void stop() {
		animation.stop();
	}
}
