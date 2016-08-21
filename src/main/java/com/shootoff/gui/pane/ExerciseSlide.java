package com.shootoff.gui.pane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.ExerciseListener;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
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
	
	private int exerciseCount = 0;

	private Stage sessionViewerStage;
	private Object pluginManagerStage;
	
	public ExerciseSlide(Pane parentControls, Pane parentBody, ExerciseListener exerciseListener) {
		super(parentControls, parentBody);
		
		this.parentControls = parentControls;
		this.parentBody = parentBody;
		this.exerciseListener = exerciseListener;
		
		addSlideControlButton("Get Exercises", (event) -> {
			showExerciseManager();
		});
		
		addSlideControlButton("View Sessions", (event) -> {
			showSessionViewer();
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
		final Button exerciseButton = new Button("None");
		exerciseButton.setContentDisplay(ContentDisplay.TOP);
		exerciseButton.setTextAlignment(TextAlignment.CENTER);
		exerciseButton.setPrefSize(BUTTON_DIMS, BUTTON_DIMS);
		exerciseButton.setWrapText(true);

		exerciseButton.setOnAction((event) -> {
			selectedExercise = null;
			exerciseListener.setExercise(null);
		});
		
		exerciseButtonContainer.getChildren().add(exerciseButton);
		
		scrollPane.setPrefViewportHeight((BUTTON_DIMS + 65) * (exerciseCount / COLUMNS));
	}

	//TODO: Implement
	private void showSessionViewer() {
		/*FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/SessionViewer.fxml"));
		loader.load();

		sessionViewerStage = new Stage();

		sessionViewerStage.setTitle("Session Viewer");
		sessionViewerStage.setScene(new Scene(loader.getRoot()));
		sessionViewerStage.show();

		SessionViewerController sessionViewerController = (SessionViewerController) loader.getController();
		sessionViewerController.init(config);*/
	}

	//TODO: Implement
	private void showExerciseManager() {
		/*if (pluginManagerStage == null) {
			FXMLLoader loader = new FXMLLoader(
					getClass().getClassLoader().getResource("com/shootoff/gui/PluginManager.fxml"));
			loader.load();

			pluginManagerStage = new Stage();

			pluginManagerStage.initOwner(shootOFFStage);
			pluginManagerStage.setTitle("Exercise Manager");
			pluginManagerStage.setScene(new Scene(loader.getRoot()));
			pluginManagerStage.show();
			pluginManagerStage.setOnCloseRequest((e) -> {
				pluginManagerStage = null;
			});
			((PluginManagerController) loader.getController()).init(pluginEngine);
		} else {
			pluginManagerStage.show();
			pluginManagerStage.toFront();
		}*/
	}

	@Override
	public void registerExercise(TrainingExercise exercise) {
		
		exerciseCount++;
	
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		final Button exerciseButton = new Button(exercise.getInfo().getName());
		exerciseButton.setContentDisplay(ContentDisplay.TOP);
		exerciseButton.setTextAlignment(TextAlignment.CENTER);
		exerciseButton.setPrefSize(BUTTON_DIMS, BUTTON_DIMS);
		exerciseButton.setTooltip(t);
		exerciseButton.setWrapText(true);

		exerciseButton.setOnAction((event) -> {
			selectedExercise = exercise;
			exerciseListener.setExercise(exercise);
		});

		exerciseButtonContainer.getChildren().add(exerciseButton);
		
		scrollPane.setPrefViewportHeight((BUTTON_DIMS + 65) * (exerciseCount / COLUMNS));
		
	}
	
	

	@Override
	public void registerProjectorExercise(TrainingExercise exercise) {
		exerciseCount++;
		
		final Tooltip t = new Tooltip(exercise.getInfo().getDescription());
		final Button exerciseButton = new Button(exercise.getInfo().getName());
		exerciseButton.setContentDisplay(ContentDisplay.TOP);
		exerciseButton.setTextAlignment(TextAlignment.CENTER);
		exerciseButton.setPrefSize(BUTTON_DIMS, BUTTON_DIMS);
		exerciseButton.setTooltip(t);
		exerciseButton.setWrapText(true);

		exerciseButton.setOnAction((event) -> {
			selectedExercise = exercise;
			exerciseListener.setExercise(exercise);
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
		// TODO Auto-generated method stub
		
	}


	
}
