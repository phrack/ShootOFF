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

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraCalibrationListener;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.util.TimerPool;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class CalibrationManager implements CameraCalibrationListener {
	private static final int MAX_AUTO_CALIBRATION_TIME = 10 * 1000;
	private static final Logger logger = LoggerFactory.getLogger(CalibrationManager.class);

	private final CalibrationConfigurator calibrationConfigurator;
	private final CameraManager calibratingCameraManager;
	private final CanvasManager calibratingCanvasManager;
	private final CalibrationListener calibrationListener;
	private final ProjectorArenaController arenaController;

	private ScheduledFuture<?> autoCalibrationFuture = null;

	private Optional<TargetView> calibrationTarget = Optional.empty();
	private Optional<Dimension2D> perspectivePaperDims = Optional.empty();

	private final AtomicBoolean isCalibrating = new AtomicBoolean(false);
	private final AtomicBoolean isShowingPattern = new AtomicBoolean(false);

	public CalibrationManager(CalibrationConfigurator calibrationConfigurator, CameraManager calibratingCameraManager,
			ProjectorArenaController arenaController) {
		this.calibrationConfigurator = calibrationConfigurator;
		this.calibratingCameraManager = calibratingCameraManager;
		calibratingCanvasManager = (CanvasManager) calibratingCameraManager.getCameraView();
		this.calibrationListener = (CalibrationListener) arenaController;
		this.arenaController = arenaController;

		arenaController.setFeedCanvasManager(calibratingCanvasManager);
		calibratingCameraManager.setCalibrationManager(this);
	}

	public void enableCalibration() {
		isCalibrating.set(true);

		calibrationConfigurator.toggleCalibrating();

		// Sets calibrating and not detecting
		calibratingCameraManager.setCalibrating(true);
		calibratingCameraManager.setProjectionBounds(null);

		if (arenaController.isFullScreen()) {
			enableAutoCalibration();
		} else {
			showFullScreenRequest();
		}
	}

	public void stopCalibration() {
		isCalibrating.set(false);

		if (calibrationTarget.isPresent())
			calibrate(calibrationTarget.get().getTargetGroup().getBoundsInParent(), Optional.empty(), true);

		calibratingCameraManager.disableAutoCalibration();

		TimerPool.cancelTimer(autoCalibrationFuture);

		calibrationConfigurator.toggleCalibrating();

		removeFullScreenRequest();
		removeAutoCalibrationMessage();
		removeManualCalibrationRequestMessage();
		removeCalibrationTargetIfPresent();

		PerspectiveManager pm = null;
		if (PerspectiveManager.isCameraSupported(calibratingCameraManager.getName())
				&& calibratingCameraManager.getFeedWidth() == 1280 && calibratingCameraManager.getFeedHeight() == 720) {
			if (perspectivePaperDims.isPresent()) {
				pm = new PerspectiveManager(calibratingCameraManager.getName(),
						calibratingCameraManager.getProjectionBounds().get(),
						new Dimension2D(calibratingCameraManager.getFeedWidth(),
								calibratingCameraManager.getFeedHeight()),
						perspectivePaperDims.get());
			} else {
				pm = new PerspectiveManager(calibratingCameraManager.getName(),
						calibratingCameraManager.getProjectionBounds().get());

				pm.setCameraFeedSize(calibratingCameraManager.getFeedWidth(), calibratingCameraManager.getFeedHeight());
			}

			// Should come from config
			pm.setShooterDistance(3406);

			// If no pattern to work with, and no camera parameters to work
			// with, we
			// can't calculate both
			// So we guess (for now) that the camera distance is 3580mm
			if (!perspectivePaperDims.isPresent() && !pm.isCameraParamsKnown()) {
				// TODO: Prompt the user to enter a distance
				pm.setCameraDistance(3580);
			} else if (pm.isCameraParamsKnown()) {
				pm.setCameraDistance(-1);
			}
		}

		calibrationListener.calibrated(Optional.ofNullable(pm));

		calibratingCameraManager.setCalibrating(false);

		isShowingPattern.set(false);

		// We disable shot detection briefly because the pattern going away can
		// cause false shots
		// This statement applies to all the cam feeds rather than just the
		// arena. I don't think that should
		// be a problem?
		calibrationConfigurator.disableShotDetection(400);
	}

	@Override
	public void calibrate(Bounds arenaBounds, Optional<Dimension2D> perspectivePaperDims,
			boolean calibratedFromCanvas) {
		removeCalibrationTargetIfPresent();

		if (!calibratedFromCanvas) arenaBounds = calibratingCanvasManager.translateCameraToCanvas(arenaBounds);
		createCalibrationTarget(arenaBounds.getMinX(), arenaBounds.getMinY(), arenaBounds.getWidth(),
				arenaBounds.getHeight());

		configureArenaCamera(calibrationConfigurator.getSelectedCalibrationOption(), arenaBounds);
		this.perspectivePaperDims = perspectivePaperDims;

		if (isCalibrating()) stopCalibration();
	}

	public void configureArenaCamera(CalibrationOption option) {
		calibratingCameraManager.setCropFeedToProjection(CalibrationOption.CROP.equals(option));
		calibratingCameraManager.setLimitDetectProjection(CalibrationOption.ONLY_IN_BOUNDS.equals(option));
	}

	public void arenaClosing() {
		calibratingCameraManager.setProjectionBounds(null);
	}

	private void createCalibrationTarget(double x, double y, double width, double height) {
		RectangleRegion calibrationRectangle = new RectangleRegion(x, y, width, height);
		calibrationRectangle.setFill(Color.PURPLE);
		calibrationRectangle.setOpacity(TargetIO.DEFAULT_OPACITY);

		Group calibrationGroup = new Group();
		calibrationGroup.setOnMouseClicked((e) -> {
			calibrationGroup.requestFocus();
		});
		calibrationGroup.getChildren().add(calibrationRectangle);

		calibrationTarget = Optional.of((TargetView) calibratingCanvasManager.addTarget(null, calibrationGroup, false));
		calibrationTarget.get().setKeepInBounds(true);
	}

	private void removeCalibrationTargetIfPresent() {
		if (calibrationTarget.isPresent()) {
			calibratingCanvasManager.removeTarget(calibrationTarget.get());
			calibrationTarget = Optional.empty();
		}
	}

	private void configureArenaCamera(CalibrationOption option, Bounds bounds) {
		Bounds translatedToCameraBounds = calibratingCanvasManager.translateCanvasToCamera(bounds);

		calibratingCanvasManager.setProjectorArena(arenaController, bounds);
		configureArenaCamera(option);
		calibratingCameraManager.setProjectionBounds(translatedToCameraBounds);
	}

	private void enableManualCalibration() {
		logger.trace("enableManualCalibration");

		final int DEFAULT_DIM = 75;
		final int DEFAULT_POS = 150;

		removeAutoCalibrationMessage();

		showManualCalibrationRequestMessage();

		if (!calibrationTarget.isPresent()) {
			createCalibrationTarget(DEFAULT_DIM, DEFAULT_DIM, DEFAULT_POS, DEFAULT_POS);
		} else {
			calibratingCameraManager.getCameraView().addTarget(calibrationTarget.get());
		}
	}

	private void disableManualCalibration() {
		removeCalibrationTargetIfPresent();

		removeManualCalibrationRequestMessage();
	}

	private Label manualCalibrationRequestMessage = null;
	private volatile boolean showingManualCalibrationRequestMessage = false;

	private void showManualCalibrationRequestMessage() {
		if (showingManualCalibrationRequestMessage) return;

		showingManualCalibrationRequestMessage = true;
		manualCalibrationRequestMessage = calibratingCanvasManager
				.addDiagnosticMessage("Please manually calibrate the projection region", 20000, Color.ORANGE);
	}

	private void removeManualCalibrationRequestMessage() {
		logger.trace("removeFullScreenRequest {}", manualCalibrationRequestMessage);

		if (showingManualCalibrationRequestMessage) {
			showingManualCalibrationRequestMessage = false;
			calibratingCanvasManager.removeDiagnosticMessage(manualCalibrationRequestMessage);
			manualCalibrationRequestMessage = null;
		}
	}

	private Label fullScreenRequestMessage = null;
	private volatile boolean showingFullScreenRequestMessage = false;

	private void showFullScreenRequest() {
		if (showingFullScreenRequestMessage) return;

		showingFullScreenRequestMessage = true;
		fullScreenRequestMessage = calibratingCanvasManager
				.addDiagnosticMessage("Please move the arena to your projector and hit F11", Color.YELLOW);
	}

	private void removeFullScreenRequest() {
		logger.trace("removeFullScreenRequest {}", fullScreenRequestMessage);

		if (showingFullScreenRequestMessage) {
			showingFullScreenRequestMessage = false;

			calibratingCanvasManager.removeDiagnosticMessage(fullScreenRequestMessage);
			fullScreenRequestMessage = null;
		}
	}

	private void enableAutoCalibration() {
		logger.trace("enableAutoCalibration");

		calibrationListener.startCalibration();
		arenaController.setCalibrationMessageVisible(false);
		// We may already be calibrating if the user decided to move the arena
		// to another screen while calibrating. If we save the background in
		// that case we are saving the calibration pattern as the background.
		if (!isShowingPattern.get()) arenaController.saveCurrentBackground();
		setArenaBackground("pattern.png");
		isShowingPattern.set(true);

		showAutoCalibrationMessage();

		launchAutoCalibrationTimer();
	}

	private void launchAutoCalibrationTimer() {
		TimerPool.cancelTimer(autoCalibrationFuture);

		autoCalibrationFuture = TimerPool.schedule(() -> {
			Platform.runLater(() -> {
				if (isCalibrating.get() && isFullScreen) {
					calibratingCameraManager.disableAutoCalibration();
					enableManualCalibration();
				}
				// Keep waiting
				else if (!isFullScreen) launchAutoCalibrationTimer();
			});
		}, MAX_AUTO_CALIBRATION_TIME);
	}

	@Override
	public void setArenaBackground(String resourceFilename) {
		if (resourceFilename != null) {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceFilename);
			LocatedImage img = new LocatedImage(is, resourceFilename);
			arenaController.setBackground(img);
		} else {
			arenaController.setBackground(null);
		}
	}

	private Label autoCalibrationMessage = null;
	private volatile boolean showingAutoCalibrationMessage = false;

	private void showAutoCalibrationMessage() {
		logger.trace("showAutoCalibrationMessage - showingAutoCalibrationMessage {} autoCalibrationMessage {}",
				showingAutoCalibrationMessage, autoCalibrationMessage);

		if (showingAutoCalibrationMessage) return;

		showingAutoCalibrationMessage = true;
		autoCalibrationMessage = calibratingCanvasManager.addDiagnosticMessage("Attempting autocalibration", 11000,
				Color.CYAN);
	}

	private void removeAutoCalibrationMessage() {
		logger.trace("removeAutoCalibrationMessage - showingAutoCalibrationMessage {} autoCalibrationMessage {}",
				showingAutoCalibrationMessage, autoCalibrationMessage);

		if (showingAutoCalibrationMessage) {
			showingAutoCalibrationMessage = false;

			if (logger.isTraceEnabled()) logger.trace("removeAutoCalibrationMessage {} ", autoCalibrationMessage);
			calibratingCanvasManager.removeDiagnosticMessage(autoCalibrationMessage);
			autoCalibrationMessage = null;
		}
	}

	private boolean isFullScreen = false;

	public void setFullScreenStatus(boolean fullScreen) {
		this.isFullScreen = fullScreen;

		logger.trace("setFullScreenStatus - {} {}", fullScreen, isCalibrating);

		if (!isCalibrating.get()) {
			enableCalibration();
		} else if (!fullScreen) {
			calibratingCameraManager.disableAutoCalibration();

			removeCalibrationTargetIfPresent();

			removeAutoCalibrationMessage();

			disableManualCalibration();

			showFullScreenRequest();
		} else {
			removeFullScreenRequest();
			// Delay slightly to prevent #444 bug
			TimerPool.schedule(() -> {
				Platform.runLater(() -> {
					if (isCalibrating.get()) enableAutoCalibration();
				});
			}, 100);
		}
	}

	public boolean isCalibrating() {
		return isCalibrating.get();
	}
}
