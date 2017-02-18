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
import java.util.Map.Entry;
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
import com.shootoff.camera.processors.ShotProcessor;
import com.shootoff.camera.processors.VirtualMagazineProcessor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.gui.ParListener;
import com.shootoff.gui.ShotEntry;
import com.shootoff.targets.Target;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/**
 * This class implements common training exercise operations. All training
 * exercises should extend it unless they are meant to be used solely on the
 * projector arena. Projector arena exercises should extend
 * {@link com.shootoff.plugins.ProjectorTrainingExerciseBase}.
 * 
 * @author phrack
 */
public abstract class TrainingExerciseBase {
	private static final Logger logger = LoggerFactory.getLogger(TrainingExerciseBase.class);

	protected Configuration config;
	private static boolean isSilenced = false;

	@SuppressWarnings("unused") private List<Target> targets;
	private CamerasSupervisor camerasSupervisor;
	private TrainingExerciseView exerciseView;
	private VBox buttonsContainer;
	private Pane trainingExerciseContainer;
	private TableView<ShotEntry> shotTimerTable;
	private boolean changedRowColor = false;
	private boolean haveDelayControls = false;
	private boolean haveParControls = false;

	private final Map<CameraView, Label> exerciseLabels = new HashMap<>();
	private final List<Pane> exercisePanes = new ArrayList<>();
	private final Map<String, TableColumn<ShotEntry, String>> exerciseColumns = new HashMap<>();
	private final List<Button> exerciseButtons = new ArrayList<>();

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public TrainingExerciseBase() {}

	public TrainingExerciseBase(List<Target> targets) {
		this.targets = targets;
	}

	public void init(CamerasSupervisor camerasSupervisor, TrainingExerciseView exerciseView) {
		config = Configuration.getConfig();
		init(config, camerasSupervisor, exerciseView.getButtonsPane(), exerciseView.getShotEntryTable());
		trainingExerciseContainer = exerciseView.getTrainingExerciseContainer();
		this.exerciseView = exerciseView;

		if (exerciseView.getArenaView().isPresent()) {
			final Label exerciseLabel = new Label();
			exerciseLabel.setTextFill(Color.WHITE);
			final CameraView arenaView = exerciseView.getArenaView().get();
			arenaView.addChild(exerciseLabel);
			exerciseLabels.put(arenaView, exerciseLabel);
		}
	}

