package com.shootoff.camera;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

/**
 * This interface is implemented by classes that act as the entry point to some
 * implementation of shot detection.
 */
public abstract class ShotDetector {
	private static final Logger logger = LoggerFactory.getLogger(ShotDetector.class);

	private final CameraManager cameraManager;
	private final Configuration config;
	private final CameraView cameraView;

	private long startTime = 0;

	public ShotDetector(final CameraManager cameraManager, final Configuration config, final CameraView cameraView) {
		this.cameraManager = cameraManager;
		this.config = config;
		this.cameraView = cameraView;
	}

	public void reset() {
		startTime = System.currentTimeMillis();
		cameraManager.getDeduplicationProcessor().reset();
	}

	/**
	 * Process <code>frameBGR</code> to detect shots that appear in it. The
	 * frame is in blue, green, red format, which is the default used by OpenCV
	 * when it reads a frame off of a webcam. The behavior when
	 * <code>isDetecting</code> is <code>false</code> is dependent on the
	 * specific implementation of the shot detection algorithm. Some may perform
	 * no processing in this case, others may still update filters, collect
	 * diagnostic information (e.g. to show users where noise may occur), etc.
	 * 
	 * @param frameBGR
	 *            the frame to process in search of a shot
	 * @param isDetecting
	 *            <code>true</code> if the algorithm should perform the full
	 *            detection process, otherwise stop after collecting
	 *            diagnostic/filter information
	 */
	public abstract void processFrame(Mat frameBGR, boolean isDetecting);

	/**
	 * Notify the shot detector of the dimensions of webcam frames (e.g. the
	 * webcam's resolution). This method may be called at any time if the
	 * webcam's resolution is changed at runtime.
	 * 
	 * @param width
	 *            the width of frames in pixels
	 * @param height
	 *            the height of frames in pixels
	 */
	public abstract void setFrameSize(final int width, final int height);

	/**
	 * Alert the canvas tied to the camera the instantiation of this detector is
	 * tied to of a new shot. This method preprocesses the shot by ensuring it
	 * is not of an ignored color, it has the appropriate translation for
	 * projectors if it's a shot on the arena, it has the appropriate
	 * translation if the display resolution differs from the camera resolution,
	 * and it is not a duplicate shot.
	 * 
	 * @param color
	 *            the color of the detected shot (red or green)
	 * @param x
	 *            the exact x coordinate of the shot in the video frame it was
	 *            detected in
	 * @param y
	 *            the exact y coordinate of the shot in the video frame it was
	 *            detected in
	 * @param scaleShot
	 *            <code>true</code> if the shot needs to be scaled if the
	 *            display resolution differs from the webcam's resolution. This
	 *            is always <code>false</code> for click-to-shoot.
	 * @return <code>true</code> if the shot wasn't rejected during
	 *         preprocessing
	 */
	public boolean addShot(Color color, double x, double y, boolean scaleShot) {
		if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent()
				&& color.equals(config.getIgnoreLaserColor().get())) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot rejected by ignoreLaserColor {}",
					config.getIgnoreLaserColor().get());
			return false;
		}

		if (startTime == 0) startTime = cameraManager.getCurrentFrameTimestamp();

		final Shot shot;

		if (scaleShot && (cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {

			final Bounds b = cameraManager.getProjectionBounds().get();

			shot = new Shot(color, x + b.getMinX(), y + b.getMinY(), cameraManager.getCurrentFrameTimestamp() - startTime,
					cameraManager.getFrameCount(), config.getMarkerRadius());
		} else {
			shot = new Shot(color, x, y, cameraManager.getCurrentFrameTimestamp() - startTime, cameraManager.getFrameCount(),
					config.getMarkerRadius());
		}

		// If the shot didn't come from click to shoot (cameFromCanvas) and the
		// resolution of the display and feed differ, translate shot coordinates
		if (scaleShot && (config.getDisplayWidth() != cameraManager.getFeedWidth()
				|| config.getDisplayHeight() != cameraManager.getFeedHeight())) {
			shot.setTranslation(config.getDisplayWidth(), config.getDisplayHeight(), cameraManager.getFeedWidth(),
					cameraManager.getFeedHeight());
		}

		if (!cameraManager.getDeduplicationProcessor().processShot(shot)) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot Rejected By {}",
					cameraManager.getDeduplicationProcessor().getClass().getName());
			return false;
		}

		if (logger.isInfoEnabled()) logger.info("Suspected shot accepted: Center ({}, {}), cl {} fr {}", shot.getX(),
				shot.getY(), shot.getColor(), cameraManager.getFrameCount());

		cameraView.addShot(shot, false);

		return true;
	}
}
