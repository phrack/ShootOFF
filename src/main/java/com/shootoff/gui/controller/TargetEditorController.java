/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

package com.shootoff.gui.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.TargetListener;
import com.shootoff.gui.pane.TagEditorPane;
import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.PolygonRegion;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.animation.GifAnimation;
import com.shootoff.targets.animation.SpriteAnimation;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;

public class TargetEditorController {
	@FXML private VBox targetEditorPane;
	@FXML private Pane canvasPane;
	@FXML private ToggleButton cursorButton;
	@FXML private ToggleButton imageButton;
	@FXML private ToggleButton rectangleButton;
	@FXML private ToggleButton ovalButton;
	@FXML private ToggleButton triangleButton;
	@FXML private ToggleButton appleseedThreeButton;
	@FXML private ToggleButton appleseedFourButton;
	@FXML private ToggleButton appleseedFiveButton;
	@FXML private ToggleButton freeformButton;
	@FXML private Button sendBackwardButton;
	@FXML private Button bringForwardButton;
	@FXML private ToggleButton tagsButton;
	@FXML private ChoiceBox<String> regionColorChoiceBox;

	private static final Logger logger = LoggerFactory.getLogger(TargetEditorController.class);

	private static final Color DEFAULT_FILL_COLOR = Color.BLACK;
	private static final Color DARK_GRAY = Color.rgb(0x50, 0x50, 0x50, 1.0f);

	private static final int MOVEMENT_DELTA = 1;
	private static final int SCALE_DELTA = 1;

	private final ImageView backgroundImageView = new ImageView();
	private TargetListener targetListener = null;
	private Optional<Node> cursorRegion = Optional.empty();
	private final List<Node> targetRegions = new ArrayList<>();
	private final Map<String, String> targetTags = new HashMap<>();
	private Optional<TagEditorPane> tagEditor = Optional.empty();
	private final List<Double> freeformPoints = new ArrayList<>();
	private final Stack<Shape> freeformShapes = new Stack<>();
	private Optional<Line> freeformEdge = Optional.empty();
	private double lastMouseX = 0;
	private double lastMouseY = 0;

