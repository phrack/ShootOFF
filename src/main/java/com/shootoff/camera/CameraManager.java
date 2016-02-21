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

package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import javafx.geometry.Bounds;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.arenamask.ArenaMaskManager;
import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.DebuggerListener;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.util.TimerPool;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Callback;

public class CameraManager {
	public static final int DEFAULT_FEED_WIDTH = 640;
	public static final int DEFAULT_FEED_HEIGHT = 480;

	private int feedWidth = DEFAULT_FEED_WIDTH;
	private int feedHeight = DEFAULT_FEED_HEIGHT;

	public static final int MIN_SHOT_DETECTION_FPS = 5;
	public static final int DEFAULT_FPS = 30;

	private final static int DIAGNOSTIC_MESSAGE_DURATION = 1000; // ms

	private long lastVideoTimestamp = -1;
	private long lastCameraTimestamp = -1;
	private long lastFrameCount = 0;
	private static final int SECOND_IN_MICROSECONDS = 1000 * 1000;

	private final ShotDetectionManager shotDetectionManager;

	private static final Logger logger = LoggerFactory.getLogger(CameraManager.class);
	private final Optional<Camera> webcam;
	private final Object processingLock;
	private boolean processedVideo = false;
	private final CanvasManager canvasManager;
	private final Configuration config;
	private Optional<Bounds> projectionBounds = Optional.empty();

	private volatile boolean isStreaming = true;
	private volatile boolean isDetecting = true;
	private volatile boolean isCalibrating = false;
	private boolean shownBrightnessWarning = false;
	private boolean cropFeedToProjection = false;
	private boolean limitDetectProjection = false;

	private Optional<Integer> minimumShotDimension = Optional.empty();

	private Optional<DebuggerListener> debuggerListener = Optional.empty();

	private boolean recordingStream = false;
	private boolean isFirstStreamFrame = true;
	private IMediaWriter videoWriterStream;
	private long recordingStartTime;

	private boolean recordingShots = false;
	private RollingRecorder rollingRecorder;
	private Map<Shot, ShotRecorder> shotRecorders = new ConcurrentHashMap<Shot, ShotRecorder>();

	private boolean[][] sectorStatuses;

	private int frameCount = 0;
	private long currentFrameTimestamp = -1;

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	private static double webcamFPS = DEFAULT_FPS;

	private AutoCalibrationManager acm = null;
	private boolean autoCalibrationEnabled = false;
	public boolean cameraAutoCalibrated = false;

	private ShootOFFController controller;

	public ShootOFFController getController() {
		return controller;
	}

	public void setController(ShootOFFController controller) {
		this.controller = controller;
	}

	private final ArenaMaskManager arenaMaskManager = new ArenaMaskManager();

	private final DeduplicationProcessor deduplicationProcessor = new DeduplicationProcessor(this);

	public DeduplicationProcessor getDeduplicationProcessor() {
		return deduplicationProcessor;
	}

