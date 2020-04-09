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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;

import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.MarsUtil;
import de.gsi.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.paint.Color;

import javafx.util.Callback;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.shape.Line;
import javafx.scene.control.ContentDisplay;

import com.fasterxml.jackson.core.JsonParser;

public class PlotSeries extends AbstractJsonConvertibleRecord {
		private ComboBox<String> yColumnField, xColumnField, typeField;
		private ComboBox<String> lineStyle;
		private JFXColorPicker colorField, segmentColorField;
		private JFXTextField widthField, segmentsWidthField;
		private JFXCheckBox drawSegmentsColumn;
		private RadioButton chartTracking;
		
		private XYChart chart;
		
		private ArrayList<String> columnHeadings;

		protected static String[] types = {"Line","Scatter"};
		
		public PlotSeries(ArrayList<String> columnHeadings) {
			this.columnHeadings = columnHeadings;
			initComponents();
			load();
		}

		public PlotSeries(ArrayList<String> columnHeadings, String xColumn, String yColumn) {
			this.columnHeadings = columnHeadings;
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
			lineStyle = new ComboBox<>();
			colorField = new JFXColorPicker();
			widthField = new JFXTextField();

			drawSegmentsColumn = new JFXCheckBox();
			segmentColorField = new JFXColorPicker();
			segmentsWidthField = new JFXTextField();
		}
		
		void load() {
			typeField.getItems().addAll(types);
			
			xColumnField.getItems().addAll(columnHeadings);
			xColumnField.setPrefWidth(Double.MAX_VALUE);
			
			yColumnField.getItems().addAll(columnHeadings);
			yColumnField.setPrefWidth(Double.MAX_VALUE);
			
			lineStyle.getItems().addAll(" ", "20 20", "12 12", "8 8", "4 4", 
					"26 4 8 4", "15 3 3 3", "20 3 3 3 3 3 3 3", "12 3 3");
			Callback<ListView<String>, ListCell<String>> cellFactory = new Callback<ListView<String>, ListCell<String>>() {
		        @Override 
		        public ListCell<String> call(ListView<String> p) {
		            return new ListCell<String>() {
	                    private final Line line;
	                    { 
	                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY); 
	                        line = new Line(0, 20, 60, 20);
	                    }

	                    @Override 
	                    protected void updateItem(String style, boolean empty) {
	                        super.updateItem(style, empty);
	                        if(style != null && !empty) {
	                        	if (!style.equals(" "))
	                            	line.setStyle("-fx-stroke-dash-array: " + style + ";");
	                        	else 
	                        		line.setStyle(null);
	                            setGraphic(line);
	                        }
	                    }
	                };
		        }
		    };
			lineStyle.setCellFactory(cellFactory);
			lineStyle.setButtonCell(cellFactory.call(null));
			
			//colorField.setPrefWidth(50);
			
			widthField.setText("1.0");
			//widthField.setPrefWidth(30);
			
			//segmentColorField.setPrefWidth(50);
			
			segmentsWidthField.setText("1.0");
			//segmentsWidthField.setPrefWidth(50);
			
			setDefaults();
		}
		
		void setDefaults() {
			typeField.getSelectionModel().select("Line");
			lineStyle.getSelectionModel().select(" ");
			colorField.setValue(Color.BLACK);
			segmentColorField.setValue(Color.RED);
		}
		
		public ComboBox<String> getTypeField() {
			return typeField;
		}
		
		public ComboBox<String> lineStyle() {
			return lineStyle;
		}
		
		public String getLineStyle() {
			return lineStyle.getSelectionModel().getSelectedItem();
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
		
		public boolean track() {
			return chartTracking.isSelected();
		}
		
		public void setChart(XYChart chart) {
			this.chart = chart;
		}
		
		public XYChart getChart() {
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

		@Override
		protected void createIOMaps() {
			outputMap.put("Track", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeBooleanField("Track", track()), IOException.class));
			outputMap.put("Type", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("Type", getType()), IOException.class));
			outputMap.put("xColumn", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("xColumn", getXColumn()), IOException.class));
			outputMap.put("yColumn", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("yColumn", getYColumn()), IOException.class));
			outputMap.put("Style", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("Style", getLineStyle()), IOException.class));
			outputMap.put("Color", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("Color", getColor().toString()), IOException.class));
			outputMap.put("Stroke", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("Stroke", getWidth()), IOException.class));
			outputMap.put("ShowSegments", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeBooleanField("ShowSegments", drawSegments()), IOException.class));
			outputMap.put("SegmentsColor", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("SegmentsColor", getSegmentsColor().toString()), IOException.class));
			outputMap.put("SegmentsStroke", MarsUtil.catchConsumerException(jGenerator ->
			jGenerator.writeStringField("SegmentsStroke", getSegmentsWidth()), IOException.class));
			
			inputMap.put("Track", MarsUtil.catchConsumerException(jParser -> {
				getTrackingButton().setSelected(jParser.getBooleanValue());
			}, IOException.class));
			inputMap.put("Type", MarsUtil.catchConsumerException(jParser -> {
				typeField.getSelectionModel().select(jParser.getText());
			}, IOException.class));
			inputMap.put("xColumn", MarsUtil.catchConsumerException(jParser -> {
				//Do I need to add the selection in case the column is not present for new datasets.
				xColumnField().getSelectionModel().select(jParser.getText());
			}, IOException.class));
			inputMap.put("yColumn", MarsUtil.catchConsumerException(jParser -> {
				//Do I need to add the selection in case the column is not present for new datasets.
				yColumnField().getSelectionModel().select(jParser.getText());
			}, IOException.class));
			inputMap.put("Style", MarsUtil.catchConsumerException(jParser -> {
				lineStyle().getSelectionModel().select(jParser.getText());
			}, IOException.class));
			inputMap.put("Color", MarsUtil.catchConsumerException(jParser -> {
				getColorField().setValue(Color.web(jParser.getText()));
			}, IOException.class));
			inputMap.put("Stroke", MarsUtil.catchConsumerException(jParser -> {
				getWidthField().setText(jParser.getText());
			}, IOException.class));
			inputMap.put("ShowSegments", MarsUtil.catchConsumerException(jParser -> {
				getDrawSegmentsField().setSelected(jParser.getBooleanValue());
			}, IOException.class));
			inputMap.put("SegmentsColor", MarsUtil.catchConsumerException(jParser -> {
				getSegmentsColorField().setValue(Color.web(jParser.getText()));
			}, IOException.class));
			inputMap.put("SegmentsStroke", MarsUtil.catchConsumerException(jParser -> {
				getSegmentsWidthField().setText(jParser.getText());
			}, IOException.class));
		}
}

