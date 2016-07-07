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

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.targets.Target;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class TargetDistancePane extends GridPane {
	private static final Logger logger = LoggerFactory.getLogger(TargetDistancePane.class);

	private String currentTargetWidth;
	private String currentTargetHeight;
	private String currentTargetDistance;
	private String shooterDistance;
	private String cameraDistance;
	private String newTargetDistance;

	private final Target target;
	private final PerspectiveManager perspectiveManager;
	private final Configuration config;
	private final String cameraName;

	private boolean userCancelled = false;
	private boolean cameraDistanceEdited = false;

	public TargetDistancePane(Target target, PerspectiveManager perspectiveManager, Configuration config) {
		this.target = target;
		this.perspectiveManager = perspectiveManager;
		this.config = config;
		this.cameraName = perspectiveManager.getCalibratedCameraName();

		if (target.tagExists(Target.TAG_DEFAULT_PERCEIVED_WIDTH)) {
			currentTargetWidth = target.getTag(Target.TAG_DEFAULT_PERCEIVED_WIDTH);
		} else {
			currentTargetWidth = "";
		}

		if (target.tagExists(Target.TAG_DEFAULT_PERCEIVED_HEIGHT)) {
			currentTargetHeight = target.getTag(Target.TAG_DEFAULT_PERCEIVED_HEIGHT);
		} else {
			currentTargetHeight = "";
		}

		if (target.tagExists(Target.TAG_CURRENT_PERCEIVED_DISTANCE)) {
			currentTargetDistance = target.getTag(Target.TAG_CURRENT_PERCEIVED_DISTANCE);
		} else if (target.tagExists(Target.TAG_DEFAULT_PERCEIVED_DISTANCE)) {
			currentTargetDistance = target.getTag(Target.TAG_DEFAULT_PERCEIVED_DISTANCE);
		} else {
			currentTargetDistance = "";
		}

		if (perspectiveManager.getCameraDistance() > 0) {
			cameraDistance = String.valueOf(perspectiveManager.getCameraDistance());
		} else if (config.getCameraDistance(cameraName).isPresent()) {
			cameraDistance = String.valueOf(config.getCameraDistance(cameraName).get());
		} else {
			cameraDistance = "";
		}

		if (!cameraDistance.isEmpty() && !perspectiveManager.isCameraParamsKnown()
				&& (perspectiveManager.getProjectionWidth() == -1 || perspectiveManager.getProjectionHeight() == -1)) {
			throw new AssertionError("The camera parameters and paper dimensions are unknown. We should not have been "
					+ "able to get here.");
		}

		if (target.tagExists(Target.TAG_SHOOTER_DISTANCE)) {
			shooterDistance = target.getTag(Target.TAG_SHOOTER_DISTANCE);
		} else if (!cameraDistance.isEmpty()) {
			shooterDistance = cameraDistance;
		} else {
			shooterDistance = "";
		}

		layoutGui();
	}

	private static class NumberOnlyChangeListener implements ChangeListener<String> {
		private final TextField observedTextField;

		public NumberOnlyChangeListener(TextField observedTextField) {
			this.observedTextField = observedTextField;
		}

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			if (newValue.matches("\\d*")) {
				// int value = Integer.parseInt(newValue);
			} else {
				observedTextField.setText(oldValue);
				observedTextField.positionCaret(observedTextField.getLength());
			}
		}
	}

	private void layoutGui() {
		final int ROW_OFFSET;
		final boolean collectDimensions = currentTargetWidth.isEmpty() || currentTargetHeight.isEmpty();
		final TextField targetWidthTextField = new TextField(currentTargetWidth);
		final TextField targetHeightTextField = new TextField(currentTargetHeight);

		if (collectDimensions) {
			final int WIDTH_ROW = 0;
			targetWidthTextField.textProperty().addListener(new NumberOnlyChangeListener(targetWidthTextField));
			final Label targetWidthLabel = new Label("Target Width (mm): ");
			targetWidthLabel.setLabelFor(targetWidthTextField);

			this.add(targetWidthLabel, 0, WIDTH_ROW);
			this.add(targetWidthTextField, 1, WIDTH_ROW);

			final int HEIGHT_ROW = 1;
			targetHeightTextField.textProperty().addListener(new NumberOnlyChangeListener(targetHeightTextField));
			final Label targetHeightLabel = new Label("Target Height (mm): ");
			targetHeightLabel.setLabelFor(targetHeightTextField);

			this.add(targetHeightLabel, 0, HEIGHT_ROW);
			this.add(targetHeightTextField, 1, HEIGHT_ROW);

			ROW_OFFSET = 2;
		} else {
			ROW_OFFSET = 0;
		}

		final int DISTANCE_ROW = ROW_OFFSET;
		final TextField targetDistanceTextField = new TextField(currentTargetDistance);
		targetDistanceTextField.textProperty().addListener(new NumberOnlyChangeListener(targetDistanceTextField));
		final Label targetDistanceLabel = new Label("Target Distance (mm): ");
		targetDistanceLabel.setLabelFor(targetDistanceTextField);

		this.add(targetDistanceLabel, 0, DISTANCE_ROW);
		this.add(targetDistanceTextField, 1, DISTANCE_ROW);

		final int SHOOTER_DISTANCE_ROW = 1 + ROW_OFFSET;
		final TextField shooterDistanceTextField = new TextField(shooterDistance);
		shooterDistanceTextField.textProperty().addListener(new NumberOnlyChangeListener(shooterDistanceTextField));
		final Label shooterDistanceLabel = new Label("Shooter Distance (mm): ");
		shooterDistanceLabel.setLabelFor(shooterDistanceTextField);

		this.add(shooterDistanceLabel, 0, SHOOTER_DISTANCE_ROW);
		this.add(shooterDistanceTextField, 1, SHOOTER_DISTANCE_ROW);

		final int CAMERA_DISTANCE_ROW = 2 + ROW_OFFSET;
		final TextField cameraDistanceTextField = new TextField(cameraDistance);
		cameraDistanceTextField.textProperty().addListener(new NumberOnlyChangeListener(cameraDistanceTextField));
		final Label cameraDistanceLabel = new Label("Camera Distance (mm): ");
		cameraDistanceLabel.setLabelFor(cameraDistanceTextField);

		this.add(cameraDistanceLabel, 0, CAMERA_DISTANCE_ROW);
		this.add(cameraDistanceTextField, 1, CAMERA_DISTANCE_ROW);

		final int BUTTONS_ROW = 3 + ROW_OFFSET;
		final Button cancelButton = new Button("Cancel");

		cancelButton.setOnAction((event) -> {
			userCancelled = true;
			close();
		});

		final Button okButton = new Button("OK");

		okButton.setOnAction((event) -> {
			if (collectDimensions
					&& (targetWidthTextField.getText().isEmpty() || targetHeightTextField.getText().isEmpty())
					|| targetDistanceTextField.getText().isEmpty()) {
				Alert missingDataAlert = new Alert(AlertType.ERROR);

				String message = "All target distance settings must be filled in, otherwise there is not enough "
						+ "information to calculate the target size.";

				missingDataAlert.setTitle("Missing Data");
				missingDataAlert.setHeaderText("Critical Distance Data Missing!");
				missingDataAlert.setResizable(true);
				missingDataAlert.setContentText(message);
				missingDataAlert.initOwner((Stage) this.getScene().getWindow());
				missingDataAlert.showAndWait();
			} else {
				if (collectDimensions) {
					currentTargetWidth = targetWidthTextField.getText();
					currentTargetHeight = targetHeightTextField.getText();
				}
				newTargetDistance = targetDistanceTextField.getText();
				shooterDistance = shooterDistanceTextField.getText();
				if (!cameraDistance.equals(cameraDistanceTextField.getText())) cameraDistanceEdited = true;
				cameraDistance = cameraDistanceTextField.getText();

				persistSettings();

				close();
			}
		});

		okButton.setDefaultButton(true);

		this.add(okButton, 0, BUTTONS_ROW);
		this.add(cancelButton, 1, BUTTONS_ROW);
	}

	public int getDefaultTargetWidth() {
		return Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_WIDTH));
	}

	public int getDefaultTargetHeight() {
		return Integer.parseInt(target.getTag(Target.TAG_DEFAULT_PERCEIVED_HEIGHT));
	}

	public int getCurrentTargetDistance() {
			return Integer.parseInt(currentTargetDistance);
	}

	public int getNewTargetDistance() {
		return Integer.parseInt(newTargetDistance);
	}

	private void close() {
		((Stage) this.getScene().getWindow()).close();
	}

	public boolean userCancelled() {
		return userCancelled;
	}

	private void persistSettings() {
		Map<String, String> tags = target.getAllTags();
		if (!currentTargetWidth.isEmpty()) {
			tags.put(Target.TAG_DEFAULT_PERCEIVED_WIDTH, currentTargetWidth);
		}
		if (!currentTargetHeight.isEmpty()) {
			tags.put(Target.TAG_DEFAULT_PERCEIVED_HEIGHT, currentTargetHeight);
		}
		tags.put(Target.TAG_CURRENT_PERCEIVED_DISTANCE, newTargetDistance);
		tags.put(Target.TAG_SHOOTER_DISTANCE, shooterDistance);

		if (cameraDistanceEdited) {
			config.setCameraDistance(cameraName, Integer.parseInt(cameraDistance));
			try {
				config.writeConfigurationFile();
			} catch (ConfigurationException | IOException e) {
				logger.error("Failed to persist user-defined camera distance", e);
			}
		}

		perspectiveManager.setShooterDistance(Integer.parseInt(shooterDistance));
		perspectiveManager.setCameraDistance(Integer.parseInt(cameraDistance));
	}
}
