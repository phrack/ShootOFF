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
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.Bounds;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.arenamask.ArenaMaskManager;
import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.util.TimerPool;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/**
 * This class is responsible for fetching frames from its assigned camera and
 * preprocessing them for shot detection. It also ensures the view showing the
 * camera frames is aware of any new frames from the camera.
 * 
 * @author phrack and dmaul
 */
public class CameraManager {
	protected static final Logger logger = LoggerFactory.getLogger(CameraManager.class);
	public static final int DEFAULT_FEED_WIDTH = 640;
	public static final int DEFAULT_FEED_HEIGHT = 480;

	protected int feedWidth = DEFAULT_FEED_WIDTH;
	protected int feedHeight = DEFAULT_FEED_HEIGHT;

	public static final int MIN_SHOT_DETECTION_FPS = 5;
	public static final int DEFAULT_FPS = 30;

	protected final static int DIAGNOSTIC_MESSAGE_DURATION = 1000; // ms

	private long lastCameraTimestamp = -1;
	private long lastFrameCount = 0;

	protected final ShotDetectionManager shotDetectionManager;

	protected final Optional<Camera> webcam;
	private final Optional<CameraErrorView> cameraErrorView;

	protected final CameraView cameraView;
	protected final Configuration config;
	protected Optional<Bounds> projectionBounds = Optional.empty();

	private final AtomicBoolean isStreaming = new AtomicBoolean(true);
	private final AtomicBoolean isDetecting = new AtomicBoolean(true);
	private final AtomicBoolean isCalibrating = new AtomicBoolean(false);
	private boolean shownBrightnessWarning = false;
	private boolean cropFeedToProjection = false;
	private boolean limitDetectProjection = false;

	protected Optional<Integer> minimumShotDimension = Optional.empty();

	protected Optional<CameraDebuggerListener> debuggerListener = Optional.empty();

	protected boolean recordingStream = false;
	protected boolean isFirstStreamFrame = true;
	protected IMediaWriter videoWriterStream;
	protected long recordingStartTime;

	protected boolean recordingShots = false;
	protected RollingRecorder rollingRecorder;
	protected Map<Shot, ShotRecorder> shotRecorders = new ConcurrentHashMap<Shot, ShotRecorder>();

	protected boolean[][] sectorStatuses;

	protected int frameCount = 0;
	protected long currentFrameTimestamp = -1;

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	private double webcamFPS = DEFAULT_FPS;
	private boolean showedFPSWarning = false;

	private AutoCalibrationManager acm = null;
	private final AtomicBoolean isAutoCalibrating = new AtomicBoolean(false);
	protected boolean cameraAutoCalibrated = false;

	protected final DeduplicationProcessor deduplicationProcessor = new DeduplicationProcessor(this);
	protected final ArenaMaskManager arenaMaskManager;

	private CameraCalibrationListener cameraCalibrationListener;

	public void setCalibrationManager(CameraCalibrationListener calibrationManager) {
		this.cameraCalibrationListener = calibrationManager;
	}

	public DeduplicationProcessor getDeduplicationProcessor() {
		return deduplicationProcessor;
	}

	public CameraManager(Camera webcam, CameraErrorView cameraErrorView, CameraView view, Configuration config) {
		if (Configuration.USE_ARENA_MASK) {
			arenaMaskManager = new ArenaMaskManager();
		} else {
			arenaMaskManager = null;
		}

		this.webcam = Optional.of(webcam);
		this.cameraErrorView = Optional.ofNullable(cameraErrorView);
		this.cameraView = view;
		this.config = config;

		this.cameraView.setCameraManager(this);

		initDetector(new Detector());

		this.shotDetectionManager = new ShotDetectionManager(this, config, view);
	}

