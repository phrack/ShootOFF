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
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.courses.Course;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Resetter;
import com.shootoff.gui.TargetDistancePane;
import com.shootoff.gui.TargetView;
import com.shootoff.targets.Target;
import com.shootoff.util.TimerPool;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ProjectorArenaController implements CalibrationListener {
	private static final Logger logger = LoggerFactory.getLogger(ProjectorArenaController.class);

	protected Stage arenaStage;
	private Stage shootOFFStage;
	@FXML protected AnchorPane arenaAnchor;
	@FXML private Group arenaCanvasGroup;
	@FXML private Label calibrationLabel;

	protected Configuration config;
	protected CanvasManager canvasManager;
	private Label mouseOnArenaLabel = null;
	private Optional<LocatedImage> background = Optional.empty();
	private Optional<LocatedImage> savedBackground = Optional.empty();

	private Screen originalArenaHomeScreen;
	private Optional<Screen> detectedProjectorScreen = Optional.empty();
	private Point2D arenaScreenOrigin = new Point2D(0, 0);
	private Screen arenaHome;

	private CalibrationManager calibrationManager;
	private Optional<PerspectiveManager> perspectiveManager = Optional.empty();

	// Used for testing
	public void init(Configuration config, CanvasManager canvasManager) {
		this.config = config;
		this.canvasManager = canvasManager;

		arenaStage = new Stage();
		arenaAnchor = new AnchorPane(canvasManager.getCanvasGroup());
		final Scene scene = new Scene(arenaAnchor, 500, 500);
		arenaStage.setScene(scene);
	}

	public void init(Stage shootOFFStage, Configuration config, Resetter resetter) {
		this.config = config;

		this.shootOFFStage = shootOFFStage;
		arenaStage = (Stage) arenaAnchor.getScene().getWindow();

		arenaStage.setFullScreenExitHint("");

		canvasManager = new CanvasManager(arenaCanvasGroup, config, resetter, "arena", null);
		canvasManager.updateBackground(null, Optional.empty());

		arenaAnchor.setOnMouseClicked((event) -> {
			canvasManager.toggleTargetSelection(Optional.empty());
		});

		arenaAnchor.widthProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		arenaAnchor.heightProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		arenaAnchor.setStyle("-fx-background-color: #333333;");
	}

	public void setCalibrationManager(CalibrationManager calibrationManager) {
		this.calibrationManager = calibrationManager;
	}

	private Optional<Screen> getStageHomeScreen(Stage stage) {
		final ObservableList<Screen> stageHomeScreens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1);

		if (stageHomeScreens.isEmpty()) {
			final StringBuilder message = new StringBuilder(
					String.format("Didn't find screen for stage with title %s at (%f, %f)." + " Existing screens: %n%n",
							stage.getTitle(), stage.getX(), stage.getY()));

			final Iterator<Screen> it = Screen.getScreens().iterator();

			while (it.hasNext()) {
				Screen s = it.next();

				message.append(String.format("(w = %f, h = %f, dpi = %f)", s.getBounds().getWidth(),
						s.getBounds().getHeight(), s.getDpi()));

				if (it.hasNext()) {
					message.append("\n");
				}
			}

			logger.error(message.toString());

			return Optional.empty();
		} else if (stageHomeScreens.size() > 1) {
			logger.warn("Found multiple screens as the possible arena home screen, this is unexpected: {}",
					stageHomeScreens.size());
		}

		return Optional.of(stageHomeScreens.get(0));
	}

	public void autoPlaceArena() {
		Optional<Screen> homeScreen = getStageHomeScreen(arenaStage);

		if (homeScreen.isPresent()) {
			originalArenaHomeScreen = homeScreen.get();
		} else {
			return;
		}

		// Place the arena on what we hope is the projector with the following
		// precidence:
		// 1. If the user has place the arena on a screen before, place it on
		// that screen again
		// 2. If the user has never placed the arena before and there are only
		// two screens,
		// put it on the screen the ShootOFF window isn't on
		// 3. If the arena has never been placed and there are more than two
		// screens, place
		// the arena on the smallest screen
		if (config.getArenaPosition().isPresent()) {
			logger.debug("Projector has been manually placed previously");

			final Point2D arenaPosition = config.getArenaPosition().get();

			final ObservableList<Screen> screens = Screen.getScreensForRectangle(arenaPosition.getX(),
					arenaPosition.getY(), 1, 1);

			if (!screens.isEmpty()) {
				arenaStage.setX(arenaPosition.getX());
				arenaStage.setY(arenaPosition.getY());

				Platform.runLater(() -> toggleFullScreen());

				return;
			} else {
				logger.debug("Saved screen coordinates ({}, {}) no longer exists, attempting fallback approaches...",
						arenaPosition.getX(), arenaPosition.getY());
			}
		}

		Optional<Screen> projector = Optional.empty();

		if (Screen.getScreens().size() == 2) {
			logger.debug("Two screens present");

			homeScreen = getStageHomeScreen(shootOFFStage);

			if (!homeScreen.isPresent()) return;

			final Screen shootOFFScreen = homeScreen.get();

			for (Screen screen : Screen.getScreens()) {
				if (!screen.equals(shootOFFScreen)) {
					projector = Optional.of(screen);
					break;
				}
			}
		} else if (Screen.getScreens().size() > 2) {
			logger.debug("More than two screens present");

			Screen smallest = null;

			// Find screen with the smallest area
			for (Screen screen : Screen.getScreens()) {
				if (smallest == null) {
					smallest = screen;
				} else {
					if (screen.getBounds().getHeight()
							* screen.getBounds().getWidth() < smallest.getBounds().getHeight()
									* smallest.getBounds().getWidth()) {
						smallest = screen;
					}
				}
			}

			projector = Optional.ofNullable(smallest);
		}

		if (projector.isPresent()) {
			arenaHome = projector.get();

			double newX = arenaHome.getVisualBounds().getMinX();
			double newY = arenaHome.getVisualBounds().getMinY();

			logger.debug("Found likely projector screen: resolution = {}x{}, newX = {}, newY = {}",
					arenaHome.getBounds().getWidth(), arenaHome.getBounds().getHeight(), newX, newY);

			arenaStage.setX(newX + 10);
			arenaStage.setY(newY + 10);

			detectedProjectorScreen = projector;

			Platform.runLater(() -> toggleFullScreen());

			Rectangle2D arenaScreenBounds = arenaHome.getBounds();
			arenaScreenOrigin = new Point2D(arenaScreenBounds.getMinX(), arenaScreenBounds.getMinY());

		} else {
			logger.debug("Did not find screen that is a likely projector");
		}
	}

	public void toggleArena() throws IOException {
		if (arenaStage.isShowing()) {
			arenaStage.hide();
		} else {
			arenaStage.show();
		}
	}

	public double getWidth() {
		return arenaAnchor.getWidth();
	}

	public double getHeight() {
		return arenaAnchor.getHeight();
	}

	public Dimension2D getArenaStageResolution() {
		return new Dimension2D(arenaStage.getWidth(), arenaStage.getHeight());
	}

	public Point2D getArenaScreenOrigin() {
		return arenaScreenOrigin;
	}

	public CanvasManager getCanvasManager() {
		return canvasManager;
	}

	public void close() {
		arenaStage.close();
		TimerPool.cancelTimer(mouseExitedFuture);
	}

	public void setBackground(LocatedImage img) {
		background = Optional.ofNullable(img);
		canvasManager.updateBackground(img);
	}

	/**
	 * Used to temporarily save the background before autocalibration
	 */
	public void saveCurrentBackground() {
		if (background.isPresent()) {
			savedBackground = background;
		}
	}

	/**
	 * Used to restore the background that was saved before autocalibration with
	 * saveCurrentBackground.
	 */
	public void restoreCurrentBackground() {
		if (savedBackground.isPresent()) {
			setBackground(savedBackground.get());
			savedBackground = Optional.empty();
		} else {
			setBackground(null);
		}
	}

	public Optional<LocatedImage> getBackground() {
		return background;
	}

	public Configuration getConfiguration() {
		return config;
	}

	public void setCourse(final Course course) {
		if (course.getBackground().isPresent()) {
			setBackground(course.getBackground().get());
		} else {
			setBackground(null);
		}

		canvasManager.clearTargets();

		boolean scaleCourse = course.getResolution().isPresent()
				&& (Math.abs(course.getResolution().get().getWidth() - getWidth()) > .0001
						|| Math.abs(course.getResolution().get().getHeight() - getHeight()) > .0001);

		double widthScaleFactor = 1;
		double heightScaleFactor = 1;

		if (scaleCourse) {
			widthScaleFactor = getWidth() / course.getResolution().get().getWidth();
			heightScaleFactor = getHeight() / course.getResolution().get().getHeight();
		}

		for (Target t : course.getTargets()) {
			if (scaleCourse) {
				final double newWidth = t.getDimension().getWidth() * widthScaleFactor;
				final double widthDelta = newWidth - t.getDimension().getWidth();
				final double newX = t.getBoundsInParent().getMinX() * widthScaleFactor;
				final double deltaX = newX - t.getBoundsInParent().getMinX() + (widthDelta / 2);

				final double newHeight = t.getDimension().getHeight() * heightScaleFactor;
				final double heightDelta = newHeight - t.getDimension().getHeight();
				final double newY = t.getBoundsInParent().getMinY() * heightScaleFactor;
				final double deltaY = newY - t.getBoundsInParent().getMinY() + (heightDelta / 2);

				t.setPosition(t.getPosition().getX() + deltaX, t.getPosition().getY() + deltaY);

				t.setDimensions(newWidth, newHeight);
			}

			canvasManager.addTarget((TargetView) t);
		}
	}

	@FXML
	public void canvasKeyPressed(KeyEvent event) throws Exception {
		boolean macFullscreen = event.getCode() == KeyCode.F && event.isControlDown() && event.isShortcutDown();
		if (event.getCode() == KeyCode.F11 || macFullscreen) {
			toggleFullScreen();

			// Manually going full screen with an arena that was manually
			// moved to another screen
			final Optional<Screen> currentArenaScreen = getStageHomeScreen(arenaStage);

			if (!currentArenaScreen.isPresent()) return;

			arenaHome = currentArenaScreen.get();

			final Rectangle2D arenaScreenBounds = currentArenaScreen.get().getBounds();
			arenaScreenOrigin = new Point2D(arenaScreenBounds.getMinX(), arenaScreenBounds.getMinY());

			final boolean fullyManual = !detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen()
					&& !originalArenaHomeScreen.equals(currentArenaScreen.get());
			final boolean movedAfterAuto = detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen()
					&& !detectedProjectorScreen.equals(currentArenaScreen.get());

			if (fullyManual || movedAfterAuto) {
				config.setArenaPosition(arenaStage.getX(), arenaStage.getY());
				try {
					config.writeConfigurationFile();
				} catch (ConfigurationException | IOException e) {
					logger.error("Error writing configuration with arena location", e);
					throw e;
				}
			}

		}
	}

	private void toggleFullScreen() {
		arenaStage.setAlwaysOnTop(!arenaStage.isAlwaysOnTop());
		arenaStage.setFullScreen(!arenaStage.isFullScreen());

		calibrationManager.setFullScreenStatus(arenaStage.isFullScreen());
	}

	public boolean isFullScreen() {
		return arenaStage.isFullScreen();
	}

	public void setTargetsVisible(final boolean visible) {
		for (Target t : canvasManager.getTargets())
			t.setVisible(visible);
	}

	public void setCalibrationMessageVisible(final boolean visible) {
		calibrationLabel.setVisible(visible);
	}

	@FXML
	public void canvasMouseEntered(MouseEvent event) throws IOException {
		arenaAnchor.requestFocus();
	}

	@Override
	public void startCalibration() {
		setTargetsVisible(false);
	}

	private volatile boolean mouseInWindow = false;
	private volatile boolean showingCursorWarning = false;
	private ScheduledFuture<?> mouseExitedFuture = null;

	private CanvasManager feedCanvasManager;

	private void cursorWarningToggle(boolean mouseEntered) {
		if (feedCanvasManager == null || calibrationManager.isCalibrating()) return;

		// If everything is still the same, return
		if (mouseEntered && showingCursorWarning && !calibrationManager.isCalibrating()) return;

		// If the mouse entered OR the mouse is in the window but we haven't
		// been showing the warning, show the warning
		if (mouseEntered || (mouseInWindow && !showingCursorWarning)) {
			mouseInWindow = true;
			if (!calibrationManager.isCalibrating() && arenaStage.isFullScreen()) {
				showingCursorWarning = true;
				mouseOnArenaLabel = feedCanvasManager.addDiagnosticMessage("Cursor On Arena: Shot Detection Disabled",
						15000 /* ms */, Color.YELLOW);

				feedCanvasManager.getCameraManager().setDetecting(false);
			}
		} else if (showingCursorWarning) {
			mouseInWindow = false;

			TimerPool.cancelTimer(mouseExitedFuture);

			mouseExitedFuture = TimerPool.schedule(() -> {
				if (showingCursorWarning) {
					feedCanvasManager.removeDiagnosticMessage(mouseOnArenaLabel);
					showingCursorWarning = false;
					mouseOnArenaLabel = null;
				}

				if (!calibrationManager.isCalibrating()) {
					// Delay restarting shot detection to minimize chance of
					// false shots being detected when the mouse moves
					try {
						Thread.sleep(500 /* ms */);
					} catch (Exception e) {
						logger.error("Exception thrown when re-enabling shot detection due to mouse leaving arena", e);
					}

					feedCanvasManager.getCameraManager().setDetecting(true);
				}
			}, 100 /* ms */);
		}
	}

	@Override
	public void calibrated(Optional<PerspectiveManager> perspectiveManager) {
		setCalibrationMessageVisible(false);
		setTargetsVisible(true);
		restoreCurrentBackground();

		cursorWarningToggle(false);

		this.perspectiveManager = perspectiveManager;

		// Now that we've calibrated and have a new perspective manager,
		// go through all current arena targets and try to resize them
		// to their default real world heights
		if (perspectiveManager.isPresent()) {
			for (Target t : canvasManager.getTargets()) {
				resizeTargetToDefaultPerspective(t);
			}
		}
	}

	public void setFeedCanvasManager(CanvasManager canvasManager) {
		this.feedCanvasManager = canvasManager;

		if (arenaStage != null) {
			arenaStage.getScene().setOnMouseEntered((event) -> {
				cursorWarningToggle(true);
			});

			arenaStage.getScene().setOnMouseExited((event) -> {
				cursorWarningToggle(false);
				this.canvasManager.toggleTargetSelection(Optional.empty());
			});
		}
	}

	public void targetAdded(Target target) {
		resizeTargetToDefaultPerspective(target);

		if (!(target instanceof TargetView)) throw new AssertionError(
				"Target is no longer an instance of TargetView. This code path was not upgraded at some point.");

		final TargetView tv = (TargetView) target;
		final EventHandler<? super MouseEvent> mouseClickedHandler = tv.getTargetGroup().getOnMouseClicked();

		tv.getTargetGroup().setOnMouseClicked((event) -> {
			if (MouseButton.SECONDARY.equals(event.getButton())) {
				final MenuItem setDistanceMenuItem = new MenuItem("Set Target Distance");

				setDistanceMenuItem.setOnAction((e) -> {
					if (perspectiveManager.isPresent()) {
						showTargetResizeDialog(target, event);
					} else {
						showRecalibrationMessage();
					}
				});

				final ContextMenu menu = new ContextMenu(setDistanceMenuItem);
				menu.show(tv.getTargetGroup(), event.getScreenX(), event.getScreenY());
			}

			if (mouseClickedHandler != null) mouseClickedHandler.handle(event);
		});
	}

	private void showTargetResizeDialog(Target target, MouseEvent event) {
		PerspectiveManager pm = perspectiveManager.get();

		TargetDistancePane distanceSettingsPane = new TargetDistancePane(target, pm, config);

		final Stage distanceSettingsStage = new Stage();
		final Scene scene = new Scene(distanceSettingsPane);
		distanceSettingsStage.initOwner(arenaStage);
		distanceSettingsStage.initModality(Modality.WINDOW_MODAL);
		distanceSettingsStage.setTitle("Target Distance Settings");
		distanceSettingsStage.setScene(scene);
		distanceSettingsStage.setX(event.getScreenX());
		distanceSettingsStage.setY(event.getScreenY());
		distanceSettingsStage.showAndWait();

		if (!distanceSettingsPane.userCancelled()) {
			int currentWidth = distanceSettingsPane.getCurrentTargetWidth();
			int currentHeight = distanceSettingsPane.getCurrentTargetHeight();
			int currentDistance = distanceSettingsPane.getCurrentTargetDistance();
			int newDistance = distanceSettingsPane.getNewTargetDistance();

			if (logger.isTraceEnabled()) {
				logger.trace(
						"New target settings from distance settings pane: current width = {}, "
								+ "current height = {}, current distance = {}, new distance = {}",
						currentWidth, currentHeight, currentDistance, newDistance);
			}

			Optional<Dimension2D> targetDimensions = pm.calculateObjectSize(currentWidth, currentHeight,
					currentDistance, newDistance);

			if (targetDimensions.isPresent()) {
				Dimension2D d = targetDimensions.get();
				target.setDimensions(d.getWidth(), d.getHeight());
			}
		}
	}

	private void resizeTargetToDefaultPerspective(Target target) {
		if (perspectiveManager.isPresent() && target.tagExists(Target.TAG_DEFAULT_PERCEIVED_WIDTH)
				&& target.tagExists(Target.TAG_DEFAULT_PERCEIVED_HEIGHT)
				&& target.tagExists(Target.TAG_DEFAULT_PERCEIVED_DISTANCE)) {

			PerspectiveManager pm = perspectiveManager.get();

			pm.setShooterDistance(pm.getCameraDistance());

			if (pm.isInitialized()) {
				int width = Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_WIDTH));
				int height = Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_HEIGHT));
				int distance = Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_DISTANCE));

				Optional<Dimension2D> targetDimensions = pm.calculateObjectSize(width, height, distance, distance);

				if (targetDimensions.isPresent()) {
					Dimension2D d = targetDimensions.get();
					target.setDimensions(d.getWidth(), d.getHeight());
				}
			}
		}
	}

	private void showRecalibrationMessage() {
		Alert recalibrationAlert = new Alert(AlertType.ERROR);

		String message = "Data required to set a target's distance is missing and there is no way "
				+ "to determine the correct values unless you recalibrate the arena with the perspective "
				+ "calibration pattern.\n\nPlease print out this file and tape it to your wall or "
				+ "projector screen in view of the camera pointed at your projection:\n\n"
				+ System.getProperty("shootoff.home") + File.separator + "targets" + File.separator
				+ "PerspectiveCalibrationPattern.pdf\n\nThen hit Projector -> Start Calibrating.";

		recalibrationAlert.setTitle("Missing Perspective Data");
		recalibrationAlert.setHeaderText("Critical Distance Data Missing!");
		recalibrationAlert.setResizable(true);
		recalibrationAlert.setContentText(message);
		recalibrationAlert.initOwner(arenaStage);
		recalibrationAlert.showAndWait();
	}
}
