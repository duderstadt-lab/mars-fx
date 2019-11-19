package de.mpg.biochem.mars.fx.plot;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

public class DefaultMoleculeSubPlot extends AbstractMoleculeSubPlot<Molecule>{
	public DefaultMoleculeSubPlot(PlotPane plotPane, String plotTitle) {
		super(plotPane, plotTitle);
	}
}
