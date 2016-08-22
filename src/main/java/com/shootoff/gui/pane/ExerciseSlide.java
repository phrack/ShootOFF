package com.shootoff.gui.pane;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.container.ItemSelectionPane;
import com.shootoff.gui.container.listeners.ItemSelectionListener;
import com.shootoff.gui.controller.PluginManagerController;
import com.shootoff.gui.controller.SessionViewerController;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ExerciseSlide extends Slide implements PluginListener, ItemSelectionListener  {
	private static final Logger logger = LoggerFactory.getLogger(ExerciseSlide.class);

	private ExerciseListener exerciseListener = null;
	
	private final ItemSelectionPane itemPane = new ItemSelectionPane(true, this);
	
	
	public ExerciseSlide(Pane parentControls, Pane parentBody, ExerciseListener exerciseListener) {
		super(parentControls, parentBody);
		
		this.exerciseListener = exerciseListener;
		
		addSlideControlButton("Get Exercises", (event) -> {
			Optional<FXMLLoader> loader = createPluginManagerStage();

			if (loader.isPresent()) {
				PluginManagerController pluginManagerController = (PluginManagerController) loader.get().getController();
				pluginManagerController.init(exerciseListener.getPluginEngine(), (Stage) parentControls.getScene().getWindow());
				
				final PluginManagerSlide pluginViewerSlide = new PluginManagerSlide(parentControls, parentBody, pluginManagerController);
				pluginViewerSlide.showControls();
				pluginViewerSlide.showBody();
			}
		});
		
		addSlideControlButton("View Sessions", (event) -> {
			Optional<FXMLLoader> loader = createSessionViewerStage();

			if (loader.isPresent()) {
				SessionViewerController sessionViewerController = (SessionViewerController) loader.get().getController();
				sessionViewerController.init(exerciseListener.getConfiguration());
				
				final SessionViewerSlide sessionViewerSlide = new SessionViewerSlide(parentControls, parentBody, sessionViewerController);
				sessionViewerSlide.showControls();
				sessionViewerSlide.showBody();
			}
		});
			
		addNoneButton();
		
		addBodyNode(itemPane);
	}
	
	@Override
	public void showControls()
	{
		super.showControls();
		showBody();
	}

	private void addNoneButton() {
		itemPane.addButton("None", "None");
		itemPane.setDefault("None");
	}
	
	
	private Optional<FXMLLoader> createSessionViewerStage() {
		FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/SessionViewer.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("Cannot load SessionViewer.fxml", e);
			return Optional.empty();
		}

		return Optional.of(loader);
	}
	
	
	private Optional<FXMLLoader> createPluginManagerStage() {
		FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/PluginManager.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("Cannot load SessionViewer.fxml", e);
			return Optional.empty();
		}

		return Optional.of(loader);
	}


	@Override
	public void registerExercise(TrainingExercise exercise) {
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		itemPane.addButton(exercise, exercise.getInfo().getName(), Optional.empty(), Optional.of(t));
	}
	
	// TODO: Extend ItemSelectionPane into a multi section pane and disable projector exercises when not appropriate

	@Override
	public void registerProjectorExercise(TrainingExercise exercise) {
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		itemPane.addButton(exercise, exercise.getInfo().getName(), Optional.empty(), Optional.of(t));
	}

	@Override
	public void unregisterExercise(TrainingExercise exercise) {
		itemPane.removeButton(exercise);
	}

	public void disableProjectorExercises() {
		// TODO Implement
		
	}

	@Override
	public void onItemClicked(Object ref) {
		if (ref instanceof String && (String)ref == "None")
		{
			exerciseListener.setExercise(null);
		}
		else if (ref instanceof ProjectorTrainingExerciseBase)
		{
			exerciseListener.setProjectorExercise((TrainingExercise)ref);
		}
		else if (ref instanceof TrainingExercise)
		{
			exerciseListener.setExercise((TrainingExercise)ref);
		}
		else
		{
			logger.error("Did not expect this object - %s", ref);
		}
	}


	
}
