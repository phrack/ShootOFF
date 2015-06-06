package com.shootoff.gui;

import com.shootoff.camera.CameraManager;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
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
	public void updateThreshold(Image thresholdImg) {
		thresholdImageView.setImage(thresholdImg);
	}
}
