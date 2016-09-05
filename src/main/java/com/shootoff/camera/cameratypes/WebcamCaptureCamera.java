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

package com.shootoff.camera.cameratypes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import com.github.sarxos.webcam.Webcam;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.NativeShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;

public class WebcamCaptureCamera extends CalculatedFPSCamera {
	
	public static final int CV_CAP_PROP_EXPOSURE = 15;
	
	private int cameraIndex = -1;
	private final VideoCapture camera;

	// For testing
	protected WebcamCaptureCamera() {
		camera = null;
	}

	public WebcamCaptureCamera(final String cameraName) {
		final List<Webcam> webcams = Webcam.getWebcams();
		int cameraIndex = -1;

		for (int i = 0; i < webcams.size(); i++) {
			if (webcams.get(i).getName().equals(cameraName)) {
				cameraIndex = i;
				break;
			}
		}

		if (cameraIndex < 0) throw new IllegalArgumentException("Camera not found: " + cameraName);

		camera = new VideoCapture();
		this.cameraIndex = cameraIndex;

	}


	@Override
	public Mat getMatFrame() {
		final Mat frame = new Mat();
		try {
			if (!isOpen() || !camera.read(frame) || frame.size().height == 0) return null;
		} catch (Exception e)
		{
			// Sometimes there is a race condition on closing the camera vs. read()
			return null;
		}

		frameCount++;
		currentFrameTimestamp = System.currentTimeMillis();
		return frame;
	}


	@Override
	public BufferedImage getBufferedImage() {		
		Mat frame = getMatFrame();

		if (frame == null) {
			return null;
		} else {
			return Camera.matToBufferedImage(getMatFrame());
		}
	}


	
	@Override
	public boolean open() {
		boolean open;

		open = camera.open(cameraIndex);
		// Set the max FPS to 60. If we don't set this it defaults
		// to 30, which unnecessarily hampers higher end cameras
		camera.set(5, 60);

		return open;
	}

	
	@Override
	public boolean isOpen() {
		return camera.isOpened();
	}


	@Override
	public void close() {
		camera.release();
		return;
	}


	@Override
	public String getName() {
		return Webcam.getWebcams().get(cameraIndex).getName();
	}

	@Override
	public void setViewSize(final Dimension size) {
			camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, size.getWidth());
			camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, size.getHeight());
	}
	

	@Override
	public Dimension getViewSize() {
			return new Dimension((int) camera.get(Highgui.CV_CAP_PROP_FRAME_WIDTH),
					(int) camera.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT));
	}


	public void launchCameraSettings() {
		camera.set(Highgui.CV_CAP_PROP_SETTINGS, 1);
	}
	
	@Override
	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final Configuration config, final CameraView cameraView)
	{
		if (NativeShotDetector.isSystemSupported())
			return new NativeShotDetector(cameraManager, config, cameraView);
		else if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, config, cameraView);
		else
			return null;
	}

	@Override
	public void run() {
		while (isOpen())
		{
			if (cameraEventListener.isPresent())
				cameraEventListener.get().newFrame(getMatFrame());
			
			if (((int) (getFrameCount() % Math.min(getFPS(), 5)) == 0)  && cameraState != CameraState.CALIBRATING) {
				estimateCameraFPS();
			}
			
		}
		if (cameraEventListener.isPresent())
			cameraEventListener.get().cameraClosed();
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	private Optional<Double> origExposure = Optional.empty();
	@Override
	public boolean supportsExposureAdjustment() {
		origExposure = Optional.of(camera.get(CV_CAP_PROP_EXPOSURE));
		
		boolean res = decreaseExposure();
		if (!res) return false;
		
		resetExposure();
		return true;
	}

	@Override
	public boolean decreaseExposure() {
		final double curExp = camera.get(CV_CAP_PROP_EXPOSURE);
		final double newExp = curExp - (.1 * curExp);
		camera.set(CV_CAP_PROP_EXPOSURE, newExp);
		return (camera.get(CV_CAP_PROP_EXPOSURE) == newExp);
	}
	
	private void resetExposure()
	{
		if (origExposure.isPresent())
			camera.set(CV_CAP_PROP_EXPOSURE, origExposure.get());
	}

	
	
}
