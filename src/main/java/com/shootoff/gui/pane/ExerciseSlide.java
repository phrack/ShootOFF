package com.shootoff.gui.pane;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.controller.PluginManagerController;
import com.shootoff.gui.controller.SessionViewerController;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginListener;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class ExerciseSlide extends Slide implements PluginListener  {
	private static final Logger logger = LoggerFactory.getLogger(ExerciseSlide.class);
	private static final int COLUMNS = 6;
	private static final int BUTTON_DIMS = 150;
	
	private final TilePane exerciseButtonContainer = new TilePane(30, 30);
	private final Pane parentControls;
	private final Pane parentBody;
	private final ScrollPane scrollPane;
	
	private ExerciseListener exerciseListener = null;
	
	TrainingExercise selectedExercise = null;
	ToggleGroup toggleGroup = new ToggleGroup();
	final ToggleButton noneButton = new ToggleButton("None");
	
	private int exerciseCount = 0;
	
	public ExerciseSlide(Pane parentControls, Pane parentBody, ExerciseListener exerciseListener) {
		super(parentControls, parentBody);
		
		this.parentControls = parentControls;
		this.parentBody = parentBody;
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
		
		
		exerciseButtonContainer.setPrefColumns(COLUMNS);
		exerciseButtonContainer.setPadding(new Insets(0, 65, 65, 65));

		scrollPane = new ScrollPane(exerciseButtonContainer);
		scrollPane.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color:transparent;");
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToHeight(true);
		
		addNoneButton();
		
		addBodyNode(scrollPane);

	}
	
	@Override
	public void showControls()
	{
		super.showControls();
		showBody();
	}

	private void addNoneButton() {
		noneButton.setContentDisplay(ContentDisplay.TOP);
		noneButton.setTextAlignment(TextAlignment.CENTER);
		noneButton.setPrefSize(BUTTON_DIMS, BUTTON_DIMS);
		noneButton.setWrapText(true);
		noneButton.setToggleGroup(toggleGroup);

		noneButton.setOnAction((event) -> {
			selectedExercise = null;
			exerciseListener.setExercise(null);
			toggleGroup.selectToggle(noneButton);
		});
		
		exerciseButtonContainer.getChildren().add(noneButton);
		
		scrollPane.setPrefViewportHeight((BUTTON_DIMS + 65) * (exerciseCount / COLUMNS));
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
		
		exerciseCount++;
	
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		final ToggleButton exerciseButton = new ToggleButton(exercise.getInfo().getName());
		exerciseButton.setContentDisplay(ContentDisplay.TOP);
		exerciseButton.setTextAlignment(TextAlignment.CENTER);
		exerciseButton.setPrefSize(BUTTON_DIMS, BUTTON_DIMS);
		exerciseButton.setTooltip(t);
		exerciseButton.setWrapText(true);
		exerciseButton.setToggleGroup(toggleGroup);

		exerciseButton.setOnAction((event) -> {
			selectedExercise = exercise;
			exerciseListener.setExercise(exercise);
			toggleGroup.selectToggle(exerciseButton);
		});

		exerciseButtonContainer.getChildren().add(exerciseButton);
		
		scrollPane.setPrefViewportHeight((BUTTON_DIMS + 65) * (exerciseCount / COLUMNS));
		
	}
	
	

	@Override
	public void registerProjectorExercise(TrainingExercise exercise) {
		exerciseCount++;
		
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		final ToggleButton exerciseButton = new ToggleButton(exercise.getInfo().getName());
		exerciseButton.setContentDisplay(ContentDisplay.TOP);
		exerciseButton.setTextAlignment(TextAlignment.CENTER);
		exerciseButton.setPrefSize(BUTTON_DIMS, BUTTON_DIMS);
		exerciseButton.setTooltip(t);
		exerciseButton.setWrapText(true);
		exerciseButton.setToggleGroup(toggleGroup);

		exerciseButton.setOnAction((event) -> {
			selectedExercise = exercise;
			exerciseListener.setExercise(exercise);
			toggleGroup.selectToggle(exerciseButton);
		});
		
		//TODO: separate projector items
		//if (arenaController == null) exerciseItem.setDisable(true);


		exerciseButtonContainer.getChildren().add(exerciseButton);
		
		scrollPane.setPrefViewportHeight((BUTTON_DIMS + 65) * (exerciseCount / COLUMNS));
	}

	@Override
	public void unregisterExercise(TrainingExercise exercise) {
		Platform.runLater(() -> {
			// If we just unregistered the exercise that is on, disable it
			if (selectedExercise != null && selectedExercise == exercise)
			{
				exerciseListener.setExercise(null);
				selectedExercise = null;
				toggleGroup.selectToggle(noneButton);
			}
			
			for (Node n : exerciseButtonContainer.getChildren())
			{
				if (((Button)n).getText() == exercise.getInfo().getName())
				{
					exerciseButtonContainer.getChildren().remove(n);
					exerciseCount--;
					scrollPane.setPrefViewportHeight((BUTTON_DIMS + 65) * (exerciseCount / COLUMNS));
				}
			}
		});
	}

	public void disableProjectorExercises() {
		// TODO Implement
		
	}


	
}
