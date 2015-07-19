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

import java.io.IOException;
import java.util.Optional;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CanvasManager;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ProjectorArenaController implements CalibrationListener {
	private Stage arenaStage;
	@FXML private AnchorPane arenaAnchor;
	@FXML private Group arenaCanvasGroup;
	@FXML private Label calibrationLabel;
	
	@SuppressWarnings("unused")
	private Configuration config;
	private CanvasManager canvasManager;
	
	public void init(Configuration config, CamerasSupervisor camerasSupervisor) {
		this.config = config;
		arenaStage = (Stage)arenaAnchor.getScene().getWindow();
		canvasManager = new CanvasManager(arenaCanvasGroup, config, camerasSupervisor, null);
		canvasManager.updateBackground(null, Optional.empty());
		arenaAnchor.setStyle("-fx-background-color: #696969;");
	}
	
	public void toggleArena() throws IOException {
		if (arenaStage.isShowing()) {
			arenaStage.hide();
		} else {
			arenaStage.show();
		}
	}
	
	public double getWidth() {
		return arenaAnchor.getWidth();
	}
	
	public double getHeight() {
		return arenaAnchor.getHeight();
	}
	
	public CanvasManager getCanvasManager() {
		return canvasManager;
	}
	
	public void close() {
		arenaStage.close();
	}
	
	@FXML
	public void canvasKeyPressed(KeyEvent event) {
		boolean macFullscreen = event.getCode() == KeyCode.F &&
			event.isControlDown() && event.isShortcutDown();
		if (event.getCode() == KeyCode.F11 || macFullscreen) {
			arenaStage.setAlwaysOnTop(!arenaStage.isAlwaysOnTop());
			arenaStage.setFullScreen(!arenaStage.isFullScreen());
		}
	}
	
	@FXML 
	public void canvasMouseEntered(MouseEvent event) throws IOException {
		arenaAnchor.requestFocus();
    }

	@Override
	public void calibrated() {
		calibrationLabel.setVisible(false);
	}
}
