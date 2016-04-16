/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraView;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.gui.ParListener;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.controller.DelayedStartIntervalController;
import com.shootoff.gui.controller.ParIntervalController;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.targets.TargetManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * This class implements common training exercise operations. All training
 * exercises should extend it.
 * 
 * @author phrack
 */
public class TrainingExerciseBase {
	private static final Logger logger = LoggerFactory.getLogger(TrainingExerciseBase.class);

	private static boolean isSilenced = false;

	@SuppressWarnings("unused") private List<Group> targets;
	private Configuration config;
	private CamerasSupervisor camerasSupervisor;
	private TargetManager targetManager;
	private GridPane buttonsPane;
	private TableView<ShotEntry> shotTimerTable;
	private boolean changedRowColor = false;

	private final static Map<CameraView, Label> exerciseLabels = new HashMap<CameraView, Label>();
	private final static Map<String, TableColumn<ShotEntry, String>> exerciseColumns = new HashMap<String, TableColumn<ShotEntry, String>>();
	private final static List<Button> exerciseButtons = new ArrayList<Button>();

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public TrainingExerciseBase() {}

	public TrainingExerciseBase(List<Group> targets) {
		this.targets = targets;
	}

	public void init(Configuration config, CamerasSupervisor camerasSupervisor, ShootOFFController controller) {
		init(config, camerasSupervisor, controller.getButtonsPane(), controller.getShotEntryTable());
		this.targetManager = (TargetManager) controller;
	}

	// This is only required for unit tests where we don't want to create a full
	// ShootOFFController
	public void init(final Configuration config, final CamerasSupervisor camerasSupervisor, final GridPane buttonsPane,
			final TableView<ShotEntry> shotEntryTable) {
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.buttonsPane = buttonsPane;
		this.shotTimerTable = shotEntryTable;

		for (final CameraView cv : camerasSupervisor.getCameraViews()) {
			final Label exerciseLabel = new Label();
			exerciseLabel.setTextFill(Color.WHITE);
			cv.addChild(exerciseLabel);
			exerciseLabels.put(cv, exerciseLabel);
		}
	}

	/**
	 * Allows sounds to be silenced or on. If silenced, instead of playing a
	 * sound the file name will be printed to stdout. This exists so that
	 * components can be easily tested even if they are reliant on sounds.
	 * 
	 * @param isSilenced
	 *            set to <tt>true</tt> if sound file names should instead be
	 *            printed to stdout, <tt>false</tt> for normal operation.
	 */
	public static void silence(boolean isSilenced) {
		TrainingExerciseBase.isSilenced = isSilenced;
	}

	/**
	 * Returns the current instance of this class. This method exists so that we
	 * can call methods in this class when in an internal class (e.g. to
	 * implement Callable) that doesn't have access to super.
	 * 
	 * @return the current instance of this class
	 */
	public TrainingExerciseBase getInstance() {
		return this;
	}

	public Stage getShootOFFStage() {
		return (Stage) shotTimerTable.getScene().getWindow();
	}

	public void getDelayedStartInterval(final DelayedStartListener listener) {
		final FXMLLoader loader = new FXMLLoader(
				TrainingExerciseBase.class.getClassLoader().getResource("com/shootoff/gui/DelayedStartInterval.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("Error reading DelayedStartInterval FXML file", e);
		}

		final Stage delayedStartIntervalStage = new Stage();

		final DelayedStartIntervalController controller = (DelayedStartIntervalController) loader.getController();
		controller.init(listener);

		delayedStartIntervalStage.initOwner(getShootOFFStage());
		delayedStartIntervalStage.initModality(Modality.WINDOW_MODAL);
		delayedStartIntervalStage.setTitle("Delayed Start Interval");
		delayedStartIntervalStage.setScene(new Scene(loader.getRoot()));
		delayedStartIntervalStage.showAndWait();
	}

	/**
	 * A copy of getDelayedStartInterval() plus setting the PAR time.
	 * 
	 * @param listener
	 */
	public void getParInterval(final ParListener listener) {
		final FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/ParInterval.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("Error reading ParInterval FXML file", e);
		}

		final Stage delayedStartIntervalStage = new Stage();

		final ParIntervalController controller = (ParIntervalController) loader.getController();
		controller.init(listener);

		delayedStartIntervalStage.initOwner(getShootOFFStage());
		delayedStartIntervalStage.initModality(Modality.WINDOW_MODAL);
		delayedStartIntervalStage.setTitle("Par Intervals");
		delayedStartIntervalStage.setScene(new Scene(loader.getRoot()));
		delayedStartIntervalStage.showAndWait();
	}

