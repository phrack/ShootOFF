/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ShootOFFController implements Initializable {
	@FXML private MenuBar mainMenu;
	@FXML private Canvas defaultCanvas;

	public Configuration config;
	
	public void setConfig(Configuration config) {
		this.config = config;
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Webcam defaultCamera = Webcam.getDefault();
		defaultCamera.setViewSize(new Dimension(640, 480));
		defaultCamera.open();
		BufferedImage buffImg = defaultCamera.getImage();
		Image img = SwingFXUtils.toFXImage(buffImg, null);
		defaultCanvas.getGraphicsContext2D().drawImage(img, 0, 0);
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
