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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.controllers.ProjectorArenaController;
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
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class CanvasManager {
	private final Logger logger = LoggerFactory.getLogger(CanvasManager.class);
	private final Group canvasGroup;
	private final Configuration config;
	private final CamerasSupervisor camerasSupervisor;
	private final ObservableList<ShotEntry> shotEntries;
	private final ImageView background = new ImageView();
	private final List<Shot> shots;
	private final List<Group> targets = new ArrayList<Group>();
	
	private ProgressIndicator progress;
	private Optional<ContextMenu> contextMenu;
	private Optional<Group> selectedTarget = Optional.empty();
	private long startTime = 0;
	private boolean showShots = true;
	
	private Optional<ProjectorArenaController> arenaController = Optional.empty();
	private Optional<Bounds> projectionBounds = Optional.empty();
	
	public CanvasManager(Group canvasGroup, Configuration config, CamerasSupervisor camerasSupervisor, 
			ObservableList<ShotEntry> shotEntries) {
		this.canvasGroup = canvasGroup;
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.shotEntries = shotEntries;
		shots = Collections.synchronizedList(new ArrayList<Shot>());
	
		this.background.setOnMouseClicked((event) -> {
				toggleTargetSelection(Optional.empty());
				selectedTarget = Optional.empty();
				canvasGroup.requestFocus();
			});

		if (Platform.isFxApplicationThread()) {
			progress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
			progress.setPrefHeight(480);
			progress.setPrefWidth(640);
			canvasGroup.getChildren().add(progress);
		}
		
		canvasGroup.setOnMouseClicked((event) -> {
			if (config.inDebugMode() && event.getButton() == MouseButton.PRIMARY) {
				// Click to shoot
				if (event.isShiftDown()) {
					addShot(Color.RED, event.getX(), event.getY());
				} else if (event.isControlDown()) {
					addShot(Color.GREEN, event.getX(), event.getY());
				}
			} else if (contextMenu.isPresent() && event.getButton() == MouseButton.SECONDARY) {
				contextMenu.get().show(canvasGroup, event.getScreenX(), event.getScreenY());
			}
		});
	}	
	
	public void setContextMenu(ContextMenu menu) {
		this.contextMenu = Optional.of(menu);
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
	
	public void clearShots() {
		Platform.runLater(() -> {
			for (Shot shot : shots) {
				canvasGroup.getChildren().remove(shot.getMarker());
			}
			
			shots.clear();
			if (shotEntries != null) shotEntries.clear();
			if (arenaController.isPresent()) arenaController.get().getCanvasManager().clearShots();
		}); 
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
		
		if (arenaController.isPresent()) {
			arenaController.get().getCanvasManager().reset();
		}
		
		clearShots();
	}
	
	public void setProjectorArena(ProjectorArenaController arenaController, Bounds projectionBounds) {		
		this.arenaController = Optional.ofNullable(arenaController);
		this.projectionBounds = Optional.ofNullable(projectionBounds);
	}
	
	public void setShowShots(boolean showShots) {
		if (this.showShots != showShots) {
			for (Shot shot : shots) shot.getMarker().setVisible(showShots);
		}
		
		this.showShots = showShots;
	}
	
	public void addShot(Color color, double x, double y) {
		if (startTime == 0) startTime = System.currentTimeMillis();
		Shot shot = new Shot(color, x, y, 
				System.currentTimeMillis() - startTime, config.getMarkerRadius());
		
		for (ShotProcessor processor : config.getShotProcessors()) {
			if (!processor.processShot(shot)) {
				logger.debug("Processing Shot: Shot Rejected By {}", processor.getClass().getName());
				return;
			}
		}
		
		shotEntries.add(new ShotEntry(shot));
		shots.add(shot);
		drawShot(shot);
		
		Optional<TrainingProtocol> currentProtocol = config.getProtocol();
		Optional<TargetRegion> hitRegion = checkHit(shot);
		if (hitRegion.isPresent() && hitRegion.get().tagExists("command")) executeRegionCommands(hitRegion.get());
		
		boolean processedShot = false;
		
		if (arenaController.isPresent() && projectionBounds.isPresent()) {
			Bounds b = projectionBounds.get();
			
			if (b.contains(shot.getX(), shot.getY())) {
				double x_scale = arenaController.get().getWidth() / b.getWidth();
				double y_scale = arenaController.get().getHeight() / b.getHeight();
				
				Shot arenaShot = new Shot(shot.getColor(), 
						(shot.getX() - b.getMinX()) * x_scale, (shot.getY() - b.getMinY()) * y_scale,
						shot.getTimestamp(), config.getMarkerRadius());
				
				processedShot = arenaController.get().getCanvasManager().addArenaShot(arenaShot);
			}
		}
		
		if (currentProtocol.isPresent() && !processedShot) currentProtocol.get().shotListener(shot, hitRegion);
	}
	
	public boolean addArenaShot(Shot shot) {
		shots.add(shot);
		drawShot(shot);
		
		Optional<TrainingProtocol> currentProtocol = config.getProtocol();
		Optional<TargetRegion> hitRegion = checkHit(shot);
		if (hitRegion.isPresent() && hitRegion.get().tagExists("command")) executeRegionCommands(hitRegion.get());
		if (currentProtocol.isPresent()) {
			currentProtocol.get().shotListener(shot, hitRegion);
			return true;
		}
		
		return false;
	}
	
	private void drawShot(Shot shot) {
		Platform.runLater(() -> {
				canvasGroup.getChildren().add(shot.getMarker());
				shot.getMarker().setVisible(showShots);
			});
	}
	
	private Optional<TargetRegion> checkHit(Shot shot) {
		for (Group target : targets) {
			if (target.getBoundsInParent().contains(shot.getX(), shot.getY())) {				
				// Target was hit, see if a specific region was hit
				for (int i = target.getChildren().size() - 1; i >= 0; i--) {
					Node node = target.getChildren().get(i);
					if (node.contains(node.parentToLocal(shot.getX(), shot.getY()))) {
						// If we hit an image region on a transparent pixel, ignore it
						TargetRegion region = (TargetRegion)node;
						if (region.getType() == RegionType.IMAGE) {
							Image currentImage = ((ImageRegion)region).getImage();
							int adjustedX = (int)(shot.getX() - node.getBoundsInParent().getMinX());
							int adjustedY = (int)(shot.getY() - node.getBoundsInParent().getMinY());
							
							if (currentImage.getHeight() > adjustedY && 
								currentImage.getWidth() > adjustedX && 
									currentImage.getPixelReader().getArgb(adjustedX, adjustedY) >> 24 == 0) {
								continue;
							}
						}
						
						if (config.inDebugMode()) {
							Map<String, String> tags = region.getAllTags();
							
							StringBuilder tagList = new StringBuilder();
							for (Iterator<String> it = tags.keySet().iterator(); it.hasNext();) {
								String tagName = it.next();
								tagList.append(tagName);
								tagList.append(":");
								tagList.append(tags.get(tagName));
								if (it.hasNext()) tagList.append(", ");
							}
						
							logger.debug("Processing Shot: Found Hit Region For Shot ({}, {}), Type ({}), Tags ({})", 
									shot.getX(), shot.getY(), region.getType(), tagList.toString());	
						}
						
						return Optional.of((TargetRegion)node);
					}
				}
			}
		}
		
		logger.debug("Processing Shot: Did Not Find Hit For Shot ({}, {})", 
				shot.getX(), shot.getY());
		
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
	
	public Optional<Group> addTarget(File targetFile) {
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
					target.get().requestFocus();
				});
			
			addTarget(target.get(), true);
		}
		return target;
	}
	
	public void addTarget(Group target, boolean userDeletable) {
		Platform.runLater(() -> { canvasGroup.getChildren().add(target); });
		new TargetContainer(target, config, this, userDeletable);
		targets.add(target);
	}
	
	public void removeTarget(Group target) {
		Platform.runLater(() -> { canvasGroup.getChildren().remove(target); });
		targets.remove(target);
	}
	
	public List<Group> getTargets() {
		return targets;
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