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
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.controller.PluginManagerController;
import com.shootoff.gui.controller.SessionViewerController;
import com.shootoff.plugins.ExerciseMetadata;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginListener;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.io.SessionIO;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExerciseSlide extends Slide implements PluginListener, ItemSelectionListener<TrainingExercise> {
	private static final Logger logger = LoggerFactory.getLogger(ExerciseSlide.class);

	private final Configuration config;
	private final ExerciseListener exerciseListener;

	private final TitledPane projectorPane;
	private final ToggleButton noneButton;

	private final ItemSelectionPane<TrainingExercise> exerciseItemPane = new ItemSelectionPane<>(true, this);
	private final ItemSelectionPane<TrainingExercise> projectorExerciseItemPane = new ItemSelectionPane<>(
			exerciseItemPane.getToggleGroup(), this);

	private static final TrainingExercise noneExercise = new TrainingExercise() {
		@Override
		public void init() {}

		@Override
		public void targetUpdate(Target target, TargetChange change) {}

		@Override
		public ExerciseMetadata getInfo() {
			return null;
		}

		@Override
		public void shotListener(Shot shot, Optional<Hit> hit) {}

		@Override
		public void reset(List<Target> targets) {}

		@Override
		public void destroy() {}
	};

	public ExerciseSlide(Pane parentControls, Pane parentBody, ExerciseListener exerciseListener) {
		super(parentControls, parentBody);

		this.exerciseListener = exerciseListener;
		config = Configuration.getConfig();

		addSlideControlButton("Get Exercises", (event) -> {
			final Optional<FXMLLoader> loader = createPluginManagerStage();

			if (loader.isPresent()) {
				final PluginManagerController pluginManagerController = (PluginManagerController) loader.get()
						.getController();
				pluginManagerController.init(exerciseListener.getPluginEngine(),
						(Stage) parentControls.getScene().getWindow());

				final PluginManagerSlide pluginViewerSlide = new PluginManagerSlide(parentControls, parentBody,
						pluginManagerController);
				pluginViewerSlide.showControls();
				pluginViewerSlide.showBody();
			}
		});

		addSlideControlButton("Record Session", (event) -> {
			if (config.getSessionRecorder().isPresent()) {
				stopRecordingSession();

				((Button) event.getSource()).setText("Record Session");
			} else {
				startRecordingSession();

				((Button) event.getSource()).setText("Stop Recording");
			}

			hide();
		});

		addSlideControlButton("View Sessions", (event) -> {
			final Optional<FXMLLoader> loader = createSessionViewerStage();

			if (loader.isPresent()) {
				final SessionViewerController sessionViewerController = (SessionViewerController) loader.get()
						.getController();
				sessionViewerController.init(Configuration.getConfig());

				final SessionViewerSlide sessionViewerSlide = new SessionViewerSlide(parentControls, parentBody,
						sessionViewerController);
				sessionViewerSlide.showControls();
				sessionViewerSlide.showBody();
			}
		});

		noneButton = addNoneButton();

		final TitledPane universalPane = new TitledPane("Universal Exercises", exerciseItemPane);
		projectorPane = new TitledPane("Projector Exercises", projectorExerciseItemPane);
		projectorPane.setDisable(true);
		projectorPane.setExpanded(false);

		addBodyNode(new VBox(universalPane, projectorPane));
	}

	@Override
	public void showControls() {
		super.showControls();
		showBody();
	}

	private ToggleButton addNoneButton() {
		final ToggleButton noneButton = (ToggleButton) exerciseItemPane.addButton(noneExercise, "None");
		noneButton.setSelected(true);
		exerciseItemPane.setDefault(noneExercise);

		return noneButton;
	}

	private Optional<FXMLLoader> createSessionViewerStage() {
		final FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/SessionViewer.fxml"));
		try {
			loader.load();
		} catch (final IOException e) {
			logger.error("Cannot load SessionViewer.fxml", e);
			return Optional.empty();
		}

		return Optional.of(loader);
	}

	private Optional<FXMLLoader> createPluginManagerStage() {
		final FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/PluginManager.fxml"));
		try {
			loader.load();
		} catch (final IOException e) {
			logger.error("Cannot load SessionViewer.fxml", e);
			return Optional.empty();
		}

		return Optional.of(loader);
	}

	@Override
	public void registerExercise(TrainingExercise exercise) {
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		t.setPrefWidth(500);
		t.setWrapText(true);
		exerciseItemPane.addButton(exercise, exercise.getInfo().getName(), Optional.empty(), Optional.of(t));
	}

	@Override
	public void registerProjectorExercise(TrainingExercise exercise) {
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		t.setPrefWidth(500);
		t.setWrapText(true);
		projectorExerciseItemPane.addButton(exercise, exercise.getInfo().getName(), Optional.empty(), Optional.of(t));
	}

	@Override
	public void unregisterExercise(TrainingExercise exercise) {
		exerciseItemPane.removeButton(exercise);
	}

	public void toggleProjectorExercises(boolean isDisabled) {
		if (isDisabled) {
			noneButton.fire();
		}

		projectorPane.setDisable(isDisabled);
		projectorPane.setExpanded(!isDisabled);
	}

	public ExerciseListener getExerciseListener() {
		return exerciseListener;
	}

	private void startRecordingSession() {
		config.setSessionRecorder(new SessionRecorder());

		for (final CameraManager cm : config.getRecordingManagers()) {
			cm.startRecordingShots();
		}
	}

	public void stopRecordingSession() {
		for (final CameraManager cm : config.getRecordingManagers()) {
			cm.stopRecordingShots();
		}

		SessionIO.saveSession(config.getSessionRecorder().get(), new File(System.getProperty("shootoff.home")
				+ File.separator + "sessions/" + config.getSessionRecorder().get().getSessionName() + ".xml"));

		config.setSessionRecorder(null);
	}

	@Override
	public void onItemClicked(TrainingExercise selectedExercise) {
		if (selectedExercise.equals(noneExercise)) {
			exerciseListener.setExercise(null);
		} else if (selectedExercise instanceof ProjectorTrainingExerciseBase) {
			exerciseListener.setProjectorExercise(selectedExercise);
		} else {
			exerciseListener.setExercise(selectedExercise);
		}

		hide();
	}
}