	public CameraManager(Camera webcam, CanvasManager canvas, Configuration config) {
		((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.DEBUG);

		this.webcam = Optional.of(webcam);
		processingLock = null;
		this.canvasManager = canvas;
		this.config = config;

		this.canvasManager.setCameraManager(this);

		initDetector(new Detector());

		this.shotDetectionManager = new ShotDetectionManager(this, config, canvas);

	}

	File videoFile;

	protected CameraManager(File videoFile, Object processingLock, CanvasManager canvas, Configuration config,
			boolean[][] sectorStatuses, Optional<Bounds> projectionBounds) {
		((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.DEBUG);

		this.webcam = Optional.empty();
		this.processingLock = processingLock;
		this.canvasManager = canvas;
		this.config = config;

		this.canvasManager.setCameraManager(this);

		setSectorStatuses(sectorStatuses);

		if (projectionBounds.isPresent()) {
			setLimitDetectProjection(true);
			setProjectionBounds(projectionBounds.get());
		}

		this.shotDetectionManager = new ShotDetectionManager(this, config, canvas);

		this.videoFile = videoFile;

	}

	public void processVideo() {
		Detector detector = new Detector();

		IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
		reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		reader.addListener(detector);

		logger.trace("opening {}", videoFile.getAbsolutePath());

		while (reader.readPacket() == null)
			do {} while (false);
	}

	private void initDetector(Detector detector) {

		sectorStatuses = new boolean[ShotDetectionManager.SECTOR_ROWS][ShotDetectionManager.SECTOR_COLUMNS];

		// Turn on all shot sectors by default

		for (int x = 0; x < ShotDetectionManager.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotDetectionManager.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}

		new Thread(detector, "ShotDetector").start();
	}

	public boolean isSectorOn(int x, int y) {
		return sectorStatuses[y][x];
	}

	public void setSectorStatuses(boolean[][] sectorStatuses) {
		this.sectorStatuses = sectorStatuses;
	}

	public int getFeedWidth() {
		return feedWidth;
	}

	public int getFeedHeight() {
		return feedHeight;
	}

	// This exists for future improvements. It doesn't handle
	// potential side effects of modifying the feed resolution
	// on the fly.
	public void setFeedResolution(int width, int height) {
		feedWidth = width;
		feedHeight = height;
	}

	public void clearShots() {
		canvasManager.clearShots();
	}

	public void reset() {
		canvasManager.reset();
	}

	public void close() {
		if (arenaMaskManager != null) arenaMaskManager.isStreaming = false;

		getCanvasManager().close();
		setDetecting(false);
		setStreaming(false);
		if (webcam.isPresent()) webcam.get().close();
		if (recordingStream) stopRecordingStream();
		TimerPool.cancelTimer(brightnessDiagnosticFuture);
		TimerPool.cancelTimer(motionDiagnosticFuture);

		if (recordingCalibratedArea) stopRecordingCalibratedArea();
	}

	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	public void setDetecting(boolean isDetecting) {
		// Lock this to false during calibration
		if (this.isCalibrating && isDetecting) {
			logger.info("Not changing detection to true during calibration");
			return;
		}

		logger.trace("setDetecting was {} now {}", this.isDetecting, isDetecting);

		this.isDetecting = isDetecting;
	}

	public void setCalibrating(boolean isCalibrating) {
		this.isCalibrating = isCalibrating;
		if (isCalibrating) setDetecting(false);
	}

	public boolean isDetecting() {
		return isDetecting;
	}

	public void setProjectionBounds(Bounds projectionBounds) {
		this.projectionBounds = Optional.ofNullable(projectionBounds);
	}

	public void setCropFeedToProjection(boolean cropFeed) {
		cropFeedToProjection = cropFeed;
	}

	public void setLimitDetectProjection(boolean limitDetection) {
		limitDetectProjection = limitDetection;
	}

	public boolean isCroppingFeedToProjection() {
		return cropFeedToProjection;
	}

	public boolean isLimitingDetectionToProjection() {
		return limitDetectProjection;
	}

	public Optional<Bounds> getProjectionBounds() {
		return projectionBounds;
	}

	public void startRecordingStream(File videoFile) {
		logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
		videoWriterStream = ToolFactory.makeWriter(videoFile.getName());
		videoWriterStream.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, getFeedWidth(), getFeedHeight());
		recordingStartTime = System.currentTimeMillis();
		isFirstStreamFrame = true;

		recordingStream = true;
	}

	public void stopRecordingStream() {
		recordingStream = false;
		videoWriterStream.close();
	}

	public void notifyShot(Shot shot) {
		shotRecorders.put(shot, rollingRecorder.fork());
	}

	public ShotRecorder getRevelantRecorder(Shot shot) {
		return shotRecorders.get(shot);
	}

	public void startRecordingShots() {
		String sessionName = null;
		if (config.getSessionRecorder().isPresent()) {
			sessionName = config.getSessionRecorder().get().getSessionName();

			File sessionVideoFolder = new File(System.getProperty("shootoff.home") + File.separator + "sessions"
					+ File.separator + config.getSessionRecorder().get().getSessionName());

			if (!sessionVideoFolder.exists()) {
				if (!sessionVideoFolder.mkdirs()) {
					logger.error("Could not create video folder for session: {}", sessionVideoFolder.getAbsolutePath());
				}
			}
		}

		String cameraName = "UNNAMED";

		if (webcam.isPresent()) {
			Optional<String> userCameraName = config.getWebcamsUserName(webcam.get());

			if (userCameraName.isPresent()) {
				cameraName = userCameraName.get();
			} else {
				cameraName = webcam.get().getName();
			}
		}

		setDetecting(false);

		rollingRecorder = new RollingRecorder(ICodec.ID.CODEC_ID_MPEG4, ".mp4", sessionName, cameraName, this);
		recordingShots = true;
	}

	public void stopRecordingShots() {
		recordingShots = false;
		for (ShotRecorder r : shotRecorders.values())
			r.close();
		shotRecorders.clear();
		if (rollingRecorder != null) {
			rollingRecorder.close();
			rollingRecorder = null;
		}

		setDetecting(true);
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

	public void setMinimumShotDimension(int minDim) {
		minimumShotDimension = Optional.of(minDim);
		logger.debug("Set the minimum dimension for shots to: {}", minDim);
	}

	public Optional<Integer> getMinimumShotDimension() {
		return minimumShotDimension;
	}

	public void setThresholdListener(DebuggerListener thresholdListener) {
		this.debuggerListener = Optional.ofNullable(thresholdListener);
	}

	public Optional<DebuggerListener> getDebuggerListener() {
		return debuggerListener;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public void setFrameCount(int i) {
		frameCount = i;
	}

	public double getFPS() {
		return webcamFPS;
	}

	private ScheduledFuture<?> brightnessDiagnosticFuture = null;
	private ScheduledFuture<?> motionDiagnosticFuture = null;

	public Mat curFrameMask = null;

	private boolean recordCalibratedArea = false;
	private IMediaWriter videoWriterCalibratedArea;
	private long recordingCalibratedAreaStartTime;
	private boolean isFirstCalibratedAreaFrame;
	private boolean recordingCalibratedArea;

	public void startRecordingCalibratedArea(File videoFile, int width, int height) {
		logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
		videoWriterCalibratedArea = ToolFactory.makeWriter(videoFile.getName());
		videoWriterCalibratedArea.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, width, height);
		recordingCalibratedAreaStartTime = System.currentTimeMillis();
		isFirstCalibratedAreaFrame = true;

		recordingCalibratedArea = true;
	}

	public void stopRecordingCalibratedArea() {
		recordingCalibratedArea = false;
		videoWriterCalibratedArea.close();
	}

	private class Detector extends MediaListenerAdapter implements Runnable {
		private boolean showedFPSWarning = false;

		@Override
		public void run() {
			if (webcam.isPresent()) {
				if (!webcam.get().isOpen()) {
					webcam.get().setViewSize(new Dimension(getFeedWidth(), getFeedHeight()));
					webcam.get().open();

					Dimension openDimension = webcam.get().getViewSize();

					if ((int) openDimension.getWidth() != getFeedWidth()
							|| (int) openDimension.getHeight() != getFeedHeight()) {
						logger.warn("Camera dimension differs from requested dimensions, requested {} {} actual {} {}",
								getFeedWidth(), getFeedHeight(), (int) openDimension.getWidth(),
								(int) openDimension.getHeight());
					}

					setFeedResolution((int) openDimension.getWidth(), (int) openDimension.getHeight());
				}

				streamCameraFrames();
			}
		}

		/**
		 * From the MediaListenerAdapter. This method is used to get a new frame
		 * from a video that is being played back in a unit test, not to get a
		 * frame from the webcam.
		 */

		private long initialSystemTimeAtVideoStart = -1;

		@Override
		public void onVideoPicture(IVideoPictureEvent event) {
			BufferedImage currentFrame = event.getImage();

			if (initialSystemTimeAtVideoStart == -1) initialSystemTimeAtVideoStart = System.currentTimeMillis();

			currentFrameTimestamp = (event.getTimeStamp() / 1000) + initialSystemTimeAtVideoStart;

			if (getFrameCount() == 0) {
				setFeedResolution(currentFrame.getWidth(), currentFrame.getHeight());
				shotDetectionManager.reInitializeDimensions();
			}

			if (lastVideoTimestamp > -1 && (getFrameCount() % 5) == 0) {

				double estimateFPS = (double) SECOND_IN_MICROSECONDS
						/ (double) (event.getTimeStamp() - lastVideoTimestamp);

				setFPS(estimateFPS);
			}
			lastVideoTimestamp = event.getTimeStamp();

			processFrame(currentFrame);
		}

		@Override
		public void onClose(ICloseEvent event) {
			synchronized (processingLock) {
				processedVideo = true;
				processingLock.notifyAll();
			}
		}

		private void streamCameraFrames() {
			while (isStreaming) {
				if (!webcam.isPresent() || !webcam.get().isImageNew()) continue;

				BufferedImage currentFrame = webcam.get().getImage();
				currentFrameTimestamp = System.currentTimeMillis();

				if (currentFrame == null && webcam.isPresent() && !webcam.get().isOpen()) {
					// Camera appears to have closed
					showMissingCameraError();
					return;
				} else if (currentFrame == null && webcam.isPresent() && webcam.get().isOpen()) {
					// Camera appears to be open but got a null frame
					logger.warn("Null frame from camera: {}", webcam.get().getName());
					continue;
				}

				if ((int) (getFrameCount() % getFPS()) == 0) {
					estimateCameraFPS();
				}

				currentFrame = processFrame(currentFrame);
				if (currentFrame == null) continue;

				if (cropFeedToProjection && projectionBounds.isPresent()) {
					Bounds b = projectionBounds.get();

					currentFrame = currentFrame.getSubimage((int) b.getMinX(), (int) b.getMinY(), (int) b.getWidth(),
							(int) b.getHeight());
				}

				if (recordingShots) {
					rollingRecorder.recordFrame(currentFrame);

					List<Shot> removeKeys = new ArrayList<Shot>();
					for (Entry<Shot, ShotRecorder> r : shotRecorders.entrySet()) {
						if (r.getValue().isComplete()) {
							r.getValue().close();
							removeKeys.add(r.getKey());
						} else {
							r.getValue().recordFrame(currentFrame);
						}
					}

					for (Shot s : removeKeys)
						shotRecorders.remove(s);
				}

				if (recordingStream) {
					BufferedImage image = ConverterFactory.convertToType(currentFrame, BufferedImage.TYPE_3BYTE_BGR);
					IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

					IVideoPicture frame = converter.toPicture(image,
							(System.currentTimeMillis() - recordingStartTime) * 1000);
					frame.setKeyFrame(isFirstStreamFrame);
					frame.setQuality(0);
					isFirstStreamFrame = false;

					videoWriterStream.encodeVideo(0, frame);
				}

				if (cropFeedToProjection && projectionBounds.isPresent()) {
					canvasManager.updateBackground(currentFrame, projectionBounds);
				} else {
					canvasManager.updateBackground(currentFrame, Optional.empty());
				}

			}
		}

		private int lumsMaAcrossFrame = 0;

		private BufferedImage processFrame(BufferedImage currentFrame) {
			frameCount++;

			if (autoCalibrationEnabled) {
				acm.processFrame(currentFrame);
				return currentFrame;
			}

			Mat matFrame = Camera.bufferedImageToMat(currentFrame);
			Imgproc.cvtColor(matFrame, matFrame, Imgproc.COLOR_BGR2HSV);

			Mat mask = null;

			if (cameraAutoCalibrated && projectionBounds.isPresent()) {
				matFrame = acm.undistortFrame(matFrame);

				Mat submatFrame = matFrame.submat((int) projectionBounds.get().getMinY(),
						(int) projectionBounds.get().getMaxY(), (int) projectionBounds.get().getMinX(),
						(int) projectionBounds.get().getMaxX());

				// curFrameMask = new
				// Mat((int)projectionBounds.get().getHeight(),
				// (int)projectionBounds.get().getWidth(), CvType.CV_8UC1);

				if (recordingCalibratedArea) {
					Imgproc.cvtColor(submatFrame, submatFrame, Imgproc.COLOR_HSV2BGR);
					BufferedImage image = ConverterFactory.convertToType(Camera.matToBufferedImage(submatFrame),
							BufferedImage.TYPE_3BYTE_BGR);
					IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

					IVideoPicture frame = converter.toPicture(image,
							(System.currentTimeMillis() - recordingCalibratedAreaStartTime) * 1000);
					frame.setKeyFrame(isFirstCalibratedAreaFrame);
					frame.setQuality(0);
					isFirstCalibratedAreaFrame = false;

					videoWriterCalibratedArea.encodeVideo(0, frame);
					Imgproc.cvtColor(submatFrame, submatFrame, Imgproc.COLOR_BGR2HSV);
				}

				// logger.debug("processFrame time {}",
				// getCurrentFrameTimestamp());

				mask = arenaMaskManager.getMask();

				long lumsCurrentAcrossFrame = 0;
				for (int y = 0; y < submatFrame.rows(); y++) {
					for (int x = 0; x < submatFrame.cols(); x++) {
						byte[] px = { 0, 0, 0 };
						submatFrame.get(y, x, px);
						int matS = px[1] & 0xFF;
						int matV = px[2] & 0xFF;

						int curLum = ((255 - matS) * matV);

						lumsCurrentAcrossFrame += curLum;

						// byte diff = (byte) (mask.get(y,x)[0]/(255-matS));
						// px[2] = (byte) (px[2] - diff);

						// if (x==50&&y==50)
						// logger.debug("{} {} - {} {} - {}", matS, matV,
						// mask.get(y,x)[0], mask.get(y,x)[0]/(255-matS), diff);

						/*
						 * if (curLum > mask.get(y,x)[0]-5000) { px[2] = (byte)
						 * (.8*px[2]); }
						 */
						// submatFrame.put(y,x,px);
					}
				}

				lumsCurrentAcrossFrame /= submatFrame.rows() * submatFrame.cols();
				lumsMaAcrossFrame = ((lumsMaAcrossFrame * 4) + (int) lumsCurrentAcrossFrame) / 5;

				arenaMaskManager.updateAvgLums(lumsMaAcrossFrame, getCurrentFrameTimestamp());

				//logger.warn("{}", mask.get(200, 200)[0]);

				if (debuggerListener.isPresent()) {
					debuggerListener.get().updateDebugView(Camera.matToBufferedImage(submatFrame));
				}

			}

			shotDetectionManager.processFrame(matFrame, mask, isDetecting);

			Imgproc.cvtColor(matFrame, matFrame, Imgproc.COLOR_HSV2BGR);

			return Camera.matToBufferedImage(matFrame);

		}

		private void estimateCameraFPS() {
			if (lastCameraTimestamp > -1) {

				double estimateFPS = ((double) getFrameCount() - (double) lastFrameCount)
						/ (((double) System.currentTimeMillis() - (double) lastCameraTimestamp) / 1000.0);

				setFPS(estimateFPS);
				logger.trace("fps comparison estimate {} reported {}", estimateFPS, webcam.get().getFPS());
			}
			lastCameraTimestamp = System.currentTimeMillis();
			lastFrameCount = getFrameCount();

			if (debuggerListener.isPresent()) {
				debuggerListener.get().updateFeedData(getFPS(), Optional.empty());
			}
			checkIfMinimumFPS();
		}

		private void setFPS(double newFPS) {
			if (newFPS < 1.0) {
				logger.debug("New FPS read from webcam is very low: {}", newFPS);
			}

			// This just tells us if it's the first FPS estimate
			if (getFrameCount() > DEFAULT_FPS)
				webcamFPS = ((webcamFPS * 4.0) + newFPS) / 5.0;
			else
				webcamFPS = newFPS;
			deduplicationProcessor.setThresholdUsingFPS(getFPS());

		}

		private void checkIfMinimumFPS() {
			if (getFPS() < MIN_SHOT_DETECTION_FPS && !showedFPSWarning) {
				logger.warn("[{}] Current webcam FPS is {}, which is too low for reliable shot detection",
						webcam.get().getName(), getFPS());
				showFPSWarning(getFPS());
				showedFPSWarning = true;
			}
		}

		private void showMissingCameraError() {
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
				if (controller != null) cameraAlert.initOwner(controller.getStage());
				cameraAlert.show();
			});
		}

		private void showFPSWarning(double fps) {
			Platform.runLater(() -> {
				Alert cameraAlert = new Alert(AlertType.WARNING);

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
				if (controller != null) cameraAlert.initOwner(controller.getStage());
				cameraAlert.show();
			});
		}
	}

