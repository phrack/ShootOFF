/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.io.IOException;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.MenuBar;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ShootOFFController {
	private Stage shootOFFStage;
	@FXML private MenuBar mainMenu;
	@FXML private Canvas defaultCanvas;
	@FXML private CamerasSupervisor camerasSupervisor;

	public Configuration config;
	
	public void init(Configuration config) {
		this.config = config;
		this.camerasSupervisor = new CamerasSupervisor(config);
		
		Webcam defaultCamera = Webcam.getDefault();
		camerasSupervisor.addCameraManager(defaultCamera, new CanvasManager(defaultCanvas));
	
		shootOFFStage = (Stage)mainMenu.getScene().getWindow();
		shootOFFStage.setOnCloseRequest((value) -> {
			camerasSupervisor.setStreamingAll(false);
		});
	}
	
	@FXML 
	public void preferencesClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));
		loader.load();
		
		Stage preferencesStage = new Stage();
		
		preferencesStage.initOwner(mainMenu.getScene().getWindow());
		preferencesStage.initModality(Modality.WINDOW_MODAL);
        preferencesStage.setTitle("Preferences");
        preferencesStage.setScene(new Scene(loader.getRoot()));
        preferencesStage.show();
        ((PreferencesController)loader.getController()).setConfig(config);
    }
	
	@FXML 
	public void toggleArenaClicked(ActionEvent event) throws IOException {
		new ProjectorArenaController().toggleArena();
    }
}
