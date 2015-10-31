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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.courses.Course;
import com.shootoff.gui.CalibrationListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ProjectorArenaController implements CalibrationListener {
	private final Logger logger = LoggerFactory.getLogger(ProjectorArenaController.class);
	
	private Stage arenaStage;
	@FXML private AnchorPane arenaAnchor;
	@FXML private Group arenaCanvasGroup;
	@FXML private Label calibrationLabel;
	
	private Configuration config;
	private CanvasManager canvasManager;
	private Optional<LocatedImage> background = Optional.empty();
	
	private Screen originalHomeScreen;
	private Optional<Screen> detectedProjectorScreen = Optional.empty();
	
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
	
	private Screen getArenaHomeScreen() {
		ObservableList<Screen> arenaHomeScreens = Screen.getScreensForRectangle(arenaStage.getX(), arenaStage.getY(), 1, 1);
		
		if (arenaHomeScreens.size() > 0) {
			logger.warn("Found multiple screens as the possible arena home screen, this is unexpected: {}", 
					arenaHomeScreens.size());
		}
		
		return arenaHomeScreens.get(0);
	}
	
	public void autoPlaceArena() {
		originalHomeScreen = getArenaHomeScreen();
		
		// Place the arena on what we hope is the projector with the following precidence:
		// 1. If the user has place the arena on a screen before, place it on that screen again
		// 2. If the user has never placed the arena before and there are only two screens, 
		//    put it on the non-primary screen
		// 3. If the arena has never been placed and there are more than two screens, place
		//    the arena on the smallest screen
		
		Optional<Screen> projector = Optional.empty();
		
		if (config.getArenaPosition().isPresent()) {
			logger.debug("Projector has been manually placed previously");
			
			Point2D arenaPosition = config.getArenaPosition().get();
			
			arenaStage.setX(arenaPosition.getX());
			arenaStage.setY(arenaPosition.getY());
			
			toggleFullScreen();
			
			return;
		} else if (Screen.getScreens().size() == 2) {
			logger.debug("Two screens present");
			
			Screen primary = Screen.getPrimary();
			
			for (Screen screen : Screen.getScreens()) {
				if (!screen.equals(primary)) {
					projector = Optional.of(screen);
					break;
				}
			}
		} else if (Screen.getScreens().size() > 2) {
			logger.debug("More than two screens present");
			
			Screen smallest = null;
			
			// Find screen with the smallest area
			for (Screen screen : Screen.getScreens()) {
				if (smallest == null) {
					smallest = screen;
				} else {
					if (screen.getBounds().getHeight() * screen.getBounds().getWidth() < 
							smallest.getBounds().getHeight() * smallest.getBounds().getWidth()) {
						smallest = screen;
					}
				}
			}
			
			projector = Optional.ofNullable(smallest);
		}
		
		if (projector.isPresent()) {
			Screen arenaHome = projector.get();
			
			double newX = arenaHome.getVisualBounds().getMinX();
			double newY = arenaHome.getVisualBounds().getMinY();
			
			logger.debug("Found likely projector screen: resolution = {}x{}, newX = {}, newY = {}", 
					arenaHome.getBounds().getWidth(), arenaHome.getBounds().getHeight(),
					newX, newY);
			
			arenaStage.setX(newX + 10);
			arenaStage.setY(newY + 10);
			
			detectedProjectorScreen = projector;
			
			toggleFullScreen();
		} else {
			logger.debug("Did not find screen that is a likely projector");
		}
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
	public void canvasKeyPressed(KeyEvent event) throws Exception {
		boolean macFullscreen = event.getCode() == KeyCode.F &&
			event.isControlDown() && event.isShortcutDown();
		if (event.getCode() == KeyCode.F11 || macFullscreen) {
			// Manually going full screen with an arena that was manually
			// moved to another screen
			boolean fullyManual = !detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen() && !originalHomeScreen.equals(getArenaHomeScreen());
			boolean movedAfterAuto = detectedProjectorScreen.isPresent() && !arenaStage.isFullScreen() && !detectedProjectorScreen.equals(getArenaHomeScreen());
			
			if (fullyManual || movedAfterAuto) {
				config.setArenaPosition(arenaStage.getX(), arenaStage.getY());
				try {
					config.writeConfigurationFile();
				} catch (ConfigurationException | IOException e) {
					logger.error("Error writing configuration with arena location", e);
					throw e;
				}
			}
			
			toggleFullScreen();
		}
	}
	
	private void toggleFullScreen() {
		arenaStage.setAlwaysOnTop(!arenaStage.isAlwaysOnTop());
		arenaStage.setFullScreen(!arenaStage.isFullScreen());
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
