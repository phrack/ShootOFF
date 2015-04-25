/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.TargetRegion;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
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
	
	private static final Color DEFAULT_FILL_COLOR = Color.BLACK;
	private static final Color UNSELECTED_STROKE_COLOR = Color.BLACK;
	private static final double DEFAULT_OPACITY = 0.7;
	private static final int MOVEMENT_DELTA = 1;
	private static final int SCALE_DELTA = 1;
	
	private Optional<Shape> cursorShape = Optional.empty();
	private final List<Shape> targetShapes = new ArrayList<Shape>();
	private double lastMouseX = 0;
	private double lastMouseY = 0;
	
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
	
	private String getColorName(Color color) {
		if (color == Color.BLACK) {
			return "black";
		} else if (color == Color.BLUE) {
			return "blue";
		} else if (color == Color.GREEN) {
			return "green";
		} else if (color == Color.ORANGE) {
			return "orange";
		} else if (color == Color.RED) {
			return "red";
		} else if (color == Color.WHITE) {
			return "white";
		} else {
			return "cornsilk";
		}
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
		selected.setOnKeyPressed((e) -> { shapeKeyPressed(e); }); 
		
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
		selected.requestFocus();
		toggleShapeControls(true);
		cursorShape = Optional.of(selected);
		regionColorChoiceBox.getSelectionModel().select(
				getColorName((Color)selected.getFill()));
	}
	
	@SuppressWarnings("incomplete-switch")
	public void shapeKeyPressed(KeyEvent event) {
		Shape selected = (Shape)event.getTarget();
		TargetRegion region = (TargetRegion)selected;
		
		switch (event.getCode()) {
		case DELETE:
			targetShapes.remove(selected);
			canvasPane.getChildren().remove(selected);
			toggleShapeControls(false);
			break;
			
		case LEFT:
			if (event.isShiftDown()) {
				region.changeWidth(SCALE_DELTA * -1);
			} else {
				selected.setLayoutX(selected.getLayoutX() - MOVEMENT_DELTA);
			}
			break;
			
		case RIGHT:
			if (event.isShiftDown()) {
				region.changeWidth(SCALE_DELTA);
			} else {
				selected.setLayoutX(selected.getLayoutX() + MOVEMENT_DELTA);
			}
			break;
			
		case UP:
			if (event.isShiftDown()) {
				region.changeHeight(SCALE_DELTA * -1);
			} else {
				selected.setLayoutY(selected.getLayoutY() - MOVEMENT_DELTA);
			}
			break;

		case DOWN:
			if (event.isShiftDown()) {
				region.changeHeight(SCALE_DELTA);
			} else {
				selected.setLayoutY(selected.getLayoutY() + MOVEMENT_DELTA);
			}
			break;
		}
		
		event.consume();
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
		
		if (cursorShape.isPresent() && 
				!targetShapes.contains(cursorShape.get())) {
			canvasPane.getChildren().remove(cursorShape.get());
		}
		
		drawShape();
	}
	
	private void drawShape() {
		Shape newShape = null;

		if (rectangleButton.isSelected()) {
			newShape = new RectangleRegion(lastMouseX, lastMouseY, 
				30, 30);
		} else if (ovalButton.isSelected()) {
			final int radius = 15;
			newShape = new EllipseRegion(lastMouseX + radius, 
					lastMouseY + radius, radius, radius);
		} else {
			cursorShape = Optional.empty();
			System.err.println("Unimplemented region type selected.");
			return;
		}

		newShape.setFill(DEFAULT_FILL_COLOR);
		newShape.setOpacity(DEFAULT_OPACITY);
		canvasPane.getChildren().add(newShape);
		
		cursorShape = Optional.of(newShape);
	}
	
	@FXML
	public void bringForward(ActionEvent event) {
		if (cursorShape.isPresent() && 
				!targetShapes.contains(cursorShape.get())) return;
		
		ObservableList<Node> shapesList = canvasPane.getChildren();
		int selectedIndex = shapesList.indexOf(cursorShape.get());

		if (selectedIndex < shapesList.size() - 1) {
			// We have to do this dance instead of just calling
			// Collections.swap otherwise we get an IllegalArgumentException
			// from the Scene for duplicating a child node
			Node topShape = shapesList.get(selectedIndex + 1);
			Node bottomShape = shapesList.get(selectedIndex);
			shapesList.remove(selectedIndex + 1);
			shapesList.remove(selectedIndex);
			shapesList.add(selectedIndex, topShape);
			shapesList.add(selectedIndex + 1, bottomShape);
		}
	}
	
	@FXML
	public void sendBackward(ActionEvent event) {
		if (cursorShape.isPresent() && 
				!targetShapes.contains(cursorShape.get())) return;
		
		ObservableList<Node> shapesList = canvasPane.getChildren();
		int selectedIndex = shapesList.indexOf(cursorShape.get());

		if (selectedIndex > 0) {
			Node topShape = shapesList.get(selectedIndex);
			Node bottomShape = shapesList.get(selectedIndex - 1);
			shapesList.remove(selectedIndex);
			shapesList.remove(selectedIndex - 1);
			shapesList.add(selectedIndex - 1, topShape);
			shapesList.add(selectedIndex, bottomShape);
		}
	}
	
	@FXML
	public void toggleTagEditor(ActionEvent event) {
		
	}
}
