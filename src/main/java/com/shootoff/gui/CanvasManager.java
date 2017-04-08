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

package com.shootoff.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.Shot;
import com.shootoff.camera.processors.MalfunctionsProcessor;
import com.shootoff.camera.processors.ShotProcessor;
import com.shootoff.camera.processors.VirtualMagazineProcessor;
import com.shootoff.camera.recorders.ShotRecorder;
import com.shootoff.camera.shot.ArenaShot;
import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.gui.targets.MirroredTarget;
import com.shootoff.gui.targets.TargetCommands;
import com.shootoff.gui.targets.TargetView;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.targets.Hit;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import com.shootoff.util.SwingFXUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Pair;

public class CanvasManager implements CameraView {
	private final Logger logger = LoggerFactory.getLogger(CanvasManager.class);
	private final Group canvasGroup;
	private final Configuration config;
	protected CameraManager cameraManager;

	private final VBox diagnosticsVBox = new VBox();
	private static final int DIAGNOSTIC_POOL_SIZE = 10;
	private static final int DIAGNOSTIC_CHIME_DELAY = 5000; // ms
	private final ScheduledExecutorService diagnosticExecutorService = Executors
			.newScheduledThreadPool(DIAGNOSTIC_POOL_SIZE);
	private final Map<Label, ScheduledFuture<Void>> diagnosticFutures = new HashMap<>();
	private final Image muteImage = new Image(CanvasManager.class.getResourceAsStream("/images/mute.png"));
	private final Image soundImage = new Image(CanvasManager.class.getResourceAsStream("/images/sound.png"));

	private final Resetter resetter;
	private final String cameraName;
	private final ObservableList<ShotEntry> shotEntries;
	private final ImageView background = new ImageView();
	private final List<DisplayShot> shots = Collections.synchronizedList(new ArrayList<DisplayShot>());
	private final List<Target> targets = new ArrayList<>();

	private ProgressIndicator progress;
	private Optional<ContextMenu> contextMenu = Optional.empty();
	private Optional<TargetView> selectedTarget = Optional.empty();
	private boolean showShots = true;
	private boolean hadMalfunction = false;
	private boolean hadReload = false;

	private static final int MAX_FEED_FPS = 15;
	private static final int MINIMUM_FRAME_DELTA = 1000 / MAX_FEED_FPS; // ms
	private long lastFrameTime = 0;

	protected Optional<ProjectorArenaPane> arenaPane = Optional.empty();
	private Optional<Bounds> projectionBounds = Optional.empty();

	public CanvasManager(Group canvasGroup, Resetter resetter, String cameraName,
			ObservableList<ShotEntry> shotEntries) {
		this.canvasGroup = canvasGroup;
		config = Configuration.getConfig();
		this.resetter = resetter;
		this.cameraName = cameraName;
		this.shotEntries = shotEntries;

		background.setOnMouseClicked((event) -> {
			toggleTargetSelection(Optional.empty());
		});

		if (Platform.isFxApplicationThread()) {
			progress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
			progress.setPrefHeight(config.getDisplayHeight());
			progress.setPrefWidth(config.getDisplayWidth());
			canvasGroup.getChildren().add(progress);
			canvasGroup.getChildren().add(diagnosticsVBox);
			diagnosticsVBox.setAlignment(Pos.CENTER);
			diagnosticsVBox.setFillWidth(true);
			diagnosticsVBox.setPrefWidth(config.getDisplayWidth());
		}

		canvasGroup.setOnMouseClicked((event) -> {
			if (contextMenu.isPresent() && contextMenu.get().isShowing()) contextMenu.get().hide();

			if (config.inDebugMode() && event.getButton() == MouseButton.PRIMARY) {
				// Click to shoot
				final ShotColor shotColor;

				if (event.isShiftDown()) {
					shotColor = ShotColor.RED;
				} else if (event.isControlDown()) {
					shotColor = ShotColor.GREEN;
				} else {
					return;
				}

				// Skip the camera manager for injected shots made from the
				// arena tab otherwise they get scaled before the call to
				// addArenaShot when they go through the arena camera feed's
				// canvas manager
				if (this instanceof MirroredCanvasManager) {
					final long shotTimestamp = System.currentTimeMillis();

					addShot(new DisplayShot(new Shot(shotColor, event.getX(), event.getY(), shotTimestamp), config.getMarkerRadius()), false);
				} else {
					cameraManager.injectShot(shotColor, event.getX(), event.getY(), false);
				}
				return;
			} else if (contextMenu.isPresent() && event.getButton() == MouseButton.SECONDARY) {
				contextMenu.get().show(canvasGroup, event.getScreenX(), event.getScreenY());
			}
		});
	}