	/**
	 * Adds a column to the shot timer table. The <tt>name</tt> is used to
	 * reference this column for the purposes of setting text and cleaning up.
	 * 
	 * @param name
	 *            both the text that will appear for name of the column and the
	 *            name used to reference this column whenever it needs to be
	 *            looked up
	 * @param width
	 *            the width of the new column
	 */
	public void addShotTimerColumn(String name, int width) {
		final TableColumn<ShotEntry, String> newCol = new TableColumn<ShotEntry, String>(name);
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
	 * Inserts text into the named column for the last entry in the shot timer
	 * table.
	 * 
	 * @param name
	 *            the name of the column to insert the text into
	 * @param value
	 *            the text that should be inserted
	 */
	public void setShotTimerColumnText(final String name, final String value) {
		if (shotTimerTable != null) {
			Platform.runLater(() -> {
				shotTimerTable.getItems().get(shotTimerTable.getItems().size() - 1).setExerciseValue(name, value);
			});
		}
	}

	/**
	 * Set the background color for rows for shots added to the shot timer after
	 * this method is called.
	 * 
	 * @param c
	 *            the color to use in the style string for the row. Set to null
	 *            to return the row color to the default color
	 */
	public void setShotTimerRowColor(final Color c) {
		changedRowColor = true;
		config.setShotTimerRowColor(c);
	}

	/**
	 * Shows a message on every single webcam feed.
	 * 
	 * @param message
	 *            the message to show on every webcam feed
	 */
	public void showTextOnFeed(String message) {
		if (config.inDebugMode()) System.out.println(message);

		if (config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordExerciseFeedMessage(message);
		}

		Platform.runLater(() -> {
			for (Label exerciseLabel : exerciseLabels.values()) {
				exerciseLabel.setText(message);
			}
		});
	}

	/**
	 * Clear all present shots.
	 */
	public void clearShots() {
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

		if (config.getExercise().isPresent()) config.getExercise().get().reset(targetManager.getTargets());
	}

	/**
	 * Get a list of all of the targets on every canvas manager
	 */

	public List<Group> getCurrentTargets() {
		return targetManager.getTargets();
	}

	/**
	 * Sets whether or not shot detection is paused.
	 * 
	 * @param isPaused
	 *            <tt>true</tt> to temporarily stop detecting shots
	 */
	public void pauseShotDetection(final boolean isPaused) {
		camerasSupervisor.setDetectingAll(!isPaused);
	}

	/**
	 * Adds a button to the right of the reset button with caption <tt>text</tt>
	 * and action handler <tt>eventHandler</tt>.
	 */
	public Button addShootOFFButton(final String text, final EventHandler<ActionEvent> eventHandler) {
		final Button exerciseButton = new Button(text);
		exerciseButton.setOnAction(eventHandler);
		GridPane.setMargin(exerciseButton, GridPane.getMargin(buttonsPane.getChildren().get(0)));
		GridPane.setHalignment(exerciseButton, HPos.CENTER);
		exerciseButtons.add(exerciseButton);

		buttonsPane.add(exerciseButton, buttonsPane.getChildren().size(), 0);

		return exerciseButton;
	}

	public void removeShootOFFButton(final Button exerciseButton) {
		if (!exerciseButtons.contains(exerciseButton)) return;

		buttonsPane.getChildren().remove(exerciseButton);
		exerciseButtons.remove(exerciseButton);
	}

	/**
	 * Plays an audio file asynchronously.
	 * 
	 * @param soundFilePath
	 *            the audio file to play (e.g. "sounds/metal_clang.wav")
	 */
	public static void playSound(final String soundFilePath) {
		playSound(new File(soundFilePath));
	}

	public static void playSound(final File soundFile) {
		playSound(soundFile, Optional.empty());
	}

	public static void playSound(final InputStream is) {
		playSound(is, Optional.empty());
	}

	public static void playSound(final InputStream is, Optional<LineListener> listener) {
		if (isSilenced) {
			System.out.println("Playing audio for modular exercise.");
			return;
		}

		try {
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(is);
			playSound(audioInputStream, listener);
		} catch (UnsupportedAudioFileException | IOException e) {
			logger.error("Error reading sound stream to play", e);
		}
	}

	private static void playSound(File soundFile, Optional<LineListener> listener) {
		if (isSilenced) {
			System.out.println(soundFile.getPath());
			return;
		}

		if (!soundFile.isAbsolute()) {
			soundFile = new File(System.getProperty("shootoff.home") + File.separator + soundFile.getPath());
		}

		try {
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
			playSound(audioInputStream, listener);
		} catch (UnsupportedAudioFileException | IOException e) {
			logger.error(String.format("Error reading sound file to play: soundFile = %s", soundFile), e);
		}
	}

	private static void playSound(AudioInputStream audioInputStream, Optional<LineListener> listener) {
		final AudioFormat format = audioInputStream.getFormat();
		final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

		SourceDataLine line = null;

		try {
			line = (SourceDataLine) AudioSystem.getLine(info);

			line.open(format);
			line.start();

			if (listener.isPresent()) {
				line.addLineListener(listener.get());
			} else {
				line.addLineListener((e) -> {
					if (LineEvent.Type.STOP.equals(e.getType())) {
						e.getLine().close();
						try {
							audioInputStream.close();
						} catch (Exception e1) {
							logger.error("Error closing audio input stream", e1);
						}
					}
				});
			}

			final SourceDataLine sourceLine = line;
			new Thread(() -> {
				int nBytesRead = 0;
				byte[] abData = new byte[1024];
				while (nBytesRead != -1) {
					try {
						nBytesRead = audioInputStream.read(abData, 0, abData.length);
					} catch (IOException e) {
						logger.error("Error playing sound clip", e);
					}
					if (nBytesRead >= 0) {
						sourceLine.write(abData, 0, nBytesRead);
					}
				}

				sourceLine.drain();
				sourceLine.close();
			}).start();
		} catch (LineUnavailableException e) {
			if (line != null) line.close();
			logger.error("Error playing sound clip", e);
		}
	}

	public static void playSounds(final List<File> soundFiles) {
		if (isSilenced) {
			soundFiles.forEach(System.out::println);
		} else {
			final SoundQueue sq = new SoundQueue(soundFiles);
			sq.play();
		}
	}

	private static class SoundQueue implements LineListener {
		private final List<File> soundFiles;
		private int queueIndex = 0;

		public SoundQueue(List<File> soundFiles) {
			this.soundFiles = soundFiles;
		}

		public void play() {
			playSound(soundFiles.get(queueIndex), Optional.of(this));
		}

		@Override
		public void update(final LineEvent event) {
			if (LineEvent.Type.STOP.equals(event.getType())) {
				event.getLine().close();

				queueIndex++;

				if (queueIndex < soundFiles.size()) {
					playSound(soundFiles.get(queueIndex), Optional.of(this));
				}
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

		if (shotTimerTable != null) {
			for (TableColumn<ShotEntry, String> column : exerciseColumns.values()) {
				shotTimerTable.getColumns().remove(column);
			}
		}

		for (final CameraView cv : camerasSupervisor.getCameraViews()) {
			cv.removeChild(exerciseLabels.get(cv));
		}

		exerciseLabels.clear();

		final Iterator<Button> itExerciseButtons = exerciseButtons.iterator();

		while (itExerciseButtons.hasNext()) {
			final Button exerciseButton = itExerciseButtons.next();
			buttonsPane.getChildren().remove(exerciseButton);
			itExerciseButtons.remove();
		}

		pauseShotDetection(false);
	}

	protected CamerasSupervisor getCamerasSupervisor() {
		return camerasSupervisor;
	}
}