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
import javafx.geometry.Dimension2D;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.ObservableCloseable;
import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.cameratypes.Camera.CameraState;
import com.shootoff.camera.cameratypes.CameraEventListener;
import com.shootoff.camera.cameratypes.PS3EyeCamera;
import com.shootoff.camera.cameratypes.SarxosCaptureCamera;
import com.shootoff.camera.processors.DeduplicationProcessor;
import com.shootoff.camera.recorders.RollingRecorder;
import com.shootoff.camera.recorders.ShotRecorder;
import com.shootoff.camera.shotdetection.CameraStateListener;
import com.shootoff.camera.shotdetection.FrameProcessingShotDetector;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.camera.shotdetection.ShotYieldingShotDetector;
import com.shootoff.config.Configuration;
import com.shootoff.util.TimerPool;
import com.xuggle.mediatool.IMediaWriter;
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

/**
 * This class is responsible for fetching frames from its assigned camera and
 * preprocessing them for shot detection. It also ensures the view showing the
 * camera frames is aware of any new frames from the camera.
 *
 * @author phrack and dmaul
 */
public class CameraManager implements ObservableCloseable, CameraEventListener, CameraCalibrationListener {
	private static final int MAXIMUM_CONSECUTIVE_CAMERA_ERRORS = 5;
	private static final Logger logger = LoggerFactory.getLogger(CameraManager.class);
	public static final int DEFAULT_FEED_WIDTH = 640;
	public static final int DEFAULT_FEED_HEIGHT = 480;
	public static final int MIN_SHOT_DETECTION_FPS = 5;

	protected int feedWidth = DEFAULT_FEED_WIDTH;
	protected int feedHeight = DEFAULT_FEED_HEIGHT;

	protected final static int DIAGNOSTIC_MESSAGE_DURATION = 1000; // ms

	protected Optional<CameraDebuggerListener> debuggerListener = Optional.empty();

	protected final ShotDetector shotDetector;
	private long startTime = 0;

	protected final Camera camera;
	private final Optional<CameraErrorView> cameraErrorView;
	private Optional<CloseListener> closeListener = Optional.empty();

	protected final CameraView cameraView;
	protected final Configuration config;
	private final Object projectionBoundsLock = new Object();
	protected Optional<Bounds> projectionBounds = Optional.empty();

	private final AtomicBoolean isStreaming = new AtomicBoolean(true);
	private final AtomicBoolean isDetectionLocked = new AtomicBoolean(false);
	private final AtomicBoolean isDetecting = new AtomicBoolean(true);
	private final AtomicBoolean isCalibrating = new AtomicBoolean(false);
	private boolean shownBrightnessWarning = false;
	private boolean cropFeedToProjection = false;
	private boolean limitDetectProjection = false;

	protected Optional<Integer> minimumShotDimension = Optional.empty();

	protected boolean recordingStream = false;
	protected boolean isFirstStreamFrame = true;
	protected IMediaWriter videoWriterStream;
	protected long recordingStartTime;

	protected boolean recordingShots = false;
	protected RollingRecorder rollingRecorder;
	protected Map<Shot, ShotRecorder> shotRecorders = new ConcurrentHashMap<Shot, ShotRecorder>();

	protected boolean[][] sectorStatuses;

	private boolean showedFPSWarning = false;

	protected AutoCalibrationManager acm = null;
	private final AtomicBoolean isAutoCalibrating = new AtomicBoolean(false);
	protected boolean cameraAutoCalibrated = false;

	protected final DeduplicationProcessor deduplicationProcessor = new DeduplicationProcessor(this);

	private CameraCalibrationListener cameraCalibrationListener;

	public void setCalibrationManager(CameraCalibrationListener calibrationManager) {
		this.cameraCalibrationListener = calibrationManager;
	}

	public DeduplicationProcessor getDeduplicationProcessor() {
		return deduplicationProcessor;
	}

	public CameraManager() {
		this.camera = null;
		this.cameraErrorView = Optional.empty();
		this.cameraView = null;
		this.config = null;
		this.shotDetector = null;
	}

	public CameraManager(Camera cameraInterface, CameraErrorView cameraErrorView, CameraView view,
			Configuration config) {

		this.camera = cameraInterface;

		this.cameraErrorView = Optional.ofNullable(cameraErrorView);
		this.cameraView = view;
		this.config = config;

		this.cameraView.setCameraManager(this);

		camera.setCameraEventListener(this);

		this.shotDetector = camera.getPreferredShotDetector(this, config, view);

		if (this.shotDetector == null)
			logger.error("No suitable shot detector found for camera {}", this.camera.getName());

	}

	public String getName() {
		return camera.getName();
	}

