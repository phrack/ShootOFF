package com.shootoff.gui.pane;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Closeable;
import com.shootoff.targets.CameraViews;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class FileSlide extends SlidePane {
	private static final Logger logger = LoggerFactory.getLogger(FileSlide.class);
			
	public FileSlide(Pane parent, CameraViews cameraViews, Closeable mainWindow) {
		super(parent);
		
		addSlideControlButton("Preferences", (event) -> {
			
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
			final File feedFile = fileChooser.showSaveDialog(tabAnchor.getScene().getWindow());

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
