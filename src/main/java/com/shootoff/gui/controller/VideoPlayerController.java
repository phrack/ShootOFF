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

package com.shootoff.gui.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.PlaybackListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class VideoPlayerController implements PlaybackListener {
	@FXML private TabPane videoTabPane;
	@FXML private Slider timeSlider;
	@FXML private Label timeLabel;
	@FXML private Button togglePlaybackButton;

	private static final Logger logger = LoggerFactory.getLogger(VideoPlayerController.class);

	private final Map<String, PlaybackContext> contexts = new HashMap<String, PlaybackContext>();
	private PlaybackContext currentContext;

	public void init(Map<String, File> videos) {
		togglePlaybackButton.setGraphic(new ImageView(
				new Image(VideoPlayerController.class.getResourceAsStream("/images/gnome_media_playback_start.png"))));
		createTabs(videos);
		currentContext = contexts.get(videoTabPane.getSelectionModel().getSelectedItem().getText());
		timeSlider.setMax(currentContext.getDuration());

		timeSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
				if (newValue == null) {
					timeLabel.setText("");
					return;
				}

				setTime(newValue.longValue());
			}
		});

		videoTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				togglePlaybackButton.setGraphic(new ImageView(new Image(
						VideoPlayerController.class.getResourceAsStream("/images/gnome_media_playback_start.png"))));

				currentContext.pausePlayback();
				currentContext = contexts.get(newValue.getText());

				timeSlider.setMax(currentContext.getDuration());
				timeSlider.setValue(currentContext.getTimestamp());
			}
		});
	}

	private void setTime(long timestamp /* ms */) {
		Date date = new Date(timestamp);
		DateFormat formatter = new SimpleDateFormat("mm:ss:SSS");
		timeLabel.setText(formatter.format(date));
	}

	@Override
	public void frameUpdated(long timestamp) {
		timeSlider.setValue(timestamp);

		if (timestamp == currentContext.getDuration()) {
			togglePlaybackButton.setGraphic(new ImageView(new Image(
					VideoPlayerController.class.getResourceAsStream("/images/gnome_media_playback_start.png"))));
		}
	}

	private static class PlaybackContext extends MediaListenerAdapter {
		private final IMediaReader mediaReader;
		private final PlaybackListener listener;
		private final long duration;
		private boolean isPlaying = false;
		private final ImageView imageView = new ImageView();
		private boolean doDelay = true;
		private long lastTimestamp = 0;

		public PlaybackContext(File videoFile, PlaybackListener listener) {
			this.listener = listener;

			mediaReader = ToolFactory.makeReader(videoFile.getPath());
			mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
			mediaReader.open();
			duration = mediaReader.getContainer().getDuration() / 1000; // microseconds
																		// to
																		// milliseconds
			mediaReader.addListener(this);
		}

		public long getDuration() {
			return duration;
		}

		public long getTimestamp() {
			return lastTimestamp;
		}

		public void onVideoPicture(IVideoPictureEvent event) {
			long currentTimestamp = event.getTimeStamp(TimeUnit.MILLISECONDS);

			if (doDelay) {
				try {
					long delay = currentTimestamp - lastTimestamp;
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					logger.error("Error while reading video frames", e);
				}
			}

			lastTimestamp = currentTimestamp;
			imageView.setImage(SwingFXUtils.toFXImage(event.getImage(), null));
			if (isPlaying || !doDelay) Platform.runLater(() -> listener.frameUpdated(currentTimestamp));
		}

		private void playVideo() {
			new Thread(() -> {
				IError ret = mediaReader.readPacket();
				while (isPlaying && ret == null) {
					ret = mediaReader.readPacket();
				}

				// ret is null if movie was paused
				if (ret != null && ret.getType() == IError.Type.ERROR_EOF) {
					isPlaying = false;
					lastTimestamp = getDuration();
					Platform.runLater(() -> listener.frameUpdated(getDuration()));
				}
			}, "PlayVideo").start();
		}

		private void playFromBeginning() {
			lastTimestamp = 0;
			mediaReader.open();
			mediaReader.getContainer().seekKeyFrame(0, 0, 0, 0, IContainer.SEEK_FLAG_ANY);
			playVideo();
		}

		public void nextFrame() {
			doDelay = false;
			mediaReader.readPacket();
			doDelay = true;
		}

		public void pausePlayback() {
			isPlaying = false;
		}

		public void togglePlayback() {
			isPlaying = !isPlaying;

			if (isPlaying) {
				if (lastTimestamp != getDuration()) {
					playVideo();
				} else {
					playFromBeginning();
				}
			}
		}

		public boolean isPlaying() {
			return isPlaying;
		}

		public ImageView getImageView() {
			return imageView;
		}
	}

	private void createTabs(Map<String, File> videos) {
		for (Entry<String, File> video : videos.entrySet()) {
			Tab videoTab = new Tab(video.getKey());
			videoTabPane.getTabs().add(videoTab);

			PlaybackContext context = new PlaybackContext(video.getValue(), this);
			videoTab.setContent(context.getImageView());
			contexts.put(video.getKey(), context);
		}
	}

	@FXML
	public void nextButtonClicked(ActionEvent event) {
		currentContext.nextFrame();
	}

	@FXML
	public void togglePlaybackButtonClicked(ActionEvent event) {
		currentContext.togglePlayback();

		if (currentContext.isPlaying()) {
			togglePlaybackButton.setGraphic(new ImageView(new Image(
					VideoPlayerController.class.getResourceAsStream("/images/gnome_media_playback_pause.png"))));
		} else {
			togglePlaybackButton.setGraphic(new ImageView(new Image(
					VideoPlayerController.class.getResourceAsStream("/images/gnome_media_playback_start.png"))));
		}
	}

	public Stage getStage() {
		return (Stage) togglePlaybackButton.getScene().getWindow();
	}
}
