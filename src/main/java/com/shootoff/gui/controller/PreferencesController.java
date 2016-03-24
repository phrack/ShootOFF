/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.shootoff.camera.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.CameraSelectorScene;
import com.shootoff.gui.DesignateShotRecorderListener;
import com.shootoff.gui.ImageCell;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class PreferencesController implements DesignateShotRecorderListener {
	@FXML private ListView<String> webcamListView;
	@FXML private Slider markerRadiusSlider;
	@FXML private Label markerRadiusLabel;
	@FXML private ChoiceBox<String> ignoreLaserColorChoiceBox;
	@FXML private CheckBox redLaserSoundCheckBox;
	@FXML private TextField redLaserSoundTextField;
	@FXML private Button redLaserSoundButton;
	@FXML private CheckBox greenLaserSoundCheckBox;
	@FXML private TextField greenLaserSoundTextField;
	@FXML private Button greenLaserSoundButton;
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
	private final Set<Camera> recordingCameras = new HashSet<Camera>();
	private final List<Camera> configuredCameras = new ArrayList<Camera>();
	private final ObservableList<String> configuredNames = FXCollections.observableArrayList();

	public void setConfig(Configuration config, CameraConfigListener cameraConfigListener) throws IOException {
		ImageCell.createImageCache(Camera.getWebcams());

		preferencesStage = (Stage) markerRadiusSlider.getScene().getWindow();

		this.cameraConfigListener = cameraConfigListener;

		ignoreLaserColorChoiceBox.setItems(FXCollections.observableArrayList("None", "red", "green"));

		webcamListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> list) {
				return new ImageCell(configuredCameras, configuredNames, Optional.of(PreferencesController.this),
						Optional.of(config.getRecordingCameras()));
			}
		});

		webcamListView.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
				ObservableList<String> selectedNames = webcamListView.getSelectionModel().getSelectedItems();

				for (Iterator<Camera> it = configuredCameras.iterator(); it.hasNext();) {
					Camera webcam = it.next();
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

		linkSliderToLabel(markerRadiusSlider, markerRadiusLabel);
		linkSliderToLabel(virtualMagazineSlider, virtualMagazineLabel);
		linkSliderToLabel(malfunctionsSlider, malfunctionsLabel);

		for (String webcamName : config.getWebcams().keySet()) {
			configuredNames.add(webcamName);
			configuredCameras.add(config.getWebcams().get(webcamName));
		}

		markerRadiusSlider.setValue(config.getMarkerRadius());
		ignoreLaserColorChoiceBox.setValue(config.getIgnoreLaserColorName());
		redLaserSoundCheckBox.setSelected(config.useRedLaserSound());
		redLaserSoundTextField.setText(config.getRedLaserSound().getPath());
		redLaserSoundTextField.setDisable(!config.useRedLaserSound());
		redLaserSoundButton.setDisable(!config.useRedLaserSound());
		greenLaserSoundCheckBox.setSelected(config.useGreenLaserSound());
		greenLaserSoundTextField.setText(config.getGreenLaserSound().getPath());
		greenLaserSoundTextField.setDisable(!config.useGreenLaserSound());
		greenLaserSoundButton.setDisable(!config.useGreenLaserSound());
		virtualMagazineCheckBox.setSelected(config.useVirtualMagazine());
		virtualMagazineSlider.setDisable(!config.useVirtualMagazine());
		virtualMagazineSlider.setValue(config.getVirtualMagazineCapacity());
		malfunctionsCheckBox.setSelected(config.useMalfunctions());
		malfunctionsSlider.setValue(config.getMalfunctionsProbability());
		malfunctionsSlider.setDisable(!config.useMalfunctions());
	}

	private void linkSliderToLabel(final Slider slider, final Label label) {
		slider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
				if (newValue == null) {
					label.setText("");
					return;
				}
				label.setText(String.valueOf(newValue.intValue()));
			}
		});
	}

	@Override
	public void registerShotRecorder(String webcamName) {
		for (Camera c : configuredCameras) {
			if (c.getName().equals(webcamName)) {
				cameraConfigChanged = recordingCameras.add(c);
				break;
			}
		}
	}

	@Override
	public void unregisterShotRecorder(String webcamName) {
		for (Camera c : configuredCameras) {
			if (c.getName().equals(webcamName)) {
				cameraConfigChanged = recordingCameras.remove(c);
				break;
			}
		}
	}

	@FXML
	public void redLaserSoundCheckBoxClicked(ActionEvent event) {
		redLaserSoundTextField.setDisable(!redLaserSoundCheckBox.isSelected());
		redLaserSoundButton.setDisable(!redLaserSoundCheckBox.isSelected());
	}

	@FXML
	public void greenLaserSoundCheckBoxClicked(ActionEvent event) {
		greenLaserSoundTextField.setDisable(!greenLaserSoundCheckBox.isSelected());
		greenLaserSoundButton.setDisable(!greenLaserSoundCheckBox.isSelected());
	}

	@FXML
	public void redLaserSoundButtonClicked(ActionEvent event) {
		Optional<File> soundFile = selectSoundFile();
		if (soundFile.isPresent()) redLaserSoundTextField.setText(soundFile.get().getPath());
	}

	@FXML
	public void greenLaserSoundButtonClicked(ActionEvent event) {
		Optional<File> soundFile = selectSoundFile();
		if (soundFile.isPresent()) greenLaserSoundTextField.setText(soundFile.get().getPath());
	}

	private Optional<File> selectSoundFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Shot Sound");
		fileChooser.setInitialDirectory(new File(System.getProperty("shootoff.home") + File.separator + "sounds"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Sound File", "*.mp3", "*.wav"));

		return Optional.ofNullable(fileChooser.showOpenDialog(preferencesStage));
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
		CameraSelectorScene cameraSelector = new CameraSelectorScene(config, preferencesStage, configuredCameras);

		cameraSelector.setOnHidden((e) -> {
			if (cameraSelector.getSelectedWebcams().isEmpty()) return;

			for (Camera webcam : cameraSelector.getSelectedWebcams()) {
				boolean changed = configuredNames.add(webcam.getName());
				configuredCameras.add(webcam);

				if (!cameraConfigChanged && changed) cameraConfigChanged = changed;
			}
		});
	}

	@FXML
	public void okClicked(ActionEvent event) throws ConfigurationException, IOException {
		config.setWebcams(configuredNames, configuredCameras);
		config.setRecordingCameras(recordingCameras);
		config.setMarkerRadius((int) markerRadiusSlider.getValue());
		config.setIgnoreLaserColor(!ignoreLaserColorChoiceBox.getValue().equals("None"));
		config.setIgnoreLaserColorName(ignoreLaserColorChoiceBox.getValue());
		config.setUseRedLaserSound(redLaserSoundCheckBox.isSelected());
		config.setRedLaserSound(new File(redLaserSoundTextField.getText()));
		config.setUseGreenLaserSound(greenLaserSoundCheckBox.isSelected());
		config.setGreenLaserSound(new File(greenLaserSoundTextField.getText()));
		config.setUseVirtualMagazine(virtualMagazineCheckBox.isSelected());
		config.setVirtualMagazineCapacity((int) virtualMagazineSlider.getValue());
		config.setMalfunctions(malfunctionsCheckBox.isSelected());
		config.setMalfunctionsProbability((float) malfunctionsSlider.getValue());

		if (config.writeConfigurationFile()) {
			preferencesStage.close();

			if (cameraConfigChanged) cameraConfigListener.cameraConfigUpdated();
		}
	}

	@FXML
	public void cancelClicked(ActionEvent event) {
		preferencesStage.close();
	}
}
