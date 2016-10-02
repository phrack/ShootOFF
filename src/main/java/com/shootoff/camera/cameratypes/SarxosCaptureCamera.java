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
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.NativeShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;

public class SarxosCaptureCamera extends CalculatedFPSCamera {
	private static final Logger logger = LoggerFactory.getLogger(SarxosCaptureCamera.class);

	public static final int CV_CAP_PROP_EXPOSURE = 15;

	private int cameraIndex = -1;
	private final VideoCapture camera;

	private AtomicBoolean closing = new AtomicBoolean(false);

	// For testing
	protected SarxosCaptureCamera() {
		camera = null;
	}

	public SarxosCaptureCamera(final String cameraName) {
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

	public SarxosCaptureCamera(final String cameraName, int cameraIndex) {
		if (cameraIndex < 0) throw new IllegalArgumentException("Camera not found: " + cameraName);

		camera = new VideoCapture();
		this.cameraIndex = cameraIndex;

	}

	@Override
	public Mat getMatFrame() {
		final Mat frame = new Mat();
		try {
			if (!isOpen() || !camera.read(frame) || frame.size().height == 0 || frame.size().width == 0) return null;
		} catch (Exception e) {
			// Sometimes there is a race condition on closing the camera vs.
			// read()
			return null;
		}

		frameCount++;
		currentFrameTimestamp = System.currentTimeMillis();
		return frame;
	}

	@Override
	public BufferedImage getBufferedImage() {
		final Mat frame = getMatFrame();

		if (frame == null) {
			return null;
		} else {
			return Camera.matToBufferedImage(getMatFrame());
		}
	}

	@Override
	public synchronized boolean open() {
		logger.trace("{} - open request isOpen {} closing {}", getName(), isOpen(), closing);

		if (isOpen() && !closing.get()) return true;

		closing.set(false);

		final boolean open = camera.open(cameraIndex);

		if (open) {
			// Set the max FPS to 60. If we don't set this it defaults
			// to 30, which unnecessarily hampers higher end cameras
			camera.set(5, 60);

			CameraFactory.openCamerasAdd(this);
		}

		return open;
	}

	@Override
	public boolean isOpen() {
		return camera.isOpened();
	}

	@Override
	public synchronized void close() {
		logger.trace("{} - close request isOpen {} closing {}", getName(), isOpen(), closing);

		if (isOpen() && !closing.get()) {
			closing.set(true);
			resetExposure();
			camera.release();

		} else if (isOpen() && closing.get()) {
			return;
		} else if (!isOpen()) {
			closing.set(false);
		}

		CameraFactory.openCamerasRemove(this);

		if (cameraEventListener.isPresent()) cameraEventListener.get().cameraClosed();

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
	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager,	final CameraView cameraView) {
		if (NativeShotDetector.isSystemSupported())
			return new NativeShotDetector(cameraManager, cameraView);
		else if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, cameraView);
		else
			return null;
	}

	@Override
	public void run() {
		while (isOpen() && !closing.get()) {
			if (cameraEventListener.isPresent()) cameraEventListener.get().newFrame(getMatFrame());

			if (((int) (getFrameCount() % Math.min(getFPS(), 5)) == 0) && cameraState != CameraState.CALIBRATING) {
				estimateCameraFPS();
			}

		}

		logger.trace("{} camera closed during run thread isOpen {} closing {}", getName(), isOpen(), closing);

		if (!closing.get()) close();
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	private Optional<Double> origExposure = Optional.empty();

	@Override
	public boolean supportsExposureAdjustment() {
		// If we already verified that it works,
		// we have an origExposure value set
		if (origExposure.isPresent()) return true;

		final double exp = camera.get(CV_CAP_PROP_EXPOSURE);

		if (logger.isInfoEnabled()) logger.info("Initial camera exposure {}", exp);

		if (exp == 0) return false;

		origExposure = Optional.of(exp);

		if (!decreaseExposure()) {
			resetExposure();
			origExposure = Optional.empty();
			return false;
		}

		resetExposure();
		return true;
	}

	@Override
	public boolean decreaseExposure() {
		// Logic:
		// If camera exposure is positive, decrease towards zero
		// If camera exposure is negative and between -9.9 and 0, increase
		// towards zero (Logitech c270)
		// If camera exposure is negative and less than -10, decrease away from
		// zero (oCam)

		// In any case, if exposure doesn't change in the same direction when we
		// change it, fail out.
		final double curExp = camera.get(CV_CAP_PROP_EXPOSURE);
		final double newExp;
		if (curExp <= -10.0) {
			newExp = curExp + (.1 * curExp);
		} else {
			newExp = curExp - (.1 * curExp);
		}

		if (logger.isTraceEnabled()) logger.trace("curExp[ {} newExp {}", curExp, newExp);

		// If they don't have the same sign, ABORT
		if (!((curExp < 0) == (newExp < 0)) || Math.abs(curExp - newExp) < .001f) return false;

		camera.set(CV_CAP_PROP_EXPOSURE, newExp);

		if (logger.isTraceEnabled()) logger.trace("Reducing exposure - curExp[ {} newExp {} res {}", curExp, newExp,
				camera.get(CV_CAP_PROP_EXPOSURE));

		if (curExp <= -10.0)
			return (camera.get(CV_CAP_PROP_EXPOSURE) < curExp);
		else
			return (Math.abs(camera.get(CV_CAP_PROP_EXPOSURE)) < Math.abs(curExp));
	}

	public void resetExposure() {
		if (origExposure.isPresent()) camera.set(CV_CAP_PROP_EXPOSURE, origExposure.get());
	}

	public boolean limitsFrames() {
		return false;
	}
}
