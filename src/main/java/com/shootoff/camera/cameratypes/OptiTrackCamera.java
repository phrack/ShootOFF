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
import java.io.File;
import java.util.Optional;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.NativeShotDetector;
import com.shootoff.camera.shotdetection.OptiTrackShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;

public class OptiTrackCamera implements Camera {
	private static final Logger logger = LoggerFactory.getLogger(OptiTrackCamera.class);

	private static boolean initialized = false;
	protected CameraState cameraState;
	protected Optional<CameraEventListener> cameraEventListener = Optional.empty();
	protected long currentFrameTimestamp = -1;
	private Dimension dimension = null;
	private int viewWidth = 0;
	private int viewHeight = 0;
	private static final int MAXIMUM_EXPOSURE = 480;

	public OptiTrackCamera() {
		if (!initialized) init();
		
	}

	public static void init() {
		if (initialized) return;

		try {
			File lib = new File(System.mapLibraryName("OptiTrackCamera"));
			System.load(lib.getAbsolutePath());
			initialize();
			initialized = true;
		} catch (UnsatisfiedLinkError exception) {
			initialized = false;
		}

		if (initialized && cameraAvailableNative()) {
			CameraFactory.registerCamera(new OptiTrackCamera());
		}
	}

	public boolean setState(CameraState cameraState) {
		switch (cameraState) {
		case DETECTING:
			// TODO: If 780nm, enable. If visible, keep disabled
			if (!getIRFilterState()) toggleIRFilter();
			break;
		case CALIBRATING:
			resetExposure();
			if (getIRFilterState()) toggleIRFilter();
			break;
		case CLOSED:
			if (this.cameraState != CameraState.CLOSED) {
				this.cameraState = cameraState;
				close();
			}
			break;
		case NORMAL:
		default:
			break;

		}
		this.cameraState = cameraState;

		return true;
	}

	public CameraState getState() {
		return cameraState;
	}

	public void setCameraEventListener(CameraEventListener cameraEventListener) {
		this.cameraEventListener = Optional.ofNullable(cameraEventListener);
	}

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	public String getName() {
		return "OptiTrack";
	}

	private native static void initialize();

	private native static boolean cameraAvailableNative();

	private native int getViewWidth();

	private native int getViewHeight();

	private native byte[] getImageNative();

	private native boolean getIRFilterState();

	private native void toggleIRFilter();

	private native int getExposure();

	private native void setExposure(int exposure);

	public synchronized native void close();

	public synchronized native boolean open();

	public native boolean isOpen();

	@Override
	public native double getFPS();

	@Override
	public native int getFrameCount();

	public void setViewSize(final Dimension size) {
		return;
	}

	public Dimension getViewSize() {
		if (dimension != null) return dimension;

		dimension = new Dimension(getViewWidth(), getViewHeight());

		return dimension;
	}

	public Mat translateCameraArrayToMat(byte[] imageBuffer) {
		if (viewHeight == 0) viewHeight = getViewHeight();
		if (viewWidth == 0) viewWidth = getViewWidth();

		Mat mat = new Mat(viewHeight, viewWidth, CvType.CV_8UC1);
		Mat dst = new Mat(viewHeight, viewWidth, CvType.CV_8UC3);

		mat.put(0, 0, imageBuffer);
		Imgproc.cvtColor(mat, dst, Imgproc.COLOR_GRAY2BGR);
		return dst;
	}

	public Mat getMatFrame() {
		byte[] frame = getImageNative();
		currentFrameTimestamp = System.currentTimeMillis();
		Mat mat = translateCameraArrayToMat(frame);
		return mat;
	}

	@Override
	public BufferedImage getBufferedImage() {
		return Camera.matToBufferedImage(getMatFrame());
	}

	@Override
	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final Configuration config,
			final CameraView cameraView) {
		if (OptiTrackShotDetector.isSystemSupported())
			return new OptiTrackShotDetector(cameraManager, config, cameraView);
		else if (NativeShotDetector.isSystemSupported())
			return new NativeShotDetector(cameraManager, config, cameraView);
		else if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, config, cameraView);
		else
			return null;
	}

	public static boolean initialized() {
		return initialized;
	}

	@Override
	public void run() {}

	// TODO: Switch timestamps to optitrack internal timestamps
	private void receiveFrame(byte[] frame) {
		currentFrameTimestamp = System.currentTimeMillis();
		Mat mat = translateCameraArrayToMat(frame);

		if (cameraEventListener.isPresent()) {
			cameraEventListener.get().newFrame(mat);
		}

		if (cameraEventListener.isPresent()) cameraEventListener.get().newFPS(getFPS());
	}

	private void cameraClosed() {
		if (cameraEventListener.isPresent()) cameraEventListener.get().cameraClosed();

		if (isOpen()) close();
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	private Optional<Integer> origExposure = Optional.empty();

	@Override
	public boolean supportsExposureAdjustment() {
		if (!origExposure.isPresent()) origExposure = Optional.of(getExposure());
		return true;
	}

	@Override
	public boolean decreaseExposure() {
		final int curExp = getExposure();
		final int newExp = (int) (curExp - (.1 * (double) curExp));
		logger.trace("curExp[ {} newExp {}", curExp, newExp);

		if (newExp < 20) return false;

		setExposure(newExp);
		logger.trace("curExp[ {} newExp {} res {}", curExp, newExp, getExposure());
		return (getExposure() == newExp);
	}

	public void resetExposure() {
		if (origExposure.isPresent()) setExposure(origExposure.get());
	}

	public boolean limitsFrames() {
		return true;
	}

}
