package com.shootoff.camera.shotdetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.cameratypes.OptiTrackCamera;
import com.shootoff.config.Configuration;
import javafx.scene.paint.Color;

public class OptiTrackShotDetector extends ShotYieldingShotDetector {
	private final CameraManager cameraManager;
	private final Configuration config;

	private static final Logger logger = LoggerFactory.getLogger(OptiTrackShotDetector.class);
	

	public OptiTrackShotDetector(final CameraManager cameraManager, final Configuration config,
			final CameraView cameraView) {
		super(cameraManager, config, cameraView);

		this.cameraManager = cameraManager;
		this.config = config;
		
		logger.warn("using optitrack shot detector");
	}
	
	public static boolean isSystemSupported() {
		return OptiTrackCamera.initialized();
	}

	private native void startDetectionModeNative();
	private native void launchDetection();
	
	@Override
	public void setFrameSize(int width, int height) {
		// TODO: Should this be a noop for optitrack shot detection?
	}

	/**
	 * Called by the native code to notify this class when a shot is detected.
	 * 
	 * @param x
	 *            the x coordinate of the new shot
	 * @param y
	 *            the y coordinate of the new shot
	 * @param rgb
	 *            the rgb color of the new shot
	 */
	public void foundShot(int x, int y, int rgb) {
		if (!cameraManager.isDetecting())
			return;
		
		Color c = Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb >> 8) & 0xFF, 1.0);

		super.addShot(c, x, y, true);
	}

	@Override
	public void startDetecting() {
		logger.debug("start");	
		startDetectionModeNative();
		new Thread(() -> launchDetection()).start();
	}

	@Override
	public void stopDetecting() {
		// TODO Auto-generated method stub
		
	}
}