	private Label brightnessDiagnosticWarning = null;

	public void showBrightnessWarning() {
		if (!TimerPool.isWaiting(brightnessDiagnosticFuture)) {
			Platform.runLater(() -> {
				brightnessDiagnosticWarning = canvasManager.addDiagnosticMessage("Warning: Excessive brightness",
						Color.RED);
			});
		} else {
			// Stop the existing timer and start a new one
			TimerPool.cancelTimer(brightnessDiagnosticFuture);
		}
		brightnessDiagnosticFuture = TimerPool.schedule(() -> {
			Platform.runLater(() -> {
				if (brightnessDiagnosticWarning != null) {
					canvasManager.removeDiagnosticMessage(brightnessDiagnosticWarning);
					brightnessDiagnosticWarning = null;
				}
			});
		} , DIAGNOSTIC_MESSAGE_DURATION);

		if (!webcam.isPresent() || shownBrightnessWarning) return;
		shownBrightnessWarning = true;
		Platform.runLater(() -> {
			Alert brightnessAlert = new Alert(AlertType.WARNING);

			Optional<String> cameraName = config.getWebcamsUserName(webcam.get());
			String messageFormat = "The camera %s is streaming frames that are very bright. "
					+ " This will increase the odds of shots falsely being detected."
					+ " For best results, please do any mix of the following:\n\n"
					+ "-Turn off auto white balance and auto focus on your webcam and reduce the brightness\n"
					+ "-Remove any bright light sources in the camera's view\n"
					+ "-Turn down your projector's brightness and contrast";
			String message;
			if (cameraName.isPresent()) {
				message = String.format(messageFormat, cameraName.get());
			} else {
				message = String.format(messageFormat, webcam.get().getName());
			}

			brightnessAlert.setTitle("Conditions Very Bright");
			brightnessAlert.setHeaderText("Webcam detected very bright conditions!");
			brightnessAlert.setResizable(true);
			brightnessAlert.setContentText(message);
			if (controller != null) brightnessAlert.initOwner(controller.getStage());
			brightnessAlert.show();
		});
	}

