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

package com.shootoff.gui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.cameratypes.Camera;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.converter.DefaultStringConverter;

public class CheckableImageListCell extends TextFieldListCell<String> {
	private static final Logger logger = LoggerFactory.getLogger(CheckableImageListCell.class);

	private static final Map<Camera, Pane> containerCache = new HashMap<>();
	private static final Map<Camera, CheckBox> checkCache = new HashMap<>();
	private final List<Camera> webcams;
	private final List<String> configuredNames;
	private final List<Camera> configuredCameras;
	private final Optional<Set<Camera>> recordingCameras;

	public CheckableImageListCell(List<Camera> webcams, List<String> configuredNames, List<Camera> configuredCameras,
			CameraRenamedListener cameraRenamedListener, final DesignateShotRecorderListener designatedListener,
			final Optional<Set<Camera>> recordingCameras) {
		this.webcams = new ArrayList<>(webcams);
		this.configuredNames = configuredNames;
		this.configuredCameras = configuredCameras;
		this.recordingCameras = recordingCameras;

		this.setConverter(new DefaultStringConverter());

		this.itemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (oldValue == null || newValue == null) return;

				final Optional<Pane> webcamContainer = fetchWebcamControls(oldValue);

				if (webcamContainer.isPresent()) {
					setGraphic(webcamContainer.get());
				}

				cameraRenamedListener.cameraRenamed(oldValue, newValue);
			}
		});

		if (designatedListener != null) {
			this.setOnMouseClicked((event) -> {
				if (!fetchWebcamChecked(getText())) {
					this.setEditable(false);
					return;
				}

				if (event.getClickCount() > 1 && fetchWebcamChecked(getText())) {
					this.setEditable(true);
					startEdit();
				}

				if (!event.isAltDown()) return;

				cancelEdit();

				// If camera is not checked, don't designate it and start
				// editing
				if (!fetchWebcamChecked(getText())) {
					startEdit();
					return;
				}

				if (getStyle().isEmpty()) {
					setStyle("-fx-background-color: green");
					designatedListener.registerShotRecorder(getText());
				} else {
					setStyle("");
					designatedListener.unregisterShotRecorder(getText());
				}

				final Optional<Pane> webcamContainer = fetchWebcamControls(CheckableImageListCell.this.getText());

				if (webcamContainer.isPresent()) {
					setGraphic(webcamContainer.get());
				}
			});
		}
	}

	public static void createImageCache(List<Camera> webcams, CameraSelectionListener listener) {
		for (Camera c : webcams) {
			if (containerCache.containsKey(c)) continue;

			cacheCamera(c, listener);
		}
	}

	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

		if (empty || item == null) {
			setGraphic(null);
			setText(null);
			return;
		}

		if (recordingCameras.isPresent()) {
			for (Camera recordingCamera : recordingCameras.get()) {
				if (recordingCamera.getName().equals(item)) {
					setStyle("-fx-background-color: green");
					break;
				}
			}
		}

		final Optional<Pane> webcamContainer = fetchWebcamControls(item);
		
		if (webcamContainer.isPresent()) {
			setGraphic(webcamContainer.get());
		}

		setText(item);
	}

	public static void cacheCamera(Camera c, CameraSelectionListener listener) {
		final ImageView iv = new ImageView();
		iv.setFitWidth(100);
		iv.setFitHeight(75);
		
		new Thread(() -> {
			Optional<Image> img = fetchWebcamImage(c);

			if (img.isPresent()) {
				iv.setImage(img.get());
			}
		}, "FetchImageCellWebcamImages").start();
		
		final CheckBox cb = new CheckBox();
		cb.setOnAction((event) -> {
			if (listener != null) listener.cameraSelectionChanged(c, cb.isSelected());
		});
		checkCache.put(c, cb);
		
		final HBox webcamContainer = new HBox(cb, iv);
		webcamContainer.setAlignment(Pos.CENTER);
		containerCache.put(c, webcamContainer);
	}
	
	public interface CameraRenamedListener {
		void cameraRenamed(String oldName, String newName);
	}
	
	public interface CameraSelectionListener {
		void cameraSelectionChanged(Camera camera, boolean isSelected);
	}
	
	public static Map<Camera, CheckBox> getCameraCheckBoxes() {
		return checkCache;
	}

	private Optional<Pane> fetchWebcamControls(String webcamName) {
		Optional<Pane> webcamContainer = Optional.empty();

		if (configuredNames == null) {
			webcamContainer = fetchUnrenamedWebcamControls(webcamName);
		} else {
			try {
				int cameraIndex = configuredNames.indexOf(webcamName);
				if (cameraIndex >= 0) {
					webcamContainer = Optional.of(containerCache.get(configuredCameras.get(cameraIndex)));
				} else {
					webcamContainer = fetchUnrenamedWebcamControls(webcamName);
				}
			} catch (NullPointerException e) {
				logger.error("Error fetching cached controls for configured camera: " + webcamName, e);
				throw e;
			}
		}

		return webcamContainer;
	}
	
	private Optional<Pane> fetchUnrenamedWebcamControls(String webcamName) {
		for (Camera webcam : webcams) {
			if (webcam.getName().equals(webcamName)) {
				return Optional.of(containerCache.get(webcam));
			}
		}
		
		return Optional.empty();
	}
	
	private boolean fetchWebcamChecked(String webcamName) {
		boolean isChecked = false;

		if (configuredNames == null) {
			isChecked = fetchUnrenamedWebcamChecked(webcamName);
		} else {
			try {
				int cameraIndex = configuredNames.indexOf(webcamName);
				if (cameraIndex >= 0) {
					isChecked = checkCache.get(configuredCameras.get(cameraIndex)).isSelected();
				} else {
					isChecked = fetchUnrenamedWebcamChecked(webcamName);
				}
			} catch (NullPointerException e) {
				logger.error("Error fetching cached check state for configured camera: " + webcamName, e);
				throw e;
			}
		}

		return isChecked;
	}
	
	private boolean fetchUnrenamedWebcamChecked(String webcamName) {
		for (Camera webcam : webcams) {
			if (webcam.getName().equals(webcamName)) {
				return checkCache.get(webcam).isSelected();
			}
		}
		
		return false;
	}

	private static Optional<Image> fetchWebcamImage(Camera webcam) {
		boolean cameraOpened = false;

		synchronized (webcam) {
			if (!webcam.isOpen()) {
				webcam.setViewSize(new Dimension(CameraManager.DEFAULT_FEED_WIDTH, CameraManager.DEFAULT_FEED_HEIGHT));
				webcam.open();
				cameraOpened = true;
			}

			Image webcamImg = null;
			if (webcam.isOpen()) {
				BufferedImage img = webcam.getBufferedImage();

				if (img != null) {
					webcamImg = SwingFXUtils.toFXImage(img, null);
				}
			}

			if (cameraOpened == true) {
				webcam.close();
			}

			return Optional.ofNullable(webcamImg);
		}
	}
}
