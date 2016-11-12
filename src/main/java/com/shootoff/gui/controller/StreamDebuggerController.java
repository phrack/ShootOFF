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

import java.awt.image.BufferedImage;

import com.shootoff.camera.CameraDebuggerListener;
import com.shootoff.camera.CameraManager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import com.shootoff.util.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class StreamDebuggerController implements CameraDebuggerListener {
	private Stage streamDebuggerStage;
	@FXML private ImageView thresholdImageView;
	@FXML private Slider centerBorderSlider;
	@FXML private Slider minDimSlider;

	private String defaultWindowTitle = "";

	public void init(CameraManager cameraManager) {
		streamDebuggerStage = (Stage) thresholdImageView.getScene().getWindow();
		defaultWindowTitle = streamDebuggerStage.getTitle();

		cameraManager.setThresholdListener(this);

		minDimSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
				if (newValue == null) return;

				cameraManager.setMinimumShotDimension(newValue.intValue());
			}
		});
	}

	public ImageView getThresholdImageView() {
		return thresholdImageView;
	}

	@Override
	public void updateDebugView(BufferedImage debugImg) {
		thresholdImageView.setImage(SwingFXUtils.toFXImage(debugImg, null));
	}

	@Override
	public void updateFeedData(double fps) {
		Platform.runLater(() -> {
			streamDebuggerStage.setTitle(String.format(defaultWindowTitle + " %.2f FPS", fps));
		});
	}
}
