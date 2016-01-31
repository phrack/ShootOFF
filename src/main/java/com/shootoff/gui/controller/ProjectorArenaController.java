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

package com.shootoff.gui.controller;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.courses.Course;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;
import com.shootoff.util.TimerPool;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ProjectorArenaController implements CalibrationListener {
	private static final Logger logger = LoggerFactory.getLogger(ProjectorArenaController.class);

	private Stage arenaStage;
	private Stage shootOFFStage;
	@FXML private AnchorPane arenaAnchor;
	@FXML private Group arenaCanvasGroup;
	@FXML private Label calibrationLabel;

	private Configuration config;
	private CanvasManager canvasManager;
	private Label mouseOnArenaLabel = null;
	private Optional<LocatedImage> background = Optional.empty();
	private Optional<LocatedImage> savedBackground = Optional.empty();

	private Screen originalArenaHomeScreen;
	private Optional<Screen> detectedProjectorScreen = Optional.empty();

	private ShootOFFController shootOFFController;

	// Used for testing
	public void init(Configuration config, CanvasManager canvasManager) {
		this.config = config;
		this.canvasManager = canvasManager;

		arenaStage = new Stage();
		arenaAnchor = new AnchorPane(canvasManager.getCanvasGroup());
		Scene scene = new Scene(arenaAnchor, 500, 500);
		arenaStage.setScene(scene);
	}

	public void init(ShootOFFController shootOFFController, Configuration config, CamerasSupervisor camerasSupervisor) {
		this.config = config;

		this.shootOFFController = shootOFFController;

		shootOFFStage = shootOFFController.getStage();
		arenaStage = (Stage) arenaAnchor.getScene().getWindow();

		canvasManager = new CanvasManager(arenaCanvasGroup, config, camerasSupervisor, "arena", null);
		canvasManager.updateBackground(null, Optional.empty());

		arenaAnchor.widthProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		arenaAnchor.heightProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		arenaAnchor.setStyle("-fx-background-color: #333333;");
	}

	private Optional<Screen> getStageHomeScreen(Stage stage) {
		ObservableList<Screen> stageHomeScreens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1);

		if (stageHomeScreens.isEmpty()) {
			StringBuilder message = new StringBuilder(
					String.format("Didn't find screen for stage with title %s at (%f, %f)." + " Existing screens: %n%n",
							stage.getTitle(), stage.getX(), stage.getY()));

			Iterator<Screen> it = Screen.getScreens().iterator();

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

			Point2D arenaPosition = config.getArenaPosition().get();

			ObservableList<Screen> screens = Screen.getScreensForRectangle(arenaPosition.getX(), arenaPosition.getY(),
					1, 1);

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

			Screen shootOFFScreen = homeScreen.get();

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
			Screen arenaHome = projector.get();

			double newX = arenaHome.getVisualBounds().getMinX();
			double newY = arenaHome.getVisualBounds().getMinY();

			logger.debug("Found likely projector screen: resolution = {}x{}, newX = {}, newY = {}",
					arenaHome.getBounds().getWidth(), arenaHome.getBounds().getHeight(), newX, newY);

			arenaStage.setX(newX + 10);
			arenaStage.setY(newY + 10);

			detectedProjectorScreen = projector;

			Platform.runLater(() -> toggleFullScreen());
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

	public void setCourse(Course course) {
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
				double newWidth = t.getDimension().getWidth() * widthScaleFactor;
				double widthDelta = newWidth - t.getDimension().getWidth();
				double newX = t.getTargetGroup().getBoundsInParent().getMinX() * widthScaleFactor;
				double deltaX = newX - t.getTargetGroup().getBoundsInParent().getMinX() + (widthDelta / 2);
			
				double newHeight = t.getDimension().getHeight() * heightScaleFactor;
				double heightDelta = newHeight - t.getDimension().getHeight();
				double newY = t.getTargetGroup().getBoundsInParent().getMinY() * heightScaleFactor;
				double deltaY = newY - t.getTargetGroup().getBoundsInParent().getMinY() + (heightDelta / 2);
				
				t.setPosition(t.getPosition().getX() + deltaX, t.getPosition().getY() + deltaY);
				
				t.setDimensions(newWidth, newHeight);
			}

			canvasManager.addTarget(t);
		}
	}

	@FXML
	public void canvasKeyPressed(KeyEvent event) throws Exception {
		boolean macFullscreen = event.getCode() == KeyCode.F && event.isControlDown() && event.isShortcutDown();
		if (event.getCode() == KeyCode.F11 || macFullscreen) {
			// Manually going full screen with an arena that was manually
			// moved to another screen
			Optional<Screen> currentArenaScreen = getStageHomeScreen(arenaStage);

			if (!currentArenaScreen.isPresent()) return;

			boolean fullyManual = !detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen()
					&& !originalArenaHomeScreen.equals(currentArenaScreen.get());
			boolean movedAfterAuto = detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen()
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

			toggleFullScreen();
		}
	}

	private void toggleFullScreen() {
		arenaStage.setAlwaysOnTop(!arenaStage.isAlwaysOnTop());
		arenaStage.setFullScreen(!arenaStage.isFullScreen());

		shootOFFController.setFullScreenStatus(arenaStage.isFullScreen());
	}

	public boolean isFullScreen() {
		return arenaStage.isFullScreen();
	}

	public void setTargetsVisible(boolean visible) {
		for (Target t : canvasManager.getTargets())
			t.getTargetGroup().setVisible(visible);
	}

	public void setCalibrationMessageVisible(boolean visible) {
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
		if (feedCanvasManager == null || shootOFFController.isCalibrating()) return;

		// If everything is still the same, return
		if (mouseEntered && showingCursorWarning && !shootOFFController.isCalibrating()) return;

		// If the mouse entered OR the mouse is in the window but we haven't
		// been showing the warning, show the warning
		if (mouseEntered || (mouseInWindow && !showingCursorWarning)) {
			Platform.runLater(new Runnable() {
				public void run() {
					mouseInWindow = true;
					if (!shootOFFController.isCalibrating() && arenaStage.isFullScreen()) {
						showingCursorWarning = true;
						mouseOnArenaLabel = feedCanvasManager.addDiagnosticMessage(
								"Cursor On Arena: Shot Detection Disabled", 15000 /* ms */, Color.YELLOW);

						feedCanvasManager.getCameraManager().setDetecting(false);
					}
				}
			});
		} else if (!mouseEntered && showingCursorWarning) {
			mouseInWindow = false;

			TimerPool.cancelTimer(mouseExitedFuture);

			mouseExitedFuture = TimerPool.schedule(() -> {
				Platform.runLater(() -> {
					if (showingCursorWarning) {
						feedCanvasManager.removeDiagnosticMessage(mouseOnArenaLabel);
						showingCursorWarning = false;
						mouseOnArenaLabel = null;
					}
					if (!shootOFFController.isCalibrating()) feedCanvasManager.getCameraManager().setDetecting(true);
				});
			} , 100 /* ms */);
		}
	}

	@Override
	public void calibrated() {
		setCalibrationMessageVisible(false);
		setTargetsVisible(true);
		restoreCurrentBackground();

		cursorWarningToggle(false);
	}

	public void setFeedCanvasManager(CanvasManager canvasManager) {
		this.feedCanvasManager = canvasManager;

		arenaStage.getScene().setOnMouseEntered((event) -> {
			cursorWarningToggle(true);
		});

		arenaStage.getScene().setOnMouseExited((event) -> {
			cursorWarningToggle(false);
		});
	}
}
