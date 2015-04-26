package com.shootoff.gui;

import java.util.List;
import java.util.Optional;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import com.github.sarxos.webcam.Webcam;

public class ImageCell extends ListCell<String> {
	private final List<Webcam> webcams;
	
	public ImageCell(List<Webcam> webcams) {
		this.webcams = webcams;
	}
	
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        
        Optional<Image> webcamImg = Optional.empty();
        
        for (Webcam webcam : webcams) {
        	if (webcam.getName().equals(item)) {
				webcamImg = Optional.of(
						SwingFXUtils.toFXImage(webcam.getImage(), null));
        	}
        }
        
        if (webcamImg.isPresent()) {
            ImageView img = new ImageView(webcamImg.get());
            img.setFitWidth(100);
            img.setFitHeight(75);
            if (item != null) {
                setGraphic(img);
                setText(item);
            }
        }
    }
}
