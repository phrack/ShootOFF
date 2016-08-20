package com.shootoff.gui.pane;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.gui.TargetListener;
import com.shootoff.gui.controller.TargetEditorController;
import com.shootoff.targets.CameraViews;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextAlignment;
import marytts.util.io.FileFilter;

public class TargetSlide extends Slide implements TargetListener {
	private static final Logger logger = LoggerFactory.getLogger(TargetSlide.class);
	private static final int TARGET_COLUMNS = 6;
	private static final int TARGET_BUTTON_DIMS = 150;
	
	private final TilePane targetButtonContainer = new TilePane(30, 30);
	private final Pane parentControls;
	private final Pane parentBody;
	private final CameraViews cameraViews;
	private final ScrollPane scrollPane;
	
	private enum Mode { ADD, EDIT };
	
	private Mode mode;
	
	private int targetCount = 0;
	
	public TargetSlide(Pane parentControls, Pane parentBody, CameraViews cameraViews) {
		super(parentControls, parentBody);
		
		this.parentControls = parentControls;
		this.parentBody = parentBody;
		
		addSlideControlButton("Add Target", (event) -> {
			mode = Mode.ADD;
			showBody();
		});
		
		addSlideControlButton("Create Target", (event) -> {
			Optional<FXMLLoader> loader = createTargetEditorStage();

			if (loader.isPresent()) {
				CameraManager currentCamera = cameraViews.getSelectedCameraManager();
				Image currentFrame = currentCamera.getCurrentFrame();
				TargetEditorController editorController = (TargetEditorController) loader.get().getController();
				editorController.init(currentFrame, this);
				
				final TargetEditorSlide targetEditorSlide = new TargetEditorSlide(parentControls, parentBody, editorController);
				targetEditorSlide.showControls();
				targetEditorSlide.showBody();
			}
		});
		
		addSlideControlButton("Edit Target", (event) -> {
			mode = Mode.EDIT;
			showBody();
		});
		
		targetButtonContainer.setPrefColumns(TARGET_COLUMNS);
		targetButtonContainer.setPadding(new Insets(0, 65, 65, 65));
		this.cameraViews = cameraViews;

		scrollPane = new ScrollPane(targetButtonContainer);
		scrollPane.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color:transparent;");
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToHeight(true);
		
		addBodyNode(scrollPane);
		
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
		
		final Button targetButton = new Button(targetName);
		targetButton.setContentDisplay(ContentDisplay.TOP);
		targetButton.setTextAlignment(TextAlignment.CENTER);
		targetButton.setGraphic(targetImageView);
		targetButton.setPrefSize(TARGET_BUTTON_DIMS, TARGET_BUTTON_DIMS);
		targetButton.setWrapText(true);

		targetButton.setOnAction((event) -> {
			if (Mode.ADD.equals(mode)) {
				cameraViews.getSelectedCameraView().addTarget(targetFile);
				hide();
			} else {
				Optional<FXMLLoader> loader = createTargetEditorStage();

				if (loader.isPresent()) {
					CameraManager currentCamera = cameraViews.getSelectedCameraManager();
					Image currentFrame = currentCamera.getCurrentFrame();
					TargetEditorController editorController = (TargetEditorController) loader.get().getController();
					editorController.init(currentFrame, this, targetFile);
					
					final TargetEditorSlide targetEditorSlide = new TargetEditorSlide(parentControls, parentBody, editorController);
					targetEditorSlide.showControls();
					targetEditorSlide.showBody();
				}
			}
		});

		targetButtonContainer.getChildren().add(targetButton);
		
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

		return Optional.of(loader);
	}

	// TODO: Add target to arena
}
