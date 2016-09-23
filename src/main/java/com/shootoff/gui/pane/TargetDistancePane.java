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

package com.shootoff.gui.pane;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.targets.Target;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class TargetDistancePane extends Pane {
	private final static Logger logger = LoggerFactory.getLogger(TargetDistancePane.class);
	
	private final Target target;
	private final PerspectiveManager perspectiveManager;
	private final Configuration config;
	private final String cameraName;
	private final String originalTargetDistance;
	private final String originalCameraDistance;
	private final TextField shooterDistance;
	private final TextField targetDistance;
	private final TextField cameraDistance;
	private final TextField targetWidth;
	private final TextField targetHeight;
	
	private boolean defaultsSet = false;

	public TargetDistancePane(Target target, PerspectiveManager perspectiveManager, Configuration config) {
		this.target = target;
		this.perspectiveManager = perspectiveManager;
		this.config = config;
		this.cameraName = perspectiveManager.getCalibratedCameraName();
		
		final Image backgroundImage = new Image(
				TargetDistancePane.class.getResourceAsStream("/images/perspective_settings.png"));
		this.getChildren().add(new ImageView(backgroundImage));
		
		shooterDistance = createDistanceTextField(234, 68);
		targetDistance = createDistanceTextField(534, 126);
		cameraDistance = createDistanceTextField(329, 252);
		targetWidth = createDistanceTextField(745, 48);
		targetHeight = createDistanceTextField(863, 191);
		
		this.getChildren().add(shooterDistance);
		this.getChildren().add(targetDistance);
		this.getChildren().add(cameraDistance);
		this.getChildren().add(targetWidth);
		this.getChildren().add(targetHeight);
		
		setDefaults();
		
		originalCameraDistance = cameraDistance.getText();
		originalTargetDistance = targetDistance.getText();
	}
	
	private TextField createDistanceTextField(double x, double y) {
		final TextField distanceTextField = new TextField();
		distanceTextField.setPromptText("(mm)");
		distanceTextField.setLayoutX(x);
		distanceTextField.setLayoutY(y);
		distanceTextField.setPrefWidth(75);
		distanceTextField.setAlignment(Pos.CENTER);
		distanceTextField.textProperty().addListener(new NumberOnlyChangeListener(distanceTextField));
		
		return distanceTextField;
	}
	
	private class NumberOnlyChangeListener implements ChangeListener<String> {
		private final TextField observedTextField;

		public NumberOnlyChangeListener(TextField observedTextField) {
			this.observedTextField = observedTextField;
		}

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			if (!defaultsSet) return;
			
			if (!newValue.matches("\\d*")) {
				observedTextField.setText(oldValue);
				observedTextField.positionCaret(observedTextField.getLength());
			} else {
				setDistance();
			}
		}
	}
	
	private void setDefaults() {
		if (target.tagExists(Target.TAG_DEFAULT_PERCEIVED_WIDTH)) {
			targetWidth.setText(target.getTag(Target.TAG_DEFAULT_PERCEIVED_WIDTH));
		}

		if (target.tagExists(Target.TAG_DEFAULT_PERCEIVED_HEIGHT)) {
			targetHeight.setText(target.getTag(Target.TAG_DEFAULT_PERCEIVED_HEIGHT));
		}
		
		if (target.tagExists(Target.TAG_CURRENT_PERCEIVED_DISTANCE)) {
			targetDistance.setText(target.getTag(Target.TAG_CURRENT_PERCEIVED_DISTANCE));
		} else if (target.tagExists(Target.TAG_DEFAULT_PERCEIVED_DISTANCE)) {
			targetDistance.setText(target.getTag(Target.TAG_DEFAULT_PERCEIVED_DISTANCE));
		}

		if (perspectiveManager.getCameraDistance() > 0) {
			cameraDistance.setText(String.valueOf(perspectiveManager.getCameraDistance()));
		} else if (config.getCameraDistance(cameraName).isPresent()) {
			cameraDistance.setText(String.valueOf(config.getCameraDistance(cameraName).get()));
		}

		if (!cameraDistance.getText().isEmpty() && !perspectiveManager.isCameraParamsKnown()
				&& (perspectiveManager.getProjectionWidth() == -1 || perspectiveManager.getProjectionHeight() == -1)) {
			throw new AssertionError("The camera parameters and paper dimensions are unknown. We should not have been "
					+ "able to get here.");
		}

		if (target.tagExists(Target.TAG_SHOOTER_DISTANCE)) {
			shooterDistance.setText(target.getTag(Target.TAG_SHOOTER_DISTANCE));
		} else if (!cameraDistance.getText().isEmpty()) {
			shooterDistance.setText(cameraDistance.getText());
		}
		
		defaultsSet = true;
	}
	
	private void setDistance() {
		if (!validateDistanceData()) return;
		
		persistSettings();
		
		int width = Integer.parseInt(targetWidth.getText());
		int height = Integer.parseInt(targetHeight.getText());
		int distance = Integer.parseInt(targetDistance.getText());

		if (logger.isTraceEnabled()) {
			logger.trace(
					"New target settings from distance settings pane: current width = {}, "
							+ "default height = {}, default distance = {}, new distance = {}",
					width, height, originalTargetDistance, distance);
		}

		Optional<Dimension2D> targetDimensions = perspectiveManager.calculateObjectSize(width, height, distance);

		if (targetDimensions.isPresent()) {
			Dimension2D d = targetDimensions.get();
			target.setDimensions(d.getWidth(), d.getHeight());
		}
	}
	
	private boolean validateDistanceData() {
		boolean isValid = validateDistanceField(shooterDistance); 
		isValid = validateDistanceField(targetDistance) && isValid;
		isValid = validateDistanceField(cameraDistance) && isValid;
		isValid = validateDistanceField(targetWidth) && isValid;
		isValid = validateDistanceField(targetHeight) && isValid;
	
		if (!isValid) return isValid;
		
		if ("0".equals(targetDistance.getText())) {
			Alert invalidDataAlert = new Alert(AlertType.ERROR);

			String message = "Target Distance cannot be 0, please set a value greater than 0.";

			invalidDataAlert.setTitle("Invalid Target Distance");
			invalidDataAlert.setHeaderText("Target Distance Cannot Be Zero");
			invalidDataAlert.setResizable(true);
			invalidDataAlert.setContentText(message);
			invalidDataAlert.initOwner((Stage) this.getScene().getWindow());
			invalidDataAlert.showAndWait();
			
			isValid = false;
		}
		
		return isValid;
	}
	
	private boolean validateDistanceField(TextField field) {
		boolean isValid = true;
		
		if (field.getText().isEmpty()) {
			isValid = false;
			field.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
		} else {
			field.setStyle("");
		}
		
		return isValid;
	}
		
	private void persistSettings() {
		Map<String, String> tags = target.getAllTags();
		if (!targetWidth.getText().isEmpty()) {
			tags.put(Target.TAG_DEFAULT_PERCEIVED_WIDTH, targetWidth.getText());
		}
		if (!targetHeight.getText().isEmpty()) {
			tags.put(Target.TAG_DEFAULT_PERCEIVED_HEIGHT, targetHeight.getText());
		}
		tags.put(Target.TAG_CURRENT_PERCEIVED_DISTANCE, targetDistance.getText());
		tags.put(Target.TAG_SHOOTER_DISTANCE, shooterDistance.getText());

		if (!originalCameraDistance.equals(cameraDistance.getText())) {
			config.setCameraDistance(cameraName, Integer.parseInt(cameraDistance.getText()));
			try {
				config.writeConfigurationFile();
			} catch (ConfigurationException | IOException e) {
				logger.error("Failed to persist user-defined camera distance", e);
			}
		}

		perspectiveManager.setShooterDistance(Integer.parseInt(shooterDistance.getText()));
		perspectiveManager.setCameraDistance(Integer.parseInt(cameraDistance.getText()));
	}
}
