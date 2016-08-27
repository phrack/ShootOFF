package com.shootoff.gui.pane;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.Resetter;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.targets.CameraViews;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ProjectorSlide extends Slide implements CalibrationConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(ProjectorSlide.class);
	
	private final Configuration config;
	private final CameraViews cameraViews;
	private final Stage shootOffStage;
	private final Resetter resetter;
	private final ExerciseSlide exerciseSlide;
	private final Button calibrateButton;
	
	private ProjectorArenaController arenaController;
	private Optional<CalibrationManager> calibrationManager = Optional.empty();
	
	public ProjectorSlide(Pane parentControls, Pane parentBody, Configuration config, CameraViews cameraViews,
			Stage shootOffStage, Resetter resetter, ExerciseSlide exerciseSlide) {
		super(parentControls, parentBody);
		
		this.config = config;
		this.cameraViews = cameraViews;
		this.shootOffStage = shootOffStage;
		this.resetter = resetter;
		this.exerciseSlide = exerciseSlide;
		
		calibrateButton = addSlideControlButton("Calibrate", (event) -> {
			if (!calibrationManager.isPresent()) return;

			if (!calibrationManager.get().isCalibrating()) {
				calibrationManager.get().enableCalibration();
			} else {
				calibrationManager.get().stopCalibration();
			}
		});
		
		addSlideControlButton("Background", (event) -> {
			
		});
		
		addSlideControlButton("Courses", (event) -> {
			
		});
	}
	
	@Override
	public CalibrationOption getCalibratedFeedBehavior() {
		return config.getCalibratedFeedBehavior();
	}
	
	@Override 
	public void calibratedFeedBehaviorsChanged() {
		if (calibrationManager.isPresent())
			calibrationManager.get().configureArenaCamera(config.getCalibratedFeedBehavior());
		
		if (arenaController != null) 
			arenaController.getCanvasManager().setShowShots(config.showArenaShotMarkers());
	}
	
	@Override
	public void toggleCalibrating() {
		final Runnable toggleCalibrationAction = () -> {
			if (calibrateButton.getText().equals("Calibrate"))
				calibrateButton.setText("Stop Calibrating");
			else
				calibrateButton.setText("Calibrate");
		};

		if (Platform.isFxApplicationThread()) {
			toggleCalibrationAction.run();
		} else {
			Platform.runLater(toggleCalibrationAction);
		}
	}
	
	public ProjectorArenaController getArenaController() {
		return arenaController;
	}
	
	public Optional<CalibrationManager> getCalibrationManager() {
		return calibrationManager;
	}
	
	public void startArena() {
		if (arenaController == null) {
			final FXMLLoader loader = new FXMLLoader(
					getClass().getClassLoader().getResource("com/shootoff/gui/ProjectorArena.fxml"));
			try {
				loader.load();
			} catch (IOException e) {
				logger.error("Cannot load ProjectorArena.fxml", e);
				return;
			}

			final Stage arenaStage = new Stage();

			arenaStage.setTitle("Projector Arena");
			arenaStage.setScene(new Scene(loader.getRoot()));

			arenaController = (ProjectorArenaController) loader.getController();
			arenaController.init(shootOffStage, config, resetter);
			
			final CameraManager calibratingCameraManager = cameraViews.getSelectedCameraManager();
			calibrationManager = Optional.of(new CalibrationManager(this, calibratingCameraManager, arenaController));
			arenaController.setCalibrationManager(calibrationManager.get());
			
			exerciseSlide.toggleProjectorExercises(false);
			arenaController.getCanvasManager().setShowShots(config.showArenaShotMarkers());

			calibrateButton.fire();
			
			arenaStage.setOnCloseRequest((e) -> {
				if (config.getExercise().isPresent()
						&& config.getExercise().get() instanceof ProjectorTrainingExerciseBase) {
					exerciseSlide.toggleProjectorExercises(true);
				}
				
				if (calibrationManager.isPresent()) {
					if (calibrationManager.get().isCalibrating()) {
						calibrationManager.get().stopCalibration();
					} else {
						calibrationManager.get().arenaClosing();
					}
				}
				
				arenaController.setFeedCanvasManager(null);
				arenaController = null;
			});
		}
	}
	
	public void closeArena() {
		if (arenaController != null) {
			arenaController.getCanvasManager().close();
			arenaController.close();
		}
	}
}
