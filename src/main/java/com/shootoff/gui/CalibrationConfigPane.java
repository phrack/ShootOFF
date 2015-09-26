/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.gui;

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class CalibrationConfigPane extends BorderPane {
	private final Pane parent;
	private final ToggleGroup configToggleGroup = new ToggleGroup();
	private final RadioButton detectEverywhereRadioButton = new RadioButton("Detect everywhere");
	private final RadioButton minimizeDetectionRadioButton = new RadioButton("Only detect in projector bounds"); 
	private final RadioButton cropRadioButton = new RadioButton("Crop feed to projector bounds");
	
	public CalibrationConfigPane(Pane parent) {
		this.parent = parent;
		
		detectEverywhereRadioButton.setToggleGroup(configToggleGroup);
		minimizeDetectionRadioButton.setToggleGroup(configToggleGroup);
		cropRadioButton.setToggleGroup(configToggleGroup);
		
		minimizeDetectionRadioButton.setSelected(true);
		
		detectEverywhereRadioButton.setTextFill(Color.WHITE);
		minimizeDetectionRadioButton.setTextFill(Color.WHITE);
		cropRadioButton.setTextFill(Color.WHITE);
		
		this.setTop(detectEverywhereRadioButton);
		this.setCenter(minimizeDetectionRadioButton);
		this.setBottom(cropRadioButton);
		
		this.setStyle("-fx-background-color: darkgray;");
		
		parent.getChildren().add(this);
	}
	
	public CalibrationConfigPane(Pane parent, boolean detectEverywhere, boolean limitDetection, boolean cropProjection) {
		this(parent);
		
		if (detectEverywhere) {
			detectEverywhereRadioButton.setSelected(true);
		} else if (limitDetection) {
			minimizeDetectionRadioButton.setSelected(true);
		} else if (cropProjection) {
			cropRadioButton.setSelected(true);
		}
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
