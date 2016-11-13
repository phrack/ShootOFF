package com.shootoff.headless;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraErrorView;
import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.Resetter;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginEngine;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class HeadlessController implements CameraErrorView, Resetter, ExerciseListener, CalibrationConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(HeadlessController.class);

	private final Configuration config;
	private final CamerasSupervisor camerasSupervisor;

	public HeadlessController() {
		config = Configuration.getConfig();
		camerasSupervisor = new CamerasSupervisor(config);

		final Optional<Camera> defaultCamera = CameraFactory.getDefault();
		if (defaultCamera.isPresent()) {
			final Camera c = defaultCamera.get();

			if (c.isLocked() && !c.isOpen()) {
				logger.error("Default camera is locked, cannot proceed");
				return;
			}

			final CanvasManager canvasManager = new CanvasManager(new Group(), this, "Default", null);
			final CameraManager cameraManager = camerasSupervisor.addCameraManager(c, this, canvasManager);

			final Stage arenaStage = new Stage();
			// TODO: Pass controls added to this pane to the device controlling
			// SBC
			final Pane trainingExerciseContainer = new Pane();

			final ProjectorArenaPane arenaPane = new ProjectorArenaPane(arenaStage, null, trainingExerciseContainer,
					this, null);

			arenaStage.setTitle("Projector Arena");
			arenaStage.setScene(new Scene(arenaPane));
			arenaStage.setFullScreenExitHint("");

			// TODO: Camera views to non-null value to handle calibration issues
			final CalibrationManager calibrationManager = new CalibrationManager(this, cameraManager, arenaPane, null,
					this);

			arenaPane.setCalibrationManager(calibrationManager);

			arenaPane.toggleArena();
			arenaPane.autoPlaceArena();
			calibrationManager.enableCalibration();
		}
	}

	@Override
	public void reset() {
		camerasSupervisor.reset();
	}

	@Override
	public void showCameraLockError(Camera webcam, boolean allCamerasFailed) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void showMissingCameraError(Camera webcam) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void showFPSWarning(Camera webcam, double fps) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void showBrightnessWarning(Camera webcam) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void setProjectorExercise(TrainingExercise exercise) {
		// TODO: Set exercise
	}

	@Override
	public void setExercise(TrainingExercise exercise) {
		// TODO: Set exercise
	}

	@Override
	public Configuration getConfiguration() {
		return config;
	}

	@Override
	public PluginEngine getPluginEngine() {
		// TODO: Does this need to be implemented?
		return null;
	}

	@Override
	public CalibrationOption getCalibratedFeedBehavior() {
		return CalibrationOption.ONLY_IN_BOUNDS;
	}

	@Override
	public void calibratedFeedBehaviorsChanged() {}

	@Override
	public void toggleCalibrating() {}
}
