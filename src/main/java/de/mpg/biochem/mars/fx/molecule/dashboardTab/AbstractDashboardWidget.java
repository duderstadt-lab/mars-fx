package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH;

import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import de.jensd.fx.glyphs.GlyphIcons;
import javafx.scene.layout.AnchorPane;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.stage.Stage;

public abstract class AbstractDashboardWidget extends AbstractJsonConvertibleRecord implements DashboardWidget {
	
	protected final BorderPane rootPane;
	protected final AnchorPane anchorPane;
	protected final TabPane tabs;

	protected static final int RESIZE_REGION = 5;
	protected double MINIMUM_WIDTH = 250;
	protected double MINIMUM_HEIGHT = 250;
	protected double y, x;
	protected boolean initHeight, initWidth;
	protected boolean dragX, dragY;
	
	protected DashboardTab parent;
	protected Button closeButton, loadButton;
	
	protected MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	public AbstractDashboardWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive, DashboardTab parent) {
		this.archive = archive;
		this.parent = parent;
		rootPane = new BorderPane();
		anchorPane = new AnchorPane();
		tabs = new TabPane();
		tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabs.setStyle("");
		tabs.getStylesheets().clear();
		tabs.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/WidgetTabPane.css");

        AnchorPane.setTopAnchor(tabs, 0.0);
        AnchorPane.setLeftAnchor(tabs, 0.0);
        AnchorPane.setRightAnchor(tabs, 0.0);
        AnchorPane.setBottomAnchor(tabs, 0.0);

        rootPane.setBorder(new Border(new BorderStroke(Color.BLACK, 
                BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));

		Text closeIcon = OctIconFactory.get().createIcon(CLOSE, "1.0em");
		closeButton = new Button();
		closeButton.setPickOnBounds(true);
		closeButton.setGraphic(closeIcon);
		closeButton.getStyleClass().add("icon-button");
		closeButton.setAlignment(Pos.CENTER);
		closeButton.setOnMouseClicked(e -> {
		     close();
		});	
        AnchorPane.setTopAnchor(closeButton, 5.0);
        AnchorPane.setLeftAnchor(closeButton, 5.0);
        closeButton.setPrefWidth(20);
        closeButton.setPrefHeight(20);
		
		Text syncIcon = OctIconFactory.get().createIcon(REFRESH, "1.0em");
		loadButton = new Button();
		loadButton.setGraphic(syncIcon);
		loadButton.setCenterShape(true);
		loadButton.getStyleClass().add("icon-button");
		loadButton.setOnMouseClicked(e -> {
		     load();
		});
		AnchorPane.setTopAnchor(loadButton, 5.0);
        AnchorPane.setRightAnchor(loadButton, 5.0);
        loadButton.setPrefWidth(20);
        loadButton.setPrefHeight(20);
        
        anchorPane.getChildren().addAll(tabs, closeButton, loadButton);
		
		/*
		RotateTransition rt = new RotateTransition(Duration.millis(500), updateLabel);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.setByAngle(0);
		rt.setByAngle(360);
	    rt.setCycleCount(Animation.INDEFINITE);
	     
		updateLabel.setOnMouseClicked(e -> {
		     //rt.play();
		     Task<Void> spin = new Task<Void>() {
	            @Override
	            protected Void call() throws Exception {
	    		     rt.setByAngle(360);
	    		     rt.setCycleCount(10000);
	    		     rt.play();
	                return null;
	            }
	         };
	         spin.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
	            @Override
	            public void handle(WorkerStateEvent event) {
	            	
	            }
	         });
	          new Thread(spin).start();
	          
	        
		     subPlot.update();
	         //rt.stop();
		});
		*/
	
		rootPane.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mousePressed(event);
			}
		});
		rootPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseDragged(event);
			}
		});
		rootPane.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseOver(event);
			}
		});
		rootPane.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mouseReleased(event);
			}
		});
		
		rootPane.setCenter(anchorPane);
	}

	protected void mouseReleased(MouseEvent event) {
		dragX = dragY = false;
		rootPane.setCursor(Cursor.DEFAULT);
	}

	protected void mouseOver(MouseEvent event) {
		if (isInDraggableY(event))
				rootPane.setCursor(Cursor.S_RESIZE);
		else if (isInDraggableX(event))
				rootPane.setCursor(Cursor.E_RESIZE);
		else
			rootPane.setCursor(Cursor.DEFAULT);
	}
	
	protected boolean isInDraggableY(MouseEvent event) {
		return event.getY() > (rootPane.getHeight() - RESIZE_REGION);
	}
	
	protected boolean isInDraggableX(MouseEvent event) {
		return event.getX() > (rootPane.getWidth() - RESIZE_REGION);
	}

	protected void mouseDragged(MouseEvent event) {
		if (!dragX && !dragY) {
			return;
		}
		if (dragY) {
			double mousey = event.getY();
			double newHeight = rootPane.getMinHeight() + (mousey - y);
			if (newHeight > MINIMUM_HEIGHT) {
				rootPane.setMinHeight(newHeight);
				rootPane.setMaxHeight(newHeight);
				y = mousey;
			}
		}

		if (dragX) {
			double mousex = event.getX();
			double newWidth = rootPane.getMinWidth() + (mousex - x);
			if (newWidth > MINIMUM_WIDTH) {
				rootPane.setMinWidth(newWidth);
				rootPane.setMaxWidth(newWidth);
				x = mousex;
			}
		}
		
		parent.getWidgetPane().clearLayout();
		parent.getWidgetPane().layout();
	}

	protected void mousePressed(MouseEvent event) {
		if (isInDraggableX(event))
			dragX = true;
		else if(isInDraggableY(event))
			dragY = true;
		else
			return;

		if (!initHeight) {
			//MINIMUM_HEIGHT = rootPane.getHeight();
			rootPane.setMinHeight(rootPane.getHeight());
			rootPane.setMaxHeight(rootPane.getHeight());
			initHeight = true;
		}

		y = event.getY();

		if (!initWidth) {
			//MINIMUM_WIDTH = rootPane.getWidth();
			rootPane.setMinWidth(rootPane.getWidth());
			rootPane.setMaxWidth(rootPane.getWidth());
			initWidth = true;
		}

		x = event.getX();
	}
	
	public abstract GlyphIcons getIcon();

	@Override
	public Node getNode() {
		return rootPane;
	}
	
	public TabPane getTabPane() {
		return tabs;
	}
	
	@Override
	public void setParent(DashboardTab parent) {
		this.parent = parent;
	}
	
	@Override
	public DashboardTab getParent() {
		return this.parent;
	}
	
	@Override
	public void close() {
		if (parent != null)
			parent.removeWidget(this);
	}
}
