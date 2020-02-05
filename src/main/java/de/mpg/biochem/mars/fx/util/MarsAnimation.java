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

import javafx.animation.Timeline;
import javafx.scene.text.Text;
import javafx.animation.KeyFrame;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.BlurType;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;

public class MarsAnimation extends BorderPane {
	
	private TranslateTransition animation;
	
	private Text text;
	private Timeline timeline;
	private int hours = 0, mins = 0, secs = 0, millis = 0;
	
	public MarsAnimation() {
		Image image = new Image("de/mpg/biochem/mars/fx/molecule/mars.jpg");
		
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
        
        Pane pane = new Pane();
        pane.getChildren().add(imageGroup);
        pane.setClip(new Circle(50, 50, 50));
        pane.setMaxSize(100, 100);
        
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
    	//setPrefSize(150, 100);
		setCenter(stack);
		
		text = new Text("0.0");
		text.setFont(Font.font("Courier", 16));
		text.setFill(Color.valueOf("#fff"));
		timeline = new Timeline(new KeyFrame(Duration.millis(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				updateTimer(text);
			}
		}));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.setAutoReverse(false);
		
		//VBox vbox = new VBox();
		//vbox.setPadding(new Insets(10, 10, 10, 10));
		//vbox.setAlignment(Pos.CENTER);
		//vbox.getChildren().add(text);
		BorderPane.setAlignment(text, Pos.CENTER);
		BorderPane.setMargin(text, new Insets(5,5,5,5));
		setBottom(text);
	}
	
	private void updateTimer(Text text) {
		if (millis == 1000) {
			secs++;
			millis = 0;
		}
		if (secs == 60) {
			mins++;
			secs = 0;
		}
		if (mins == 60) {
			hours++;
			mins = 0;
		}
		if (hours > 0) {
			text.setText((((hours/10) == 0) ? "0" : "") + hours + ":"
				+ (((mins/10) == 0) ? "0" : "") + mins + ":"
				 + (((secs/10) == 0) ? "0" : "") + secs + ":" 
					+ (((millis/10) == 0) ? "00" : (((millis/100) == 0) ? "0" : "")) + millis++);
		} else if (mins > 0) {
			text.setText((((mins/10) == 0) ? "0" : "") + mins + ":"
					 + (((secs/10) == 0) ? "0" : "") + secs + ":" 
						+ (((millis/10) == 0) ? "00" : (((millis/100) == 0) ? "0" : "")) + millis++);
		} else {
			text.setText((((secs/10) == 0) ? "0" : "") + secs + ":" 
						+ (((millis/10) == 0) ? "00" : (((millis/100) == 0) ? "0" : "")) + millis++);
		}
    }
	
	public void play() {
		animation.play();
		//Reset timer
		hours = 0;
		mins = 0;
		secs = 0;
		millis = 0;
		timeline.play();
	}
	
	public void stop() {
		animation.stop();
		timeline.stop();
	}
}
