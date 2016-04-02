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

package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.shootoff.camera.Camera;
import com.shootoff.config.Configuration;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

/**
 * Used to display a list of connected cameras to the user that are not
 * currently being used by ShootOFF. More specifically, this GUI lets users add
 * new cameras to be used for shot detection. Also used to allow users to
 * register and de-register IP cameras via the GUI.
 * 
 * @see com.shootoff.camera.Camera
 * 
 * @author phrack
 */
public class CameraSelectorScene extends Stage {
	private final Configuration config;
	private final List<Camera> configuredCameras;
	private final List<Camera> unconfiguredWebcams = new ArrayList<Camera>();
	private final List<Camera> selectedWebcams = new ArrayList<Camera>();
	private final ListView<String> webcamListView = new ListView<String>();
	private final ObservableList<String> webcams = FXCollections.observableArrayList();

	public CameraSelectorScene(Configuration config, Window parent, List<Camera> configuredCameras) {
		super();
		this.initOwner(parent);
		this.initModality(Modality.WINDOW_MODAL);

		this.config = config;
		this.configuredCameras = configuredCameras;

		for (Camera webcam : Camera.getWebcams()) {
			if (!configuredCameras.contains(webcam)) {
				unconfiguredWebcams.add(webcam);
				Platform.runLater(() -> {
					webcams.add(webcam.getName());
				});
			}
		}

		BorderPane pane = new BorderPane();

		final Button registerIpCamButton = new Button("Register IPCam");
		registerIpCamButton.setOnAction((event) -> {
			collectIpCamInfo();
		});

		webcamListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> list) {
				return new ImageCell(unconfiguredWebcams, null, Optional.empty(), Optional.empty());
			}
		});

		webcamListView.setOnMouseClicked((event) -> {
			if (event.getClickCount() == 2) {
				addSelection();
			}
		});

		webcamListView.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				addSelection();
			} else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
				removeSelectedIpCams();
			}
		});

		webcamListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		webcamListView.setItems(webcams);

		VBox v = new VBox(registerIpCamButton, webcamListView);
		v.setAlignment(Pos.CENTER);
		pane.setTop(v);

		final Button okButton = new Button("OK");
		okButton.setDefaultButton(true);
		okButton.setOnAction((event) -> {
			addSelection();
		});

		final Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction((event) -> {
			this.close();
		});

		HBox h = new HBox(okButton, cancelButton);
		h.setSpacing(10);
		pane.setRight(h);

		Scene scene = new Scene(pane);
		this.setScene(scene);
		this.show();
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
				Alert ipcamInfoAlert = new Alert(AlertType.ERROR);
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
				ImageCell.cacheCamera(cam.get());

				if (!configuredCameras.contains(cam.get())) {
					unconfiguredWebcams.add(cam.get());

					Platform.runLater(() -> {
						webcamListView.setItems(null);
						webcams.add(cam.get().getName());
						webcamListView.setItems(webcams);
					});
				}
			}

			ipcamStage.close();
		});

		final Scene scene = new Scene(ipcamPane);
		ipcamStage.initOwner(this);
		ipcamStage.initModality(Modality.WINDOW_MODAL);
		ipcamStage.setTitle("Register IPCam");
		ipcamStage.setScene(scene);
		ipcamStage.showAndWait();
	}

	private void addSelection() {
		final ObservableList<String> selectedNames = webcamListView.getSelectionModel().getSelectedItems();

		if (selectedNames.isEmpty()) return;

		for (Camera webcam : unconfiguredWebcams) {
			if (selectedNames.contains(webcam.getName())) {
				selectedWebcams.add(webcam);
			}
		}

		this.close();
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

		webcams.removeAll(removedCameraNames);
	}

	public List<Camera> getSelectedWebcams() {
		return selectedWebcams;
	}
}
