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
import com.shootoff.targets.PolygonRegion;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class TargetEditorController {
	@FXML private BorderPane targetEditorPane;
	@FXML private Pane canvasPane;
	@FXML private ToggleButton cursorButton;
	@FXML private ToggleButton rectangleButton;
	@FXML private ToggleButton ovalButton;
	@FXML private ToggleButton triangleButton;
	@FXML private ToggleButton appleseedThreeButton;
	@FXML private ToggleButton appleseedFourButton;
	@FXML private ToggleButton appleseedFiveButton;
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
	private Optional<TagEditorPanel> tagEditor = Optional.empty();
	private double lastMouseX = 0;
	private double lastMouseY = 0;
	
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
		boolean tagEditorOpen = false;
		
		if (cursorShape.isPresent()) {
			Shape previous = cursorShape.get();
			
			// Unhighlight the old selection
			if (!previous.equals(selected)) {
				previous.setStroke(UNSELECTED_STROKE_COLOR);
				
				if (tagEditor.isPresent()) {
					// Close tag editor
					tagsButton.setSelected(false);
					toggleTagEditor();
					tagEditorOpen = true;
				}
			}
		}

		selected.setStroke(Color.GOLD);
		selected.requestFocus();
		toggleShapeControls(true);
		cursorShape = Optional.of(selected);
		regionColorChoiceBox.getSelectionModel().select(
				getColorName((Color)selected.getFill()));
		
		// Re-open editor
		if (tagEditorOpen) {
			tagsButton.setSelected(true);
			toggleTagEditor();
		}
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
			if (tagEditor.isPresent()) {
				tagsButton.setSelected(false);
				toggleTagEditor();
			}
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

		final int DEFAULT_DIM = 40;
		final double AQT_SCALE = 2.5;
		
		if (rectangleButton.isSelected()) {
			newShape = new RectangleRegion(lastMouseX, lastMouseY, 
				DEFAULT_DIM, DEFAULT_DIM);
		} else if (ovalButton.isSelected()) {
			final int RADIUS = DEFAULT_DIM / 2;
			newShape = new EllipseRegion(lastMouseX + RADIUS, 
					lastMouseY + RADIUS, RADIUS, RADIUS);
		} else if (triangleButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX, lastMouseY + (DEFAULT_DIM / 2),
					lastMouseX + DEFAULT_DIM, lastMouseY + (DEFAULT_DIM / 2),
					lastMouseX + (DEFAULT_DIM / 2), lastMouseY);
		} else if (appleseedThreeButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX+15.083*AQT_SCALE, lastMouseY+13.12*AQT_SCALE, 
		            lastMouseX+15.083*AQT_SCALE, lastMouseY+-0.147*AQT_SCALE, 
		            lastMouseX+14.277*AQT_SCALE, lastMouseY+-2.508*AQT_SCALE, 
		            lastMouseX+13.149*AQT_SCALE, lastMouseY+-4.115*AQT_SCALE, 
		            lastMouseX+11.841*AQT_SCALE, lastMouseY+-5.257*AQT_SCALE, 
		            lastMouseX+10.557*AQT_SCALE, lastMouseY+-6.064*AQT_SCALE, 
		            lastMouseX+8.689*AQT_SCALE, lastMouseY+-6.811*AQT_SCALE, 
		            lastMouseX+7.539*AQT_SCALE, lastMouseY+-8.439*AQT_SCALE, 
		            lastMouseX+7.076*AQT_SCALE, lastMouseY+-9.978*AQT_SCALE, 
		            lastMouseX+6.104*AQT_SCALE, lastMouseY+-11.577*AQT_SCALE, 
		            lastMouseX+4.82*AQT_SCALE, lastMouseY+-12.829*AQT_SCALE, 
		            lastMouseX+3.43*AQT_SCALE, lastMouseY+-13.788*AQT_SCALE, 
		            lastMouseX+1.757*AQT_SCALE, lastMouseY+-14.386*AQT_SCALE, 
		            lastMouseX+0.083*AQT_SCALE, lastMouseY+-14.55*AQT_SCALE, 
		            lastMouseX+-1.59*AQT_SCALE, lastMouseY+-14.386*AQT_SCALE, 
		            lastMouseX+-3.263*AQT_SCALE, lastMouseY+-13.788*AQT_SCALE, 
		            lastMouseX+-4.653*AQT_SCALE, lastMouseY+-12.829*AQT_SCALE, 
		            lastMouseX+-5.938*AQT_SCALE, lastMouseY+-11.577*AQT_SCALE, 
		            lastMouseX+-6.909*AQT_SCALE, lastMouseY+-9.978*AQT_SCALE, 
		            lastMouseX+-7.372*AQT_SCALE, lastMouseY+-8.439*AQT_SCALE, 
		            lastMouseX+-8.522*AQT_SCALE, lastMouseY+-6.811*AQT_SCALE, 
		            lastMouseX+-10.39*AQT_SCALE, lastMouseY+-6.064*AQT_SCALE, 
		            lastMouseX+-11.674*AQT_SCALE, lastMouseY+-5.257*AQT_SCALE, 
		            lastMouseX+-12.982*AQT_SCALE, lastMouseY+-4.115*AQT_SCALE, 
		            lastMouseX+-14.11*AQT_SCALE, lastMouseY+-2.508*AQT_SCALE, 
		            lastMouseX+-14.917*AQT_SCALE, lastMouseY+-0.147*AQT_SCALE, 
		            lastMouseX+-14.917*AQT_SCALE, lastMouseY+13.12*AQT_SCALE);
		} else if (appleseedFourButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX+11.66*AQT_SCALE, lastMouseY+5.51*AQT_SCALE, 
	                lastMouseX+11.595*AQT_SCALE, lastMouseY+0.689*AQT_SCALE, 
	                lastMouseX+11.1*AQT_SCALE, lastMouseY+-1.084*AQT_SCALE, 
	                lastMouseX+9.832*AQT_SCALE, lastMouseY+-2.441*AQT_SCALE, 
	                lastMouseX+7.677*AQT_SCALE, lastMouseY+-3.322*AQT_SCALE, 
	                lastMouseX+5.821*AQT_SCALE, lastMouseY+-4.709*AQT_SCALE, 
	                lastMouseX+4.715*AQT_SCALE, lastMouseY+-6.497*AQT_SCALE, 
	                lastMouseX+4.267*AQT_SCALE, lastMouseY+-8.135*AQT_SCALE, 
	                lastMouseX+3.669*AQT_SCALE, lastMouseY+-9.41*AQT_SCALE, 
	                lastMouseX+2.534*AQT_SCALE, lastMouseY+-10.553*AQT_SCALE, 
	                lastMouseX+1.436*AQT_SCALE, lastMouseY+-11.091*AQT_SCALE, 
	                lastMouseX+0.083*AQT_SCALE, lastMouseY+-11.323*AQT_SCALE, 
	                lastMouseX+-1.269*AQT_SCALE, lastMouseY+-11.091*AQT_SCALE, 
	                lastMouseX+-2.367*AQT_SCALE, lastMouseY+-10.553*AQT_SCALE, 
	                lastMouseX+-3.502*AQT_SCALE, lastMouseY+-9.41*AQT_SCALE, 
	                lastMouseX+-4.1*AQT_SCALE, lastMouseY+-8.135*AQT_SCALE, 
	                lastMouseX+-4.548*AQT_SCALE, lastMouseY+-6.497*AQT_SCALE, 
	                lastMouseX+-5.654*AQT_SCALE, lastMouseY+-4.709*AQT_SCALE, 
	                lastMouseX+-7.51*AQT_SCALE, lastMouseY+-3.322*AQT_SCALE, 
	                lastMouseX+-9.665*AQT_SCALE, lastMouseY+-2.441*AQT_SCALE, 
	                lastMouseX+-10.933*AQT_SCALE, lastMouseY+-1.084*AQT_SCALE, 
	                lastMouseX+-11.428*AQT_SCALE, lastMouseY+0.689*AQT_SCALE, 
	                lastMouseX+-11.493*AQT_SCALE, lastMouseY+5.51*AQT_SCALE);
		} else if (appleseedFiveButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX+7.893*AQT_SCALE, lastMouseY+3.418*AQT_SCALE, 
	                lastMouseX+7.893*AQT_SCALE, lastMouseY+1.147*AQT_SCALE, 
	                lastMouseX+7.255*AQT_SCALE, lastMouseY+0.331*AQT_SCALE, 
	                lastMouseX+5.622*AQT_SCALE, lastMouseY+-0.247*AQT_SCALE, 
	                lastMouseX+4.187*AQT_SCALE, lastMouseY+-1.124*AQT_SCALE, 
	                lastMouseX+2.833*AQT_SCALE, lastMouseY+-2.339*AQT_SCALE, 
	                lastMouseX+1.917*AQT_SCALE, lastMouseY+-3.594*AQT_SCALE, 
	                lastMouseX+1.219*AQT_SCALE, lastMouseY+-5.048*AQT_SCALE, 
	                lastMouseX+0.9*AQT_SCALE, lastMouseY+-6.223*AQT_SCALE, 
	                lastMouseX+0.801*AQT_SCALE, lastMouseY+-7.1*AQT_SCALE, 
	                lastMouseX+0.521*AQT_SCALE, lastMouseY+-7.558*AQT_SCALE, 
	                lastMouseX+0.083*AQT_SCALE, lastMouseY+-7.617*AQT_SCALE, 
	                lastMouseX+-0.354*AQT_SCALE, lastMouseY+-7.558*AQT_SCALE, 
	                lastMouseX+-0.634*AQT_SCALE, lastMouseY+-7.1*AQT_SCALE, 
	                lastMouseX+-0.733*AQT_SCALE, lastMouseY+-6.223*AQT_SCALE, 
	                lastMouseX+-1.052*AQT_SCALE, lastMouseY+-5.048*AQT_SCALE, 
	                lastMouseX+-1.75*AQT_SCALE, lastMouseY+-3.594*AQT_SCALE, 
	                lastMouseX+-2.666*AQT_SCALE, lastMouseY+-2.339*AQT_SCALE, 
	                lastMouseX+-4.02*AQT_SCALE, lastMouseY+-1.124*AQT_SCALE, 
	                lastMouseX+-5.455*AQT_SCALE, lastMouseY+-0.247*AQT_SCALE, 
	                lastMouseX+-7.088*AQT_SCALE, lastMouseY+0.331*AQT_SCALE, 
	                lastMouseX+-7.726*AQT_SCALE, lastMouseY+1.147*AQT_SCALE, 
	                lastMouseX+-7.726*AQT_SCALE, lastMouseY+3.418*AQT_SCALE);
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
			
			Collections.swap(targetShapes, selectedIndex, selectedIndex + 1);
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
			
			Collections.swap(targetShapes, selectedIndex - 1, selectedIndex);
		}
	}
	
	@FXML
	public void toggleTagEditor(ActionEvent event) {
		if (cursorShape.isPresent() && 
				!targetShapes.contains(cursorShape.get())) return;
		
		toggleTagEditor();
	}
	
	public void toggleTagEditor() {
		TargetRegion selected = (TargetRegion)cursorShape.get();
		
		if (tagsButton.isSelected()) {
			TagEditorPanel editor = new TagEditorPanel(selected.getAllTags());
			tagEditor = Optional.of(editor);
			targetEditorPane.getChildren().add(editor);
			editor.setLayoutX(tagsButton.getLayoutX() + tagsButton.getPadding().getLeft() - 2);
			editor.setLayoutY(tagsButton.getLayoutY() + tagsButton.getHeight() + 
					tagsButton.getPadding().getBottom() + 2);
		} else if (tagEditor.isPresent()) {
			TagEditorPanel editor = tagEditor.get();
			targetEditorPane.getChildren().remove(editor);
			selected.replaceAllTags(editor.getTags());
			tagEditor = Optional.empty();
		}
	}
}
