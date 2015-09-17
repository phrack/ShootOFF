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
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.controller.DelayedStartIntervalController;

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
 * This class implements common training exercise operations. All
 * training exercises should extend it.
 * 
 * @author phrack
 */
public class TrainingExerciseBase {
	@SuppressWarnings("unused")
	private List<Group> targets;
	private Configuration config;
	private CamerasSupervisor camerasSupervisor;
	private TableView<ShotEntry> shotTimerTable;
	private boolean changedRowColor = false;
	
	private final Map<CanvasManager, Label> exerciseLabels = new HashMap<CanvasManager, Label>();
	private final Map<String, TableColumn<ShotEntry, String>> exerciseColumns = 
			new HashMap<String, TableColumn<ShotEntry, String>>();

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public TrainingExerciseBase() {}
	
	public TrainingExerciseBase(List<Group> targets) {
		this.targets = targets;
	}
	
	public void init(Configuration config, CamerasSupervisor camerasSupervisor, 
			TableView<ShotEntry> shotTimerTable) {
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.shotTimerTable = shotTimerTable;
		
		for (CanvasManager canvasManager : camerasSupervisor.getCanvasManagers()) {
			Label exerciseLabel = new Label();
			exerciseLabel.setTextFill(Color.WHITE);
			canvasManager.getCanvasGroup().getChildren().add(exerciseLabel);
			exerciseLabels.put(canvasManager, exerciseLabel);
		}
	}
	
	/**
	 * Returns the current instance of this class. This method exists so that we can
	 * call methods in this class when in an internal class (e.g. to implement Callable)
	 * that doesn't have access to super.
	 * 
	 * @return the current instance of this class
	 */
	public TrainingExerciseBase getInstance() {
		return this;
	}
	
	public Stage getShootOFFStage() {
		return (Stage)shotTimerTable.getScene().getWindow();
	}
	
	public void getDelayedStartInterval(DelayedStartListener listener) {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/DelayedStartInterval.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Stage delayedStartIntervalStage = new Stage();
		
		DelayedStartIntervalController controller = (DelayedStartIntervalController)loader.getController();
		controller.init(listener);
		
		delayedStartIntervalStage.initOwner(getShootOFFStage());
		delayedStartIntervalStage.initModality(Modality.WINDOW_MODAL);
		delayedStartIntervalStage.setTitle("Delayed Start Interval");
		delayedStartIntervalStage.setScene(new Scene(loader.getRoot()));
		delayedStartIntervalStage.showAndWait();
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
		         return new SimpleStringProperty(p.getValue().getExerciseValue(name));
		     }
		  });
		
		exerciseColumns.put(name, newCol);
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
					shotTimerTable.getItems().get(shotTimerTable.getItems().size() - 1).setExerciseValue(name, value);
				});
		}
	}
	
	/**
	 * Set the background color for rows for shots added to the shot timer after this
	 * method is called. 
	 * 
	 * @param c		the color to use in the style string for the row. Set to null to
	 * 				return the row color to the default color
	 */
	public void setShotTimerRowColor(Color c) {
		changedRowColor = true;
		config.setShotTimerRowColor(c);
	}
	
	/**
	 * Shows a message on every single webcam feed.
	 * 
	 * @param message	the message to show on every webcam feed
	 */
	public void showTextOnFeed(String message) {
		if (config.inDebugMode()) System.out.println(message);
		
		if (config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordExerciseFeedMessage(message);
		}
		
		Platform.runLater(() -> {
				for (Label exerciseLabel : exerciseLabels.values())
					exerciseLabel.setText(message);
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
		
		if (changedRowColor) {
			config.setShotTimerRowColor(null);
			changedRowColor = false;
		}
		
		if (config.getExercise().isPresent()) config.getExercise().get().reset(camerasSupervisor.getTargets());	
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
		playSound(new File(soundFilePath));
	}
	
	public static void playSound(File soundFile) {
		if (!soundFile.isAbsolute()) 
			soundFile = new File(System.getProperty("shootoff.home") + File.separator + soundFile.getPath());
		
		AudioInputStream audioInputStream = null;

		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (audioInputStream != null) {
			AudioFormat format = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			
			Clip clip = null;
			
			try {
				clip = (Clip) AudioSystem.getLine(info);
				clip.open(audioInputStream);
				clip.start();
				
				clip.addLineListener((e) -> {
						if (e.getType().equals(LineEvent.Type.STOP)) e.getLine().close();
					});
			} catch (LineUnavailableException | IOException e) {
				if (clip != null) clip.close();
				e.printStackTrace();
			}
		} 
	}
	
	/**
	 * Removes all objects the training exercise has added to the GUI.
	 */
	public void destroy() {
		if (changedRowColor) {
			config.setShotTimerRowColor(null);
			changedRowColor = false;
		}
		
		for (TableColumn<ShotEntry, String> column : exerciseColumns.values()) {
			shotTimerTable.getColumns().remove(column);
		}
		
		for (CanvasManager canvasManager : camerasSupervisor.getCanvasManagers()) {
			canvasManager.getCanvasGroup().getChildren().remove(exerciseLabels.get(canvasManager));
		}
		
		exerciseLabels.clear();
		
		pauseShotDetection(false);
	}
}
