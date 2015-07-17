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

package com.shootoff.camera;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.DebuggerListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.ICloseEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;

public class CameraManager {
	public static final int FEED_WIDTH = 640;
	public static final int FEED_HEIGHT = 480;
	public static final int MIN_SHOT_DETECTION_FPS = 5;
	public static final int LIGHTING_CONDITION_THRESHOLD = 90; 	// Greater is a bright room, less than is a dark room
																// for the purposes of tuning other thresholds.
																// Calculated using all test videos
	public static final float IDEAL_R_AVERAGE = 171; // Determined by averaging all of the red pixels per frame
												     // for a video recorded using a webcam with hw settings that
												     // worked well
	public static final float IDEAL_LUM = 136;		 // See comment above
	public static final int INIT_FRAME_COUNT = 5; // Used by current pixel transformer to decide how many frames
												  // to use for initialization

	private final PixelTransformer pixelTransformer = new BrightnessPixelTransformer();

	private final Logger logger = LoggerFactory.getLogger(CameraManager.class);
	private final Optional<Webcam> webcam;
	private final Object processingLock;
	private boolean processedVideo = false;
	private final CanvasManager canvasManager;
	private final Configuration config;
	private Optional<Bounds> projectionBounds = Optional.empty();

	private boolean isStreaming = true;
	private boolean isDetecting = true;
	private boolean cropFeedToProjection = false;
	private boolean limitDetectProjection = false;
	private Optional<Double> colorDiffThreshold = Optional.empty();
	private Optional<Integer> centerApproxBorderSize = Optional.empty();
	private Optional<Integer> minimumShotDimension = Optional.empty();
	private Optional<DebuggerListener> debuggerListener = Optional.empty();

	private boolean recording = false;
	private boolean isFirstFrame = true;
	private IMediaWriter videoWriter;
	private long recordingStartTime;
	private boolean[][] sectorStatuses;

	protected CameraManager(Webcam webcam, CanvasManager canvas, Configuration config) {
		this.webcam = Optional.of(webcam);
		processingLock = null;
		this.canvasManager = canvas;
		this.config = config;

		init(new Detector());
	}

	protected CameraManager(File videoFile, Object processingLock, CanvasManager canvas,
			Configuration config, boolean[][] sectorStatuses) {
		this.webcam = Optional.empty();
		this.processingLock = processingLock;
		this.canvasManager = canvas;
		this.config = config;

		Detector detector = new Detector();
		
	    IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
	    reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
	    reader.addListener(detector);

	    setSectorStatuses(sectorStatuses);
	    
	    while (reader.readPacket() == null)
	      do {} while(false);
	}

	private void init(Detector detector) {
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];