	@Override
	public void close() {
		diagnosticExecutorService.shutdownNow();
	}

	@Override
	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	@Override
	public boolean addChild(Node c) {
		return getCanvasGroup().getChildren().add(c);
	}

	@Override
	public boolean removeChild(Node c) {
		return getCanvasGroup().getChildren().remove(c);
	}

	public Label addDiagnosticMessage(final String message, final long chimeDelay, final Color backgroundColor) {
		final Label diagnosticLabel = new Label(message);
		diagnosticLabel.setStyle("-fx-background-color: " + colorToWebCode(backgroundColor));

		final ImageView muteView = new ImageView();
		muteView.setFitHeight(20);
		muteView.setFitWidth(muteView.getFitHeight());
		if (config.isChimeMuted(message)) {
			muteView.setImage(muteImage);
		} else {
			muteView.setImage(soundImage);
		}

		diagnosticLabel.setContentDisplay(ContentDisplay.RIGHT);
		diagnosticLabel.setGraphic(muteView);
		diagnosticLabel.setOnMouseClicked((event) -> {
			if (config.isChimeMuted(message)) {
				muteView.setImage(soundImage);
				config.unmuteMessageChime(message);
			} else {
				muteView.setImage(muteImage);
				config.muteMessageChime(message);
			}

			try {
				config.writeConfigurationFile();
			} catch (final Exception e) {
				logger.error("Failed persisting message's (" + message + ") chime mute settings.", e);
			}
		});

		Platform.runLater(() -> diagnosticsVBox.getChildren().add(diagnosticLabel));

		if (chimeDelay > 0 && !config.isChimeMuted(message) && !diagnosticExecutorService.isShutdown()) {
			@SuppressWarnings("unchecked")
			final ScheduledFuture<Void> chimeFuture = (ScheduledFuture<Void>) diagnosticExecutorService.schedule(
					() -> TrainingExerciseBase.playSound("sounds/chime.wav"), chimeDelay, TimeUnit.MILLISECONDS);
			diagnosticFutures.put(diagnosticLabel, chimeFuture);
		}

		return diagnosticLabel;
	}

	@Override
	public Label addDiagnosticMessage(String message, Color backgroundColor) {
		return addDiagnosticMessage(message, DIAGNOSTIC_CHIME_DELAY, backgroundColor);
	}

	@Override
	public void removeDiagnosticMessage(Label diagnosticLabel) {
		if (diagnosticFutures.containsKey(diagnosticLabel)) {
			diagnosticFutures.get(diagnosticLabel).cancel(false);
			diagnosticFutures.remove(diagnosticLabel);
		}

		Platform.runLater(() -> diagnosticsVBox.getChildren().remove(diagnosticLabel));
	}

