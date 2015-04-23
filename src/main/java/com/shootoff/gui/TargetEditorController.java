/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.shootoff.targets.RectangleRegion;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class TargetEditorController {
	@FXML private Pane canvasPane;
	@FXML private ToggleButton cursorButton;
	@FXML private ToggleButton rectangleButton;
	@FXML private ToggleButton ovalButton;
	@FXML private Button sendBackwardButton;
	@FXML private Button bringForwardButton;
	@FXML private ToggleButton tagsButton;
	@FXML private ChoiceBox<String> regionColorChoiceBox;
	
	private final Color DEFAULT_FILL_COLOR = Color.BLACK;
	private final Color UNSELECTED_STROKE_COLOR = Color.BLACK;
	private final double DEFAULT_OPACITY = 0.7;
	
	private Optional<Shape> cursorShape = Optional.empty();
	private final Set<Shape> targetShapes = new HashSet<Shape>();
	private double lastMouseX = 0;
	private double lastMouseY = 0;
	
	// TODO: Send shape back
	// TODO: Bring shape forward
	// TODO: Set color choice box on region selection
	// TODO: Add/remove tags for selected shape
	
	public void init(Image backgroundImg) {
		regionColorChoiceBox.setItems(FXCollections.observableArrayList(
	    		"black", "blue", "green", "orange", "red", "white"));
		
		regionColorChoiceBox.getSelectionModel().selectedItemProperty().addListener(
			new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable,
						String oldValue, String newValue) {
					
					if (cursorShape.isPresent()) {
						cursorShape.get().setFill(createColor(newValue));
					}
				}
				
				private Color createColor(String name) {
					switch (name) {
					case "black": return Color.BLACK;
					case "blue": return Color.BLUE;
					case "green": return Color.GREEN;
					case "orange": return Color.ORANGE;
					case "red": return Color.RED;
					case "white": return Color.WHITE;
					default: return Color.CORNSILK;
					}
				}
		    });
	}
	
	private void toggleShapeControls(boolean enabled) {
		sendBackwardButton.setDisable(!enabled);
		bringForwardButton.setDisable(!enabled);
		tagsButton.setDisable(!enabled);
		regionColorChoiceBox.setDisable(!enabled);
	}

	@FXML
	public void mouseMoved(MouseEvent event) {
		if (!cursorShape.isPresent() || cursorButton.isSelected()) return;
		
		Shape selected = cursorShape.get();
		
		lastMouseX = event.getX() - (selected.getLayoutBounds().getWidth() / 2);
		lastMouseY = event.getY() - (selected.getLayoutBounds().getHeight() / 2);
		
		if (lastMouseX >= 0)
			selected.setLayoutX(lastMouseX - selected.getLayoutBounds().getMinX());
		
		if (lastMouseY >= 0)
			selected.setLayoutY(lastMouseY - selected.getLayoutBounds().getMinY());
	}
	
	@FXML
	public void shapeDropped(MouseEvent event) {
		if (!cursorShape.isPresent() || cursorButton.isSelected()) return;
		
		Shape selected = cursorShape.get();
		targetShapes.add(selected);
		selected.setOnMouseClicked((e) -> { shapeClicked(e); });
		
		drawShape();
	}
	
	public void shapeClicked(MouseEvent event) {
		if (!cursorButton.isSelected()) {
			// We want to drop a new shape
			shapeDropped(event);
			return;
		}
		
		// Want to select the current shape
		Shape selected = (Shape)event.getTarget();
		
		if (cursorShape.isPresent()) {
			Shape previous = cursorShape.get();
			
			// Unhighlight the old selection
			if (!previous.equals(selected))
				previous.setStroke(UNSELECTED_STROKE_COLOR);
		}

		selected.setStroke(Color.GOLD);
		
		toggleShapeControls(true);
		cursorShape = Optional.of(selected);
	}
	
	@FXML
	public void cursorSelected(ActionEvent event) {
		if (!cursorShape.isPresent()) return;
		
		// Remove shape that was never actually placed
		Shape selected = cursorShape.get();
		if (!targetShapes.contains(selected)) 
			canvasPane.getChildren().remove(selected);
		
		cursorShape = Optional.empty();
	}
	
	@FXML
	public void drawShape(ActionEvent event) {
		lastMouseX = 0;
		lastMouseY = 0;
		
		drawShape();
	}
	
	private void drawShape() {
		Shape newShape = null;

		if (rectangleButton.isSelected()) {
			newShape = new RectangleRegion(lastMouseX, lastMouseY, 
				30, 30);
		} else if (ovalButton.isSelected()) {
			// TODO: Draw oval
		}

		newShape.setFill(DEFAULT_FILL_COLOR);
		newShape.setOpacity(DEFAULT_OPACITY);
		canvasPane.getChildren().add(newShape);
		
		cursorShape = Optional.of(newShape);
	}
}
