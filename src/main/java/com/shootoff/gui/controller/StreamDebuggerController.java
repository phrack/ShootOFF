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

package com.shootoff.gui.controller;

import java.awt.Color;
import java.awt.image.BufferedImage;

import com.shootoff.camera.CameraManager;
import com.shootoff.gui.ThresholdListener;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;

public class StreamDebuggerController implements ThresholdListener {
	@FXML private ImageView thresholdImageView;
	@FXML private Slider colorDifferenceSlider;
	@FXML private Slider centerBorderSlider;
	@FXML private Slider minDimSlider;
	@FXML private Slider bloomCountSlider;
	
	public void init(CameraManager cameraManager) {
		cameraManager.setThresholdListener(this);

		colorDifferenceSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override 
			public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
	        	if (newValue == null) return;
	        
        		cameraManager.setColorDiffThreshold(newValue.doubleValue());
	      	}
	    });
		
		centerBorderSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override 
			public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
	        	if (newValue == null) return;
	        
        		cameraManager.setCenterApproxBorderSize(newValue.intValue());
	      	}
	    });
		
		minDimSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override 
			public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
	        	if (newValue == null) return;
	        
        		cameraManager.setMinimumShotDimension(newValue.intValue());
	      	}
	    });
		
		bloomCountSlider.valueProperty().addListener(new ChangeListener<Number>() {
				@Override 
				public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
		        	if (newValue == null) return;
		        
	        		cameraManager.setBloomCount(newValue.intValue());
		      	}
		    });
	}
	
	public ImageView getThresholdImageView() {
		return thresholdImageView;	
	}

	@Override
	public void updateThreshold(BufferedImage thresholdImg, byte[][] mask) {
		BufferedImage coloredImg = new BufferedImage(thresholdImg.getWidth(), thresholdImg.getHeight(), 
				BufferedImage.TYPE_INT_RGB);
		coloredImg.createGraphics().drawImage(thresholdImg, 0, 0, null);
		
		for (int x = 0; x < thresholdImg.getWidth(); x++) {
			for (int y = 0; y < thresholdImg.getHeight(); y++) {
				if (mask[y][x] == 1) {
					coloredImg.setRGB(x, y, Color.RED.getRGB());
				}
			}
		}
		
		thresholdImageView.setImage(SwingFXUtils.toFXImage(coloredImg, null));
	}
}
