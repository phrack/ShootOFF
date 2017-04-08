package com.shootoff.camera.shotdetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.Shot;
import com.shootoff.camera.shot.BoundsShot;
import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.config.Configuration;
import javafx.geometry.Bounds;

/**
 * This interface is implemented by classes that act as the entry point to some
 * implementation of shot detection.
 */
public abstract class ShotDetector {
	private static final Logger logger = LoggerFactory.getLogger(ShotDetector.class);

	private final CameraManager cameraManager;
	private final Configuration config = Configuration.getConfig();
	private final CameraView cameraView;

	public static boolean isSystemSupported() {
		return false;
	}

	public ShotDetector(final CameraManager cameraManager, final CameraView cameraView) {
		this.cameraManager = cameraManager;
		this.cameraView = cameraView;
	}

	public void reset() {}

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
	 * @param timestamp
	 *            the timestamp of the shot not adjusted for the shot timer
	 * @param scaleShot
	 *            <code>true</code> if the shot needs to be scaled if the
	 *            display resolution differs from the webcam's resolution. This
	 *            is always <code>false</code> for click-to-shoot.
	 * @return <code>true</code> if the shot wasn't rejected during
	 *         preprocessing
	 */
	public boolean addShot(ShotColor color, double x, double y, long timestamp, boolean scaleShot) {
		if (!checkIgnoreColor(color)) return false;

		final Shot shot = new Shot(color, x, y, cameraManager.cameraTimeToShotTime(timestamp),
				cameraManager.getFrameCount());

		if (config.isAdjustingPOI())
		{
			if (logger.isTraceEnabled())
			{
				logger.trace("POI Adjustment: x {} y {}", config.getPOIAdjustmentX().get(), config.getPOIAdjustmentY().get());
				logger.trace("Adjusting offset via POI setting, coords were {} {} now {} {}", x, y, x+config.getPOIAdjustmentX().get(), y+config.getPOIAdjustmentY().get());
			}
			
			shot.adjustPOI(config.getPOIAdjustmentX().get(), config.getPOIAdjustmentY().get());

		}
		
		BoundsShot bShot = new BoundsShot(shot);

		if (scaleShot && (cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {
			final Bounds b = cameraManager.getProjectionBounds().get();

			if (handlesBounds()) {
				bShot.adjustBounds(b.getMinX(), b.getMinY());
			} else {
				if (cameraManager.isLimitingDetectionToProjection() && !b.contains(x, y)) return false;
			}
		}
		
		DisplayShot dShot = new DisplayShot(bShot, config.getMarkerRadius());
		

		// If the shot didn't come from click to shoot (cameFromCanvas) and the
		// resolution of the display and feed differ, translate shot coordinates
		if (scaleShot && (config.getDisplayWidth() != cameraManager.getFeedWidth()
				|| config.getDisplayHeight() != cameraManager.getFeedHeight())) {
			dShot.setDisplayVals(config.getDisplayWidth(), config.getDisplayHeight(), cameraManager.getFeedWidth(),
					cameraManager.getFeedHeight());
		}

		if (!checkDuplicate(dShot)) return false;

		submitShot(dShot);

		return true;
	}

	protected void submitShot(final DisplayShot shot) {
		if (logger.isInfoEnabled()) logger.info("Suspected shot accepted: Center ({}, {}), cl {} fr {}", shot.getX(),
				shot.getY(), shot.getColor(), cameraManager.getFrameCount());

		// Notify of new shot on a non-shot detection thread because most
		// training exercises do shot processing on whatever thread submits
		// the shot
		new Thread(() -> cameraView.addShot(shot, false), "Shot Notifier").start();
	}

	protected boolean checkDuplicate(final Shot shot) {
		if (!cameraManager.getDeduplicationProcessor().processShot(shot)) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot Rejected By {}",
					cameraManager.getDeduplicationProcessor().getClass().getName());
			return false;
		}
		return true;
	}

	protected boolean checkIgnoreColor(ShotColor color) {
		if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent()
				&& Shot.colorMap.get(color).equals(config.getIgnoreLaserColor().get())) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot rejected by ignoreLaserColor {}",
					config.getIgnoreLaserColor().get());
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @return True if this shot detector only returns shots in bounds and
	 *         offset within the bounds according to the settings in
	 *         CameraManager
	 */
	protected abstract boolean handlesBounds();
}