	public static String colorToWebCode(Color color) {
		return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
				(int) (color.getBlue() * 255));
	}

	private void jdk8094135Warning() {
		Platform.runLater(() -> {
			final Alert cameraAlert = new Alert(AlertType.ERROR);
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
		contextMenu = Optional.of(menu);
	}

	public void setBackgroundFit(double width, double height) {
		background.setFitWidth(width);
		background.setFitHeight(height);
	}

	@Override
	public void updateBackground(BufferedImage frame, Optional<Bounds> projectionBounds) {
		updateCanvasGroup();

		if (frame == null) {
			background.setX(0);
			background.setY(0);
			background.setImage(null);
			return;
		}

		// Prevent the webcam feed from being refreshed faster than some maximum
		// FPS otherwise we waste CPU cycles converting a frames to show the
		// user and these are cycles we could spend detecting shots. A lower
		// FPS (e.g. ~15) looks perfect fine to a person
		if (System.currentTimeMillis() - lastFrameTime < MINIMUM_FRAME_DELTA)
			return;
		else
			lastFrameTime = System.currentTimeMillis();

		Image img;
		if (projectionBounds.isPresent()) {
			final Bounds translatedBounds = translateCameraToCanvas(projectionBounds.get());
			background.setX(translatedBounds.getMinX());
			background.setY(translatedBounds.getMinY());

			img = SwingFXUtils.toFXImage(
					resize(frame, (int) translatedBounds.getWidth(), (int) translatedBounds.getHeight()), null);
		} else {
			background.setX(0);
			background.setY(0);

			img = SwingFXUtils.toFXImage(resize(frame, config.getDisplayWidth(), config.getDisplayHeight()), null);
		}

		Platform.runLater(() -> background.setImage(img));
	}

	public void updateBackground(Image img) {
		updateCanvasGroup();
		background.setX(0);
		background.setY(0);
		Platform.runLater(() -> background.setImage(img));
	}

	private void updateCanvasGroup() {
		if (!canvasGroup.getChildren().contains(background)) {
			if (canvasGroup.getChildren().isEmpty()) {
				canvasGroup.getChildren().add(background);
			} else {
				// Remove the wait spinner and replace it
				// with the background
				Platform.runLater(() -> canvasGroup.getChildren().set(0, background));
			}
		}
	}

	private BufferedImage resize(BufferedImage source, int width, int height) {
		if (source.getWidth() == width && source.getHeight() == height) return source;

		final BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g2 = tmp.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(source, 0, 0, width, height, null);
		g2.dispose();

		return tmp;
	}

	public BufferedImage getBufferedImage() {
		final BufferedImage projectedScene = SwingFXUtils.fromFXImage(canvasGroup.getScene().snapshot(null), null);
		return projectedScene;
	}

	public Bounds translateCameraToCanvas(Bounds bounds) {
		if (config.getDisplayWidth() == cameraManager.getFeedWidth()
				&& config.getDisplayHeight() == cameraManager.getFeedHeight())
			return bounds;

		final double scaleX = (double) config.getDisplayWidth() / (double) cameraManager.getFeedWidth();
		final double scaleY = (double) config.getDisplayHeight() / (double) cameraManager.getFeedHeight();

		final double minX = (bounds.getMinX() * scaleX);
		final double minY = (bounds.getMinY() * scaleY);
		final double width = (bounds.getWidth() * scaleX);
		final double height = (bounds.getHeight() * scaleY);

		logger.trace("translateCameraToCanvas {} {} {} {} - {} {} {} {}", bounds.getMinX(), bounds.getMinY(),
				bounds.getWidth(), bounds.getHeight(), minX, minY, width, height);

		return new BoundingBox(minX, minY, width, height);
	}

	public Bounds translateCanvasToCamera(Bounds bounds) {
		if (config.getDisplayWidth() == cameraManager.getFeedWidth()
				&& config.getDisplayHeight() == cameraManager.getFeedHeight())
			return bounds;

		final double scaleX = (double) cameraManager.getFeedWidth() / (double) config.getDisplayWidth();
		final double scaleY = (double) cameraManager.getFeedHeight() / (double) config.getDisplayHeight();

		final double minX = (bounds.getMinX() * scaleX);
		final double minY = (bounds.getMinY() * scaleY);
		final double width = (bounds.getWidth() * scaleX);
		final double height = (bounds.getHeight() * scaleY);

		logger.trace("translateCanvasToCamera {} {} {} {} - {} {} {} {}", bounds.getMinX(), bounds.getMinY(),
				bounds.getWidth(), bounds.getHeight(), minX, minY, width, height);

		return new BoundingBox(minX, minY, width, height);
	}
	
	/* Takes a point x,y and translates it from an arena canvas to a camera (feed) point.
	 * 
	 * This is hackish because it will break if any of the math for these calculations changes in other places.
	 */
	public Pair<Double, Double> translateCanvasToCameraPoint(double x, double y) {
		if (cameraManager == null)
		{
			logger.error("Called when cameraManager == null");
			return new Pair<Double, Double>(x,y);
		}
		
		if (!cameraManager.getProjectionBounds().isPresent() || !arenaPane.isPresent())
		{
			logger.error("Called when projectionBounds is not available");
			return new Pair<Double, Double>(x,y);
		}
		
		final Bounds b = cameraManager.getProjectionBounds().get();

		final double x_scale = b.getWidth() / arenaPane.get().getWidth();
		final double y_scale = b.getHeight() / arenaPane.get().getHeight();
		
		logger.trace("translateCanvasToCameraPoint scale x {} y {}", x_scale, y_scale);

		return new Pair<Double, Double>(b.getMinX() + (x * x_scale), b.getMinY() + (y*y_scale));
	}
	

	public Group getCanvasGroup() {
		return canvasGroup;
	}

	@Override
	public void clearShots() {
		final Runnable clearShotsAction = () -> {
			for (final DisplayShot shot : shots) {
				canvasGroup.getChildren().remove(shot.getMarker());
			}

			shots.clear();
			try {
				if (shotEntries != null) shotEntries.clear();
			} catch (final NullPointerException npe) {
				logger.error("JDK 8094135 exception", npe);
				jdk8094135Warning();
			}
			if (arenaPane.isPresent() && !(this instanceof MirroredCanvasManager)) arenaPane.get().getCanvasManager().clearShots();
		};

		if (Platform.isFxApplicationThread()) {
			clearShotsAction.run();
		} else {
			Platform.runLater(clearShotsAction);
		}
	}

	@Override
	public void reset() {
		// Reset animations
		for (final Target target : targets) {
			for (final TargetRegion region : target.getRegions()) {
				if (region.getType() == RegionType.IMAGE) ((ImageRegion) region).reset();
			}
		}

		if (arenaPane.isPresent() && !(this instanceof MirroredCanvasManager)) {
			arenaPane.get().getCanvasManager().reset();
		}

		clearShots();
	}

	public void setProjectorArena(ProjectorArenaPane arenaPane, Bounds projectionBounds) {
		this.arenaPane = Optional.ofNullable(arenaPane);
		this.projectionBounds = Optional.ofNullable(projectionBounds);
	}

	public void setShowShots(boolean showShots) {
		if (this.showShots != showShots) {
			for (final DisplayShot shot : shots)
				shot.getMarker().setVisible(showShots);
		}

		this.showShots = showShots;
	}

	private void notifyShot(Shot shot) {
		if (config.getSessionRecorder().isPresent()) {
			for (final CameraManager cm : config.getRecordingManagers())
				cm.notifyShot(shot);
		}
	}

	private Optional<String> createVideoString(Shot shot) {
		if (config.getSessionRecorder().isPresent() && !config.getRecordingManagers().isEmpty()) {
			final StringBuilder sb = new StringBuilder();

			for (final CameraManager cm : config.getRecordingManagers()) {
				final ShotRecorder r = cm.getRevelantRecorder(shot);

				if (sb.length() > 0) {
					sb.append(",");
				}

				sb.append(r.getCameraName().replaceAll(":", "-"));
				sb.append(":");
				sb.append(r.getRelativeVideoFile().getPath());
			}

			return Optional.of(sb.toString());
		}

		return Optional.empty();
	}

	private Optional<ShotProcessor> processShot(Shot shot) {

		Optional<ShotProcessor> rejectingProcessor = Optional.empty();

		for (final ShotProcessor processor : config.getShotProcessors()) {
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

	private void recordRejectedShot(DisplayShot shot, ShotProcessor rejectingProcessor) {

		if (!config.getSessionRecorder().isPresent()) return;

		notifyShot(shot);

		final Optional<String> videoString = createVideoString(shot);

		if (rejectingProcessor instanceof MalfunctionsProcessor) {
			config.getSessionRecorder().get().recordShot(cameraName, shot, true, false, Optional.empty(),
					Optional.empty(), videoString);
		} else if (rejectingProcessor instanceof VirtualMagazineProcessor) {
			config.getSessionRecorder().get().recordShot(cameraName, shot, false, true, Optional.empty(),
					Optional.empty(), videoString);
		}
	}

	// For testing
	protected List<DisplayShot> getShots() {
		return shots;
	}

	@Override
	public void addShot(DisplayShot shot, boolean isMirroredShot) {
		if (!isMirroredShot) {
			final Optional<ShotProcessor> rejectingProcessor = processShot(shot);
			if (rejectingProcessor.isPresent()) {
				recordRejectedShot(shot, rejectingProcessor.get());
				return;
			} else {
				notifyShot(shot);
			}

			// TODO: Add separate infrared sound or switch config to read
			// "red/infrared"
			if (config.useRedLaserSound()
					&& (ShotColor.RED.equals(shot.getColor()) || ShotColor.INFRARED.equals(shot.getColor()))) {
				TrainingExerciseBase.playSound(config.getRedLaserSound());
			} else if (config.useGreenLaserSound() && ShotColor.GREEN.equals(shot.getColor())) {
				TrainingExerciseBase.playSound(config.getGreenLaserSound());
			}
		}

		// Create a shot entry to show the shot's data
		// in the shot timer table if the shot timer
		// table is in use
		if (shotEntries != null) {
			final Optional<Shot> lastShot;

			if (shotEntries.isEmpty()) {
				lastShot = Optional.empty();
			} else {
				lastShot = Optional.of(shotEntries.get(shotEntries.size() - 1).getShot());
			}

			final ShotEntry shotEntry;
			if (hadMalfunction || hadReload) {
				shotEntry = new ShotEntry(shot, lastShot, config.getShotTimerRowColor(), hadMalfunction, hadReload);
				hadMalfunction = false;
				hadReload = false;
			} else {
				shotEntry = new ShotEntry(shot, lastShot, config.getShotTimerRowColor(), false, false);
			}

			try {
				shotEntries.add(shotEntry);
			} catch (final NullPointerException npe) {
				logger.error("JDK 8094135 exception", npe);
				jdk8094135Warning();
			}
		}

		shots.add(shot);
		drawShot(shot);

		final Optional<String> videoString = createVideoString(shot);

		boolean passedToArena = false;
		boolean processedShot = false;

		if (arenaPane.isPresent() && !(this instanceof MirroredCanvasManager) && projectionBounds.isPresent()) {
			final Bounds b = projectionBounds.get();

			if (b.contains(shot.getX(), shot.getY())) {
				passedToArena = true;


				final ArenaShot arenaShot = new ArenaShot(shot);
				
				scaleShotToArenaBounds(arenaShot);

				processedShot = arenaPane.get().getCanvasManager().addArenaShot(arenaShot, videoString, isMirroredShot);
			}
		}

		// If the arena canvas handled the shot, we don't need to do anything
		// else
		if (passedToArena || processedShot) return;

		final Optional<TrainingExercise> currentExercise = config.getExercise();
		final Optional<Hit> hit = checkHit(shot, videoString, isMirroredShot);
		if (hit.isPresent() && hit.get().getHitRegion().tagExists("command")) executeRegionCommands(hit.get(), isMirroredShot);

		if (currentExercise.isPresent() && !processedShot) {
			// If the canvas is mirrored, use the one without the camera manager
			// for exercises because that is the one for the arena window.
			// If we use the arena tab canvas manager the targets will be
			// copies and will not be the versions of the targets added
			// by exercises.
			if ((this instanceof MirroredCanvasManager) && cameraManager == null) {
				currentExercise.get().shotListener(shot, hit);
			} else if (!(this instanceof MirroredCanvasManager)) {
				currentExercise.get().shotListener(shot, hit);
			}
		}
	}

	public void scaleShotToArenaBounds(ArenaShot shot) {
		if (!projectionBounds.isPresent()) {
			logger.error("scaleShotToArenaBounds called when projectionBounds not present");
			return;
		}
		
		final double x_scale = arenaPane.get().getWidth() / projectionBounds.get().getWidth();
		final double y_scale = arenaPane.get().getHeight() / projectionBounds.get().getHeight();
		
		logger.trace("scaleShotToArenaBounds pre x {} y {}", shot.getX(), shot.getY());
		
		shot.setArenaCoords((shot.getX() - projectionBounds.get().getMinX()) * x_scale,
				(shot.getY() - projectionBounds.get().getMinY()) * y_scale);

		logger.trace("scaleShotToArenaBounds post x {} y {}", shot.getX(), shot.getY());

	
	}

	public boolean addArenaShot(ArenaShot shot, Optional<String> videoString, boolean isMirroredShot) {
		shots.add(shot);
		drawShot(shot);

		final Optional<TrainingExercise> currentExercise = config.getExercise();
		final Optional<Hit> hit = checkHit(shot, videoString, isMirroredShot);
		if (hit.isPresent() && hit.get().getHitRegion().tagExists("command")) {
			executeRegionCommands(hit.get(), isMirroredShot);
		}

		if (!isMirroredShot) {
			if (currentExercise.isPresent()) {
				currentExercise.get().shotListener(shot, hit);
				return true;
			}
		}

		return false;
	}

	private void drawShot(DisplayShot shot) {
		final Runnable drawShotAction = () -> {
			canvasGroup.getChildren().add(shot.getMarker());
			shot.getMarker().setVisible(showShots);
		};

		if (Platform.isFxApplicationThread()) {
			drawShotAction.run();
		} else {
			Platform.runLater(drawShotAction);
		}
	}

	protected Optional<Hit> checkHit(DisplayShot shot, Optional<String> videoString, boolean isMirroredShot) {		
		// Targets are in order of when they were added, thus we must search in
		// reverse to ensure shots register for the top target when targets
		// overlap
		for (final ListIterator<Target> li = targets.listIterator(targets.size()); li.hasPrevious();) {
			final Target target = li.previous();

			final Optional<Hit> hit;
			if (shot instanceof ArenaShot)
			{
				hit = target.isHit(((ArenaShot)shot).getX(), ((ArenaShot)shot).getY());
			}
			else
			{
				hit = target.isHit(shot.getX(), shot.getY());
			}

			if (hit.isPresent()) {
				hit.get().setShot(shot);
				
				final TargetRegion region = hit.get().getHitRegion();

				if (config.inDebugMode()) {
					final Map<String, String> tags = region.getAllTags();

					final StringBuilder tagList = new StringBuilder();
					for (final Iterator<Entry<String, String>> it = tags.entrySet().iterator(); it.hasNext();) {
						final Entry<String, String> entry = it.next();
						tagList.append(entry.getKey());
						tagList.append(":");
						tagList.append(entry.getValue());
						if (it.hasNext()) tagList.append(", ");
					}

					logger.debug("Processing Shot: Found Hit Region For Shot ({}, {}), Type ({}), Tags ({})",
							shot.getX(), shot.getY(), region.getType(), tagList.toString());
				}

				if (!isMirroredShot && config.getSessionRecorder().isPresent()) {
					config.getSessionRecorder().get().recordShot(cameraName, shot, false, false, Optional.of(target),
							Optional.of(target.getRegions().indexOf(region)), videoString);
				}

				return hit;
			}
		}

		logger.debug("Processing Shot: Did Not Find Hit For Shot ({}, {})", shot.getX(), shot.getY());

		if (!isMirroredShot && config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordShot(cameraName, shot, false, false, Optional.empty(),
					Optional.empty(), videoString);
		}

		return Optional.empty();
	}

	private void executeRegionCommands(Hit hit, boolean isMirroredShot) {
		if (arenaPane.isPresent())
			TargetView.parseCommandTag(hit.getHitRegion(), new TargetCommands(arenaPane.get().getCanvasManager(), targets, resetter, hit, isMirroredShot));
		else
			TargetView.parseCommandTag(hit.getHitRegion(), new TargetCommands(this, targets, resetter, hit, isMirroredShot));

	}

	protected Optional<TargetComponents> loadTarget(File targetFile, boolean playAnimations) {
		Optional<TargetComponents> targetComponents;

		if ('@' == targetFile.toString().charAt(0)) {
			if (!config.getPlugin().isPresent()) {
				throw new AssertionError("Loaded target from training exercise resources, but a plugin does not "
						+ "exist for the target.");
			}

			final ClassLoader loader = config.getPlugin().get().getLoader();

			final InputStream resourceTargetStream = loader
					.getResourceAsStream(targetFile.toString().substring(1).replace("\\", "/"));
			if (resourceTargetStream != null) {
				targetComponents = TargetIO.loadTarget(resourceTargetStream, playAnimations, loader);
			} else {
				targetComponents = Optional.empty();
				logger.error("Error adding target from stream created from resource {}",
						targetFile.toString().substring(1).replace("\\", "/"));
			}
		} else {
			targetComponents = TargetIO.loadTarget(targetFile, playAnimations);
		}

		return targetComponents;
	}

	public Optional<Target> addTarget(File targetFile, boolean playAnimations) {
		final Optional<TargetComponents> targetComponents = loadTarget(targetFile, playAnimations);

		if (targetComponents.isPresent()) {
			final TargetComponents tc = targetComponents.get();
			final Optional<Target> target = Optional
					.of(addTarget(targetFile, tc.getTargetGroup(), tc.getTargetTags(), true));

			if (config.getSessionRecorder().isPresent() && target.isPresent()) {
				config.getSessionRecorder().get().recordTargetAdded(cameraName, target.get());
			}

			return target;
		}

		return Optional.empty();
	}

	@Override
	public Optional<Target> addTarget(File targetFile) {
		return addTarget(targetFile, true);
	}

	public Target addTarget(File targetFile, Group targetGroup, Map<String, String> targetTags, boolean userDeletable) {
		final TargetView newTarget;

		if (this instanceof MirroredCanvasManager) {
			newTarget = new MirroredTarget(targetFile, targetGroup, targetTags, config, this, userDeletable);
		} else {
			newTarget = new TargetView(targetFile, targetGroup, targetTags, this, userDeletable);
		}

		return addTarget(newTarget);
	}

	@Override
	public Target addTarget(Target newTarget) {
		final Runnable addTargetAction = () -> canvasGroup.getChildren().add(((TargetView) newTarget).getTargetGroup());

		if (Platform.isFxApplicationThread()) {
			addTargetAction.run();
		} else {
			Platform.runLater(addTargetAction);
		}

		targets.add(newTarget);

		// If this is a mirrored canvas, only alert exercises of target updates
		// from the arena window, not the tab. There is no arena tab if we are in
		// headless mode, thus there is also no MirroredCanvasManager. Always
		// perform target update when the canvas isn't mirrored for that reason.
		if (!(this instanceof MirroredCanvasManager)
				|| ((this instanceof MirroredCanvasManager) && arenaPane.isPresent())) {
			final Optional<TrainingExercise> enabledExercise = config.getExercise();
			if (enabledExercise.isPresent())
				enabledExercise.get().targetUpdate(newTarget, TrainingExercise.TargetChange.ADDED);
		}

		return newTarget;
	}

	public void removeTarget(Target target) {
		final Runnable removeTargetAction = () -> canvasGroup.getChildren()
				.remove(((TargetView) target).getTargetGroup());

		if (Platform.isFxApplicationThread()) {
			removeTargetAction.run();
		} else {
			Platform.runLater(removeTargetAction);
		}

		if (config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordTargetRemoved(cameraName, target);
		}

		targets.remove(target);

		// If this is a mirrored canvas, only alert exercises of target updates
		// from the arena window, not the tab. There is no arena tab if we are in
		// headless mode, thus there is also no MirroredCanvasManager. Always
		// perform target update when the canvas isn't mirrored for that reason.
		if (!(this instanceof MirroredCanvasManager)
				|| ((this instanceof MirroredCanvasManager) && arenaPane.isPresent())) {
			final Optional<TrainingExercise> enabledExercise = config.getExercise();
			if (enabledExercise.isPresent())
				enabledExercise.get().targetUpdate(target, TrainingExercise.TargetChange.REMOVED);
		}
	}

	public void clearTargets() {
		for (final Target t : new ArrayList<>(targets)) {
			removeTarget(t);
		}
	}

	public List<Target> getTargets() {
		return targets;
	}

	public void toggleTargetSelection(Optional<TargetView> newSelection) {
		if (selectedTarget.isPresent()) selectedTarget.get().toggleSelected();

		if (newSelection.isPresent()) {
			newSelection.get().toggleSelected();
			selectedTarget = newSelection;
		} else {
			selectedTarget = Optional.empty();
			canvasGroup.requestFocus();
		}
	}
}