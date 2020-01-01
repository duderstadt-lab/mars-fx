package de.mpg.biochem.mars.fx.molecule.dashboardTab;

import org.scijava.script.ScriptLanguage;
import org.scijava.ui.swing.script.EditorPane;

public class LanguageSettableEditorPane extends EditorPane {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void setLanguage(final ScriptLanguage language) {
		setLanguage(language, false);
	}
}