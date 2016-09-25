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

package com.shootoff.gui.pane;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Closeable;
import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.courses.Course;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.MirroredCanvasManager;
import com.shootoff.gui.Resetter;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.targets.Target;
import com.shootoff.util.TimerPool;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Pair;

public class ProjectorArenaPane extends AnchorPane implements CalibrationListener, Closeable {
	private static final Logger logger = LoggerFactory.getLogger(ProjectorArenaPane.class);

	protected Stage arenaStage;
	private final Stage shootOffStage;
	private final Pane trainingExerciseContainer;
	private final Configuration config;
	private final Group arenaCanvasGroup;
	private final Label calibrationLabel;
	private final CanvasManager canvasManager;

	private Label mouseOnArenaLabel = null;
	private Optional<LocatedImage> background = Optional.empty();
	private Optional<LocatedImage> savedBackground = Optional.empty();

	private Screen originalArenaHomeScreen;
	private Optional<Screen> detectedProjectorScreen = Optional.empty();
	private Point2D arenaScreenOrigin = new Point2D(0, 0);
	private Screen arenaHome;

	private boolean calibrated = false;
	private CalibrationManager calibrationManager;
	private Optional<PerspectiveManager> perspectiveManager = Optional.empty();
	private Pair<Target, TargetDistancePane> openDistancePane;
	private boolean showedRecalibrationMessage = false;

	private ProjectorArenaPane mirroredArenaPane;
	
	// Used for testing
	public ProjectorArenaPane(Configuration config, CanvasManager canvasManager) {
		this.config = config;
		this.canvasManager = canvasManager;
		this.shootOffStage = null;
		this.trainingExerciseContainer = null;
		this.arenaCanvasGroup = new Group();
		this.calibrationLabel = null;
	}

