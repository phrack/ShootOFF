package com.shootoff.gui;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.gui.controller.TargetEditorController;
import com.shootoff.targets.TargetRepository;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import marytts.util.io.FileFilter;

public class TargetPane extends SlidePane implements TargetListener {
	private static final Logger logger = LoggerFactory.getLogger(TargetPane.class);
	private static final int TARGET_COLUMNS = 6;
	private static final int TARGET_BUTTON_DIMS = 150;
	
	private final TilePane container = new TilePane(30, 30);
	private final TargetRepository targetRepository;
	private final ToggleButton editTargetToggleButton = new ToggleButton("Edit Target");
	private final ScrollPane scrollPane;
	
	private int targetCount = 0;
	
	public TargetPane(Pane parent, TargetRepository targetRepository) {
		super(parent);

		final Button createTargetButton = new Button("Create Target");
		createTargetButton.setPrefSize(150, 90);

		editTargetToggleButton.setPrefSize(150, 90);

		createTargetButton.setOnAction((event) -> {
			Optional<FXMLLoader> loader = createTargetEditorStage();

			if (loader.isPresent()) {
				CameraManager currentCamera = targetRepository.getSelectedCameraManager();
				Image currentFrame = currentCamera.getCurrentFrame();
				((TargetEditorController) loader.get().getController()).init(currentFrame, this);
			}
		});
		
		final HBox targetOptions = new HBox(createTargetButton, editTargetToggleButton);
		targetOptions.setSpacing(30);
		targetOptions.setPadding(new Insets(65, 65, 0, 65));
		
		container.setPrefColumns(TARGET_COLUMNS);
		container.setPadding(new Insets(65, 65, 65, 65));
		this.targetRepository = targetRepository;

		scrollPane = new ScrollPane(container);
		scrollPane.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color:transparent;");
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToHeight(true);
		
		add(targetOptions);
		add(scrollPane);
		
		findTargets();
	}

	private void findTargets() {
		File targetsFolder = new File(System.getProperty("shootoff.home") + File.separator + "targets");

		File[] targetFiles = targetsFolder.listFiles(new FileFilter("target"));

		if (targetFiles != null) {
			Arrays.sort(targetFiles);
			for (File file : targetFiles) {
				newTarget(file);
			}
		} else {
			logger.error("Failed to find target files because a list of files could not be retrieved");
		}
	}

	@Override
	public void newTarget(File targetFile) {
		final Optional<TargetComponents> targetComponents = TargetIO.loadTarget(targetFile);

		if (!targetComponents.isPresent()) {
			logger.error("Notified of a new target that cannot be loaded: {}", targetFile.getAbsolutePath());
			return;
		}
		
		targetCount++;
		
		final Image targetImage = targetComponents.get().getTargetGroup().snapshot(new SnapshotParameters(), null);
		final ImageView targetImageView = new ImageView();
		targetImageView.setFitWidth(45);
		targetImageView.setFitHeight(45);
		targetImageView.setSmooth(true);
		targetImageView.setImage(targetImage);
		
		final String targetPath = targetFile.getPath();
		final String targetName = targetPath.substring(targetPath.lastIndexOf(File.separator) + 1, targetPath.lastIndexOf('.')).replace("_", " ");
		
		final Button addTargetButton = new Button(targetName);
		addTargetButton.setContentDisplay(ContentDisplay.TOP);
		addTargetButton.setTextAlignment(TextAlignment.CENTER);
		addTargetButton.setGraphic(targetImageView);
		addTargetButton.setPrefSize(TARGET_BUTTON_DIMS, TARGET_BUTTON_DIMS);
		addTargetButton.setWrapText(true);

		addTargetButton.setOnAction((event) -> {
			if (editTargetToggleButton.isSelected()) {
				editTargetToggleButton.setSelected(false);
				Optional<FXMLLoader> loader = createTargetEditorStage();

				if (loader.isPresent()) {
					CameraManager currentCamera = targetRepository.getSelectedCameraManager();
					Image currentFrame = currentCamera.getCurrentFrame();
					((TargetEditorController) loader.get().getController()).init(currentFrame, this, targetFile);
				}
			} else {
				targetRepository.getSelectedCameraView().addTarget(targetFile);
				hide();
			}
		});

		container.getChildren().add(addTargetButton);
		
		scrollPane.setPrefViewportHeight((TARGET_BUTTON_DIMS + 65) * (targetCount / TARGET_COLUMNS));
	}

	private Optional<FXMLLoader> createTargetEditorStage() {
		FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/TargetEditor.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("Cannot load TargetEditor.fxml", e);
			return Optional.empty();
		}

		Stage preferencesStage = new Stage();

		preferencesStage.initModality(Modality.WINDOW_MODAL);
		preferencesStage.setTitle("TargetEditor");
		preferencesStage.setScene(new Scene(loader.getRoot()));
		preferencesStage.show();

		return Optional.of(loader);
	}

	// TODO: Add target to arena
}