		// Turn on all shot sectors by default
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}

		new Thread(detector).start();
	}

	public boolean[][] getSectorStatuses() {
		return sectorStatuses;
	}

	public void setSectorStatuses(boolean[][] sectorStatuses) {
		this.sectorStatuses = sectorStatuses;
	}

	public void clearShots() {
		canvasManager.clearShots();
	}

	public void reset() {
		canvasManager.reset();
	}

	public void close() {
		if (webcam.isPresent()) webcam.get().close();
		if (recording) stopRecording();
	}

	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	public void setDetecting(boolean isDetecting) {
		this.isDetecting = isDetecting;
	}

	public void setProjectionBounds(Bounds cropBounds) {
		this.projectionBounds = Optional.ofNullable(cropBounds);
	}

	public void setCropFeedToProjection(boolean cropFeed) {
		cropFeedToProjection = cropFeed;
	}

	public void setLimitDetectProjection(boolean limitDetection) {
		limitDetectProjection = limitDetection;
	}

	public void startRecording(File videoFile) {
		logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
		videoWriter = ToolFactory.makeWriter(videoFile.getName());
		videoWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, FEED_WIDTH, FEED_HEIGHT);
		recordingStartTime = System.currentTimeMillis();
		isFirstFrame = true;

		recording = true;
	}

	public void stopRecording() {
		recording = false;
		videoWriter.close();
	}

	public Image getCurrentFrame() {
		if (webcam.isPresent()) {
			return SwingFXUtils.toFXImage(webcam.get().getImage(), null);
		} else {
			return null;
		}
	}

	public CanvasManager getCanvasManager() {
		return canvasManager;
	}

	public boolean isVideoProcessed() {
		return processedVideo;
	}

	public void setColorDiffThreshold(double threshold) {
		colorDiffThreshold = Optional.of(threshold);
		logger.debug("Set color component difference threshold: {}", threshold);
	}

	public void setCenterApproxBorderSize(int borderSize) {
		centerApproxBorderSize = Optional.of(borderSize);
		logger.debug("Set the shot center approximation border size to: {}", borderSize);
	}

	public void setMinimumShotDimension(int minDim) {
		minimumShotDimension = Optional.of(minDim);
		logger.debug("Set the minimum dimension for shots to: {}", minDim);
	}
	
	public void setThresholdListener(DebuggerListener thresholdListener) {
		this.debuggerListener = Optional.ofNullable(thresholdListener);
	}

	private class Detector extends MediaListenerAdapter implements Runnable {
		private boolean showedFPSWarning = false;
		private boolean pixelTransformerInitialized = false;
		private int seenFrames = 0;
		private final ExecutorService detectionExecutor = Executors.newFixedThreadPool(200);

		@Override
		public void run() {
			if (webcam.isPresent()) {
				if (!webcam.get().isOpen()) {
					webcam.get().setViewSize(new Dimension(FEED_WIDTH, FEED_HEIGHT));
					webcam.get().open();
				}

				streamCameraFrames();
			}
		}

		@Override
		/**
		 * From the MediaListenerAdapter. This method is used to get a new frame
		 * from a video that is being played back in a unit test, not to get
		 * a frame from the webcam.
		 */
		public void onVideoPicture(IVideoPictureEvent event)
		{
			BufferedImage currentFrame = event.getImage();

			AverageFrameComponents averages = fixFrame(currentFrame);

			if (pixelTransformerInitialized == false) {
				seenFrames++;
				if (seenFrames == INIT_FRAME_COUNT) pixelTransformerInitialized = true;
			} else {
				detectShots(currentFrame, averages);
			}
		}

		@Override
		public void onClose(ICloseEvent event) {
			synchronized (processingLock) {
				processedVideo = true;
				processingLock.notifyAll();
			}
			
			detectionExecutor.shutdown();
		}

		private void streamCameraFrames() {			
			long startDetectionCycle = System.currentTimeMillis();

			while (isStreaming) {
				if (!webcam.isPresent() || !webcam.get().isImageNew()) continue;
				
				BufferedImage currentFrame = webcam.get().getImage();
				final AverageFrameComponents averages = fixFrame(currentFrame);
				
				if (pixelTransformerInitialized == false) {
					seenFrames++;
					if (seenFrames == INIT_FRAME_COUNT) { 
						pixelTransformerInitialized = true;
					} else {
						continue;
					}
				}

				if (currentFrame == null && webcam.isPresent() && !webcam.get().isOpen()) {
					Platform.runLater(() -> {
							Alert cameraAlert = new Alert(AlertType.ERROR);

							Optional<String> cameraName = config.getWebcamsUserName(webcam.get());
							String messageFormat = "ShootOFF can no longer communicate with the webcam %s. Was it unplugged?";
							String message;
							if (cameraName.isPresent()) {
								message = String.format(messageFormat, cameraName.get());
							} else {
								message = String.format(messageFormat, webcam.get().getName());
							}

							cameraAlert.setTitle("Webcam Missing");
							cameraAlert.setHeaderText("Cannot Communicate with Camera!");
							cameraAlert.setResizable(true);
							cameraAlert.setContentText(message);
							cameraAlert.show();
						});

					return;
				}

				if (recording) {
					BufferedImage image = ConverterFactory.convertToType(currentFrame, BufferedImage.TYPE_3BYTE_BGR);
					IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

					IVideoPicture frame = converter.toPicture(image,
							(System.currentTimeMillis() - recordingStartTime) * 1000);
					frame.setKeyFrame(isFirstFrame);
					frame.setQuality(0);
					isFirstFrame = false;

					videoWriter.encodeVideo(0, frame);
				}

				if (cropFeedToProjection && projectionBounds.isPresent()) {
					Bounds b = projectionBounds.get();
					currentFrame = currentFrame.getSubimage((int)b.getMinX(), (int)b.getMinY(),
							(int)b.getWidth(), (int)b.getHeight());
				}

				Image img = SwingFXUtils.toFXImage(currentFrame, null);

				if (cropFeedToProjection) {
					canvasManager.updateBackground(img, projectionBounds);
				} else {
					canvasManager.updateBackground(img, Optional.empty());
				}

				if (System.currentTimeMillis() -
						startDetectionCycle >= config.getDetectionRate()) {

					startDetectionCycle = System.currentTimeMillis();
					final BufferedImage frame = currentFrame;
					detectionExecutor.submit(new Thread(() -> {detectShots(frame, averages);}));
				}
			}
			
			detectionExecutor.shutdown();
		}

		private AverageFrameComponents fixFrame(BufferedImage frame) {
			AverageFrameComponents averages = averageFrameComponents(frame);
			
			return averages;
		}

		private class AverageFrameComponents {
			private final float averageLum;
			private final float averageRed;

			public AverageFrameComponents(float lum, float red) {
				averageLum = lum; averageRed = red;
			}
			
			public float getAverageRed() {
				return averageRed;
			}
			
			public LightingCondition getLightingCondition() {
				if (averageLum > LIGHTING_CONDITION_THRESHOLD) {
					return LightingCondition.BRIGHT;
				} else {
					return LightingCondition.DARK;
				} 
			}
		}

		private AverageFrameComponents averageFrameComponents(BufferedImage frame) {
			long totalLum = 0;
			long totalRed = 0;
			for (int x = 0; x < frame.getWidth(); x++) {
				for (int y = 0; y < frame.getHeight(); y++) {
					Color c = new Color(frame.getRGB(x, y));

					pixelTransformer.updateFilter(x, y, c);

					totalLum += (c.getRed() + c.getRed() + c.getRed() +
							c.getBlue() +
							c.getGreen() + c.getGreen() + c.getGreen() + c.getGreen()) >> 3;
					totalRed += c.getRed();
				}
			}

			float totalPixels = (float)(frame.getWidth() * frame.getHeight());

			return new AverageFrameComponents((float)(totalLum) / totalPixels,
					(float)(totalRed) / totalPixels);
		}

		/* This is not perfect because it treats color temps as linear.
		 * Essentially we use the difference between the ideal average r
		 * component and the average for the current frame to adjust red
		 * and blue up or down to get roughly the ideal color temperature
		 * for shot detection.
		 */
		private void adjustColorTemperature(BufferedImage frame, float dr) {
			float db = 1 - (dr - 1);

			for (int x = 0; x < frame.getWidth(); x++) {
				for (int y = 0; y < frame.getHeight(); y++) {
					Color c = new Color(frame.getRGB(x, y));

					float r = c.getRed() * dr;
					if (r > 255) r = 255;
					if (r < 0) r = 0;
					float b = c.getBlue() * db;
					if (b > 255) b = 255;
					if (b < 0) b = 0;

					frame.setRGB(x, y, new Color((int)r, c.getGreen(), (int)b).getRGB());
				}
			}
		}
		private void detectShots(BufferedImage frame, AverageFrameComponents averages) {
			if (!isDetecting) return;

			BufferedImage workingCopy = new BufferedImage(frame.getWidth(), frame.getHeight(),
					BufferedImage.TYPE_INT_RGB);

			if (limitDetectProjection && projectionBounds.isPresent()) {
				Bounds b = projectionBounds.get();
				BufferedImage subFrame = frame.getSubimage((int)b.getMinX(), (int)b.getMinY(),
						(int)b.getWidth(), (int)b.getHeight());
				workingCopy.createGraphics().drawImage(subFrame, (int)b.getMinX(), (int)b.getMinY(), null);
			} else {
				workingCopy.createGraphics().drawImage(frame, 0, 0, null);
			}

			float averageRed = averages.getAverageRed();
			float colorCorrection = IDEAL_R_AVERAGE / averageRed;

			// If the color temperatures (using just the red component as an
			// approximation) are only a bit off from ideal step up the heat.
			// We don't want to make big changes or it will blow up in dark
			// rooms by trying to do huge corrections that max r and zero b
			// components in rgb pixels.
			if (averageRed < IDEAL_R_AVERAGE && colorCorrection < 2f) {
				adjustColorTemperature(frame, colorCorrection);
			}
			
			pixelTransformer.applyFilter(workingCopy, averages.getLightingCondition());

			BufferedImage grayScale = new BufferedImage(frame.getWidth(),
					frame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			grayScale.createGraphics().drawImage(workingCopy, 0, 0, null);

			if (webcam.isPresent()) {
				double webcamFPS = webcam.get().getFPS();
				if (debuggerListener.isPresent()) debuggerListener.get().updateCameraFPS(webcamFPS);
				if (webcamFPS < MIN_SHOT_DETECTION_FPS && !showedFPSWarning) {
					logger.warn("[{}] Current webcam FPS is {}, which is too low for reliable shot detection",
							webcam.get().getName(), webcamFPS);
					showFPSWarning(webcamFPS);
					showedFPSWarning = true;
				}
			}

			ShotSearcher shotSearcher = new ShotSearcher(config, canvasManager, sectorStatuses,
					frame, grayScale, projectionBounds);

			if (colorDiffThreshold.isPresent()) {
				shotSearcher.setColorDiffThreshold(colorDiffThreshold.get());
			}

			if (centerApproxBorderSize.isPresent()) {
				shotSearcher.setCenterApproxBorderSize(centerApproxBorderSize.get());
			}

			if (minimumShotDimension.isPresent()) {
				shotSearcher.setMinimumShotDimension(minimumShotDimension.get());
			}

			new Thread(shotSearcher).start();

			if (debuggerListener.isPresent()) {
				debuggerListener.get().updateDebugView(workingCopy);
			}
		}

		private void showFPSWarning(double fps) {
			Platform.runLater(() -> {
				Alert cameraAlert = new Alert(AlertType.ERROR);

				Optional<String> cameraName = config.getWebcamsUserName(webcam.get());
				String messageFormat = "The FPS from %s has dropped to %f, which is too low for reliable shot detection. Some"
						+ " shots may be missed. You may be able to raise the FPS by closing other applications.";
				String message;
				if (cameraName.isPresent()) {
					message = String.format(messageFormat, cameraName.get(), fps);
				} else {
					message = String.format(messageFormat, webcam.get().getName(), fps);
				}

				cameraAlert.setTitle("Webcam FPS Too Low");
				cameraAlert.setHeaderText("Webcam FPS is too low!");
				cameraAlert.setResizable(true);
				cameraAlert.setContentText(message);
				cameraAlert.show();
			});
		}
	}
}