	public void init(Image backgroundImg, TargetListener targetListener) {
		if (backgroundImg != null) {
			backgroundImageView.setImage(backgroundImg);

			backgroundImageView.setOnMouseClicked((event) -> {
				boolean reopenEditor = false;

				if (tagEditor.isPresent()) {
					// Close tag editor
					tagsButton.setSelected(false);
					toggleTagEditor();
					reopenEditor = true;
				}

				if (cursorRegion.isPresent()) {
					unhighlightRegion(cursorRegion.get());
					cursorRegion = Optional.empty();
				}

				if (reopenEditor) {
					// Re-open editor set for target tags
					tagsButton.setSelected(true);
					toggleTagEditor();
				}
			});

			canvasPane.getChildren().add(backgroundImageView);
		}

		this.targetListener = targetListener;

		regionColorChoiceBox.setItems(
				FXCollections.observableArrayList("black", "blue", "brown", "gray", "green", "orange", "red", "white"));

		regionColorChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (cursorRegion.isPresent()) {
					final TargetRegion selected = (TargetRegion) cursorRegion.get();

					if (selected.getType() != RegionType.IMAGE) ((Shape) selected).setFill(createColor(newValue));
				}
			}
		});
	}
	
	public Pane getPane() {
		return targetEditorPane;
	}
	
	public void init(Image backgroundImg, TargetListener targetListener, File targetFile) {
		init(backgroundImg, targetListener);

		final Optional<TargetComponents> targetComponents = TargetIO.loadTarget(targetFile);

		if (targetComponents.isPresent()) {
			TargetComponents tc = targetComponents.get();

			targetTags.putAll(tc.getTargetTags());
			targetRegions.addAll(tc.getTargetGroup().getChildren());

			for (Node region : tc.getTargetGroup().getChildren()) {
				region.setOnMouseClicked((e) -> {
					regionClicked(e);
				});
				region.setOnKeyPressed((e) -> {
					regionKeyPressed(e);
				});
			}

			canvasPane.getChildren().addAll(tc.getTargetGroup().getChildren());
		}
	}

	private void toggleShapeControls(final boolean enabled) {
		sendBackwardButton.setDisable(!enabled);
		bringForwardButton.setDisable(!enabled);
		tagsButton.setDisable(!enabled);
		regionColorChoiceBox.setDisable(!enabled);
	}

	public static Color createColor(final String name) {
		switch (name) {
		case "black":
			return Color.BLACK;
		case "blue":
			return Color.BLUE;
		case "brown":
			return Color.SADDLEBROWN;
		case "gray":
			return DARK_GRAY;
		case "green":
			return Color.GREEN;
		case "orange":
			return Color.ORANGE;
		case "red":
			return Color.RED;
		case "white":
			return Color.WHITE;
		default:
			return Color.CORNSILK;
		}
	}

	public static String getColorName(final Color color) {
		if (Color.BLACK.equals(color)) {
			return "black";
		} else if (Color.BLUE.equals(color)) {
			return "blue";
		} else if (Color.SADDLEBROWN.equals(color)) {
			return "brown";
		} else if (DARK_GRAY.equals(color)) {
			return "gray";
		} else if (Color.GREEN.equals(color)) {
			return "green";
		} else if (Color.ORANGE.equals(color)) {
			return "orange";
		} else if (Color.RED.equals(color)) {
			return "red";
		} else if (Color.WHITE.equals(color)) {
			return "white";
		} else {
			return "cornsilk";
		}
	}

	@FXML
	public void saveTarget(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Target");
		fileChooser.setInitialDirectory(new File(System.getProperty("shootoff.home") + File.separator + "targets"));
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("ShootOFF Target (*.target)", "*.target"));
		File targetFile = fileChooser.showSaveDialog(canvasPane.getParent().getScene().getWindow());

		if (targetFile != null) {
			String path = targetFile.getPath();
			if (!path.endsWith(".target")) path += ".target";

			targetFile = new File(path);
			final boolean isNewTarget = !targetFile.exists();

			TargetIO.saveTarget(targetTags, targetRegions, targetFile);

			if (isNewTarget) targetListener.newTarget(targetFile);
		}
	}
	
	@FXML
	public void setBackgroundImage(ActionEvent event) {
		final File selectedBackground = chooseImageFile();
		
		if (selectedBackground != null) {
			backgroundImageView.setImage(new Image(selectedBackground.toURI().toString()));
			canvasPane.setPrefSize(backgroundImageView.getBoundsInLocal().getWidth(),
					backgroundImageView.getBoundsInLocal().getWidth());
		}
	}

	@FXML
	public void mouseMoved(MouseEvent event) {
		if (freeformButton.isSelected()) {
			drawTempPolygonEdge(event);
		}

		if (!cursorRegion.isPresent() || cursorButton.isSelected()) return;

		final Node selected = cursorRegion.get();

		lastMouseX = event.getX() - (selected.getLayoutBounds().getWidth() / 2);
		lastMouseY = event.getY() - (selected.getLayoutBounds().getHeight() / 2);

		if (lastMouseX < 0) lastMouseX = 0;
		if (lastMouseY < 0) lastMouseY = 0;

		if (event.getX() + (selected.getLayoutBounds().getWidth() / 2) <= canvasPane.getWidth())
			selected.setLayoutX(lastMouseX - selected.getLayoutBounds().getMinX());

		if (event.getY() + (selected.getLayoutBounds().getHeight() / 2) <= canvasPane.getHeight())
			selected.setLayoutY(lastMouseY - selected.getLayoutBounds().getMinY());

		event.consume();
	}

	@FXML
	public void regionDropped(MouseEvent event) {
		if (freeformButton.isSelected() && event.getButton().equals(MouseButton.PRIMARY)) {
			drawPolygon(event);
		} else if (freeformButton.isSelected() && event.getButton().equals(MouseButton.SECONDARY)) {
			drawShape();
			clearFreeformState(true);
		}

		if (!cursorRegion.isPresent() || cursorButton.isSelected()) return;

		final Node selected = cursorRegion.get();
		targetRegions.add(selected);
		selected.setOnMouseClicked((e) -> {
			regionClicked(e);
		});
		selected.setOnKeyPressed((e) -> {
			regionKeyPressed(e);
		});

		if (((TargetRegion) selected).getType() == RegionType.IMAGE) {
			final ImageRegion droppedImage = (ImageRegion) selected;

			// If the new image region has an animation, play it once
			if (droppedImage.getAnimation().isPresent()) {
				final SpriteAnimation animation = droppedImage.getAnimation().get();
				animation.setCycleCount(1);

				animation.setOnFinished((e) -> {
					animation.reset();
					animation.setOnFinished(null);
				});

				animation.play();
			}

			drawImage(droppedImage.getImageFile());
		} else {
			drawShape();
		}
	}

	@FXML
	public void canvasKeyPressed(KeyEvent event) {
		if (freeformButton.isSelected() && event.isControlDown() && event.getCode() == KeyCode.Z) {

			undoPolygonStep();
		}
	}

	private void undoPolygonStep() {
		if (freeformPoints.size() <= 0) return;

		// Remove last point, line, and vertex
		freeformPoints.remove(freeformPoints.size() - 1);
		freeformPoints.remove(freeformPoints.size() - 1);

		if (freeformPoints.size() == 0 && freeformEdge.isPresent()) {
			canvasPane.getChildren().remove(freeformEdge.get());
			freeformEdge = Optional.empty();
		}

		// Edge if it exists, otherwise vertex
		if (freeformShapes.size() > 0) canvasPane.getChildren().remove(freeformShapes.pop());

		// Vertex if there was an edge
		if (freeformShapes.size() > 0) canvasPane.getChildren().remove(freeformShapes.pop());
	}

	private void drawTempPolygonEdge(MouseEvent event) {
		// Need at least one point
		if (freeformPoints.size() < 2) return;

		if (freeformEdge.isPresent()) canvasPane.getChildren().remove(freeformEdge.get());

		final double lastX = freeformPoints.get(freeformPoints.size() - 2);
		final double lastY = freeformPoints.get(freeformPoints.size() - 1);

		final Line tempEdge = new Line(lastX, lastY, event.getX(), event.getY());
		final double DASH_OFFSET = 5;

		tempEdge.getStrokeDashArray().addAll(DASH_OFFSET, DASH_OFFSET);
		freeformEdge = Optional.of(tempEdge);
		canvasPane.getChildren().add(tempEdge);
	}

	private void drawPolygon(MouseEvent event) {
		final int VERTEX_RADIUS = 3;

		final Circle vertexDot = new Circle(event.getX(), event.getY(), VERTEX_RADIUS);
		freeformShapes.add(vertexDot);
		canvasPane.getChildren().add(vertexDot);

		if (freeformPoints.size() > 0) {
			final double lastX = freeformPoints.get(freeformPoints.size() - 2);
			final double lastY = freeformPoints.get(freeformPoints.size() - 1);
			Line edge = new Line(lastX, lastY, event.getX(), event.getY());

			freeformShapes.push(edge);
			canvasPane.getChildren().add(edge);
		}

		freeformPoints.add(event.getX());
		freeformPoints.add(event.getY());
	}

	private void unhighlightRegion(Node region) {
		if (((TargetRegion) region).getType() != RegionType.IMAGE)
			((Shape) region).setStroke(TargetRegion.UNSELECTED_STROKE_COLOR);
	}

	public void regionClicked(MouseEvent event) {
		if (!cursorButton.isSelected()) {
			// We want to drop a new region
			regionDropped(event);
			return;
		}

		// Want to select the current region
		final Node selected = (Node) event.getTarget();
		boolean tagEditorOpen = false;

		if (tagEditor.isPresent()) {
			// Close tag editor
			tagsButton.setSelected(false);
			toggleTagEditor();
			tagEditorOpen = true;
		}
		
		if (cursorRegion.isPresent()) {
			final Node previous = cursorRegion.get();

			// Unhighlight the old selection
			if (!previous.equals(selected)) {
				unhighlightRegion(previous);
			}
		}

		if (((TargetRegion) selected).getType() != RegionType.IMAGE)
			((Shape) selected).setStroke(TargetRegion.SELECTED_STROKE_COLOR);
		selected.requestFocus();
		toggleShapeControls(true);
		cursorRegion = Optional.of(selected);
		if (((TargetRegion) selected).getType() != RegionType.IMAGE)
			regionColorChoiceBox.getSelectionModel().select(getColorName((Color) ((Shape) selected).getFill()));

		// Re-open editor
		if (tagEditorOpen) {
			tagsButton.setSelected(true);
			toggleTagEditor();
		}
	}

	public void regionKeyPressed(KeyEvent event) {
		final Node selected = (Node) event.getTarget();
		final TargetRegion region = (TargetRegion) selected;

		switch (event.getCode()) {
		case DELETE:
		case BACK_SPACE:
			targetRegions.remove(selected);
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

		default:
			break;
		}

		event.consume();
	}

	@FXML
	public void cursorSelected(ActionEvent event) {
		clearFreeformState(false);

		if (!cursorRegion.isPresent()) return;

		// Remove shape that was never actually placed
		final Node selected = cursorRegion.get();
		if (!targetRegions.contains(selected)) canvasPane.getChildren().remove(selected);

		cursorRegion = Optional.empty();
	}

	private File chooseImageFile() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Image");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"),
				new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png"));
		return fileChooser.showOpenDialog(canvasPane.getParent().getScene().getWindow());
	}
	
	@FXML
	public void openImage(ActionEvent event) {
		final File imageFile = chooseImageFile();

		lastMouseX = 0;
		lastMouseY = 0;

		drawImage(imageFile);
	}

	private void drawImage(File imageFile) {
		Optional<ImageRegion> imageRegion = Optional.empty();

		if (imageFile != null) {
			try {
				final int firstDot = imageFile.getName().indexOf('.') + 1;
				final String extension = imageFile.getName().substring(firstDot);

				switch (extension) {
				case "gif":
					final ImageRegion newGIFRegion = new ImageRegion(lastMouseX, lastMouseY, imageFile);
					final GifAnimation gif = new GifAnimation(newGIFRegion, imageFile);
					newGIFRegion.setImage(gif.getFirstFrame());
					if (gif.getFrameCount() > 0) newGIFRegion.setAnimation(gif);
					imageRegion = Optional.of(newGIFRegion);
					break;
				case "png":
					final ImageRegion newPNGRegion = new ImageRegion(lastMouseX, lastMouseY, imageFile);
					newPNGRegion.setImage(new Image(new FileInputStream(imageFile)));
					imageRegion = Optional.of(newPNGRegion);
					break;
				}
			} catch (IOException e) {
				logger.error("Error drawing target image region", e);
			}
		}

		if (imageRegion.isPresent()) {
			canvasPane.getChildren().add(imageRegion.get());
			cursorRegion = Optional.of(imageRegion.get());
		}
	}

	@FXML
	public void drawShape(ActionEvent event) {
		if (tagsButton.isSelected()) {
			tagsButton.setSelected(false);
			toggleTagEditor();
		}

		lastMouseX = 0;
		lastMouseY = 0;

		if (cursorRegion.isPresent() && !targetRegions.contains(cursorRegion.get())) {
			canvasPane.getChildren().remove(cursorRegion.get());
		} else if (cursorRegion.isPresent() && targetRegions.contains(cursorRegion.get())) {
			final TargetRegion selected = (TargetRegion) cursorRegion.get();

			if (selected.getType() != RegionType.IMAGE)
				((Shape) selected).setStroke(TargetRegion.UNSELECTED_STROKE_COLOR);

			toggleShapeControls(false);
			cursorRegion = Optional.empty();
		}

		clearFreeformState(false);
		drawShape();
	}

	private void drawShape() {
		Shape newShape = null;

		final int DEFAULT_DIM = 40;
		final double AQT_SCALE = 2.5;

		if (rectangleButton.isSelected()) {
			newShape = new RectangleRegion(lastMouseX, lastMouseY, DEFAULT_DIM, DEFAULT_DIM);
		} else if (ovalButton.isSelected()) {
			final int RADIUS = DEFAULT_DIM / 2;
			newShape = new EllipseRegion(lastMouseX + RADIUS, lastMouseY + RADIUS, RADIUS, RADIUS);
		} else if (triangleButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX, lastMouseY + (DEFAULT_DIM / 2), lastMouseX + DEFAULT_DIM,
					lastMouseY + (DEFAULT_DIM / 2), lastMouseX + (DEFAULT_DIM / 2), lastMouseY);
		} else if (appleseedThreeButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX + 15.083 * AQT_SCALE, lastMouseY + 13.12 * AQT_SCALE,
					lastMouseX + 15.083 * AQT_SCALE, lastMouseY + -0.147 * AQT_SCALE, lastMouseX + 14.277 * AQT_SCALE,
					lastMouseY + -2.508 * AQT_SCALE, lastMouseX + 13.149 * AQT_SCALE, lastMouseY + -4.115 * AQT_SCALE,
					lastMouseX + 11.841 * AQT_SCALE, lastMouseY + -5.257 * AQT_SCALE, lastMouseX + 10.557 * AQT_SCALE,
					lastMouseY + -6.064 * AQT_SCALE, lastMouseX + 8.689 * AQT_SCALE, lastMouseY + -6.811 * AQT_SCALE,
					lastMouseX + 7.539 * AQT_SCALE, lastMouseY + -8.439 * AQT_SCALE, lastMouseX + 7.076 * AQT_SCALE,
					lastMouseY + -9.978 * AQT_SCALE, lastMouseX + 6.104 * AQT_SCALE, lastMouseY + -11.577 * AQT_SCALE,
					lastMouseX + 4.82 * AQT_SCALE, lastMouseY + -12.829 * AQT_SCALE, lastMouseX + 3.43 * AQT_SCALE,
					lastMouseY + -13.788 * AQT_SCALE, lastMouseX + 1.757 * AQT_SCALE, lastMouseY + -14.386 * AQT_SCALE,
					lastMouseX + 0.083 * AQT_SCALE, lastMouseY + -14.55 * AQT_SCALE, lastMouseX + -1.59 * AQT_SCALE,
					lastMouseY + -14.386 * AQT_SCALE, lastMouseX + -3.263 * AQT_SCALE, lastMouseY + -13.788 * AQT_SCALE,
					lastMouseX + -4.653 * AQT_SCALE, lastMouseY + -12.829 * AQT_SCALE, lastMouseX + -5.938 * AQT_SCALE,
					lastMouseY + -11.577 * AQT_SCALE, lastMouseX + -6.909 * AQT_SCALE, lastMouseY + -9.978 * AQT_SCALE,
					lastMouseX + -7.372 * AQT_SCALE, lastMouseY + -8.439 * AQT_SCALE, lastMouseX + -8.522 * AQT_SCALE,
					lastMouseY + -6.811 * AQT_SCALE, lastMouseX + -10.39 * AQT_SCALE, lastMouseY + -6.064 * AQT_SCALE,
					lastMouseX + -11.674 * AQT_SCALE, lastMouseY + -5.257 * AQT_SCALE, lastMouseX + -12.982 * AQT_SCALE,
					lastMouseY + -4.115 * AQT_SCALE, lastMouseX + -14.11 * AQT_SCALE, lastMouseY + -2.508 * AQT_SCALE,
					lastMouseX + -14.917 * AQT_SCALE, lastMouseY + -0.147 * AQT_SCALE, lastMouseX + -14.917 * AQT_SCALE,
					lastMouseY + 13.12 * AQT_SCALE);
		} else if (appleseedFourButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX + 11.66 * AQT_SCALE, lastMouseY + 5.51 * AQT_SCALE,
					lastMouseX + 11.595 * AQT_SCALE, lastMouseY + 0.689 * AQT_SCALE, lastMouseX + 11.1 * AQT_SCALE,
					lastMouseY + -1.084 * AQT_SCALE, lastMouseX + 9.832 * AQT_SCALE, lastMouseY + -2.441 * AQT_SCALE,
					lastMouseX + 7.677 * AQT_SCALE, lastMouseY + -3.322 * AQT_SCALE, lastMouseX + 5.821 * AQT_SCALE,
					lastMouseY + -4.709 * AQT_SCALE, lastMouseX + 4.715 * AQT_SCALE, lastMouseY + -6.497 * AQT_SCALE,
					lastMouseX + 4.267 * AQT_SCALE, lastMouseY + -8.135 * AQT_SCALE, lastMouseX + 3.669 * AQT_SCALE,
					lastMouseY + -9.41 * AQT_SCALE, lastMouseX + 2.534 * AQT_SCALE, lastMouseY + -10.553 * AQT_SCALE,
					lastMouseX + 1.436 * AQT_SCALE, lastMouseY + -11.091 * AQT_SCALE, lastMouseX + 0.083 * AQT_SCALE,
					lastMouseY + -11.323 * AQT_SCALE, lastMouseX + -1.269 * AQT_SCALE, lastMouseY + -11.091 * AQT_SCALE,
					lastMouseX + -2.367 * AQT_SCALE, lastMouseY + -10.553 * AQT_SCALE, lastMouseX + -3.502 * AQT_SCALE,
					lastMouseY + -9.41 * AQT_SCALE, lastMouseX + -4.1 * AQT_SCALE, lastMouseY + -8.135 * AQT_SCALE,
					lastMouseX + -4.548 * AQT_SCALE, lastMouseY + -6.497 * AQT_SCALE, lastMouseX + -5.654 * AQT_SCALE,
					lastMouseY + -4.709 * AQT_SCALE, lastMouseX + -7.51 * AQT_SCALE, lastMouseY + -3.322 * AQT_SCALE,
					lastMouseX + -9.665 * AQT_SCALE, lastMouseY + -2.441 * AQT_SCALE, lastMouseX + -10.933 * AQT_SCALE,
					lastMouseY + -1.084 * AQT_SCALE, lastMouseX + -11.428 * AQT_SCALE, lastMouseY + 0.689 * AQT_SCALE,
					lastMouseX + -11.493 * AQT_SCALE, lastMouseY + 5.51 * AQT_SCALE);
		} else if (appleseedFiveButton.isSelected()) {
			newShape = new PolygonRegion(lastMouseX + 7.893 * AQT_SCALE, lastMouseY + 3.418 * AQT_SCALE,
					lastMouseX + 7.893 * AQT_SCALE, lastMouseY + 1.147 * AQT_SCALE, lastMouseX + 7.255 * AQT_SCALE,
					lastMouseY + 0.331 * AQT_SCALE, lastMouseX + 5.622 * AQT_SCALE, lastMouseY + -0.247 * AQT_SCALE,
					lastMouseX + 4.187 * AQT_SCALE, lastMouseY + -1.124 * AQT_SCALE, lastMouseX + 2.833 * AQT_SCALE,
					lastMouseY + -2.339 * AQT_SCALE, lastMouseX + 1.917 * AQT_SCALE, lastMouseY + -3.594 * AQT_SCALE,
					lastMouseX + 1.219 * AQT_SCALE, lastMouseY + -5.048 * AQT_SCALE, lastMouseX + 0.9 * AQT_SCALE,
					lastMouseY + -6.223 * AQT_SCALE, lastMouseX + 0.801 * AQT_SCALE, lastMouseY + -7.1 * AQT_SCALE,
					lastMouseX + 0.521 * AQT_SCALE, lastMouseY + -7.558 * AQT_SCALE, lastMouseX + 0.083 * AQT_SCALE,
					lastMouseY + -7.617 * AQT_SCALE, lastMouseX + -0.354 * AQT_SCALE, lastMouseY + -7.558 * AQT_SCALE,
					lastMouseX + -0.634 * AQT_SCALE, lastMouseY + -7.1 * AQT_SCALE, lastMouseX + -0.733 * AQT_SCALE,
					lastMouseY + -6.223 * AQT_SCALE, lastMouseX + -1.052 * AQT_SCALE, lastMouseY + -5.048 * AQT_SCALE,
					lastMouseX + -1.75 * AQT_SCALE, lastMouseY + -3.594 * AQT_SCALE, lastMouseX + -2.666 * AQT_SCALE,
					lastMouseY + -2.339 * AQT_SCALE, lastMouseX + -4.02 * AQT_SCALE, lastMouseY + -1.124 * AQT_SCALE,
					lastMouseX + -5.455 * AQT_SCALE, lastMouseY + -0.247 * AQT_SCALE, lastMouseX + -7.088 * AQT_SCALE,
					lastMouseY + 0.331 * AQT_SCALE, lastMouseX + -7.726 * AQT_SCALE, lastMouseY + 1.147 * AQT_SCALE,
					lastMouseX + -7.726 * AQT_SCALE, lastMouseY + 3.418 * AQT_SCALE);
		} else if (freeformButton.isSelected()) {
			double[] points = new double[freeformPoints.size()];

			for (int i = 0; i < freeformPoints.size(); i++) {
				points[i] = freeformPoints.get(i);
			}

			newShape = new PolygonRegion(points);

			targetRegions.add(newShape);
			newShape.setOnMouseClicked((e) -> {
				regionClicked(e);
			});
			newShape.setOnKeyPressed((e) -> {
				regionKeyPressed(e);
			});
		} else {
			cursorRegion = Optional.empty();
			logger.error("Unimplemented region type selected.");
			return;
		}

		newShape.setFill(DEFAULT_FILL_COLOR);
		newShape.setOpacity(TargetIO.DEFAULT_OPACITY);
		canvasPane.getChildren().add(newShape);

		// New freeform polygon should not be on the cursor or
		// adjusted (it can't be off the canvas)
		if (!freeformButton.isSelected()) {
			final double leftX = lastMouseX - (newShape.getLayoutBounds().getWidth() / 2);
			if (leftX < 0) newShape.setLayoutX(newShape.getLayoutX() + (leftX * -1));

			final double topY = lastMouseY - (newShape.getLayoutBounds().getHeight() / 2);
			if (topY < 0) newShape.setLayoutY(newShape.getLayoutY() + (topY * -1));

			cursorRegion = Optional.of(newShape);
		}
	}

	@FXML
	public void startPolygon(ActionEvent event) {
		if (cursorRegion.isPresent() && targetRegions.contains(cursorRegion.get())) {
			final TargetRegion selected = (TargetRegion) cursorRegion.get();

			if (selected.getType() != RegionType.IMAGE)
				((Shape) selected).setStroke(TargetRegion.UNSELECTED_STROKE_COLOR);

			if (tagsButton.isSelected()) {
				tagsButton.setSelected(false);
				toggleTagEditor();
			}
			toggleShapeControls(false);
		}

		clearFreeformState(false);
	}

	private void clearFreeformState(final boolean finishedDrawing) {
		if (cursorRegion.isPresent() && !finishedDrawing) {
			final Node selected = cursorRegion.get();
			if (!targetRegions.contains(selected)) canvasPane.getChildren().remove(selected);

			cursorRegion = Optional.empty();
		}

		freeformPoints.clear();
		canvasPane.getChildren().removeAll(freeformShapes);
		freeformShapes.clear();

		if (freeformEdge.isPresent()) {
			canvasPane.getChildren().remove(freeformEdge.get());
			freeformEdge = Optional.empty();
		}
	}

	@FXML
	public void bringForward(ActionEvent event) {
		if (cursorRegion.isPresent() && !targetRegions.contains(cursorRegion.get())) return;

		int selectedIndex = targetRegions.indexOf(cursorRegion.get());

		if (selectedIndex < targetRegions.size() - 1) {
			Collections.swap(targetRegions, selectedIndex, selectedIndex + 1);

			// Get the index separately for the shapes list because the target
			// region
			// list has fewer items (e.g. no background)
			final ObservableList<Node> shapesList = canvasPane.getChildren();
			selectedIndex = shapesList.indexOf(cursorRegion.get());

			// We have to do this dance instead of just calling
			// Collections.swap otherwise we get an IllegalArgumentException
			// from the Scene for duplicating a child node
			final Node topShape = shapesList.get(selectedIndex + 1);
			final Node bottomShape = shapesList.get(selectedIndex);
			shapesList.remove(selectedIndex + 1);
			shapesList.remove(selectedIndex);
			shapesList.add(selectedIndex, topShape);
			shapesList.add(selectedIndex + 1, bottomShape);
		}
	}

	@FXML
	public void sendBackward(ActionEvent event) {
		if (cursorRegion.isPresent() && !targetRegions.contains(cursorRegion.get())) return;

		int selectedIndex = targetRegions.indexOf(cursorRegion.get());

		if (selectedIndex > 0) {
			Collections.swap(targetRegions, selectedIndex - 1, selectedIndex);

			final ObservableList<Node> shapesList = canvasPane.getChildren();
			selectedIndex = shapesList.indexOf(cursorRegion.get());

			final Node topShape = shapesList.get(selectedIndex);
			final Node bottomShape = shapesList.get(selectedIndex - 1);
			shapesList.remove(selectedIndex);
			shapesList.remove(selectedIndex - 1);
			shapesList.add(selectedIndex - 1, topShape);
			shapesList.add(selectedIndex, bottomShape);
		}
	}

	@FXML
	public void toggleTagEditor(ActionEvent event) {
		if (cursorRegion.isPresent() && !targetRegions.contains(cursorRegion.get())) return;

		toggleTagEditor();
	}

	private void toggleTagEditor() {
		if (!tagsButton.isSelected() && tagEditor.isPresent()) {
			// Close the tag editor
			TagEditorPane editor = tagEditor.get();
			canvasPane.getChildren().remove(editor);
			tagEditor = Optional.empty();

			if (cursorRegion.isPresent()) {
				((TargetRegion) cursorRegion.get()).setTags(editor.getTags());
			} else {
				targetTags.clear();
				targetTags.putAll(editor.getTags());
			}

			return;	
		}
		

		final TagEditorPane editor;
		
		if (tagsButton.isSelected()) {
			if (cursorRegion.isPresent()) {
				// Edit tags for a selected target region
				editor = new TagEditorPane(((TargetRegion) cursorRegion.get()).getAllTags());
			} else {
				// Edit target-level tags
				editor = new TagEditorPane(targetTags);
			}
		} else {
			throw new AssertionError("It should not be possible to toggle the tags button without either the "
					+ "background or a target region selected.");
		}

		// Show the tag editor
		tagEditor = Optional.of(editor);
		canvasPane.getChildren().add(editor);
		
		editor.setLayoutX(512);
	}
}
