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

package com.shootoff.gui;

import java.io.IOException;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ProjectorArenaController {
	private Stage arenaStage;
	@FXML private AnchorPane arenaAnchor;
	@FXML private Group arenaCanvasGroup;
	
	@SuppressWarnings("unused")
	private Configuration config;
	private CanvasManager canvasManager;
	
	public void init(Configuration config, CamerasSupervisor camerasSupervisor) {
		this.config = config;
		arenaStage = (Stage)arenaAnchor.getScene().getWindow();
		canvasManager = new CanvasManager(arenaCanvasGroup, config, camerasSupervisor, null);
		canvasManager.updateBackground(null);
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
		if (event.getCode() == KeyCode.F11) {
			arenaStage.setFullScreen(!arenaStage.isFullScreen());
		}
	}
	
	@FXML 
	public void canvasMouseEntered(MouseEvent event) throws IOException {
		arenaAnchor.requestFocus();
    }
}
