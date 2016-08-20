package com.shootoff.gui.pane;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Closeable;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.controller.PreferencesController;
import com.shootoff.targets.CameraViews;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FileSlide extends SlidePane {
	private static final Logger logger = LoggerFactory.getLogger(FileSlide.class);
			
	public FileSlide(Pane parent, CameraConfigListener configListener, CameraViews cameraViews, Closeable mainWindow) {
		super(parent);
		
		addSlideControlButton("Preferences", (event) -> {
			FXMLLoader loader = new FXMLLoader(
					getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));
			try {
				loader.load();
			} catch (IOException e) {
				logger.error("Cannot load Preferences.fxml", e);
				return;
			}

			Stage preferencesStage = new Stage();

			preferencesStage.initOwner(parent.getScene().getWindow());
			preferencesStage.initModality(Modality.WINDOW_MODAL);
			preferencesStage.setTitle("Preferences");
			preferencesStage.setScene(new Scene(loader.getRoot()));
			preferencesStage.show();
			((PreferencesController) loader.getController()).setConfig(configListener.getConfiguration(), configListener);
		});
		
		addSlideControlButton("Save Feed Image", (event) -> {
			final AnchorPane tabAnchor = (AnchorPane) cameraViews.getSelectedCameraContainer();
			final RenderedImage renderedImage = SwingFXUtils.fromFXImage(tabAnchor.snapshot(new SnapshotParameters(), null),
					null);

			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save Feed Image");
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"),
					new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png"));
			
			final File feedFile = fileChooser.showSaveDialog(parent.getScene().getWindow());

			if (feedFile != null) {
				String extension = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).substring(2);
				File imageFile;

				if (feedFile.getPath().endsWith(extension)) {
					imageFile = feedFile;
				} else {
					imageFile = new File(feedFile.getPath() + "." + extension);
				}

				try {
					ImageIO.write(renderedImage, extension, imageFile);
				} catch (IOException e) {
					logger.error("Error saving feed image", e);
				}
			}
		});
		
		addSlideControlButton("Exit", (evnet) -> {
			mainWindow.close();
		});
	}
}
