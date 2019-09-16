/*******************************************************************************
 * Copyright (C) 2019, Karl Duderstadt
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.plot;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;

import de.mpg.biochem.mars.table.MarsTable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.paint.Color;

public class PlotSeries {
		private ComboBox<String> yColumnField, xColumnField, typeField;
		private JFXColorPicker colorField, segmentColorField;
		private JFXTextField widthField, segmentsWidthField;
		private JFXCheckBox drawSegmentsColumn;
		private RadioButton chartTracking;
		
		private MarsTable dataTable;
		
		private XYChart<Number, Number> chart;
		
		private String[] columnHeadings;

		protected static String[] types = {"Line","Scatter"};
		
		public PlotSeries(MarsTable dataTable) {
			this.dataTable = dataTable;
			this.columnHeadings = dataTable.getColumnHeadings();
			initComponents();
			load();
		}

		public PlotSeries(MarsTable dataTable, String xColumn, String yColumn) {
			this.dataTable = dataTable;
			this.columnHeadings = dataTable.getColumnHeadings();
			initComponents();
			load();
			
			xColumnField.getSelectionModel().select(xColumn);
			yColumnField.getSelectionModel().select(yColumn);
		}
		
		void initComponents() {
			chartTracking = new RadioButton();
			typeField = new ComboBox<>();
			xColumnField = new ComboBox<>();
			yColumnField = new ComboBox<>();
			colorField = new JFXColorPicker();
			widthField = new JFXTextField();

			drawSegmentsColumn = new JFXCheckBox();
			segmentColorField = new JFXColorPicker();
			segmentsWidthField = new JFXTextField();
		}
		
		void load() {
			typeField.getItems().addAll(types);
			
			xColumnField.getItems().addAll(columnHeadings);
			yColumnField.getItems().addAll(columnHeadings);
			
			colorField.setPrefWidth(50);
			
			widthField.setText("1.0");
			widthField.setPrefWidth(50);
			
			segmentColorField.setPrefWidth(50);
			
			segmentsWidthField.setText("1.0");
			segmentsWidthField.setPrefWidth(50);
			
			setDefaults();
		}
		
		void setDefaults() {
			typeField.getSelectionModel().select("Line");
			colorField.setValue(Color.BLACK);
			segmentColorField.setValue(Color.RED);
		}
		
		public ComboBox<String> getTypeField() {
			return typeField;
		}
		
		public JFXColorPicker getColorField() {
			return colorField;
		}
		
		public JFXColorPicker getSegmentsColorField() {
			return segmentColorField;
		}
		
		public ComboBox<String> yColumnField() {
			return yColumnField;
		}
		
		public ComboBox<String> xColumnField() {
			return xColumnField;
		}
		
		public RadioButton getTrackingButton() {
			return chartTracking;
		}
		
		public boolean trackChart() {
			return chartTracking.isSelected();
		}
		
		public void setChart(XYChart<Number, Number> chart) {
			this.chart = chart;
		}
		
		public XYChart<Number, Number> getChart() {
			return chart;
		}
					
		public JFXCheckBox getDrawSegmentsField() {
			return drawSegmentsColumn;
		}
		
		public JFXTextField getWidthField() {
			return widthField;
		}
		
		public String getWidth() {
			return widthField.getText();
		}
		
		public String getSegmentsWidth() {
			return segmentsWidthField.getText();
		}
		
		public JFXTextField getSegmentsWidthField() {
			return segmentsWidthField;
		}
		
		public String getType() {
			return typeField.getSelectionModel().getSelectedItem();
		}
		
		public boolean drawSegments() {
			return drawSegmentsColumn.isSelected();
		}
		
		public String getYColumn() {
			return yColumnField.getSelectionModel().getSelectedItem();
		}
		
		public String getXColumn() {
			return xColumnField.getSelectionModel().getSelectedItem();
		}
		
		public Color getColor() {
			return colorField.getValue();
		}
		
		public Color getSegmentsColor() {
			return segmentColorField.getValue();
		}
		
		public MarsTable getDataTable() {
			return dataTable;
		}
}