	public ProjectorArenaPane(Stage arenaStage, Stage shootOffStage, Pane trainingExerciseContainer,
			Configuration config, Resetter resetter, ObservableList<ShotEntry> shotTimerModel) {
		this.config = config;

		arenaCanvasGroup = new Group();
		calibrationLabel = new Label("Needs Calibration");
		calibrationLabel.setFont(Font.font(48));
		calibrationLabel.setTextFill(Color.web("#f5a807"));
		calibrationLabel.setLayoutX(6);
		calibrationLabel.setLayoutY(6);
		calibrationLabel.setPrefSize(628, 90);
		calibrationLabel.setAlignment(Pos.CENTER);
				
		this.getChildren().addAll(arenaCanvasGroup, calibrationLabel);
		
		this.shootOffStage = shootOffStage;
		this.arenaStage = arenaStage;
		this.trainingExerciseContainer = trainingExerciseContainer;
		
		if (shotTimerModel == null) {
			canvasManager = new MirroredCanvasManager(arenaCanvasGroup, config, resetter, "arena", null, this);
		} else {
			canvasManager = new MirroredCanvasManager(arenaCanvasGroup, config, resetter, "arena",
					shotTimerModel, this);
		}

		this.setPrefSize(640, 480);
		
		this.setOnKeyPressed((event) -> canvasKeyPressed(event));
		
		this.setOnMouseEntered((event) -> requestFocus());
		
		this.setOnMouseClicked((event) -> {
			canvasManager.toggleTargetSelection(Optional.empty());
		});

		this.widthProperty().addListener((e) -> {
			// This can happen because the mirrored pane in the tab will
			// grow the scroll pane it is in. This stretches the background
			// in weird ways when a target goes off screen. This condition
			// ensures the scroll pane never gets bigger than the separate
			// window that is the real arena.
			if (calibrated && getWidth() != arenaStage.getWidth()) return;
			
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		this.heightProperty().addListener((e) -> {
			if (calibrated && getHeight() != arenaStage.getHeight()) return;
			
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		this.setStyle("-fx-background-color: #333333;");
	}
	
	public void setArenaPaneMirror(ProjectorArenaPane mirroredArenaPane) {
		this.mirroredArenaPane = mirroredArenaPane;
	}
	
	public ProjectorArenaPane getArenaPaneMirror() {
		return mirroredArenaPane;
	}

	public void setCalibrationManager(CalibrationManager calibrationManager) {
		this.calibrationManager = calibrationManager;
	}
	

	private Optional<Screen> getStageHomeScreen(Stage stage) {
		final double dpiScaleFactor = ShootOFFController.getDpiScaleFactorForScreen();
		
		final ObservableList<Screen> stageHomeScreens = Screen.getScreensForRectangle(stage.getX() / dpiScaleFactor, stage.getY() / dpiScaleFactor, 1, 1);

		if (stageHomeScreens.isEmpty()) {
			final StringBuilder message = new StringBuilder(
					String.format("Didn't find screen for stage with title %s at (%f, %f)." + " Existing screens:%n",
							stage.getTitle(), stage.getX() / dpiScaleFactor, stage.getY() / dpiScaleFactor));

			final Iterator<Screen> it = Screen.getScreens().iterator();

			while (it.hasNext()) {
				Screen s = it.next();

				message.append(String.format("(w = %f, h = %f, x = %f, y = %f, dpi = %f)", s.getBounds().getWidth(),
						s.getBounds().getHeight(), s.getBounds().getMinX(), s.getBounds().getMinY(), s.getDpi()));

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
				
				boolean matchedOriginal = false;
				for (Screen screen : screens)
				{
					if (originalArenaHomeScreen.equals(screen))
					{
						logger.debug("Stored arena coordinates are on current home screen");
						matchedOriginal = true;
					}
				}
				
				if (!matchedOriginal)
				{
					arenaStage.setX(arenaPosition.getX());
					arenaStage.setY(arenaPosition.getY());

					Platform.runLater(() -> toggleFullScreen());
					
					arenaHome = screens.get(0);

					setArenaScreenOrigin(arenaHome);
					
					return;
				}

			} else {
				logger.debug("Saved screen coordinates ({}, {}) no longer exists, attempting fallback approaches...",
						arenaPosition.getX(), arenaPosition.getY());
			}
		}

		Optional<Screen> projector = Optional.empty();

		if (Screen.getScreens().size() == 2) {
			logger.debug("Two screens present");

			homeScreen = getStageHomeScreen(shootOffStage);

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
			final double dpiScaleFactor = ShootOFFController.getDpiScaleFactorForScreen();
			
			arenaHome = projector.get();

			final double newX = arenaHome.getBounds().getMinX() * dpiScaleFactor;
			final double newY = arenaHome.getBounds().getMinY() * dpiScaleFactor;

			logger.debug("Found likely projector screen: resolution = {}x{}, newX = {}, newY = {}",
					arenaHome.getBounds().getWidth(), arenaHome.getBounds().getHeight(), newX, newY);

			arenaStage.setX(newX + 10);
			arenaStage.setY(newY + 10);

			detectedProjectorScreen = projector;
			
			setArenaScreenOrigin(arenaHome);

			Platform.runLater(() -> toggleFullScreen());

		} else {
			logger.debug("Did not find screen that is a likely projector");
		}
	}

	public void toggleArena() {
		if (arenaStage.isShowing()) {
			arenaStage.hide();
		} else {
			arenaStage.show();
		}
	}
	
	public Screen getArenaHome() {
		return arenaHome;
	}

	public Dimension2D getArenaStageResolution() {
		return new Dimension2D(arenaStage.getWidth(), arenaStage.getHeight());
	}
	
	private void setArenaScreenOrigin(Screen screen) {
		final double dpiScaleFactor = ShootOFFController.getDpiScaleFactorForScreen();
		final Rectangle2D arenaScreenBounds = screen.getBounds();
		arenaScreenOrigin = new Point2D(arenaScreenBounds.getMinX() * dpiScaleFactor, arenaScreenBounds.getMinY() * dpiScaleFactor);
		
		logger.debug("Set arenaScreenOrigin to {}", arenaScreenOrigin);
	}

	public Point2D getArenaScreenOrigin() {
		return arenaScreenOrigin;
	}

	public CanvasManager getCanvasManager() {
		return canvasManager;
	}

	public Optional<PerspectiveManager> getPerspectiveManager() {
		return perspectiveManager;
	}

	@Override
	public void close() {
		if (feedCanvasManager != null) {
			feedCanvasManager.removeDiagnosticMessage(mouseOnArenaLabel);
		}
		
		TimerPool.cancelTimer(mouseExitedFuture);
		
		if (Platform.isFxApplicationThread()) {
			arenaStage.close();
		} else {
			Platform.runLater(() -> arenaStage.close());
		}
	}

	public void setArenaBackground(LocatedImage img) {
		background = Optional.ofNullable(img);
		canvasManager.updateBackground(img);
	}

	/**
	 * Used to temporarily save the background before autocalibration
	 */
	public void saveCurrentBackground() {
		if (background.isPresent()) {
			savedBackground = Optional.of(background.get());
		}
	}

	/**
	 * Used to restore the background that was saved before autocalibration with
	 * saveCurrentBackground.
	 */
	public void restoreCurrentBackground() {
		if (savedBackground.isPresent()) {
			setArenaBackground(savedBackground.get());
			savedBackground = Optional.empty();
		} else {
			setArenaBackground(null);
		}
	}

	public Optional<LocatedImage> getArenaBackground() {
		return background;
	}

	public Configuration getConfiguration() {
		return config;
	}

	public void setCourse(final Course course) {
		if (course.getBackground().isPresent()) {
			setArenaBackground(course.getBackground().get());
		} else {
			setArenaBackground(null);
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

			canvasManager.addTarget(t);
		}
	}

	public void canvasKeyPressed(KeyEvent event) {
		boolean macFullscreen = event.getCode() == KeyCode.F && event.isControlDown() && event.isShortcutDown();
		if (event.getCode() == KeyCode.F11 || macFullscreen) {
			toggleFullScreen();

			// Manually going full screen with an arena that was manually
			// moved to another screen
			final Optional<Screen> currentArenaScreen = getStageHomeScreen(arenaStage);

			if (!currentArenaScreen.isPresent()) return;

			arenaHome = currentArenaScreen.get();

			setArenaScreenOrigin(arenaHome);

			final boolean fullyManual = !detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen()
					&& !originalArenaHomeScreen.equals(currentArenaScreen.get());
			final boolean movedAfterAuto = detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen()
					&& !detectedProjectorScreen.equals(currentArenaScreen.get());

			if (fullyManual || movedAfterAuto) {
				final double dpiScaleFactor = ShootOFFController.getDpiScaleFactorForScreen();
								
				config.setArenaPosition(arenaStage.getX() / dpiScaleFactor, arenaStage.getY() / dpiScaleFactor);
				try {
					config.writeConfigurationFile();
				} catch (ConfigurationException | IOException e) {
					logger.error("Error writing configuration with arena location", e);
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
	
	@Override
	public void startCalibration() {
		calibrated = false;
		setTargetsVisible(false);
		
		if (mirroredArenaPane != null) mirroredArenaPane.mirroredStartCalibration();
	}
	
	public void mirroredStartCalibration() {
		startCalibration();
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
				feedCanvasManager.getCameraManager().setDetectionLockState(true);
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

					feedCanvasManager.getCameraManager().setDetectionLockState(false);
					feedCanvasManager.getCameraManager().setDetecting(true);
				}
			}, 100 /* ms */);
		}
	}

	@Override
	public void calibrated(Optional<PerspectiveManager> perspectiveManager) {
		calibrated = true;
		setCalibrationMessageVisible(false);
		setTargetsVisible(true);
		
		cursorWarningToggle(false);

		this.perspectiveManager = perspectiveManager;

		// Now that we've calibrated and have a new perspective manager,
		// go through all current arena targets and try to resize them
		// to their default real world heights
		if (perspectiveManager.isPresent()) {
			for (Target t : canvasManager.getTargets()) {
				resizeTargetToDefaultPerspective(t);
			}
			
			if (perspectiveManager.get().isInitialized() && mirroredArenaPane != null) {
				// Do not play a chime for this message
				final Label successLabel = mirroredArenaPane.getCanvasManager().addDiagnosticMessage(
						"Perspective Fully Initialized -- Using Real World Distances", -1, Color.LIMEGREEN);
				
				TimerPool.schedule(() -> mirroredArenaPane.getCanvasManager().removeDiagnosticMessage(successLabel), 5000); 
			}
		}
		
		if (mirroredArenaPane != null) mirroredArenaPane.mirrorCalibrated(perspectiveManager);
	}
	
	public void mirrorCalibrated(Optional<PerspectiveManager> perspectiveManager) {
		calibrated(perspectiveManager);
	}

	/**
	 * This methods is used by
	 * {@link com.shootoff.gui.MirroredCanvasArena} to notify the
	 * arena controller of new targets added to the arena. Without this method
	 * the targets would be added directly to the arena's canvas manager,
	 * bypassing the arena controller. Thus, the arena controller would not be
	 * able to configure arena-specific target operations (e.g. setting a
	 * targets distance).
	 * 
	 * @param target
	 *            a new target that was just added to the projector arena
	 */
	public void targetAdded(Target target) {
		resizeTargetToDefaultPerspective(target);
	
		target.setTargetSelectionListener((toggledTarget, isSelected) -> {
			if (!isSelected) {
				if (perspectiveManager.isPresent() && openDistancePane != null)
					trainingExerciseContainer.getChildren().remove(openDistancePane.getValue());
				return;
			}

			if (perspectiveManager.isPresent()) {
				if (openDistancePane != null) trainingExerciseContainer.getChildren().remove(openDistancePane.getValue());

				openDistancePane = new Pair<>(target, new TargetDistancePane(toggledTarget, perspectiveManager.get(), config));

				trainingExerciseContainer.getChildren().add(openDistancePane.getValue());
			} else {
				showRecalibrationMessage();
			}
		});
		
		if (mirroredArenaPane != null) mirroredArenaPane.mirrorTargetAdded(target);
	}
	
	public void mirrorTargetAdded(Target target) {
		targetAdded(target);
	}
	
	public void targetRemoved(Target target) {
		if (openDistancePane != null && target.equals(openDistancePane.getKey())) 
			trainingExerciseContainer.getChildren().remove(openDistancePane.getValue());
	}

	/**
	 * Used by the {@link com.shootoff.gui.CalibrationManager} to notify the
	 * arena controller of the canvas manager that belongs to the camera used to
	 * detect shots on the arena. This particular canvas manager is used by the
	 * arena controller to display arena specific diagnostic messages on the
	 * webcam feed pointed at the projection. It is also used to disable shot
	 * detection when necessary to enhance user experience (e.g. when the mouse
	 * cursor is on the arena because a user is resizing targets).
	 * 
	 * @param canvasManager
	 *            the canvas manager showing the webcam feed pointed at the
	 *            projection for the projection arena controlled by this class
	 */
	public void setFeedCanvasManager(CanvasManager canvasManager) {
		this.feedCanvasManager = canvasManager;

		if (arenaStage != null) {
			arenaStage.getScene().setOnMouseMoved((event) -> {
				// Only disable shot detection if the cursor is actually
				// over the stage. We may inject mouse movements later to
				// allow targets to be moved around on the arena from the
				// calibrated camera feed.
				if (event.getScreenX() >= arenaStage.getX() && event.getScreenX() < arenaStage.getX() + getWidth()
						&& event.getScreenY() >= arenaStage.getY()
						&& event.getScreenY() < arenaStage.getY() + getHeight()) {
					cursorWarningToggle(true);
				}
			});

			arenaStage.getScene().setOnMouseEntered((event) -> {
				cursorWarningToggle(true);
			});

			arenaStage.getScene().setOnMouseExited((event) -> {
				cursorWarningToggle(false);
				this.canvasManager.toggleTargetSelection(Optional.empty());
			});
		}
	}

	public void resizeTargetToDefaultPerspective(Target target) {
		if (perspectiveManager.isPresent() && target.tagExists(Target.TAG_DEFAULT_PERCEIVED_WIDTH)
				&& target.tagExists(Target.TAG_DEFAULT_PERCEIVED_HEIGHT)
				&& target.tagExists(Target.TAG_DEFAULT_PERCEIVED_DISTANCE)) {

			PerspectiveManager pm = perspectiveManager.get();

			if (pm.getShooterDistance() == -1) pm.setShooterDistance(pm.getCameraDistance());

			if (pm.isInitialized()) {
				int width = Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_WIDTH));
				int height = Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_HEIGHT));
				int distance = Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_DISTANCE));

				Optional<Dimension2D> targetDimensions = pm.calculateObjectSize(width, height, distance);

				if (targetDimensions.isPresent()) {
					Dimension2D d = targetDimensions.get();
					target.setDimensions(d.getWidth(), d.getHeight());
				}
			}
		}
	}

	private void showRecalibrationMessage() {
		if (showedRecalibrationMessage) return;
		
		final Alert recalibrationAlert = new Alert(AlertType.ERROR);

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
		if (shootOffStage != null) recalibrationAlert.initOwner(shootOffStage);
		recalibrationAlert.showAndWait();
		
		showedRecalibrationMessage = true;
	}
}
