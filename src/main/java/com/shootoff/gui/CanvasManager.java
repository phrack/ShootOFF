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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.DeduplicationProcessor;
import com.shootoff.camera.MalfunctionsProcessor;
import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.camera.ShotRecorder;
import com.shootoff.camera.VirtualMagazineProcessor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class CanvasManager {
	private final Logger logger = LoggerFactory.getLogger(CanvasManager.class);
	private final Group canvasGroup;
	private final Configuration config;
	protected CameraManager cameraManager;
	
	private final VBox diagnosticsVBox = new VBox();
	private static final int DIAGNOSTIC_POOL_SIZE = 10;
	private static final int DIAGNOSTIC_CHIME_DELAY = 5000; // ms
	private final ScheduledExecutorService diagnosticExecutorService = Executors.newScheduledThreadPool(DIAGNOSTIC_POOL_SIZE);
	private final Map<Label, ScheduledFuture<Void>> diagnosticFutures = new HashMap<Label, ScheduledFuture<Void>>();
	
	protected final CamerasSupervisor camerasSupervisor;
	private final String cameraName;
	private final ObservableList<ShotEntry> shotEntries;
	private final ImageView background = new ImageView();
	private final List<Shot> shots;
	private final List<Target> targets = new ArrayList<Target>();
	
	private ProgressIndicator progress;
	private Optional<ContextMenu> contextMenu = Optional.empty();
	private Optional<Group> selectedTarget = Optional.empty();
	private long startTime = 0;
	private boolean showShots = true;
	private boolean hadMalfunction = false;
	private boolean hadReload = false;
	
	private Optional<ProjectorArenaController> arenaController = Optional.empty();
	private Optional<Bounds> projectionBounds = Optional.empty();
	
	public CanvasManager(Group canvasGroup, Configuration config, CamerasSupervisor camerasSupervisor, 
			String cameraName, ObservableList<ShotEntry> shotEntries) {
		this.canvasGroup = canvasGroup;
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.cameraName = cameraName;
		this.shotEntries = shotEntries;
		shots = Collections.synchronizedList(new ArrayList<Shot>());
	
		this.background.setOnMouseClicked((event) -> {
				toggleTargetSelection(Optional.empty());
				selectedTarget = Optional.empty();
				canvasGroup.requestFocus();
			});

		if (Platform.isFxApplicationThread()) {
			progress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
			progress.setPrefHeight(CameraManager.FEED_HEIGHT);
			progress.setPrefWidth(CameraManager.FEED_WIDTH);
			canvasGroup.getChildren().add(progress);
			canvasGroup.getChildren().add(diagnosticsVBox);
			diagnosticsVBox.setAlignment(Pos.CENTER);
			diagnosticsVBox.setFillWidth(true);
			diagnosticsVBox.setPrefWidth(CameraManager.FEED_WIDTH);
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

	public void close() {
		diagnosticExecutorService.shutdownNow();
	}
	
	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}
	
	public CameraManager getCameraManager() {
		return cameraManager;
	}
	
	public Label addDiagnosticMessage(String message, long chimeDelay, Color backgroundColor) {
		Label diagnosticLabel = new Label(message);
		diagnosticLabel.setStyle("-fx-background-color: " + colorToWebCode(backgroundColor));
		diagnosticsVBox.getChildren().add(diagnosticLabel);
		
		@SuppressWarnings("unchecked")
		ScheduledFuture<Void> chimeFuture = (ScheduledFuture<Void>)diagnosticExecutorService.schedule(
				() -> TrainingExerciseBase.playSound("sounds/chime.wav"), chimeDelay, TimeUnit.MILLISECONDS);
		diagnosticFutures.put(diagnosticLabel, chimeFuture);
		
		return diagnosticLabel;
	}
	
	public Label addDiagnosticMessage(String message, Color backgroundColor) {
		return addDiagnosticMessage(message, DIAGNOSTIC_CHIME_DELAY, backgroundColor);
	}
	
	public void removeDiagnosticMessage(Label diagnosticLabel) {
		diagnosticFutures.get(diagnosticLabel).cancel(false);
		diagnosticFutures.remove(diagnosticLabel);
		diagnosticsVBox.getChildren().remove(diagnosticLabel);
	}
	
    public static String colorToWebCode(Color color)
    {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

	private void jdk8094135Warning() {
			Platform.runLater(() -> {
				Alert cameraAlert = new Alert(AlertType.ERROR);
				cameraAlert.setTitle("Internal Error");
				cameraAlert.setHeaderText("Internal Error -- Likely Too Many false Shots");
				cameraAlert.setResizable(true);
				cameraAlert.setContentText("An internal error due to JDK bug 8094135 occured in Java that will cause all "
						+ "of your shots to be lost. This error is most likely to occur when you are getting a lot of false "
						+ "shots due to poor lighting conditions and/or a poor camera setup. Please put the camera in front "
						+ "of the shooter and turn off any bright lights in front of the camera that are the same height as "
						+ "the shooter. If problems persist you may need to restart ShootOFF.");
				cameraAlert.show();
				
				shots.clear();
				shotEntries.clear();
			});
	}
	
	public String getCameraName() {
		return cameraName;
	}
	
	public void setContextMenu(ContextMenu menu) {
		this.contextMenu = Optional.of(menu);
	}
	
	public void setBackgroundFit(double width, double height) {
		background.setFitWidth(width);
		background.setFitHeight(height);
	}
	
	public void updateBackground(Image img, Optional<Bounds> projectionBounds) {
		if (!canvasGroup.getChildren().contains(background)) {
			Platform.runLater(() -> {
					if (canvasGroup.getChildren().isEmpty()) {
						canvasGroup.getChildren().add(background);
					} else {
						// Remove the wait spinner and replace it 
						// with the background
						canvasGroup.getChildren().set(0, background);
					}
				});
		}
		
		if (projectionBounds.isPresent()) {
			background.setX(projectionBounds.get().getMinX());
			background.setY(projectionBounds.get().getMinY());
		} else {
			background.setX(0);
			background.setY(0);
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
			try {
			if (shotEntries != null) shotEntries.clear();
			} catch (NullPointerException npe) {
				logger.error("JDK 8094135 exception", npe);
				jdk8094135Warning();
			}
			if (arenaController.isPresent()) arenaController.get().getCanvasManager().clearShots();
		}); 
	}
	
	public void reset() {
		startTime = System.currentTimeMillis();
		
		// Reset animations
		for (Target target : targets) {
			for (Node node : target.getTargetGroup().getChildren()) {
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
	
	private void notifyShot(Shot shot) {
		if (config.getSessionRecorder().isPresent()) {
			for (CameraManager cm : config.getRecordingManagers()) cm.notifyShot(shot);
		}
	}
	
	private Optional<String> createVideoString(Shot shot) {
		if (config.getSessionRecorder().isPresent() && 
				!config.getRecordingManagers().isEmpty()) {
			
			StringBuilder sb = new StringBuilder();
			
			for (CameraManager cm : config.getRecordingManagers()) {
				ShotRecorder r = cm.getRevelantRecorder(shot);
				
				if (sb.length() > 0) {
					sb.append(",");
				}
				
				sb.append(r.getCameraName());
				sb.append(":");
				sb.append(r.getRelativeVideoFile().getPath());
			}
			
			return Optional.of(sb.toString());
		}
		
		return Optional.empty();
	}
	
			
 	private Optional<ShotProcessor> processShot(Shot shot) {

		Optional<ShotProcessor> rejectingProcessor = Optional.empty();
		
		if (!getCameraManager().getDeduplicationProcessor().processShot(shot))
		{
			logger.debug("Processing Shot: Shot Rejected By {}", getCameraManager().getDeduplicationProcessor().getClass().getName());
			return Optional.of(getCameraManager().getDeduplicationProcessor());
		}
		
		
  		for (ShotProcessor processor : config.getShotProcessors()) {
  			if (!processor.processShot(shot)) {
 				if (processor instanceof MalfunctionsProcessor) {
 					hadMalfunction = true;
 				} else if (processor instanceof VirtualMagazineProcessor) {
 					hadReload = true;
 				}
 				
  				rejectingProcessor = Optional.of(processor);
  				logger.debug("Processing Shot: Shot Rejected By {}", processor.getClass().getName());
  				break;
  			}
  		}
  		
 		return rejectingProcessor;
 	}
 	
 	
	private void recordRejectedShot(Shot shot, ShotProcessor rejectingProcessor) {
		// Record video for rejected shots as long as they weren't rejected
		// for being dupes
		if (!config.getSessionRecorder().isPresent()) return;
		
		if (rejectingProcessor instanceof DeduplicationProcessor == false) {
			notifyShot(shot);
		
			Optional<String> videoString = createVideoString(shot);
			
			if (rejectingProcessor instanceof MalfunctionsProcessor) {
				config.getSessionRecorder().get().recordShot(cameraName, 
						shot, true, false, Optional.empty(), 
						Optional.empty(),
						videoString);		
			} else if (rejectingProcessor instanceof VirtualMagazineProcessor) {
				config.getSessionRecorder().get().recordShot(cameraName, 
						shot, false, true, Optional.empty(), 
						Optional.empty(),
						videoString);		
			}
		}
	}
	
	// For testing
	protected List<Shot> getShots() {
		return shots;
	}
	
	public void addShot(Color color, double x, double y) {
		if (startTime == 0) startTime = System.currentTimeMillis();
		
		Shot shot = new Shot(color, x, y, 
				System.currentTimeMillis() - startTime, cameraManager.getFrameCount(), config.getMarkerRadius());
	
		Optional<ShotProcessor> rejectingProcessor = processShot(shot);
		if (rejectingProcessor.isPresent()) {
			recordRejectedShot(shot, rejectingProcessor.get());
			return;
		} else {
			notifyShot(shot);
		}
		
		Optional<Shot> lastShot = Optional.empty();
		if (shotEntries.size() > 0) lastShot = Optional.of(shotEntries.get(shotEntries.size() -1).getShot());
		
		ShotEntry shotEntry;
		if (hadMalfunction || hadReload) {
			shotEntry = new ShotEntry(shot, lastShot, config.getShotTimerRowColor(), hadMalfunction, hadReload);
			hadMalfunction = false;
			hadReload = false;
		} else {
			shotEntry = new ShotEntry(shot, lastShot, config.getShotTimerRowColor(), false, false);
		}
		
		try {
			shotEntries.add(shotEntry);
		} catch (NullPointerException npe) {
			logger.error("JDK 8094135 exception", npe);
			jdk8094135Warning();
		}
		
		shots.add(shot);
		drawShot(shot);
		
		if (config.useRedLaserSound() && color.equals(Color.RED)) {
			TrainingExerciseBase.playSound(config.getRedLaserSound());
		} else if (config.useGreenLaserSound() && color.equals(Color.GREEN)) {
			TrainingExerciseBase.playSound(config.getGreenLaserSound());
		}
		
		Optional<TrainingExercise> currentExercise = config.getExercise();
		Optional<Hit> hit = checkHit(shot);
		if (hit.isPresent() && hit.get().getHitRegion().tagExists("command")) executeRegionCommands(hit.get());
		
		boolean processedShot = false;
		
		if (arenaController.isPresent() && projectionBounds.isPresent()) {
			Bounds b = projectionBounds.get();
			
			if (b.contains(shot.getX(), shot.getY())) {
				double x_scale = arenaController.get().getWidth() / b.getWidth();
				double y_scale = arenaController.get().getHeight() / b.getHeight();
				
				Shot arenaShot = new Shot(shot.getColor(), 
						(shot.getX() - b.getMinX()) * x_scale, (shot.getY() - b.getMinY()) * y_scale,

						shot.getTimestamp(), shot.getFrame(), config.getMarkerRadius());
				
				processedShot = arenaController.get().getCanvasManager().addArenaShot(arenaShot);
			}
		}
		
		if (currentExercise.isPresent() && !processedShot) {
			Optional<TargetRegion> hitRegion = Optional.empty();
			if (hit.isPresent()) hitRegion = Optional.of(hit.get().getHitRegion());
			
			currentExercise.get().shotListener(shot, hitRegion);
		}
	}
	
	public boolean addArenaShot(Shot shot) {
		shots.add(shot);
		drawShot(shot);
		
		Optional<TrainingExercise> currentExercise = config.getExercise();
		Optional<Hit> hit = checkHit(shot);
		if (hit.isPresent() && hit.get().getHitRegion().tagExists("command")) {
			executeRegionCommands(hit.get());
		}
		
		if (currentExercise.isPresent()) {
			Optional<TargetRegion> hitRegion = Optional.empty();
			if (hit.isPresent()) hitRegion = Optional.of(hit.get().getHitRegion());
			
			currentExercise.get().shotListener(shot, hitRegion);
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
	
	protected static class Hit {
		private final Target target;
		private final TargetRegion hitRegion;
		
		public Hit(Target target, TargetRegion hitRegion) {
			this.target = target;
			this.hitRegion = hitRegion;
		}
		
		public Target getTarget() {
			return target;
		}
		
		public TargetRegion getHitRegion() {
			return hitRegion;
		}
	}
	
	protected Optional<Hit> checkHit(Shot shot) {
		Optional<String> videoString = createVideoString(shot);
		
		// Targets are in order of when they were added, thus we must search in reverse
		// to ensure shots register for the top target when targets overlap
		for (ListIterator<Target> li = targets.listIterator(targets.size()); li.hasPrevious();) {
			Target target = li.previous();
			Group targetGroup = target.getTargetGroup();
			
			if (targetGroup.getBoundsInParent().contains(shot.getX(), shot.getY())) {				
				// Target was hit, see if a specific region was hit
				for (int i = targetGroup.getChildren().size() - 1; i >= 0; i--) {
					Node node = targetGroup.getChildren().get(i);
					
					Bounds nodeBounds = targetGroup.getLocalToParentTransform().transform(node.getBoundsInParent());
					
					if (nodeBounds.contains(shot.getX(), shot.getY())) {
						// If we hit an image region on a transparent pixel, ignore it
						TargetRegion region = (TargetRegion)node;
						if (region.getType() == RegionType.IMAGE) {
							// The image you get from the image view is its original size
							// We need to resize it if it has changed size to accurately
							// determine if a pixel is transparent
							Image currentImage = ((ImageRegion)region).getImage();
							
							int adjustedX = (int)(shot.getX() - nodeBounds.getMinX());
							int adjustedY = (int)(shot.getY() - nodeBounds.getMinY());
							 
							if (Math.abs(currentImage.getWidth() - nodeBounds.getWidth()) > .0000001 ||
									Math.abs(currentImage.getHeight() - nodeBounds.getHeight()) > .0000001) {
							
								BufferedImage bufferedOriginal = SwingFXUtils.fromFXImage(currentImage, null);
								
							    java.awt.Image tmp = bufferedOriginal.getScaledInstance((int)nodeBounds.getWidth(), 
							    		(int)nodeBounds.getHeight(), java.awt.Image.SCALE_SMOOTH);
							    BufferedImage bufferedResized = new BufferedImage((int)nodeBounds.getWidth(), (int)nodeBounds.getHeight(), 
							    		BufferedImage.TYPE_INT_ARGB);
	
							    Graphics2D g2d = bufferedResized.createGraphics();
							    g2d.drawImage(tmp, 0, 0, null);
							    g2d.dispose();
							    
								if (adjustedX > bufferedResized.getWidth() ||
									adjustedY > bufferedResized.getHeight() || 
										bufferedResized.getRGB(adjustedX, adjustedY) >> 24 == 0) {
									continue;
								}
							} else {
								if (adjustedX > currentImage.getWidth() ||
									adjustedY > currentImage.getHeight() ||
										currentImage.getPixelReader().getArgb(adjustedX, adjustedY) >> 24 == 0) {
									continue;
								}
							}
						} else {
							// The shot is in the bounding box but make sure it is in the shape's
							// fill otherwise we can get a shot detected where there isn't actually
							// a region showing
							Point2D localCoords = target.getTargetGroup().parentToLocal(shot.getX(), shot.getY());
							if (!node.contains(localCoords)) continue;
						}
						
						if (config.inDebugMode()) {
							Map<String, String> tags = region.getAllTags();
							
							StringBuilder tagList = new StringBuilder();
							for (Iterator<Entry<String, String>> it = tags.entrySet().iterator(); it.hasNext();) {
								Entry<String, String> entry = it.next();
								tagList.append(entry.getKey());
								tagList.append(":");
								tagList.append(entry.getValue());
								if (it.hasNext()) tagList.append(", ");
							}
						
							logger.debug("Processing Shot: Found Hit Region For Shot ({}, {}), Type ({}), Tags ({})", 
									shot.getX(), shot.getY(), region.getType(), tagList.toString());	
						}
						
						if (config.getSessionRecorder().isPresent()) {
							config.getSessionRecorder().get().recordShot(cameraName, 
									shot, false, false, Optional.of(target), 
									Optional.of(targetGroup.getChildren().indexOf(node)),
									videoString);
						}
						
						return Optional.of(new Hit(target, (TargetRegion)node));
					}
				}
			}
		}
		
		logger.debug("Processing Shot: Did Not Find Hit For Shot ({}, {})", 
				shot.getX(), shot.getY());
		
		if (config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordShot(cameraName, 
					shot, false, false, Optional.empty(), Optional.empty(),
					videoString);
		}
		
		return Optional.empty();
	}
	
	private void executeRegionCommands(Hit hit) {
		Target.parseCommandTag(hit.getHitRegion(), (commands, commandName, args) -> {	
				switch (commandName) {
				case "reset":
					camerasSupervisor.reset();
					break;
					
				case "animate":
					hit.getTarget().animate(hit.getHitRegion(), args);
					break;
					
				case "reverse":
					hit.getTarget().reverseAnimation(hit.getHitRegion());
					break;
					
				case "play_sound":
					// If there is a second parameter, we should look to see if it's an
					// image region that is down and if so, don't play the sound
					if (args.size() == 2) {
						Optional<TargetRegion> namedRegion = Target.getTargetRegionByName(targets, hit.getHitRegion(), args.get(1));
						if (namedRegion.isPresent() && namedRegion.get().getType() == RegionType.IMAGE) {
							if (!((ImageRegion)namedRegion.get()).onFirstFrame()) break;
						}
					}
					
					TrainingExerciseBase.playSound(args.get(0));
					break;
				}
			});
	}
	
	public Optional<Target> addTarget(File targetFile) {
		Optional<Group> targetGroup = TargetIO.loadTarget(targetFile);
		
		if (targetGroup.isPresent()) {				
			Optional<Target> target = Optional.of(addTarget(targetFile, targetGroup.get(), true));
			
			if (config.getSessionRecorder().isPresent() && target.isPresent()) {
				config.getSessionRecorder().get().recordTargetAdded(cameraName, target.get());
			}
			
			return target;
		}
		
		return Optional.empty();
	}
	
	public Target addTarget(File targetFile, Group targetGroup, boolean userDeletable) {

		Target newTarget = new Target(targetFile, targetGroup, config, this, userDeletable, targets.size());
		
		return addTarget(newTarget);
	}
	
	public Target addTarget(Target newTarget) {
		Platform.runLater(() -> { canvasGroup.getChildren().add(newTarget.getTargetGroup()); });
		targets.add(newTarget);
		
				
		return newTarget;
	}

	
	public void removeTarget(Target target) {
		Platform.runLater(() -> { canvasGroup.getChildren().remove(target.getTargetGroup()); });
		
		if (config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordTargetRemoved(cameraName, target);
		}
		
		targets.remove(target);
	}
	
	public List<Target> getTargets() {
		return targets;
	}
	
	public List<Group> getTargetGroups() {
		List<Group> targetGroups = new ArrayList<Group>();
		
		for (Target target : targets) targetGroups.add(target.getTargetGroup());
		
		return targetGroups;
	}
	
	protected void toggleTargetSelection(Optional<Group> newSelection) {
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