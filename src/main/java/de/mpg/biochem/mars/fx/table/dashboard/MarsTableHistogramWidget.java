/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
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

package de.mpg.biochem.mars.fx.table.dashboard;

import java.io.IOException;

import net.imagej.ops.Initializable;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.script.ScriptModule;

import de.mpg.biochem.mars.fx.dashboard.AbstractHistogramWidget;
import de.mpg.biochem.mars.table.MarsTable;

@Plugin(type = MarsTableDashboardWidget.class,
	name = "MarsTableHistogramWidget")
public class MarsTableHistogramWidget extends AbstractHistogramWidget implements
	MarsTableDashboardWidget, SciJavaPlugin, Initializable
{

	protected MarsTable table;

	@Override
	public void initialize() {
		super.initialize();

		try {
			loadScript("histogramchart",
				"#@ Context scijavaContext\n#@ MarsTable table\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setScriptInputs(ScriptModule module) {
		module.setInput("scijavaContext", context);
		module.setInput("table", table);

		if (lang.getLanguageName().equals("Python (scyjava)")) {
			module.setInput("width", Float.valueOf((float) rootPane.getWidth() / 72));
			module.setInput("height", Float.valueOf((float) (rootPane.getHeight() -
				65) / 72));
		}
	}

	public void setTable(MarsTable table) {
		this.table = table;
	}

	public MarsTable getTable() {
		return table;
	}

	@Override
	public String getName() {
		return "MarsTableHistogramWidget";
	}
}
