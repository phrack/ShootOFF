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

package com.shootoff.plugins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.DelayedStartIntervalController;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.gui.ShotEntry;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

/** 
 * This class implements common training protocol operations. All
 * training protocols should extend it.
 * 
 * @author phrack
 */
public class TrainingProtocolBase {
	@SuppressWarnings("unused")
	private List<Group> targets;
	private Configuration config;
	private CamerasSupervisor camerasSupervisor;
	private TableView<ShotEntry> shotTimerTable;
	
	private final Map<String, TableColumn<ShotEntry, String>> protocolColumns = 
			new HashMap<String, TableColumn<ShotEntry, String>>();
	private final Label protocolLabel = new Label();
	
	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public TrainingProtocolBase() {}
	
	public TrainingProtocolBase(List<Group> targets) {
		this.targets = targets;
	}
	
	public void init(Configuration config, CamerasSupervisor camerasSupervisor, 
			TableView<ShotEntry> shotTimerTable) {
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.shotTimerTable = shotTimerTable;
		
		protocolLabel.setTextFill(Color.WHITE);
		for (CanvasManager canvasManager : camerasSupervisor.getCanvasManagers()) {
			canvasManager.getCanvasGroup().getChildren().add(protocolLabel);
		}
	}
	
	/**
	 * Returns the current instance of this class. This metehod exists so that we can
	 * call methods in this class when in an internal class (e.g. to implement Callable)
	 * that doesn't have access to super.
	 * 
	 * @return the current instance of this class
	 */
	public TrainingProtocolBase getInstance() {
		return this;
	}
	
	public void getDelayedStartInterval(DelayedStartListener listener) {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/DelayedStartInterval.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Stage delayedStartIntervalStage = new Stage();
		
		delayedStartIntervalStage.initOwner((Stage)shotTimerTable.getScene().getWindow());
		delayedStartIntervalStage.initModality(Modality.WINDOW_MODAL);
		delayedStartIntervalStage.setTitle("Preferences");
		delayedStartIntervalStage.setScene(new Scene(loader.getRoot()));
		delayedStartIntervalStage.show();
		
		DelayedStartIntervalController controller = (DelayedStartIntervalController)loader.getController();
		controller.init(listener);
	}
	
	/**
	 * Adds a column to the shot timer table. The <tt>name</tt> is used to reference this column
	 * for the purposes of setting text and cleaning up.
	 * 
	 * @param name	both the text that will appear for name of the column and the name used to
	 * 				reference this column whenever it needs to be looked up
	 * @param width	the width of the new column
	 */
	public void addShotTimerColumn(String name, int width) {
		TableColumn<ShotEntry, String> newCol = new TableColumn<ShotEntry, String>(name);
		newCol.setPrefWidth(width);
		newCol.setCellValueFactory(new Callback<CellDataFeatures<ShotEntry, String>, ObservableValue<String>>() {
		     public ObservableValue<String> call(CellDataFeatures<ShotEntry, String> p) {
		         return new SimpleStringProperty(p.getValue().getProtocolValue(name));
		     }
		  });
		
		protocolColumns.put(name, newCol);
		shotTimerTable.getColumns().add(newCol);
	}
	
	/** 
	 * Inserts text into the named column for the last entry in the shot timer table.
	 * 
	 * @param name	the name of the column to insert the text into
	 * @param value	the text that should be inserted
	 */
	public void setShotTimerColumnText(String name, String value) {
		if (shotTimerTable != null) {
			Platform.runLater(() -> {
					shotTimerTable.getItems().get(shotTimerTable.getItems().size() - 1).setProtocolValue(name, value);
				});
		}
	}
	
	/**
	 * Shows a message on every single webcam feed.
	 * 
	 * @param message	the message to show on every webcam feed
	 */
	public void showTextOnFeed(String message) {
		if (config.inDebugMode()) System.out.println(message);
		
		Platform.runLater(() -> {
				protocolLabel.setText(message);
			});
	}
	
	/**
	 * Clear all present shots.
	 */
	public void clearShots()  {
		camerasSupervisor.clearShots();
	}
	
	/** 
	 * Perform the equivalent of the user hitting the reset button.
	 */
	public void reset() {
		camerasSupervisor.reset();
		if (config.getProtocol().isPresent()) config.getProtocol().get().reset(camerasSupervisor.getTargets());	
	}
	
	/**
	 * Sets whether or not shot detection is paused.
	 * 
	 * @param isPaused <tt>true</tt> to temporarily stop detecting shots
	 */
	public void pauseShotDetection(boolean isPaused) {
		camerasSupervisor.setDetectingAll(!isPaused);
	}
	
	/**
	 * Plays an audio file asyncronously.
	 * 
	 * @param soundFilePath	the audio file to play (e.g. "sounds/metal_clang.wav")
	 */
	public static void playSound(String soundFilePath) {
		AudioInputStream audioInputStream = null;

		try {
			audioInputStream = AudioSystem.getAudioInputStream(new File(soundFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (audioInputStream != null) {
			AudioFormat format = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			try {
				Clip clip = (Clip) AudioSystem.getLine(info);
				clip.open(audioInputStream);
				clip.start();
			} catch (LineUnavailableException | IOException e) {
				e.printStackTrace();
			}
		} 
	}
	
	/**
	 * Removes all objects the training protocol has added to the GUI.
	 */
	public void destroy() {
		for (String name : protocolColumns.keySet()) {
			shotTimerTable.getColumns().remove(protocolColumns.get(name));
		}
		
		for (CanvasManager canvasManager : camerasSupervisor.getCanvasManagers()) {
			canvasManager.getCanvasGroup().getChildren().remove(protocolLabel);
		}
		
		pauseShotDetection(false);
	}
}