	public void start() {
		sectorStatuses = new boolean[JavaShotDetector.SECTOR_ROWS][JavaShotDetector.SECTOR_COLUMNS];

		// Turn on all shot sectors by default
		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < JavaShotDetector.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}

		synchronized (camera) {
			if (!camera.isOpen()) {
				camera.setViewSize(new Dimension(getFeedWidth(), getFeedHeight()));

				if (!camera.open()) {
					cameraErrorView.get().showCameraLockError(camera, true);
					return;
				}

				final Dimension openDimension = camera.getViewSize();

				if ((int) openDimension.getWidth() != getFeedWidth()
						|| (int) openDimension.getHeight() != getFeedHeight()) {
					if (openDimension.getWidth() == -1) {
						cameraErrorView.get().showCameraLockError(camera, true);
						return;
					}

					if (logger.isWarnEnabled()) {
						logger.warn(
								"Camera {} dimension differs from requested dimensions, requested {} {} actual {} {}",
								getName(), getFeedWidth(), getFeedHeight(), (int) openDimension.getWidth(),
								(int) openDimension.getHeight());
					}

					setFeedResolution((int) openDimension.getWidth(), (int) openDimension.getHeight());
					shotDetector.setFrameSize((int) openDimension.getWidth(), (int) openDimension.getHeight());
				} else {
					setFeedResolution((int) openDimension.getWidth(), (int) openDimension.getHeight());
				}
			}

			if (logger.isDebugEnabled()) logger.debug("starting camera thread {}", camera.getName());
			final String threadName = String.format("Shot detection %s %s", camera.getName(),
					shotDetector.getClass().getName());
			new Thread(camera, threadName).start();

		}

		if (shotDetector instanceof ShotYieldingShotDetector)
			((ShotYieldingShotDetector) shotDetector).startDetecting();

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

	// TODO: This doesn't handle potential side effects of modifying the feed
	// resolution on the fly.
	public void setFeedResolution(int width, int height) {
		feedWidth = width;
		feedHeight = height;
		shotDetector.setFrameSize(width, height);
	}

	// Used by click-to-shoot and tests to inject a shot via the shot detector
	public void injectShot(Color color, double x, double y, boolean scaleShot) {
		shotDetector.addShot(color, x, y, scaleShot);
	}

	public void clearShots() {
		cameraView.clearShots();
	}

	public void reset() {
		resetStartTime();
		shotDetector.reset();
		deduplicationProcessor.reset();
		cameraView.reset();
	}

	private void resetStartTime() {
		startTime = System.currentTimeMillis();
	}

	@Override
	public void setOnCloseListener(CloseListener closeListener) {
		this.closeListener = Optional.of(closeListener);
	}

	@Override
	public void close() {
		getCameraView().close();
		setDetecting(false);
		setStreaming(false);

		camera.setCameraEventListener(null);

		setCameraState(CameraState.CLOSED);

		if (recordingStream) stopRecordingStream();
		TimerPool.cancelTimer(brightnessDiagnosticFuture);
		TimerPool.cancelTimer(motionDiagnosticFuture);

		if (recordingCalibratedArea) stopRecordingCalibratedArea();

		if (closeListener.isPresent()) closeListener.get().closing();
	}

	public void setStreaming(boolean isStreaming) {
		this.isStreaming.set(isStreaming);
	}

	// Sometimes it is useful to ensure that a camera that isn't detecting
	// states not detecting (e.g. to not be turned back on by reset button
	public void setDetectionLockState(boolean isLocked) {
		isDetectionLocked.set(isLocked);
	}

	List<CameraStateListener> cameraStateListeners = new ArrayList<CameraStateListener>();

	public void registerCameraStateListener(CameraStateListener csl) {
		cameraStateListeners.add(csl);
	}

	private void setCameraState(CameraState state) {
		if (camera.setState(state)) for (CameraStateListener csl : cameraStateListeners)
			csl.cameraStateChange(state);
	}

	public void setDetecting(boolean isDetecting) {
		if (isDetectionLocked.get()) {
			logger.debug("Attempted to set detection for {} to {}, but the detection state is locked.", getName(),
					isDetecting);
			return;
		}

		// Lock this to false during calibration
		if (isCalibrating.get() && isDetecting) {
			logger.info("Not changing detection to true during calibration");
			return;
		}

		if (logger.isTraceEnabled())
			logger.trace("setDetecting was {} now {} for {}", this.isDetecting, isDetecting, getName());

		if (isDetecting == true)
			setCameraState(CameraState.DETECTING);
		else
			setCameraState(CameraState.NORMAL);
		this.isDetecting.set(isDetecting);
	}

	public void setCalibrating(final boolean isCalibrating) {

		this.isCalibrating.set(isCalibrating);
		if (isCalibrating) {
			setDetecting(false);
			setCameraState(CameraState.CALIBRATING);
		}
	}

