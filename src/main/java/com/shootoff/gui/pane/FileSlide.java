package com.shootoff.gui.pane;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Closeable;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.controller.PreferencesController;
import com.shootoff.targets.CameraViews;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FileSlide extends Slide {
	private static final Logger logger = LoggerFactory.getLogger(FileSlide.class);
	
	private static PreferencesController preferencesController = null;
			
	public FileSlide(Pane parentControls, Pane parentBody, CalibrationConfigurator calibrationConfigurator, 
			CameraConfigListener configListener, CameraViews cameraViews, Closeable mainWindow) {
		super(parentControls, parentBody);
		
		addSlideControlButton("Preferences", (event) -> {
			if (preferencesController == null) {
				FXMLLoader loader = new FXMLLoader(
						getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));
				try {
					loader.load();
				} catch (IOException e) {
					logger.error("Cannot load Preferences.fxml", e);
					return;
				}

				preferencesController = (PreferencesController) loader.getController();
				preferencesController.setConfig((Stage) parentControls.getScene().getWindow(),
						configListener.getConfiguration(), calibrationConfigurator, configListener);
			}
			
			final PreferencesSlide preferencesSlide = new PreferencesSlide(parentControls, parentBody, preferencesController);
			preferencesSlide.setOnSlideHidden(() -> { if (preferencesSlide.isSaved()) hide(); });
			preferencesSlide.showControls();
			preferencesSlide.showBody();
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
			
			final File feedFile = fileChooser.showSaveDialog(parentControls.getScene().getWindow());

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
