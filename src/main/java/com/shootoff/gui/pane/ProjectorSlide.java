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

import java.util.Optional;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.MirroredCanvasManager;
import com.shootoff.gui.Resetter;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.targets.CameraViews;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ProjectorSlide extends Slide implements CalibrationConfigurator {
	private final Pane parentControls;
	private final Pane parentBody;
	private final Configuration config;
	private final CameraViews cameraViews;
	private final Stage shootOffStage;
	private final Pane trainingExerciseContainer;
	private final Resetter resetter;
	private final ExerciseSlide exerciseSlide;
	private final Button calibrateButton;
	
	private ArenaBackgroundsSlide backgroundsSlide;
	
	private ProjectorArenaPane arenaPane;
	private Optional<CalibrationManager> calibrationManager = Optional.empty();
	
	public ProjectorSlide(Pane parentControls, Pane parentBody, Configuration config, CameraViews cameraViews,
			Stage shootOffStage, Pane trainingExerciseContainer, Resetter resetter, ExerciseSlide exerciseSlide) {
		super(parentControls, parentBody);
		
		this.parentControls = parentControls;
		this.parentBody = parentBody;
		this.config = config;
		this.cameraViews = cameraViews;
		this.shootOffStage = shootOffStage;
		this.trainingExerciseContainer = trainingExerciseContainer;
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
			backgroundsSlide.showControls();
			backgroundsSlide.showBody();
		});
		
		addSlideControlButton("Courses", (event) -> {
			final ArenaCoursesSlide coursesSlide = new ArenaCoursesSlide(parentControls, 
					parentBody, arenaPane, shootOffStage);
			coursesSlide.setOnSlideHidden(() -> {
				if (coursesSlide.choseCourse()) {
					hide();
				}
			});
			coursesSlide.showControls();
			coursesSlide.showBody();
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
		
		if (arenaPane != null) 
			arenaPane.getCanvasManager().setShowShots(config.showArenaShotMarkers());
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
	
	public ProjectorArenaPane getArenaPane() {
		return arenaPane;
	}
	
	public Optional<CalibrationManager> getCalibrationManager() {
		return calibrationManager;
	}
	
	public void startArena() {	
		if (arenaPane == null) {
			final Stage arenaStage = new Stage();

			arenaPane = new ProjectorArenaPane(arenaStage, shootOffStage, trainingExerciseContainer, config, resetter, null);
			
			// Prepare calibrating manager up front so that we can switch
			// to the arena tab when it's ready (otherwise
			// getSelectedCameraManager() will fail)
			final CameraManager calibratingCameraManager = cameraViews.getSelectedCameraManager();
			
			// Mirror panes so that anything that happens to one also
			// happens to the other
			final ProjectorArenaPane arenaTabPane = new ProjectorArenaPane(arenaStage, shootOffStage, trainingExerciseContainer,
					config, resetter, cameraViews.getShotTimerModel()); 
			
			arenaTabPane.prefWidthProperty().bind(arenaPane.prefWidthProperty());
			arenaTabPane.prefHeightProperty().bind(arenaPane.prefHeightProperty());
			
			cameraViews.addNonCameraView("Arena", arenaTabPane, arenaTabPane.getCanvasManager(), true, true);
			
			arenaPane.setArenaPaneMirror(arenaTabPane);
			
			final CanvasManager arenaCanvasManager = arenaPane.getCanvasManager();
			
			if (!(arenaCanvasManager instanceof MirroredCanvasManager)) {
				throw new AssertionError("Arena canvas manager is not of type MirroredCanvasManager");
			}
			
			final MirroredCanvasManager projectorCanvasManager = (MirroredCanvasManager) arenaCanvasManager;
			
			final CanvasManager tabArenaCanvasManager = arenaTabPane.getCanvasManager();
			
			if (!(tabArenaCanvasManager instanceof MirroredCanvasManager)) {
				throw new AssertionError("Tab arena canvas manager is not of type MirroredCanvasManager");
			}
			
			final MirroredCanvasManager tabCanvasManager = (MirroredCanvasManager) tabArenaCanvasManager;
			
			projectorCanvasManager.setMirroredManager(tabCanvasManager);
			tabCanvasManager.setMirroredManager(projectorCanvasManager);
			projectorCanvasManager.updateBackground(null, Optional.empty());
			// This camera manager must be set to enable click-to-shoot for
			// the arena tab
			tabCanvasManager.setCameraManager(calibratingCameraManager);
			
			// Final preparation to display
			arenaStage.setTitle("Projector Arena");
			arenaStage.setScene(new Scene(arenaPane));
			arenaStage.setFullScreenExitHint("");
			
			calibrationManager = Optional.of(new CalibrationManager(this, calibratingCameraManager, arenaPane, 
					cameraViews, exerciseSlide.getExerciseListener()));
			
			calibrationManager.get().addCalibrationListener(new CalibrationListener() {
				@Override
				public void startCalibration() { }
				
				@Override
				public void calibrated(Optional<PerspectiveManager> perspectiveManager) {
					if (Platform.isFxApplicationThread()) {
						hide();
					} else {
						Platform.runLater(() -> hide());
					}
				}
			});
			
			arenaPane.setCalibrationManager(calibrationManager.get());
			
			exerciseSlide.toggleProjectorExercises(false);
			arenaPane.getCanvasManager().setShowShots(config.showArenaShotMarkers());

			backgroundsSlide = new ArenaBackgroundsSlide(parentControls, 
					parentBody, arenaPane, shootOffStage);
			backgroundsSlide.setOnSlideHidden(() -> { 
				if (backgroundsSlide.choseBackground()) {
					backgroundsSlide.setChoseBackground(false);
					hide(); 
				}
			});
			
			// Display the arena
			calibrateButton.fire();
			arenaPane.toggleArena();
			arenaPane.autoPlaceArena();
			
			arenaStage.setOnCloseRequest((e) -> {
				arenaStage.setOnCloseRequest(null);
				
				arenaPane.close();
				arenaPane.setFeedCanvasManager(null);
				arenaPane = null;
				
				cameraViews.removeCameraView("Arena");
				
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
				
				exerciseSlide.toggleProjectorExercises(true);
			});
		}
	}
	
	public void closeArena() {
		if (arenaPane != null) {
			arenaPane.getCanvasManager().close();
			arenaPane.close();
		}
	}
}
