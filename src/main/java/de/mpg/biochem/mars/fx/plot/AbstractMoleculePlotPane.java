package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROWS_V;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE;

import cern.extjfx.chart.AxisMode;
import cern.extjfx.chart.plugins.Zoomer;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionTool;
import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;

public abstract class AbstractMoleculePlotPane<M extends Molecule, S extends SubPlot> extends AbstractPlotPane implements MoleculeSubPane {
	
	protected M molecule;
	
	protected BooleanProperty regionSelected;
	protected BooleanProperty positionSelected;
	
	public AbstractMoleculePlotPane() {
		super();

		addChart();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
	}
	
	@Override
	protected void buildTools() {
		super.buildTools();
		
		regionSelected = new SimpleBooleanProperty();
		Action regionSelectionCursor = new Action("region", "Shortcut+R", SQUARE, 
				e -> setTool(regionSelected, () -> {
					MarsRegionSelectionTool tool = new MarsRegionSelectionTool(AxisMode.X);
					tool.setMolecule(molecule);
					return tool;
				}, Cursor.DEFAULT), 
				null, regionSelected);
		addTool(regionSelectionCursor);
		
		positionSelected = new SimpleBooleanProperty();
		Action positionSelectionCursor = new Action("position", "Shortcut+P", de.jensd.fx.glyphs.octicons.OctIcon.MILESTONE, 
				e -> setTool(positionSelected, () -> {
					MarsPositionSelectionTool<Number, Number> tool = new MarsPositionSelectionTool<Number, Number>();
					tool.setMolecule(molecule);
					return tool;
				}, Cursor.DEFAULT),
				null, positionSelected);
		addTool(positionSelectionCursor);
	}
	
	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		for (SubPlot subPlot : charts) 
			subPlot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
	}
	
	@Override
	public void onMoleculeIndicatorsChangedEvent(Molecule molecule) {
		//Nothing required...
	}

	@Override
	public void addChart() {
		SubPlot subplot = createSubPlot();
		if (molecule != null)
			subplot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		addChart(subplot);
	}
	
	public abstract S createSubPlot();
}
