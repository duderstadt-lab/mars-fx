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
			xColumnField.setPrefWidth(150);
			
			yColumnField.getItems().addAll(columnHeadings);
			yColumnField.setPrefWidth(150);
			
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
			
			setJsonField("track", 
				jGenerator -> jGenerator.writeBooleanField("track", track()),
				jParser -> getTrackingButton().setSelected(jParser.getBooleanValue()));
			
			setJsonField("type", 
				jGenerator -> jGenerator.writeStringField("type", getType()), 
				jParser -> typeField.getSelectionModel().select(jParser.getText()));
				
			setJsonField("xColumn", 
				jGenerator -> jGenerator.writeStringField("xColumn", getXColumn()),
				jParser -> xColumnField().getSelectionModel().select(jParser.getText()));
			
			setJsonField("yColumn", 
				jGenerator -> jGenerator.writeStringField("yColumn", getYColumn()),
				jParser -> yColumnField().getSelectionModel().select(jParser.getText()));
			
			setJsonField("style", 
				jGenerator -> jGenerator.writeStringField("style", getLineStyle()),
				jParser -> lineStyle().getSelectionModel().select(jParser.getText()));
			
			setJsonField("color", 
				jGenerator -> jGenerator.writeStringField("color", getColor().toString()), 
				jParser -> getColorField().setValue(Color.web(jParser.getText())));
			
			setJsonField("stroke", 
				jGenerator -> jGenerator.writeStringField("stroke", getWidth()),
				jParser -> getWidthField().setText(jParser.getText()));
			
			setJsonField("showSegments", 
				jGenerator -> jGenerator.writeBooleanField("showSegments", drawSegments()), 
				jParser -> getDrawSegmentsField().setSelected(jParser.getBooleanValue()));
			
			setJsonField("segmentsColor", 
				jGenerator -> jGenerator.writeStringField("segmentsColor", getSegmentsColor().toString()), 
				jParser -> getSegmentsColorField().setValue(Color.web(jParser.getText())));
			
			setJsonField("segmentsStroke", 
				jGenerator -> jGenerator.writeStringField("segmentsStroke", getSegmentsWidth()), 
				jParser -> getSegmentsWidthField().setText(jParser.getText()));
			
			/*
			 * 
			 * The fields below are needed for backwards compatibility.
			 * 
			 * Please remove for a future release.
			 * 
			 */

			setJsonField("Track", null,
					jParser -> getTrackingButton().setSelected(jParser.getBooleanValue()));
				
			setJsonField("Type", null, 
				jParser -> typeField.getSelectionModel().select(jParser.getText()));
				
			setJsonField("xColumn", null,
				jParser -> xColumnField().getSelectionModel().select(jParser.getText()));
			
			setJsonField("yColumn", null,
				jParser -> yColumnField().getSelectionModel().select(jParser.getText()));
			
			setJsonField("Style", null,
				jParser -> lineStyle().getSelectionModel().select(jParser.getText()));
			
			setJsonField("Color", null, 
				jParser -> getColorField().setValue(Color.web(jParser.getText())));
			
			setJsonField("Stroke", null,
				jParser -> getWidthField().setText(jParser.getText()));
			
			setJsonField("ShowSegments", null, 
				jParser -> getDrawSegmentsField().setSelected(jParser.getBooleanValue()));
			
			setJsonField("SegmentsColor", null, 
				jParser -> getSegmentsColorField().setValue(Color.web(jParser.getText())));
			
			setJsonField("SegmentsStroke", null, 
				jParser -> getSegmentsWidthField().setText(jParser.getText()));
			
		}
}

