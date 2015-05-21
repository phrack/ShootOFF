/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
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
