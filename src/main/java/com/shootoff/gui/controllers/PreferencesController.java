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

package com.shootoff.gui.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.CameraSelectorScene;
import com.shootoff.gui.ImageCell;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Callback;

public class PreferencesController {
	@FXML private ListView<String> webcamListView;
	@FXML private Slider detectionRateSlider;
	@FXML private Label detectionRateLabel;
	@FXML private Slider laserIntensitySlider;
	@FXML private Label laserIntensityLabel;
	@FXML private Slider markerRadiusSlider;
	@FXML private Label markerRadiusLabel;
	@FXML private ChoiceBox<String> ignoreLaserColorChoiceBox;
	@FXML private CheckBox virtualMagazineCheckBox;
	@FXML private Slider virtualMagazineSlider;
	@FXML private Label virtualMagazineLabel;
	@FXML private CheckBox malfunctionsCheckBox;
	@FXML private Slider malfunctionsSlider;
	@FXML private Label malfunctionsLabel;
	
	private Stage preferencesStage;
	private Configuration config;
	private CameraConfigListener cameraConfigListener; 
	private boolean cameraConfigChanged = false;
	private final List<Webcam> configuredCameras = new ArrayList<Webcam>();
	private final ObservableList<String> configuredNames = FXCollections.observableArrayList();
	
	public void setConfig(Configuration config, 
			CameraConfigListener cameraConfigListener) throws IOException {
	    preferencesStage = (Stage)detectionRateSlider.getScene().getWindow();
	    
	    this.cameraConfigListener = cameraConfigListener;
	    
	    ignoreLaserColorChoiceBox.setItems(FXCollections.observableArrayList(
	    		"None", "red", "green"));
	    
	    webcamListView.setCellFactory(new Callback<ListView<String>, 
	            ListCell<String>>() {
	                @Override 
	                public ListCell<String> call(ListView<String> list) {
	                    return new ImageCell(configuredCameras, configuredNames);
	                }
	            }
	        );
		
	    webcamListView.setOnKeyPressed((event) -> {
	    		if (event.getCode() == KeyCode.DELETE) {
	    			ObservableList<String> selectedNames =
	    					webcamListView.getSelectionModel().getSelectedItems();
	    			
	    			for (Iterator<Webcam> it = configuredCameras.iterator(); it.hasNext();) {
	    				Webcam webcam = it.next();
	    				if (selectedNames.contains(webcam.getName())) {
	    					it.remove();
	    				}
	    			}
	    			
	    			boolean changed = configuredNames.removeAll(selectedNames);
	    			if (!cameraConfigChanged && changed) cameraConfigChanged = changed;
	    		}
	    	});
	    
	    webcamListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	    webcamListView.setItems(configuredNames);
	    
		this.config = config;
		
		linkSliderToLabel(detectionRateSlider, detectionRateLabel);
		linkSliderToLabel(laserIntensitySlider, laserIntensityLabel);
		linkSliderToLabel(markerRadiusSlider, markerRadiusLabel);
		linkSliderToLabel(virtualMagazineSlider, virtualMagazineLabel);
		linkSliderToLabel(malfunctionsSlider, malfunctionsLabel);
		
		for (String webcamName : config.getWebcams().keySet()) {
			configuredNames.add(webcamName);
			configuredCameras.add(config.getWebcams().get(webcamName));
		}
		
		detectionRateSlider.setValue(config.getDetectionRate());
		laserIntensitySlider.setValue(config.getLaserIntensity());
		markerRadiusSlider.setValue(config.getMarkerRadius());
		ignoreLaserColorChoiceBox.setValue(config.getIgnoreLaserColorName());
		virtualMagazineCheckBox.setSelected(config.useVirtualMagazine());
		virtualMagazineSlider.setDisable(!config.useVirtualMagazine());
		virtualMagazineSlider.setValue(config.getVirtualMagazineCapacity());
		malfunctionsCheckBox.setSelected(config.useMalfunctions());
		malfunctionsSlider.setValue(config.getMalfunctionsProbability());
		malfunctionsSlider.setDisable(!config.useMalfunctions());
	}
	
	private void linkSliderToLabel(final Slider slider, final Label label) {
		slider.valueProperty().addListener(new ChangeListener<Number>() {
		      @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
		        if (newValue == null) {
		          label.setText("");
		          return;
		        }
		        label.setText(Math.round(newValue.intValue()) + "");
		      }
		    });
	}
	
	@FXML 
	public void virtualMagazineCheckBoxClicked(ActionEvent event) {
		virtualMagazineSlider.setDisable(!virtualMagazineCheckBox.isSelected());
	}

	@FXML 
	public void malfunctionsCheckBoxClicked(ActionEvent event) {
		malfunctionsSlider.setDisable(!malfunctionsCheckBox.isSelected());
	}
	
	@FXML
	public void addCameraClicked(ActionEvent event) {
		CameraSelectorScene cameraSelector = new CameraSelectorScene(
				preferencesStage, configuredCameras);
		
		cameraSelector.setOnHidden((e) -> {
				if (!cameraSelector.getSelectedWebcams().isEmpty()) {
					for (Webcam webcam : cameraSelector.getSelectedWebcams()) {
						boolean changed = configuredNames.add(webcam.getName());
						configuredCameras.add(webcam);
						
		    			if (!cameraConfigChanged && changed) cameraConfigChanged = changed;
					}
				}
			});
	}
	
	@FXML 
	public void okClicked(ActionEvent event) throws ConfigurationException, IOException {
		config.setWebcams(configuredNames, configuredCameras);
		config.setDetectionRate((int)detectionRateSlider.getValue());
		config.setLaserIntensity((int)laserIntensitySlider.getValue());
		config.setMarkerRadius((int)markerRadiusSlider.getValue());
		config.setIgnoreLaserColor(!ignoreLaserColorChoiceBox.getValue().equals("None"));
		config.setIgnoreLaserColorName(ignoreLaserColorChoiceBox.getValue());
		config.setUseVirtualMagazine(virtualMagazineCheckBox.isSelected());
		config.setVirtualMagazineCapacity((int)virtualMagazineSlider.getValue());
		config.setMalfunctions(malfunctionsCheckBox.isSelected());
		config.setMalfunctionsProbability((float)malfunctionsSlider.getValue());
		
		config.writeConfigurationFile();
		preferencesStage.close();
		
		if (cameraConfigChanged) cameraConfigListener.cameraConfigUpdated();
	}

	@FXML 
	public void cancelClicked(ActionEvent event) {
		preferencesStage.close();
	}
}
