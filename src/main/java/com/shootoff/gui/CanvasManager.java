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

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.config.Configuration;
import com.shootoff.plugins.TrainingProtocol;
import com.shootoff.plugins.TrainingProtocolBase;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.animation.SpriteAnimation;
import com.shootoff.targets.io.TargetIO;

import javafx.animation.Animation.Status;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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
	private final CamerasSupervisor camerasSupervisor;
	private final ObservableList<ShotEntry> shotEntries;
	private final ImageView background = new ImageView();
	private final List<Shot> shots = new ArrayList<Shot>();
	private final List<Group> targets = new ArrayList<Group>();
	
	private Optional<Group> selectedTarget = Optional.empty();
	private long startTime = 0;
	
	public CanvasManager(Group canvasGroup, Configuration config, CamerasSupervisor camerasSupervisor,
			ObservableList<ShotEntry> shotEntries) {
		this.canvasGroup = canvasGroup;
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.shotEntries = shotEntries;
	
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
							addShot(Color.RED, event.getX(), event.getY());
						} else if (event.isControlDown()) {
							addShot(Color.GREEN, event.getX(), event.getY());
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
	
	public Group getCanvasGroup() {
		return canvasGroup;
	}
	
	public void reset() {
		startTime = System.currentTimeMillis();
		
		// Reset animations
		for (Group target : targets) {
			for (Node node : target.getChildren()) {
				TargetRegion region = (TargetRegion)node;
				
				if (region.getType() == RegionType.IMAGE) ((ImageRegion)region).reset();
			}
		}
		
		Platform.runLater(() -> {
				for (Shot shot : shots) {
					canvasGroup.getChildren().remove(shot.getMarker());
				}
				
				shots.clear();
				shotEntries.clear();
			}); 
	}
	
	public void addShot(Color color, double x, double y) {
		if (startTime == 0) startTime = System.currentTimeMillis();
		Shot shot = new Shot(color, x, y, 
				System.currentTimeMillis() - startTime, config.getMarkerRadius());
		
		for (ShotProcessor processor : config.getShotProcessors()) {
			if (!processor.processShot(shot)) return;
		}
		
		shotEntries.add(new ShotEntry(shot));
		shots.add(shot);
		shot.drawShot(canvasGroup);
		
		Optional<TrainingProtocol> currentProtocol = config.getProtocol();
		Optional<TargetRegion> hitRegion = checkHit(shot);
		if (hitRegion.isPresent() && hitRegion.get().tagExists("command")) executeRegionCommands(hitRegion.get());
		if (currentProtocol.isPresent()) currentProtocol.get().shotListener(shot, hitRegion);
	}
	
	private Optional<TargetRegion> checkHit(Shot shot) {
		for (Group target : targets) {
			if (target.getBoundsInParent().contains(shot.getX(), shot.getY())) {
				// Target was hit, see if a specific region was hit
				for (int i = target.getChildren().size() - 1; i >= 0; i--) {
					Node node = target.getChildren().get(i);
					if (node.getBoundsInParent().contains(shot.getX(), shot.getY())) {
						// If we hit an image region on a transparent pixel, ignore it
						TargetRegion region = (TargetRegion)node;
						if (region.getType() == RegionType.IMAGE) {
							Image currentImage = ((ImageRegion)region).getImage();
							int adjustedX = (int)(shot.getX() - node.getBoundsInParent().getMinX());
							int adjustedY = (int)(shot.getY() - node.getBoundsInParent().getMinY());
							
							if (currentImage.getPixelReader().getArgb(adjustedX, adjustedY) >> 24 == 0) {
								continue;
							}
						}
						
						return Optional.of((TargetRegion)node);
					}
				}
			}
		}
		
		return Optional.empty();
	}
	
	private void executeRegionCommands(TargetRegion region) {
		String commandsSource = region.getTag("command");
		String commands[]  = commandsSource.split(";");		
		
		for (String command : commands) {
			int openParen = command.indexOf('(');
			String commandName;
			String args[];
			
			if (openParen > 0) {
				commandName = command.substring(0, openParen);
				args = command.substring(openParen + 1, command.indexOf(')')).split(",");
			} else {
				commandName = command;
				args = null;
			}
			
			switch (commandName) {
			case "reset":
				camerasSupervisor.reset();
				break;
				
			case "animate":
				animate(region, args);
				break;
				
			case "reverse":
				reverseAnimation(region);
				break;
				
			case "play_sound":
				// If there is a second parameter, we should look to see if it's an
				// image region that is down and if so, don't play the sound
				if (args.length == 2) {
					Optional<TargetRegion> namedRegion = getTargetRegionByName(region, args[1]);
					if (namedRegion.isPresent() && namedRegion.get().getType() == RegionType.IMAGE) {
						if (!((ImageRegion)namedRegion.get()).onFirstFrame()) break;
					}
				}
				
				TrainingProtocolBase.playSound(args[0]);
				break;
			}
		}
	}
	
	private void animate(TargetRegion region, String args[]) {
		ImageRegion imageRegion;
		
		if (args == null) {
			imageRegion = (ImageRegion)region;
		} else {
			Optional<TargetRegion> r = getTargetRegionByName(region, args[0]);
			
			if (r.isPresent()) {
				imageRegion = (ImageRegion)r.get();
			} else {
				System.err.format("Request to animate region named %s, but it "
						+ "doesn't exist.", args[0]);
				return;
			}
		}
		
		// Don't repeat animations for fallen targets
		if (!imageRegion.onFirstFrame()) return;
		
		if (imageRegion.getAnimation().isPresent()) {
			imageRegion.getAnimation().get().play();
		} else {
			System.err.println("Request to animate region, but region does "
					+ "not contain an animation.");
		}
	}
	
	private void reverseAnimation(TargetRegion region) {
		if (region.getType() != RegionType.IMAGE) {
			System.err.println("A reversal was requested on a non-image region.");
			return;
		}
		
		ImageRegion imageRegion = (ImageRegion)region;
		if (imageRegion.getAnimation().isPresent()) {
			SpriteAnimation animation = imageRegion.getAnimation().get();

			if (animation.getStatus() == Status.RUNNING) {
				animation.setOnFinished((e) -> {
						animation.reverse();
						animation.setOnFinished(null);
					});
			} else {
				animation.reverse();
			}
		} else {
			System.err.println("A reversal was requested on an image region that isn't animated.");
		}
	}
	
	private Optional<TargetRegion> getTargetRegionByName(TargetRegion region, String name) {
		for (Group target : targets) {
			if (target.getChildren().contains(region)) {
				for (Node node : target.getChildren()) {
					TargetRegion r = (TargetRegion)node;
					if (r.tagExists("name") && r.getTag("name").equals(name)) return Optional.of(r);
				}
			}
		}
		
		return Optional.empty();
	}
	
	public void addTarget(File targetFile) {
		Optional<Group> target = TargetIO.loadTarget(targetFile);
		
		if (target.isPresent()) {		
			// Make sure visible:false regions are hidden
			for (Node node : target.get().getChildren()) {
				TargetRegion region = (TargetRegion)node;

				if (region.tagExists("visible") && 
						region.getTag("visible").equals("false")) {
					
					node.setVisible(false);
				}
			}
			
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
			targets.remove(selectedTarget.get());
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
