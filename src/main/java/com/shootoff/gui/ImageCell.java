/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.converter.DefaultStringConverter;

import com.github.sarxos.webcam.Webcam;

public class ImageCell extends TextFieldListCell<String> {
	private static final Map<Webcam, Image> imageCache = new HashMap<Webcam, Image>();
	private final List<Webcam> webcams;
	private final List<String> userDefinedCameraNames;
	
	public ImageCell(List<Webcam> webcams, List<String> userDefinedCameraNames) {
		this.webcams = webcams;
		this.userDefinedCameraNames = userDefinedCameraNames;
		
		this.setConverter(new DefaultStringConverter());
	}
	
    @Override
    public void updateItem(String item, boolean empty) {  	
        super.updateItem(item, empty);
        
        if (empty || item == null) {
        	setGraphic(null);
        	setText(null);
        	return;
        }
        
        Platform.runLater(() -> {
            	Optional<Image> webcamImg = Optional.empty();
                
                if (userDefinedCameraNames == null) {
                	for (Webcam webcam : webcams) {
                		if (webcam.getName().equals(item)) {
                				webcamImg = Optional.of(fetchWebcamImage(webcam));
                			break;
                		}
                	}
                } else {
                    int cameraIndex = userDefinedCameraNames.indexOf(item);
                    if (cameraIndex >= 0) {
                    	webcamImg = Optional.of(fetchWebcamImage(webcams.get(cameraIndex)));	
                    }
                }
                
                if (webcamImg.isPresent()) {
                    ImageView img = new ImageView(webcamImg.get());
                    img.setFitWidth(100);
                    img.setFitHeight(75);
                    
                    setGraphic(img);
                    setText(item);
                }
        });
    }
    
    private Image fetchWebcamImage(Webcam webcam) {
    	if (imageCache.containsKey(webcam)) {
    		return imageCache.get(webcam);
    	}
    	
    	boolean cameraOpened = false;
    	
		if (!webcam.isOpen()) {
			webcam.setViewSize(new Dimension(640, 480));
			webcam.open();			
			cameraOpened = true;
		}

		Image webcamImg = SwingFXUtils.toFXImage(webcam.getImage(), null);
		imageCache.put(webcam, webcamImg);
		
		if (cameraOpened == true) {
			webcam.close();
		}
		
		return webcamImg;
    }
}