	protected CameraManager(CameraView view, Configuration config) {
		if (Configuration.USE_ARENA_MASK) {
			arenaMaskManager = new ArenaMaskManager();
		} else {
			arenaMaskManager = null;
		}

		this.webcam = Optional.empty();
		this.cameraErrorView = Optional.empty();
		this.cameraView = view;
		this.config = config;
		this.shotDetectionManager = new ShotDetectionManager(this, config, view);
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
		if (sectorStatuses == null) return;

		this.sectorStatuses = new boolean[sectorStatuses.length][sectorStatuses[0].length];

		for (int i = 0; i < sectorStatuses.length; i++) {
			System.arraycopy(sectorStatuses[i], 0, this.sectorStatuses[i], 0, sectorStatuses[i].length);
		}
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
		cameraView.clearShots();
	}

	public void reset() {
		cameraView.reset();
	}

	public void close() {
		if (arenaMaskManager != null) arenaMaskManager.isStreaming.set(false);

		getCameraView().close();
		setDetecting(false);
		setStreaming(false);
		if (webcam.isPresent()) webcam.get().close();
		if (recordingStream) stopRecordingStream();
		TimerPool.cancelTimer(brightnessDiagnosticFuture);
		TimerPool.cancelTimer(motionDiagnosticFuture);

		if (recordingCalibratedArea) stopRecordingCalibratedArea();
	}

	public void setStreaming(boolean isStreaming) {
		this.isStreaming.set(isStreaming);
	}

	public void setDetecting(boolean isDetecting) {
		// Lock this to false during calibration
		if (isCalibrating.get() && isDetecting) {
			logger.info("Not changing detection to true during calibration");
			return;
		}

		if (logger.isTraceEnabled()) logger.trace("setDetecting was {} now {}", this.isDetecting, isDetecting);

		this.isDetecting.set(isDetecting);
	}

	public void setCalibrating(final boolean isCalibrating) {
		this.isCalibrating.set(isCalibrating);
		if (isCalibrating) setDetecting(false);
	}

	public boolean isDetecting() {
		return isDetecting.get();
	}

	public void setProjectionBounds(final Bounds projectionBounds) {
		this.projectionBounds = Optional.ofNullable(projectionBounds);
	}

	public void setCropFeedToProjection(final boolean cropFeed) {
		cropFeedToProjection = cropFeed;
	}

