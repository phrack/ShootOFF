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
import java.util.ArrayList;
import java.util.Optional;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.courses.Course;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
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
	
	private Configuration config;
	private CanvasManager canvasManager;
	private Optional<LocatedImage> background = Optional.empty();
	
	// Used for testing
	public void init(Configuration config, CanvasManager canvasManager) {
		this.config = config;
		this.canvasManager = canvasManager;
		
		arenaStage = new Stage();
		arenaAnchor = new AnchorPane(canvasManager.getCanvasGroup());
		Scene scene = new Scene(arenaAnchor, 500, 500);
		arenaStage.setScene(scene);
	}
	
	public void init(Configuration config, CamerasSupervisor camerasSupervisor) {
		this.config = config;
		arenaStage = (Stage)arenaAnchor.getScene().getWindow();
		
		canvasManager = new CanvasManager(arenaCanvasGroup, config, camerasSupervisor, "arena", null);
		canvasManager.updateBackground(null, Optional.empty());
		
		arenaAnchor.widthProperty().addListener((e) -> {
				canvasManager.setBackgroundFit(arenaAnchor.getWidth(), arenaAnchor.getHeight());
			});
		
		arenaAnchor.heightProperty().addListener((e) -> {
			canvasManager.setBackgroundFit(arenaAnchor.getWidth(), arenaAnchor.getHeight());
		});
		
		arenaAnchor.setStyle("-fx-background-color: #333333;");
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
	
	public void setBackground(LocatedImage img) {
		background = Optional.ofNullable(img);
		canvasManager.updateBackground(img, Optional.empty());
	}
	
	public Optional<LocatedImage> getBackground() {
		return background;
	}
	
	public Configuration getConfiguration() {
		return config;
	}
	
	public void setCourse(Course course) {
		if (course.getBackground().isPresent()) {
			setBackground(course.getBackground().get());
		} else {
			setBackground(null);
		}
		
		for (Target t : new ArrayList<Target>(canvasManager.getTargets())) canvasManager.removeTarget(t);
		
		for (Target t : course.getTargets()) canvasManager.addTarget(t);
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