	private Label motionDiagnosticWarning = null;

	public void showMotionWarning() {
		if (!TimerPool.isWaiting(motionDiagnosticFuture)) {
			Platform.runLater(() -> {
				motionDiagnosticWarning = canvasManager.addDiagnosticMessage("Warning: Excessive motion", Color.RED);
			});
		} else {
			// Stop the existing timer and start a new one
			TimerPool.cancelTimer(motionDiagnosticFuture);
		}
		motionDiagnosticFuture = TimerPool.schedule(() -> {
			Platform.runLater(() -> {
				if (motionDiagnosticWarning != null) {
					canvasManager.removeDiagnosticMessage(motionDiagnosticWarning);
					motionDiagnosticWarning = null;
				}
			});
		} , DIAGNOSTIC_MESSAGE_DURATION);
	}

	private long frameDelay;

	private void fireAutoCalibration() {
		acm.reset();
		acm.setCallback(new Callback<Void, Void>() {

			@Override
			public Void call(Void param) {
				autoCalibrateSuccess(acm.getBoundsResult(), acm.getFrameDelayResult());
				return null;
			}
		});
	}

	protected void autoCalibrateSuccess(Bounds bounds, long delay) {
		if (autoCalibrationEnabled && controller != null) {
			autoCalibrationEnabled = false;

			logger.debug("autoCalibrateSuccess {} {} {} {}", (int) bounds.getMinX(), (int) bounds.getMinY(),
					(int) bounds.getWidth(), (int) bounds.getHeight());

			cameraAutoCalibrated = true;

			Platform.runLater(() -> {

				controller.calibrate(bounds, false);

			});

			controller.setArenaMaskManager(arenaMaskManager);

			arenaMaskManager.start((int) bounds.getWidth(), (int) bounds.getHeight());

			// if (!recordingStream)
			// startRecordingStream(new File("fullFrameStream.mp4"));
			if (recordCalibratedArea && !recordingCalibratedArea)
				startRecordingCalibratedArea(new File("calibratedArea.mp4"), (int) bounds.getWidth(),
						(int) bounds.getHeight());

		}

	}

	public void enableAutoCalibration(boolean calculateFrameDelay) {
		acm = new AutoCalibrationManager(this, calculateFrameDelay);
		autoCalibrationEnabled = true;
		cameraAutoCalibrated = false;
		fireAutoCalibration();
	}

	public void disableAutoCalibration() {
		autoCalibrationEnabled = false;
	}

	public void setArenaBackground(String resourceFilename) {
		if (controller == null) {
			logger.error("setArenaBackground called when controller is null");
			return;
		}

		controller.setArenaBackground(resourceFilename);
	}

}