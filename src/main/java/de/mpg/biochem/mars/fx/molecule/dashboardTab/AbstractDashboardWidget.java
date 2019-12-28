package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOSE;

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

public abstract class AbstractDashboardWidget extends AbstractJsonConvertibleRecord implements DashboardWidget {
	
	protected BorderPane rootPane;

	private static final int RESIZE_REGION = 5;
	private double y, x;
	private boolean initHeight, initWidth;
	private boolean dragX, dragY;
	
	protected DashboardTab parent;
	protected Button closeButton;
	
	protected MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	public AbstractDashboardWidget(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive, DashboardTab parent) {
		this.archive = archive;
		this.parent = parent;
		rootPane = new BorderPane();
		rootPane.setBorder(new Border(new BorderStroke(Color.BLACK, 
                BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));

		Text syncIcon = OctIconFactory.get().createIcon(CLOSE, "1.0em");
		closeButton = new Button();
		closeButton.setPickOnBounds(true);
		closeButton.setGraphic(syncIcon);
		closeButton.getStyleClass().add("icon-button");
		closeButton.setAlignment(Pos.CENTER);
		closeButton.setOnMouseClicked(e -> {
		     close();
		});	
		rootPane.setTop(closeButton);
	
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
		System.out.println(toString());
		if (!dragX && !dragY) {
			return;
		}
		System.out.println("valid");
		if (dragY) {
			double mousey = event.getY();
			double newHeight = rootPane.getMinHeight() + (mousey - y);
			rootPane.setMinHeight(newHeight);
			rootPane.setMaxHeight(newHeight);
			y = mousey;
		}

		if (dragX) {
			double mousex = event.getX();
			double newWidth = rootPane.getMinWidth() + (mousex - x);
			rootPane.setMinWidth(newWidth);
			rootPane.setMaxWidth(newWidth);
			x = mousex;
		}
	}

	protected void mousePressed(MouseEvent event) {
		//Bounds boundsInScreen = rootPane.localToScreen(rootPane.getBoundsInLocal());
		//System.out.println("min x " + boundsInScreen.getMinX() + " min y " 
		//		+ boundsInScreen.getMinY() + " max x " + boundsInScreen.getMaxX() + " max y " + boundsInScreen.getMaxY());
		//System.out.println("mouse x " + event.getScreenX() + " mouse y " + event.getScreenY());
		
		//if (!boundsInScreen.contains(event.getScreenX(), event.getScreenY()))
		//	return;
		
		if (isInDraggableX(event))
			dragX = true;
		else if(isInDraggableY(event))
			dragY = true;
		else
			return;
		
		//Are these really needed?
		
		if (!initHeight) {
			rootPane.setMinHeight(rootPane.getHeight());
			rootPane.setMaxHeight(rootPane.getHeight());
			initHeight = true;
		}

		y = event.getY();

		if (!initWidth) {
			rootPane.setMinWidth(rootPane.getWidth());
			rootPane.setMaxWidth(rootPane.getWidth());
			initWidth = true;
		}

		x = event.getX();
	}

	@Override
	public Node getNode() {
		return rootPane;
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
