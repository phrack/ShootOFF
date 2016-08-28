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
import com.shootoff.gui.TargetView;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.targets.Target;
import com.shootoff.util.TimerPool;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
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
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ProjectorArenaPane extends AnchorPane implements CalibrationListener, Closeable {
	private static final Logger logger = LoggerFactory.getLogger(ProjectorArenaPane.class);

	protected Stage arenaStage;
	private Stage shootOffStage;
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

	private CalibrationManager calibrationManager;
	private Optional<PerspectiveManager> perspectiveManager = Optional.empty();


	// Used for testing
	public ProjectorArenaPane(Configuration config, CanvasManager canvasManager) {
		this.config = config;
		this.canvasManager = canvasManager;
		this.arenaCanvasGroup = new Group();
		this.calibrationLabel = null;
	}

	public ProjectorArenaPane(Stage arenaStage, Stage shootOffStage, Configuration config, Resetter resetter) {
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
		
		canvasManager = new MirroredCanvasManager(arenaCanvasGroup, config, resetter, "arena", null);

		this.setPrefSize(640, 480);
		
		this.setOnKeyPressed((event) -> canvasKeyPressed(event));
		
		this.setOnMouseEntered((event) -> requestFocus());
		
		this.setOnMouseClicked((event) -> {
			canvasManager.toggleTargetSelection(Optional.empty());
		});

		this.widthProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		this.heightProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(getWidth(), getHeight());
		});

		this.setStyle("-fx-background-color: #333333;");
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
		arenaStage.close();
		TimerPool.cancelTimer(mouseExitedFuture);
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
			savedBackground = background;
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

			canvasManager.addTarget((TargetView) t);
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

	/**
	 * This methods is used by
	 * {@link com.shootoff.gui.controller.ShootOFFController} to notify the
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

	private void showTargetResizeDialog(Target target, MouseEvent event) {
		PerspectiveManager pm = perspectiveManager.get();

		TargetDistancePane distanceSettingsPane = new TargetDistancePane(target, pm, config);

		final Stage distanceSettingsStage = new Stage();
		final Scene scene = new Scene(distanceSettingsPane);
		distanceSettingsStage.initOwner(shootOffStage);
		distanceSettingsStage.initModality(Modality.WINDOW_MODAL);
		distanceSettingsStage.setTitle("Target Distance Settings");
		distanceSettingsStage.setScene(scene);
		distanceSettingsStage.showAndWait();

		if (!distanceSettingsPane.userCancelled()) {
			int width = distanceSettingsPane.getDefaultTargetWidth();
			int height = distanceSettingsPane.getDefaultTargetHeight();
			int currentDistance = distanceSettingsPane.getCurrentTargetDistance();
			int newDistance = distanceSettingsPane.getNewTargetDistance();

			if (logger.isTraceEnabled()) {
				logger.trace(
						"New target settings from distance settings pane: current width = {}, "
								+ "default height = {}, default distance = {}, new distance = {}",
						width, height, currentDistance, newDistance);
			}

			Optional<Dimension2D> targetDimensions = pm.calculateObjectSize(width, height, newDistance);

			if (targetDimensions.isPresent()) {
				Dimension2D d = targetDimensions.get();
				target.setDimensions(d.getWidth(), d.getHeight());
			}
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
