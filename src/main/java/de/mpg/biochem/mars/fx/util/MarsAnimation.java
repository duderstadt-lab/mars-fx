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

import java.text.DecimalFormat;
import java.math.RoundingMode;

public class MarsAnimation extends BorderPane {
	
	private TranslateTransition animation;
	private double progress = -1;
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
        
        //RadialGradient( focusAngle, focusDistance, centerX, centerY, radius, proportional,CycleMethod cycleMethod, Stop... stops)
        RadialGradient shadePaint = new RadialGradient(
                0, 0, 0.3, 0.65, 0.25, true, CycleMethod.NO_CYCLE,
                new Stop(1, Color.valueOf("rgba(100,100,100,0.3)")),
                new Stop(0, Color.valueOf("rgba(100,100,100,0.1)"))
        );
    	
    	Circle c0 = new Circle(50, 50, 50);
    	c0.setFill(shadePaint);
    	stack.getChildren().add(c0);

    	setPrefSize(125, 100);
		setCenter(stack);
		
		Text timerLabel = new Text("0.0");
		timerLabel.setFont(Font.font("Courier", 14));
		timerLabel.setFill(Color.valueOf("#fff"));
		
		Text progressLabel = new Text("");
		progressLabel.setFont(Font.font("Courier", 14));
		progressLabel.setFill(Color.valueOf("#fff"));
		
		timeline = new Timeline(new KeyFrame(Duration.millis(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				updateTimer(timerLabel);
				updateProgress(progressLabel);
			}
		}));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.setAutoReverse(false);
		
		VBox vbox = new VBox();
		vbox.setSpacing(5);
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().add(timerLabel);
		vbox.getChildren().add(progressLabel);

		setBottom(vbox);
		//Insets(double top, double right, double bottom, double left)
		BorderPane.setMargin(vbox, new Insets(10,0,0,0));
		BorderPane.setAlignment(vbox, Pos.CENTER);
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
	
	private void updateProgress(Text text) {
		if (progress >= 0 && progress <= 1) {
			DecimalFormat df = new DecimalFormat("#.0");
			df.setRoundingMode(RoundingMode.HALF_UP);
			String rounded = df.format(progress*100);
			text.setText(rounded + "%");
		} else 
			text.setText("");
	}
	
	public void setProgress(double progress) {
		this.progress = progress;
	}
	
	public void play() {
		animation.play();
		//Reset timer
		hours = 0;
		mins = 0;
		secs = 0;
		millis = 0;
		//Reset progress
		progress = -1;
		timeline.play();
	}
	
	public void stop() {
		animation.stop();
		timeline.stop();
	}
}