	// This is only required for unit tests where we don't want to create a full
	// ShootOFFController
	public void init(final Configuration config, final CamerasSupervisor camerasSupervisor, final VBox buttonsPane,
			final TableView<ShotEntry> shotEntryTable) {
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		buttonsContainer = buttonsPane;
		shotTimerTable = shotEntryTable;

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

	private static class DelayPane extends GridPane {
		public DelayPane(DelayedStartListener listener) {
			getColumnConstraints().add(new ColumnConstraints(100));
			setVgap(5);

			final Label instructionsLabel = new Label("Set interval within which a beep will sound\n"
					+ "to signal the start of a round.\nDefault: A round starts after a random wait\n"
					+ "between 4 and 8 seconds in length.\n");
			instructionsLabel.setPrefSize(300, 77);

			this.add(instructionsLabel, 0, 0, 2, 3);
			addRow(3, new Label("Min (s)"));
			addRow(4, new Label("Max (s)"));

			final TextField minTextField = new TextField("4");
			this.add(minTextField, 1, 3);

			final TextField maxTextField = new TextField("8");
			this.add(maxTextField, 1, 4);

			minTextField.textProperty().addListener((observable, oldValue, newValue) -> {
				if (!newValue.matches("\\d*")) {
					minTextField.setText(oldValue);
					minTextField.positionCaret(minTextField.getLength());
				} else {
					listener.updatedDelayedStartInterval(Integer.parseInt(minTextField.getText()),
							Integer.parseInt(maxTextField.getText()));
				}
			});

			maxTextField.textProperty().addListener((observable, oldValue, newValue) -> {
				if (!newValue.matches("\\d*")) {
					maxTextField.setText(oldValue);
					maxTextField.positionCaret(maxTextField.getLength());
				} else {
					listener.updatedDelayedStartInterval(Integer.parseInt(minTextField.getText()),
							Integer.parseInt(maxTextField.getText()));
				}
			});
		}
	}

	/**
	 * Shows controls that let the user set the interval for a random start
	 * delay in seconds. Notify interval points to a function that gets the min
	 * and max values for the interval as parameters.
	 * 
	 * @param listener
	 *            the object to notify when the user closes this window with a
	 *            set value
	 * 
	 * @since 1.4
	 */
	public void getDelayedStartInterval(final DelayedStartListener listener) {
		if (listener == null) throw new IllegalArgumentException("Delayed start listener must be non-null");

		if (haveDelayControls) return;

		final DelayPane delayPane = new DelayPane(listener);

		trainingExerciseContainer.getChildren().add(delayPane);
		exercisePanes.add(delayPane);
		haveDelayControls = true;
	}

	/**
	 * A copy of getDelayedStartInterval() plus setting the PAR time.
	 * 
	 * @param listener
	 *            the object to notify when a par time is set.
	 */
	public void getParInterval(final ParListener listener) {
		if (listener == null) throw new IllegalArgumentException("Par listener must be non-null");

		if (haveParControls) return;

		final DelayPane parPane = new DelayPane(listener);

		final TextField parTextField = new TextField("2.0");
		parPane.addRow(5, new Label("PAR Time (s)"));
		parPane.add(parTextField, 1, 5);

		parTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("^\\d*\\.?\\d*$")) {
					parTextField.setText(oldValue);
					parTextField.positionCaret(parTextField.getLength());
				} else {
					listener.updatedParInterval(Double.parseDouble(parTextField.getText()));
				}
			}
		});

		trainingExerciseContainer.getChildren().add(parPane);
		exercisePanes.add(parPane);
		haveParControls = true;
	}

	/**
	 * Add a pane with exercise-specific controls (e.g. to collect user
	 * settings) to the bottom of the main ShootOFF window.
	 * 
	 * @param pane
	 *            a pane with exercise-specific controls to add to the main
	 *            ShootOFF window
	 */
	public void addExercisePane(Pane pane) {
		trainingExerciseContainer.getChildren().add(pane);
		exercisePanes.add(pane);
	}

	/**
	 * Adds a button to the right of the reset button on the main ShootOFF
	 * window with caption <tt>text</tt> and action handler
	 * <tt>eventHandler</tt>.
	 * 
	 * @param text
	 *            the caption to display on the new button
	 * 
	 * @param eventHandler
	 *            the event handler that performs actions when this new button
	 *            is clicked
	 * 
	 * @return the new button that was added to the main ShootOFF window
	 */
	public Button addShootOFFButton(final String text, final EventHandler<ActionEvent> eventHandler) {
		final Button exerciseButton = new Button(text);
		final Button resetButton = (Button) buttonsContainer.getChildren().get(0);
		exerciseButton.setOnAction(eventHandler);
		exerciseButton.setPrefSize(resetButton.getPrefWidth(), resetButton.getPrefHeight());
		exerciseButtons.add(exerciseButton);

		buttonsContainer.getChildren().add(exerciseButton);

		return exerciseButton;
	}

	public void removeShootOFFButton(final Button exerciseButton) {
		if (!exerciseButtons.contains(exerciseButton)) return;

		buttonsContainer.getChildren().remove(exerciseButton);
		exerciseButtons.remove(exerciseButton);
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
	 * 
	 * @since 1.3
	 */
	public void addShotTimerColumn(String name, int width) {
		final TableColumn<ShotEntry, String> newCol = new TableColumn<>(name);
		newCol.setPrefWidth(width);
		newCol.setCellValueFactory(new Callback<CellDataFeatures<ShotEntry, String>, ObservableValue<String>>() {
			@Override
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
	 * 
	 * @since 1.3
	 */
	public void setShotTimerColumnText(final String name, final String value) {
		if (shotTimerTable != null && shotTimerTable.getItems() != null) {
			final Runnable shotTimerColumnTextSetter = () -> {
				if (shotTimerTable.getItems().size() == 0) {
					logger.error("Trying to set shot timer column text on an empty shot timer list",
							new AssertionError("Shot timer table is empty"));
					return;
				}

				shotTimerTable.getItems().get(shotTimerTable.getItems().size() - 1).setExerciseValue(name, value);
			};

			if (Platform.isFxApplicationThread()) {
				shotTimerColumnTextSetter.run();
			} else {
				Platform.runLater(shotTimerColumnTextSetter);
			}
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
	 * 
	 * @since 1.0
	 */
	public void showTextOnFeed(String message) {
		if (config.inDebugMode()) System.out.println(message);

		if (config.getSessionRecorder().isPresent()) {
			config.getSessionRecorder().get().recordExerciseFeedMessage(message);
		}

		Platform.runLater(() -> {
			for (final Label exerciseLabel : exerciseLabels.values()) {
				exerciseLabel.setText(message);
			}
		});
	}

	/**
	 * Reset the round count in the virtual magazine to its configured capacity.
	 * 
	 * @since 4.0
	 */
	public void reloadVirtualMagazine() {
		if (!config.useVirtualMagazine()) return;

		for (ShotProcessor processor : config.getShotProcessors()) {
			if (processor instanceof VirtualMagazineProcessor) {
				processor.reset();
			}
		}
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

		if (config.getExercise().isPresent()) config.getExercise().get().reset(exerciseView.getTargets());
	}

	/**
	 * Get a list of all of the targets on every canvas manager
	 * 
	 * @return a list of all targets that ShootOFF will detect hits (only lists
	 *         targets known at the time this method was called)
	 */
	public List<Target> getCurrentTargets() {
		return exerciseView.getTargets();
	}

	/**
	 * Sets whether or not shot detection is paused.
	 * 
	 * @param isPaused
	 *            <tt>true</tt> to temporarily stop detecting shots
	 * 
	 * @since 1.4
	 */
	public void pauseShotDetection(final boolean isPaused) {
		camerasSupervisor.setDetectingAll(!isPaused);
	}

	/**
	 * Plays an audio file asynchronously.
	 * 
	 * @param soundFilePath
	 *            the audio file to play (e.g. "sounds/metal_clang.wav")
	 * 
	 * @since 1.1
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
						} catch (final Exception e1) {
							logger.error("Error closing audio input stream", e1);
						}
					}
				});
			}

			final SourceDataLine sourceLine = line;
			new Thread(() -> {
				int nBytesRead = 0;
				final byte[] abData = new byte[1024];
				while (nBytesRead != -1) {
					try {
						nBytesRead = audioInputStream.read(abData, 0, abData.length);
					} catch (final IOException e) {
						logger.error("Error playing sound clip", e);
					}
					if (nBytesRead >= 0) {
						sourceLine.write(abData, 0, nBytesRead);
					}
				}

				sourceLine.drain();
				sourceLine.close();
			}).start();
		} catch (final LineUnavailableException e) {
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
			for (final TableColumn<ShotEntry, String> column : exerciseColumns.values()) {
				shotTimerTable.getColumns().remove(column);
			}
		}

		for (final Entry<CameraView, Label> entry : exerciseLabels.entrySet()) {
			entry.getKey().removeChild(entry.getValue());
		}

		exerciseLabels.clear();

		final Iterator<Pane> itExercisePanes = exercisePanes.iterator();

		while (itExercisePanes.hasNext()) {
			trainingExerciseContainer.getChildren().remove(itExercisePanes.next());
			itExercisePanes.remove();
		}

		final Iterator<Button> itExerciseButtons = exerciseButtons.iterator();

		while (itExerciseButtons.hasNext()) {
			buttonsContainer.getChildren().remove(itExerciseButtons.next());
			itExerciseButtons.remove();
		}

		pauseShotDetection(false);
	}

	protected CamerasSupervisor getCamerasSupervisor() {
		return camerasSupervisor;
	}
}