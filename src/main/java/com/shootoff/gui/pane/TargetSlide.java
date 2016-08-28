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
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import marytts.util.io.FileFilter;

public class TargetSlide extends Slide implements TargetListener, ItemSelectionListener<File> {
	private static final Logger logger = LoggerFactory.getLogger(TargetSlide.class);
	
	private final Pane parentControls;
	private final Pane parentBody;
	private final CameraViews cameraViews;
	
	private final ItemSelectionPane<File> itemPane = new ItemSelectionPane<File>(false, this);
	
	private enum Mode { ADD, EDIT };
	
	private Mode mode;
	
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
		
		this.cameraViews = cameraViews;		
		
		addBodyNode(itemPane);
		
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
		
		final Image targetImage = targetComponents.get().getTargetGroup().snapshot(new SnapshotParameters(), null);
		final ImageView targetImageView = new ImageView();
		targetImageView.setFitWidth(60);
		targetImageView.setFitHeight(60);
		targetImageView.setSmooth(true);
		targetImageView.setImage(targetImage);
		
		final String targetPath = targetFile.getPath();
		final String targetName = targetPath.substring(targetPath.lastIndexOf(File.separator) + 1, targetPath.lastIndexOf('.')).replace("_", " ");
		
		itemPane.addButton(targetFile, targetName, Optional.of(targetImageView), Optional.empty());
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

	@Override
	public void onItemClicked(File ref) {
		if (Mode.ADD.equals(mode)) {
			cameraViews.getSelectedCameraView().addTarget((File)ref);
			hide();
		} else {
			final Optional<FXMLLoader> loader = createTargetEditorStage();

			if (loader.isPresent()) {
				final CameraManager currentCamera = cameraViews.getSelectedCameraManager();
				final Image currentFrame = currentCamera.getCurrentFrame();
				final TargetEditorController editorController = (TargetEditorController) loader.get().getController();
				editorController.init(currentFrame, this, (File)ref);
				
				final TargetEditorSlide targetEditorSlide = new TargetEditorSlide(parentControls, parentBody, editorController);
				targetEditorSlide.showControls();
				targetEditorSlide.showBody();
			}
		}
	}

	// TODO: Add target to arena
}
