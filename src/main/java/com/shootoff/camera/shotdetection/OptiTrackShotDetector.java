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

package com.shootoff.camera.shotdetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.cameratypes.Camera.CameraState;
import com.shootoff.camera.cameratypes.OptiTrackCamera;
import com.shootoff.config.Configuration;
import javafx.scene.paint.Color;

public class OptiTrackShotDetector extends ShotYieldingShotDetector implements CameraStateListener {
	private final CameraManager cameraManager;
	private final Configuration config;

	private static final Logger logger = LoggerFactory.getLogger(OptiTrackShotDetector.class);

	public OptiTrackShotDetector(final CameraManager cameraManager, final Configuration config,
			final CameraView cameraView) {
		super(cameraManager, config, cameraView);

		this.cameraManager = cameraManager;
		this.config = config;

		cameraManager.registerCameraStateListener(this);
	}

	public static boolean isSystemSupported() {
		return OptiTrackCamera.initialized();
	}

	public void cameraStateChange(CameraState state) {
		logger.debug("got state change {}", state);
		switch (state) {
		case DETECTING:
			enableDetection();
			break;
		default:
			disableDetection();
			break;

		}
	}

	private native void startDetectionModeNative();

	private native void enableDetection();

	private native void disableDetection();

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
		enableDetection();
	}
	
	@Override
	protected boolean handlesBounds() {
		return false;
	}
}