	public boolean isDetecting() {
		return isDetecting.get();
	}

	public void setProjectionBounds(final Bounds projectionBounds) {
		synchronized (projectionBoundsLock) {
			this.projectionBounds = Optional.ofNullable(projectionBounds);
		}
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

		Optional<String> userCameraName = config.getWebcamsUserName(camera);

		if (userCameraName.isPresent()) {
			cameraName = userCameraName.get();
		} else {
			cameraName = camera.getName();
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

	public Camera getCamera() {
		return camera;
	}

	public Image getCurrentFrame() {
		return SwingFXUtils.toFXImage(camera.getBufferedImage(), null);
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

	private ScheduledFuture<?> brightnessDiagnosticFuture = null;
	private ScheduledFuture<?> motionDiagnosticFuture = null;

	private boolean recordCalibratedArea = false;
	private IMediaWriter videoWriterCalibratedArea;
	private long recordingCalibratedAreaStartTime;
	private boolean isFirstCalibratedAreaFrame;
	private boolean recordingCalibratedArea;

	public void startRecordingCalibratedArea(File videoFile, int width, int height) {
		if (logger.isDebugEnabled()) logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
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

	@Override
	public void newFrame(Mat frame) {
		if (!handleFrame(frame)) logger.warn("Invalid frame yielded from {}", camera.getName());
	}

	private int consecutiveCameraErrors = 0;

	private boolean handleFrame(Mat currentFrame) {
		boolean cameraError = false;

		if (currentFrame == null && !camera.isOpen()) {
			// Camera appears to have closed
			if (isStreaming.get() && cameraErrorView.isPresent()) cameraErrorView.get().showMissingCameraError(camera);

			cameraError = true;
		} else if (currentFrame == null && camera.isOpen()) {
			// Camera appears to be open but got a null frame
			logger.warn("Null frame from camera: {}", camera.getName());
			cameraError = true;
		} else if ((currentFrame.size().height != feedHeight || currentFrame.size().width != feedWidth)
				&& camera.isOpen()) {
			// Camera appears to be open but got an invalid size frame
			logger.warn("Invalid frame size from camera: {} gave {} expecting {},{}", camera.getName(),
					currentFrame.size(), feedWidth, feedHeight);
			cameraError = true;
		}

		if (cameraError) {
			consecutiveCameraErrors++;
			if (consecutiveCameraErrors > MAXIMUM_CONSECUTIVE_CAMERA_ERRORS) {
				if (isStreaming.get() && cameraErrorView.isPresent())
					cameraErrorView.get().showMissingCameraError(camera);

				camera.close();
			}
			return false;
		} else {
			consecutiveCameraErrors = 0;
		}

		BufferedImage currentImage = processFrame(currentFrame);

		Bounds b;

		synchronized (projectionBoundsLock) {
			if (projectionBounds.isPresent()) {
				b = projectionBounds.get();
			} else {
				b = null;
			}
		}

		if (cropFeedToProjection && b != null) {
			currentImage = currentImage.getSubimage((int) b.getMinX(), (int) b.getMinY(), (int) b.getWidth(),
					(int) b.getHeight());
		}

		if (recordingShots) {
			rollingRecorder.recordFrame(currentImage);

			List<Shot> removeKeys = new ArrayList<Shot>();
			for (Entry<Shot, ShotRecorder> r : shotRecorders.entrySet()) {
				if (r.getValue().isComplete()) {
					r.getValue().close();
					removeKeys.add(r.getKey());
				} else {
					r.getValue().recordFrame(currentImage);
				}
			}

			for (Shot s : removeKeys)
				shotRecorders.remove(s);
		}

		if (recordingStream) {
			BufferedImage image = ConverterFactory.convertToType(currentImage, BufferedImage.TYPE_3BYTE_BGR);
			IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

			IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - recordingStartTime) * 1000);
			frame.setKeyFrame(isFirstStreamFrame);
			frame.setQuality(0);
			isFirstStreamFrame = false;

			videoWriterStream.encodeVideo(0, frame);
		}

		final BufferedImage frame = currentImage;
		if (cropFeedToProjection && projectionBounds.isPresent()) {
			cameraView.updateBackground(frame, projectionBounds);
		} else {
			cameraView.updateBackground(frame, Optional.empty());
		}

		return true;
	}

	protected BufferedImage processFrame(Mat currentFrame) {
		if (isAutoCalibrating.get()) {
			acm.processFrame(currentFrame, camera.getCurrentFrameTimestamp());
			return Camera.matToBufferedImage(currentFrame);
		}

		Mat submatFrameBGR = null;

		Bounds projectionBounds;

		synchronized (projectionBoundsLock) {
			if (this.projectionBounds.isPresent()) {
				projectionBounds = this.projectionBounds.get();
			} else {
				projectionBounds = null;
			}
		}

		if (cameraAutoCalibrated && projectionBounds != null) {
			if (acm != null) {
				// MUST BE IN BGR pixel format.
				currentFrame = acm.undistortFrame(currentFrame);
			}

			submatFrameBGR = currentFrame.submat((int) projectionBounds.getMinY(), (int) projectionBounds.getMaxY(),
					(int) projectionBounds.getMinX(), (int) projectionBounds.getMaxX());

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

			if (debuggerListener.isPresent()) {
				debuggerListener.get().updateDebugView(Camera.matToBufferedImage(submatFrameBGR));
			}
		}

		if ((isLimitingDetectionToProjection() || isCroppingFeedToProjection()) && projectionBounds != null) {
			if (submatFrameBGR == null)
				submatFrameBGR = currentFrame.submat((int) projectionBounds.getMinY(), (int) projectionBounds.getMaxY(),
						(int) projectionBounds.getMinX(), (int) projectionBounds.getMaxX());

			if (shotDetector instanceof FrameProcessingShotDetector)
				((FrameProcessingShotDetector) shotDetector).processFrame(submatFrameBGR, isDetecting.get());
		} else {
			if (shotDetector instanceof FrameProcessingShotDetector)
				((FrameProcessingShotDetector) shotDetector).processFrame(currentFrame, isDetecting.get());
		}

		// matFrameBGR is showing the colored pixels for brightness and motion,
		// hence why we need to return the converted version
		return Camera.matToBufferedImage(currentFrame);
	}

	private void checkIfMinimumFPS(double cameraFPS) {
		if (cameraFPS < MIN_SHOT_DETECTION_FPS && !showedFPSWarning) {
			logger.warn("[{}] Current webcam FPS is {}, which is too low for reliable shot detection", camera.getName(),
					getFPS());
			if (cameraErrorView.isPresent()) cameraErrorView.get().showFPSWarning(camera, getFPS());
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

		if (!shownBrightnessWarning) {
			shownBrightnessWarning = true;
			if (cameraErrorView.isPresent()) cameraErrorView.get().showBrightnessWarning(camera);
		}
	}

	private Label motionDiagnosticWarning = null;

	public void showMotionWarning() {
		if (!TimerPool.isWaiting(motionDiagnosticFuture)) {
			motionDiagnosticWarning = cameraView.addDiagnosticMessage(
					"Warning: Excessive motion -- Try reducing the camera exposure setting", Color.RED);
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
	}

	protected void autoCalibrateSuccess(Bounds arenaBounds, Optional<Dimension2D> paperDims, long delay) {
		if (isAutoCalibrating.get() && cameraCalibrationListener != null) {
			isAutoCalibrating.set(false);

			logger.debug("autoCalibrateSuccess {} {} {} {} paper {}", (int) arenaBounds.getMinX(),
					(int) arenaBounds.getMinY(), (int) arenaBounds.getWidth(), (int) arenaBounds.getHeight(),
					paperDims.isPresent());

			cameraAutoCalibrated = true;
			cameraCalibrationListener.calibrate(arenaBounds, paperDims, false, delay);

			if (recordCalibratedArea && !recordingCalibratedArea)
				startRecordingCalibratedArea(new File("calibratedArea.mp4"), (int) arenaBounds.getWidth(),
						(int) arenaBounds.getHeight());
		}
	}

	public void enableAutoCalibration(boolean calculateFrameDelay) {
		if (acm == null) acm = new AutoCalibrationManager(this, camera, calculateFrameDelay);
		isAutoCalibrating.set(true);
		cameraAutoCalibrated = false;

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

	public void launchCameraSettings() {
		if (camera instanceof SarxosCaptureCamera) {
			((SarxosCaptureCamera) camera).launchCameraSettings();
		} else if (camera instanceof PS3EyeCamera) {
			((PS3EyeCamera) camera).launchCameraSettings();
		}
	}

	public double getFPS() {
		return camera.getFPS();
	}

	public int getFrameCount() {
		return camera.getFrameCount();
	}

	public long getCurrentFrameTimestamp() {
		if (startTime == 0) resetStartTime();

		return System.currentTimeMillis() - startTime;
	}

	@Override
	public void newFPS(double cameraFPS) {
		if (debuggerListener.isPresent()) debuggerListener.get().updateFeedData(cameraFPS);

		checkIfMinimumFPS(cameraFPS);
	}

	@Override
	public void cameraClosed() {
		setCameraState(CameraState.CLOSED);
		close();
	}

	@Override
	public void calibrate(Bounds arenaBounds, Optional<Dimension2D> perspectivePaperDims, boolean calibratedFromCanvas,
			long delay) {
		autoCalibrateSuccess(arenaBounds, perspectivePaperDims, delay);
	}

}