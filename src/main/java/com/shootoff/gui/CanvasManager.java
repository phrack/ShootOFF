/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.config.Configuration;
import com.shootoff.plugins.TrainingProtocol;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class CanvasManager {
	private static final int MOVEMENT_DELTA = 1;
	private static final int SCALE_DELTA = 1;
	
	private final Group canvasGroup;
	private final Configuration config;
	private final ImageView background = new ImageView();
	private final List<Shot> shots = new ArrayList<Shot>();
	private final List<Group> targets = new ArrayList<Group>();
	
	private Optional<Group> selectedTarget = Optional.empty();
	
	public CanvasManager(Group canvasGroup, Configuration config) {
		this.canvasGroup = canvasGroup;
		this.config = config;
	
		this.background.setOnMouseClicked((event) -> {
				toggleTargetSelection(Optional.empty());
				selectedTarget = Optional.empty();
				canvasGroup.requestFocus();
			});
		
		canvasGroup.setOnKeyPressed((event) -> {
				if (!selectedTarget.isPresent()) return;
				
				transformTarget(event, selectedTarget.get());
				event.consume();
			});

		if (Platform.isFxApplicationThread()) {
			ProgressIndicator progress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
			progress.setPrefHeight(480);
			progress.setPrefWidth(640);
			canvasGroup.getChildren().add(progress);
		}
		
		// Click to shoot
		if (config.inDebugMode()) {
			canvasGroup.setOnMouseClicked((event) -> {
					if (event.getButton() == MouseButton.PRIMARY) {
						if (event.isShiftDown()) {
							addShot(new Shot(Color.RED, event.getX(),
									event.getY(), 0, config.getMarkerRadius()));
						} else if (event.isControlDown()) {
							addShot(new Shot(Color.GREEN, event.getX(),
									event.getY(), 0, config.getMarkerRadius()));
						}
					}
				});
		}
	}	
	
	public void updateBackground(Image img) {
		if (!canvasGroup.getChildren().contains(background)) {
			Platform.runLater(() -> {
					canvasGroup.getChildren().clear();
					canvasGroup.getChildren().add(background);
				});
		}
		
		background.setImage(img);
	}
	
	public void reset() {
		Platform.runLater(() -> {
				for (Shot shot : shots) {
					canvasGroup.getChildren().remove(shot.getMarker());
				}
				
				shots.clear();
			}); 
	}
	
	public void addShot(Shot shot) {
		for (ShotProcessor processor : config.getShotProcessors()) {
			if (!processor.processShot(shot)) return;
		}
		
		shots.add(shot);
		shot.drawShot(canvasGroup);
		
		Optional<TrainingProtocol> currentProtocol = config.getProtocol();
		if (currentProtocol.isPresent()) currentProtocol.get().shotListener(shot, checkHit(shot));
	}
	
	private Optional<TargetRegion> checkHit(Shot shot) {
		for (Group target : targets) {
			if (target.getBoundsInParent().contains(shot.getX(), shot.getY())) {
				// Target was hit, see if a specific region was hit
				for (int i = target.getChildren().size() - 1; i >= 0; i--) {
					Node node = target.getChildren().get(i);
					if (node.getBoundsInParent().contains(shot.getX(), shot.getY())) {
						return Optional.of((TargetRegion)node);
					}
				}
			}
		}
		
		return Optional.empty();
	}
	
	public void addTarget(File targetFile) {
		Optional<Group> target = TargetIO.loadTarget(targetFile);
		
		if (target.isPresent()) {			
			target.get().setOnMouseClicked((event) -> {
					toggleTargetSelection(target);
					selectedTarget = target;
					canvasGroup.requestFocus();
				});
			
			canvasGroup.getChildren().add(target.get());
			targets.add(target.get());
		}
	}
	
	public List<Group> getTargets() {
		return targets;
	}
	
	@SuppressWarnings("incomplete-switch")
	private void transformTarget(KeyEvent event, Group selected) {
		switch (event.getCode()) {
		case DELETE:
			canvasGroup.getChildren().remove(selectedTarget.get());
			break;
			
		case LEFT:
			if (event.isShiftDown()) {
				for (Node node : selected.getChildren()) {
					TargetRegion region = (TargetRegion)node;
					region.changeWidth(SCALE_DELTA * -1);
				}
			} else {
				selected.setLayoutX(selected.getLayoutX() - MOVEMENT_DELTA);
			}
			break;
			
		case RIGHT:
			if (event.isShiftDown()) {
				for (Node node : selected.getChildren()) {
					TargetRegion region = (TargetRegion)node;
					region.changeWidth(SCALE_DELTA);
				}
			} else {
				selected.setLayoutX(selected.getLayoutX() + MOVEMENT_DELTA);
			}
			break;
			
		case UP:
			if (event.isShiftDown()) {
				for (Node node : selected.getChildren()) {
					TargetRegion region = (TargetRegion)node;
					region.changeHeight(SCALE_DELTA * -1);
				}
			} else {
				selected.setLayoutY(selected.getLayoutY() - MOVEMENT_DELTA);
			}
			break;

		case DOWN:
			if (event.isShiftDown()) {
				for (Node node : selected.getChildren()) {
					TargetRegion region = (TargetRegion)node;
					region.changeHeight(SCALE_DELTA);
				}
			} else {
				selected.setLayoutY(selected.getLayoutY() + MOVEMENT_DELTA);
			}
			break;
		}
	}
	
	private void toggleTargetSelection(Optional<Group> newSelection) {
		if (selectedTarget.isPresent())
			setTargetSelection(selectedTarget.get(), false);
		
		if (newSelection.isPresent()) {
			setTargetSelection(newSelection.get(), true);
			selectedTarget = newSelection;
		}
	}
	
	private void setTargetSelection(Group target, boolean isSelected) {
		Color stroke;
		
		if (isSelected) {
			stroke = TargetRegion.SELECTED_STROKE_COLOR;
		} else {
			stroke = TargetRegion.UNSELECTED_STROKE_COLOR;
		}
		
		for (Node node : target.getChildren()) {
			TargetRegion region = (TargetRegion)node;
			if (region.getType() != RegionType.IMAGE) {
				((Shape)region).setStroke(stroke);
			}
		}
	}
}
