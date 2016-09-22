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

import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.DesignateShotRecorderListener;
import com.shootoff.gui.CheckableImageListCell;
import com.shootoff.gui.CheckableImageListCell.CameraRenamedListener;
import com.shootoff.gui.CheckableImageListCell.CameraSelectionListener;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class PreferencesController implements DesignateShotRecorderListener, CameraSelectionListener,
		CameraRenamedListener {
	@FXML private ScrollPane preferencesPane;
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
	@FXML private ChoiceBox<String> calibratedOptionsChoiceBox;
	@FXML private CheckBox showArenaShotMarkersCheckBox;

	private Stage parent;
	private Configuration config;
	private CalibrationConfigurator calibrationConfigurator;
	private CameraConfigListener cameraConfigListener;
	private boolean cameraConfigChanged = false;
	private final Set<Camera> recordingCameras = new HashSet<>();
	private final List<Camera> configuredCameras = new ArrayList<>();
	private final List<String> configuredNames = new ArrayList<>();
	private final Set<Camera> camerasOnShown = new HashSet<>();
	private final ObservableList<String> cameras = FXCollections.observableArrayList();

	public void setConfig(Stage parent, Configuration config, CalibrationConfigurator calibrationConfigurator,
			CameraConfigListener cameraConfigListener) {
		CheckableImageListCell.createImageCache(CameraFactory.getWebcams(), this);

		this.parent = parent;
		this.config = config;
		this.calibrationConfigurator = calibrationConfigurator;
		this.cameraConfigListener = cameraConfigListener;

		ignoreLaserColorChoiceBox.setItems(FXCollections.observableArrayList("None", "red", "green"));
		calibratedOptionsChoiceBox.setItems(FXCollections.observableArrayList(CalibrationOption.EVERYWHERE.toString(), 
				CalibrationOption.ONLY_IN_BOUNDS.toString(), CalibrationOption.CROP.toString()));

		webcamListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> list) {
				return new CheckableImageListCell(CameraFactory.getWebcams(), configuredNames, configuredCameras, 
						PreferencesController.this, PreferencesController.this, 
						Optional.of(config.getRecordingCameras()));
			}
		});

		webcamListView.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
				removeSelectedIpCams();
			}
		});

		webcamListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		linkSliderToLabel(markerRadiusSlider, markerRadiusLabel);
		linkSliderToLabel(virtualMagazineSlider, virtualMagazineLabel);
		linkSliderToLabel(malfunctionsSlider, malfunctionsLabel);

		for (String webcamName : config.getWebcams().keySet()) {
			final Camera c = config.getWebcams().get(webcamName);
			
			configuredNames.add(webcamName);
			configuredCameras.add(c);
			cameras.add(webcamName);
			
			CheckableImageListCell.getCameraCheckBoxes().get(c).setSelected(true);
		}
		
		for (Camera c : CameraFactory.getWebcams()) {
			if (!cameras.contains(c.getName())) cameras.add(c.getName());
		}
		
		webcamListView.setItems(cameras);

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
		calibratedOptionsChoiceBox.setValue(config.getCalibratedFeedBehavior().toString());
		showArenaShotMarkersCheckBox.setSelected(config.showArenaShotMarkers());
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
	
	public void cameraSelectionChanged(Camera camera, boolean isSelected) {
		if (isSelected && !configuredCameras.contains(camera)) {			
			configuredCameras.add(camera);
			configuredNames.add(camera.getName());
		} else if (!isSelected) {
			int cameraIndex = configuredCameras.indexOf(camera);
			
			if (cameraIndex > -1) {
				configuredCameras.remove(cameraIndex);
				configuredNames.remove(cameraIndex);
			}
		}
		
		cameraConfigChanged = true;
	}
	
	public void cameraRenamed(String oldName, String newName) {
		int oldIndex = configuredNames.indexOf(oldName);
		
		if (oldIndex > -1) {
			configuredNames.set(oldIndex, newName);
		}
		
		cameraConfigChanged = true;
	}
	
	public void prepareToShow() {
		cameraConfigChanged = false;
		camerasOnShown.clear();
		camerasOnShown.addAll(configuredCameras);
	}
	
	public Node getPane() {
		return preferencesPane;
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

		return Optional.ofNullable(fileChooser.showOpenDialog(parent));
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
	public void registerCameraClicked(ActionEvent event) {
		collectIpCamInfo();
	}

	private void collectIpCamInfo() {
		final Stage ipcamStage = new Stage();
		final GridPane ipcamPane = new GridPane();

		final ColumnConstraints cc = new ColumnConstraints(400);
		cc.setHalignment(HPos.CENTER);
		ipcamPane.getColumnConstraints().addAll(new ColumnConstraints(), cc);

		final TextField nameTextField = new TextField();
		ipcamPane.add(new Label("IPCam Name:"), 0, 0);
		ipcamPane.add(nameTextField, 1, 0);

		final TextField userTextField = new TextField();
		userTextField.setPromptText("Optional Username");
		ipcamPane.add(new Label("Username:"), 0, 1);
		ipcamPane.add(userTextField, 1, 1);

		final PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Optional Password");
		ipcamPane.add(new Label("Password:"), 0, 2);
		ipcamPane.add(passwordField, 1, 2);

		final TextField urlTextField = new TextField("http://");
		ipcamPane.add(new Label("IPCam URL:"), 0, 3);
		ipcamPane.add(urlTextField, 1, 3);

		final Button okButton = new Button("OK");
		okButton.setDefaultButton(true);
		ipcamPane.add(okButton, 1, 4);

		okButton.setOnAction((e) -> {
			if (nameTextField.getText().isEmpty() || urlTextField.getText().isEmpty()) {
				final Alert ipcamInfoAlert = new Alert(AlertType.ERROR);
				ipcamInfoAlert.setTitle("Missing Information");
				ipcamInfoAlert.setHeaderText("Missing Required IPCam Information!");
				ipcamInfoAlert.setResizable(true);
				ipcamInfoAlert.setContentText("Please fill in both the IPCam name and the URL.");
				ipcamInfoAlert.showAndWait();
				return;
			}

			Optional<String> username = Optional.empty();
			Optional<String> password = Optional.empty();

			if (!userTextField.getText().isEmpty() || !passwordField.getText().isEmpty()) {
				username = Optional.of(userTextField.getText());
				password = Optional.of(passwordField.getText());
			}

			Optional<Camera> cam = config.registerIpCam(nameTextField.getText(), urlTextField.getText(), username,
					password);

			if (cam.isPresent()) {
				CheckableImageListCell.cacheCamera(cam.get(), PreferencesController.this);

				if (!configuredCameras.contains(cam.get())) {
					Platform.runLater(() -> {
						webcamListView.setItems(null);
						cameras.add(cam.get().getName());
						webcamListView.setItems(cameras);
					});
				}
			}

			ipcamStage.close();
		});

		final Scene scene = new Scene(ipcamPane);
		ipcamStage.initOwner(preferencesPane.getScene().getWindow());
		ipcamStage.initModality(Modality.WINDOW_MODAL);
		ipcamStage.setTitle("Register IPCam");
		ipcamStage.setScene(scene);
		ipcamStage.showAndWait();
	}

	private void removeSelectedIpCams() {
		final ObservableList<String> selectedNames = webcamListView.getSelectionModel().getSelectedItems();

		if (selectedNames.isEmpty()) return;

		final List<String> removedCameraNames = new ArrayList<String>();

		for (String webcamName : selectedNames) {
			if (config.getRegistedIpCams().containsKey(webcamName)) {
				config.unregisterIpCam(webcamName);
				removedCameraNames.add(webcamName);
			}
		}
		
		final Iterator<Camera> it = configuredCameras.iterator();
		while (it.hasNext()) {
			final Camera c = it.next();
			if (removedCameraNames.contains(c.getName())) {
				it.remove();
			}
		}

		configuredNames.removeAll(removedCameraNames);
		cameras.removeAll(removedCameraNames);
	}
	
	private boolean cameraListChanged() {
		for (Camera c : configuredCameras) {
			// Tried to remove a camera that is in the new
			// list but that wasn't in the list when the
			// GUI was shown
			if (!camerasOnShown.remove(c)) return true;
		}

		// If the list is not empty by the time we get here,
		// there was a camera configured when the GUI was 
		// started that isn't configured now
		return !camerasOnShown.isEmpty();
	}

	public void save() throws ConfigurationException, IOException {
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
		config.setCalibratedFeedBehavior(CalibrationOption.fromString(calibratedOptionsChoiceBox.getValue()));
		config.setShowArenaShotMarkers(showArenaShotMarkersCheckBox.isSelected());

		if (config.writeConfigurationFile()) {
			calibrationConfigurator.calibratedFeedBehaviorsChanged();
			if (cameraConfigChanged && cameraListChanged()) {
				cameraConfigListener.cameraConfigUpdated();
			}
		}
	}
}
