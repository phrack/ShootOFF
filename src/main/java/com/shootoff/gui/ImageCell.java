/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.gui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.Camera;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.converter.DefaultStringConverter;

public class ImageCell extends TextFieldListCell<String> {
	private static final Map<Camera, ImageView> imageCache = new HashMap<Camera, ImageView>();
	private final List<Camera> webcams;
	private final List<String> userDefinedCameraNames;
	
	public ImageCell(List<Camera> webcams, List<String> userDefinedCameraNames) {
		this.webcams = webcams;
		this.userDefinedCameraNames = userDefinedCameraNames;
		
		this.setConverter(new DefaultStringConverter());
		
		for (Camera c : webcams) {
			if (imageCache.containsKey(c)) continue;
			
			ImageView iv = new ImageView();
            iv.setFitWidth(100);
            iv.setFitHeight(75);
			imageCache.put(c, iv);
			
			new Thread(() -> {
					Optional<Image> img = fetchWebcamImage(c);
					
					if (img.isPresent()) {
						imageCache.get(c).setImage(img.get());
					}
				}).start();
		}
	}
	
    @Override
    public void updateItem(String item, boolean empty) {  	
        super.updateItem(item, empty);
        
        if (empty || item == null) {
        	setGraphic(null);
        	setText(null);
        	return;
        }

    	Optional<ImageView> webcamIV = Optional.empty();
        
        if (userDefinedCameraNames == null) {
        	for (Camera webcam : webcams) {
        		if (webcam.getName().equals(item)) {
        			webcamIV = Optional.of(imageCache.get(webcam));
        		}
        	}
        } else {
            int cameraIndex = userDefinedCameraNames.indexOf(item);
            if (cameraIndex >= 0) {
            	webcamIV = Optional.of(imageCache.get(webcams.get(cameraIndex)));	
            }
        }
        
        if (webcamIV.isPresent()) {
            setGraphic(webcamIV.get());
        }
        
        setText(item);
    }
    
    private Optional<Image> fetchWebcamImage(Camera webcam) {
    	boolean cameraOpened = false;
    	
		if (!webcam.isOpen()) {
			webcam.setViewSize(new Dimension(640, 480));
			webcam.open();			
			cameraOpened = true;
		}

		Image webcamImg = null;
		if (webcam.isOpen()) {
			BufferedImage img = webcam.getImage();
			
			if (img != null) {
				webcamImg = SwingFXUtils.toFXImage(img, null);
			}
		}
		
		if (cameraOpened == true) {
			webcam.close();
		}
		
		return Optional.ofNullable(webcamImg);
    }
}