	public void setLimitDetectProjection(final boolean limitDetection) {
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
		if (logger.isDebugEnabled()) logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
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

	public void notifyShot(final Shot shot) {
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

			if (!sessionVideoFolder.exists() && !sessionVideoFolder.mkdirs()) {
				logger.error("Could not create video folder for session: {}", sessionVideoFolder.getAbsolutePath());
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

	public CameraView getCameraView() {
		return cameraView;
	}

	public void setMinimumShotDimension(int minDim) {
		minimumShotDimension = Optional.of(minDim);
		logger.debug("Set the minimum dimension for shots to: {}", minDim);
	}

	public Optional<Integer> getMinimumShotDimension() {
		return minimumShotDimension;
	}

	public void setThresholdListener(CameraDebuggerListener thresholdListener) {
		this.debuggerListener = Optional.ofNullable(thresholdListener);
	}

	public Optional<CameraDebuggerListener> getDebuggerListener() {
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

	protected class Detector extends MediaListenerAdapter implements Runnable {
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
	}

	private void streamCameraFrames() {
		while (isStreaming.get()) {
			if (!webcam.isPresent() || !webcam.get().isImageNew()) continue;

			BufferedImage currentFrame = webcam.get().getImage();
			currentFrameTimestamp = System.currentTimeMillis();

			if (currentFrame == null && webcam.isPresent() && !webcam.get().isOpen()) {
				// Camera appears to have closed
				if (cameraErrorView.isPresent()) cameraErrorView.get().showMissingCameraError(webcam.get());
				return;
			} else if (currentFrame == null && webcam.isPresent() && webcam.get().isOpen()) {
				// Camera appears to be open but got a null frame
				logger.warn("Null frame from camera: {}", webcam.get().getName());
				continue;
			}

			if ((int) (getFrameCount() % getFPS()) == 0) {
				estimateCameraFPS();
			}

			if (currentFrame == null) continue;
			currentFrame = processFrame(currentFrame);

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

			final BufferedImage frame = currentFrame;
			if (cropFeedToProjection && projectionBounds.isPresent()) {
				cameraView.updateBackground(frame, projectionBounds);
			} else {
				cameraView.updateBackground(frame, Optional.empty());
			}
		}
	}

	protected BufferedImage processFrame(BufferedImage currentFrame) {
		frameCount++;

		if (isAutoCalibrating.get()) {
			acm.processFrame(currentFrame);
			return currentFrame;
		}

		Mat matFrameBGR = Camera.bufferedImageToMat(currentFrame);
		final Mat matFrameHSV = new Mat();
		Mat submatFrameBGR = null;
		Mat submatFrameHSV = null;

		if (cameraAutoCalibrated && projectionBounds.isPresent()) {
			if (acm != null) {
				// MUST BE IN BGR pixel format.
				matFrameBGR = acm.undistortFrame(matFrameBGR);
			}

			submatFrameBGR = matFrameBGR.submat((int) projectionBounds.get().getMinY(),
					(int) projectionBounds.get().getMaxY(), (int) projectionBounds.get().getMinX(),
					(int) projectionBounds.get().getMaxX());
			

			if (recordingCalibratedArea) {
				BufferedImage image = ConverterFactory.convertToType(Camera.matToBufferedImage(submatFrameBGR),
						BufferedImage.TYPE_3BYTE_BGR);
				IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

				IVideoPicture frame = converter.toPicture(image,
						(System.currentTimeMillis() - recordingCalibratedAreaStartTime) * 1000);
				frame.setKeyFrame(isFirstCalibratedAreaFrame);
				frame.setQuality(0);
				isFirstCalibratedAreaFrame = false;

				videoWriterCalibratedArea.encodeVideo(0, frame);
			}

			Imgproc.cvtColor(matFrameBGR, matFrameHSV, Imgproc.COLOR_BGR2HSV);

			if (Configuration.USE_ARENA_MASK) arenaMaskManager.updateAvgLums(submatFrameBGR);

			if (debuggerListener.isPresent()) {
				debuggerListener.get().updateDebugView(Camera.matToBufferedImage(submatFrameBGR));
			}

		} else {
			Imgproc.cvtColor(matFrameBGR, matFrameHSV, Imgproc.COLOR_BGR2HSV);
		}
				
		if ((isLimitingDetectionToProjection() || isCroppingFeedToProjection())
				&& getProjectionBounds().isPresent()) {
			
			submatFrameHSV = matFrameHSV.submat((int) projectionBounds.get().getMinY(),
				(int) projectionBounds.get().getMaxY(), (int) projectionBounds.get().getMinX(),
				(int) projectionBounds.get().getMaxX());
			
			if (submatFrameBGR == null)
				submatFrameBGR = matFrameBGR.submat((int) projectionBounds.get().getMinY(),
						(int) projectionBounds.get().getMaxY(), (int) projectionBounds.get().getMinX(),
						(int) projectionBounds.get().getMaxX());
			
			shotDetectionManager.processFrame(submatFrameHSV, submatFrameBGR, isDetecting.get());
		}
		else
		{
			shotDetectionManager.processFrame(matFrameHSV, matFrameBGR, isDetecting.get());
		}

		// matFrameBGR is showing the colored pixels for brightness and motion,
		// hence why we need to return the converted version
		return Camera.matToBufferedImage(matFrameBGR);
	}

	private void estimateCameraFPS() {
		if (lastCameraTimestamp > -1) {

			double estimateFPS = ((double) getFrameCount() - (double) lastFrameCount)
					/ (((double) System.currentTimeMillis() - (double) lastCameraTimestamp) / 1000.0);

			setFPS(estimateFPS);
			if (logger.isTraceEnabled())
				logger.trace("fps comparison estimate {} reported {}", estimateFPS, webcam.get().getFPS());
		}

		lastCameraTimestamp = System.currentTimeMillis();
		lastFrameCount = getFrameCount();

		if (debuggerListener.isPresent()) {
			debuggerListener.get().updateFeedData(getFPS());
		}

		checkIfMinimumFPS();
	}

	protected void setFPS(double newFPS) {
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
			if (cameraErrorView.isPresent() && webcam.isPresent())
				cameraErrorView.get().showFPSWarning(webcam.get(), getFPS());
			showedFPSWarning = true;
		}
	}

	private Label brightnessDiagnosticWarning = null;

	public void showBrightnessWarning() {
		if (!TimerPool.isWaiting(brightnessDiagnosticFuture)) {
			brightnessDiagnosticWarning = cameraView.addDiagnosticMessage("Warning: Excessive brightness", Color.RED);
		} else {
			// Stop the existing timer and start a new one
			TimerPool.cancelTimer(brightnessDiagnosticFuture);
		}
		brightnessDiagnosticFuture = TimerPool.schedule(() -> {
			if (brightnessDiagnosticWarning != null) {
				cameraView.removeDiagnosticMessage(brightnessDiagnosticWarning);
				brightnessDiagnosticWarning = null;
			}
		}, DIAGNOSTIC_MESSAGE_DURATION);

		if (webcam.isPresent() && !shownBrightnessWarning) {
			shownBrightnessWarning = true;
			if (cameraErrorView.isPresent()) cameraErrorView.get().showBrightnessWarning(webcam.get());
		}
	}

	private Label motionDiagnosticWarning = null;

	public void showMotionWarning() {
		if (!TimerPool.isWaiting(motionDiagnosticFuture)) {
			motionDiagnosticWarning = cameraView.addDiagnosticMessage("Warning: Excessive motion", Color.RED);
		} else {
			// Stop the existing timer and start a new one
			TimerPool.cancelTimer(motionDiagnosticFuture);
		}
		motionDiagnosticFuture = TimerPool.schedule(() -> {
			if (motionDiagnosticWarning != null) {
				cameraView.removeDiagnosticMessage(motionDiagnosticWarning);
				motionDiagnosticWarning = null;
			}
		}, DIAGNOSTIC_MESSAGE_DURATION);
	}

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
		if (isAutoCalibrating.get() && cameraCalibrationListener != null) {
			isAutoCalibrating.set(false);

			logger.debug("autoCalibrateSuccess {} {} {} {}", (int) bounds.getMinX(), (int) bounds.getMinY(),
					(int) bounds.getWidth(), (int) bounds.getHeight());

			cameraAutoCalibrated = true;
			cameraCalibrationListener.calibrate(bounds, false);

			if (Configuration.USE_ARENA_MASK) {
				cameraCalibrationListener.setArenaMaskManager(arenaMaskManager);

				shotDetectionManager.setArenaMaskManager(arenaMaskManager);
				arenaMaskManager.start((int) bounds.getWidth(), (int) bounds.getHeight());
			}

			if (recordCalibratedArea && !recordingCalibratedArea)
				startRecordingCalibratedArea(new File("calibratedArea.mp4"), (int) bounds.getWidth(),
						(int) bounds.getHeight());
		}
	}

	public void enableAutoCalibration(boolean calculateFrameDelay) {
		acm = new AutoCalibrationManager(this, calculateFrameDelay);
		isAutoCalibrating.set(true);
		cameraAutoCalibrated = false;
		// Turns off using mask
		shotDetectionManager.setArenaMaskManager(null);

		fireAutoCalibration();
	}

	public void disableAutoCalibration() {
		isAutoCalibrating.set(false);
	}

	public void setArenaBackground(String resourceFilename) {
		if (cameraCalibrationListener == null) {
			logger.error("setArenaBackground called when controller is null");
			return;
		}

		cameraCalibrationListener.setArenaBackground(resourceFilename);
	}
}