package com.shootoff.camera;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;

import javafx.geometry.Bounds;

/**
 * This interface is implemented by classes that act as the entry point to some
 * implementation of shot detection.
 */
public abstract class ShotDetector {
	private static final Logger logger = LoggerFactory.getLogger(ShotDetector.class);

	private final CameraManager cameraManager;
	private final Configuration config;
	private final CameraView cameraView;

	public ShotDetector(final CameraManager cameraManager, final Configuration config, final CameraView cameraView) {
		this.cameraManager = cameraManager;
		this.config = config;
		this.cameraView = cameraView;
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
	 * tied to of a new shot. This method ensures the shot is not a duplicate
	 * and not an ignored color before passing the shot along.
	 * 
	 * @param shot
	 *            a new shot we want to preprocess before passing along for
	 * @return <code>true</code> if the shot wasn't rejected during
	 *         preprocessing
	 */
	public boolean addShot(Shot shot) {
		if (!cameraManager.getDeduplicationProcessor().processShot(shot)) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot Rejected By {}",
					cameraManager.getDeduplicationProcessor().getClass().getName());
			return false;
		}
		if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent()
				&& shot.getColor().equals(config.getIgnoreLaserColor().get())) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot rejected by ignoreLaserColor {}",
					config.getIgnoreLaserColor().get());
			return false;
		}

		if (logger.isInfoEnabled()) logger.info("Suspected shot accepted: Center ({}, {}), cl {} fr {}", shot.getX(),
				shot.getY(), shot.getColor(), cameraManager.getFrameCount());

		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {

			final Bounds b = cameraManager.getProjectionBounds().get();

			cameraView.addShot(shot.getColor(), shot.getX() + b.getMinX(), shot.getY() + b.getMinY());
		} else {
			cameraView.addShot(shot.getColor(), shot.getX(), shot.getY());
		}
		
		return true;
	}
}
