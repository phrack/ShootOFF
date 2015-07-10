package com.shootoff.gui;

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class CalibrationConfigPane extends BorderPane {
	private final Pane parent;
	private final ToggleGroup configToggleGroup = new ToggleGroup();
	private final RadioButton minimizeDetectionRadioButton = new RadioButton("Only detect in projector bounds"); 
	private final RadioButton cropRadioButton = new RadioButton("Crop feed to projector bounds");
	
	public CalibrationConfigPane(Pane parent) {
		this.parent = parent;
		
		minimizeDetectionRadioButton.setToggleGroup(configToggleGroup);
		cropRadioButton.setToggleGroup(configToggleGroup);
		
		minimizeDetectionRadioButton.setStyle("-fx-background-color: darkgray;");
		minimizeDetectionRadioButton.setTextFill(Color.WHITE);
		cropRadioButton.setStyle("-fx-background-color: darkgray;");
		cropRadioButton.setTextFill(Color.WHITE);
		
		this.setLeft(minimizeDetectionRadioButton);
		this.setRight(cropRadioButton);
		
		parent.getChildren().add(this);
	}
	
	public boolean limitDetectProjection() {
		return minimizeDetectionRadioButton.isSelected();
	}
	
	public boolean cropFeed() {
		return cropRadioButton.isSelected();
	}
	
	public void close() {
		parent.getChildren().remove(this);
	}
}
