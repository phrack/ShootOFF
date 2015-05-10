/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.io.IOException;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ShootOFFController implements CameraConfigListener {
	private Stage shootOFFStage;
	@FXML private MenuBar mainMenu;
	@FXML private TabPane cameraTabPane;
	@FXML private Group defaultCanvasGroup;
	
	private CamerasSupervisor camerasSupervisor;
	private Configuration config;
	
	public void init(Configuration config) {
		this.config = config;
		this.camerasSupervisor = new CamerasSupervisor(config);
		
		if (config.getWebcams().isEmpty()) {
			Webcam defaultCamera = Webcam.getDefault();
			camerasSupervisor.addCameraManager(defaultCamera, 
					new CanvasManager(defaultCanvasGroup, config));
		} else {
			addConfiguredCameras();
		}
		
		shootOFFStage = (Stage)mainMenu.getScene().getWindow();
		shootOFFStage.setOnCloseRequest((value) -> {
			camerasSupervisor.setStreamingAll(false);
		});
	}
	
	@Override
	public void cameraConfigUpdated() {
		addConfiguredCameras();
	}
	
	private void addConfiguredCameras() {
		cameraTabPane.getTabs().clear();
		
		for (Webcam webcam : config.getWebcams()) {
			Tab cameraTab = new Tab(webcam.getName());
			Group cameraCanvasGroup = new Group();
			// 640 x 480
			cameraTab.setContent(new AnchorPane(cameraCanvasGroup));
			
			camerasSupervisor.addCameraManager(webcam, 
					new CanvasManager(cameraCanvasGroup, config));
			
			cameraTabPane.getTabs().add(cameraTab);
		}
	}
	
	@FXML 
	public void preferencesClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));
		loader.load();
		
		Stage preferencesStage = new Stage();
		
		preferencesStage.initOwner(shootOFFStage);
		preferencesStage.initModality(Modality.WINDOW_MODAL);
        preferencesStage.setTitle("Preferences");
        preferencesStage.setScene(new Scene(loader.getRoot()));
        preferencesStage.show();
        ((PreferencesController)loader.getController()).setConfig(config, this);
    }
	
	@FXML 
	public void toggleArenaClicked(ActionEvent event) throws IOException {
		new ProjectorArenaController().toggleArena();
    }
	
	@FXML
	public void exitMenuClicked(ActionEvent event) {
		camerasSupervisor.setStreamingAll(false);
		shootOFFStage.close();
	}

	@FXML 
	public void createTargetMenuClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/TargetEditor.fxml"));
		loader.load();
		
		Stage preferencesStage = new Stage();
		
		preferencesStage.initOwner(shootOFFStage);
		preferencesStage.initModality(Modality.WINDOW_MODAL);
        preferencesStage.setTitle("TargetEditor");
        preferencesStage.setScene(new Scene(loader.getRoot()));
        preferencesStage.show();
        
        CameraManager currentCamera = camerasSupervisor.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
        Image currentFrame = currentCamera.getCurrentFrame();
        ((TargetEditorController)loader.getController()).init(currentFrame);
	}
	
	@FXML
	public void resetClicked(ActionEvent event) {
		camerasSupervisor.reset();
	}
}
