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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.Frame;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.NativeShotDetector;
import com.shootoff.camera.shotdetection.OptiTrackShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;

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
			final File lib = new File(System.mapLibraryName("OptiTrackCamera"));
			System.load(lib.getAbsolutePath());
			initialize();
			initialized = true;
		} catch (final UnsatisfiedLinkError exception) {
			initialized = false;
		}

		if (initialized && cameraAvailableNative()) {
			CameraFactory.registerCamera(new OptiTrackCamera());
		}
	}

	@Override
	public boolean setState(CameraState cameraState) {
		switch (cameraState) {
		case DETECTING_CALIBRATED:
			setIRFilter(true);
			break;
		case CALIBRATING:
			resetExposure();
			setIRFilter(false);
			break;
		case DETECTING:
			// This camera does not detect when not calibrated, intentionally
			setIRFilter(false);
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

	@Override
	public CameraState getState() {
		return cameraState;
	}

	@Override
	public void setCameraEventListener(CameraEventListener cameraEventListener) {
		this.cameraEventListener = Optional.ofNullable(cameraEventListener);
	}

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	@Override
	public String getName() {
		return "OptiTrack";
	}

	private native static void initialize();

	private native static boolean cameraAvailableNative();

	private native int getViewWidth();

	private native int getViewHeight();

	private native byte[] getImageNative();

	private native boolean getIRFilterState();

	private native void setIRFilter(boolean filter);

	private native int getExposure();

	private native void setExposure(int exposure);

	@Override
	public synchronized native void close();

	@Override
	public synchronized native boolean open();

	@Override
	public native boolean isOpen();

	@Override
	public native double getFPS();

	@Override
	public native int getFrameCount();

	@Override
	public void setViewSize(final Dimension size) {
		return;
	}

	@Override
	public Dimension getViewSize() {
		if (dimension != null) return dimension;

		dimension = new Dimension(getViewWidth(), getViewHeight());

		return dimension;
	}

	public Mat translateCameraArrayToMat(byte[] imageBuffer) {
		if (viewHeight == 0) viewHeight = getViewHeight();
		if (viewWidth == 0) viewWidth = getViewWidth();

		final Mat mat = new Mat(viewHeight, viewWidth, CvType.CV_8UC1);
		final Mat dst = new Mat(viewHeight, viewWidth, CvType.CV_8UC3);

		mat.put(0, 0, imageBuffer);
		Imgproc.cvtColor(mat, dst, Imgproc.COLOR_GRAY2BGR);
		return dst;
	}

	@Override
	public Frame getFrame() {
		final byte[] frame = getImageNative();
		final Mat mat = translateCameraArrayToMat(frame);
		final long currentFrameTimestamp = System.currentTimeMillis();
		return new Frame(mat, currentFrameTimestamp);
	}

	@Override
	public BufferedImage getBufferedImage() {
		return getFrame().getOriginalBufferedImage();
	}

	@Override
	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final CameraView cameraView) {
		if (OptiTrackShotDetector.isSystemSupported())
			return new OptiTrackShotDetector(cameraManager, cameraView);
		else if (NativeShotDetector.isSystemSupported())
			return new NativeShotDetector(cameraManager, cameraView);
		else if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, cameraView);
		else
			return null;
	}

	public static boolean initialized() {
		return initialized;
	}

	private final ReentrantLock frameLock = new ReentrantLock(true);
	private AtomicBoolean frameAvailable = new AtomicBoolean(false);
	private byte[] frameBytes;
	private long frameTS;

	@Override
	public void run() {
		while (isOpen()) {
			Frame frame = null;

			synchronized (frameLock) {
				try {
					if (frameAvailable.get() == false) {
						frameLock.wait();
					}
				} catch (InterruptedException e) {}

				if (frameAvailable.compareAndSet(true, false)) {
					frame = new Frame(translateCameraArrayToMat(frameBytes), frameTS);
				}
			}

			if (frame != null) {
				if (cameraEventListener.isPresent()) {
					final boolean shouldDedistort = (cameraState == CameraState.NORMAL) ? true : false;
					cameraEventListener.get().newFrame(frame, shouldDedistort);
				}

				if (cameraEventListener.isPresent()) cameraEventListener.get().newFPS(getFPS());
			}
		}
	}

	private void receiveFrame(byte[] frameBytes, long frameTS) {
		synchronized (frameLock) {
			this.frameBytes = frameBytes;
			this.frameTS = frameTS;
			frameLock.notifyAll();
			frameAvailable.set(true);
		}

	}

	private void cameraClosed() {
		if (cameraEventListener.isPresent()) cameraEventListener.get().cameraClosed();

		cameraEventListener = Optional.empty();

		if (isOpen()) close();

		synchronized (frameLock) {
			frameLock.notifyAll();
		}
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
		final int newExp = (int) (curExp - (.1 * curExp));
		logger.trace("curExp[ {} newExp {}", curExp, newExp);

		if (newExp < 20) return false;

		setExposure(newExp);
		logger.trace("curExp[ {} newExp {} res {}", curExp, newExp, getExposure());
		return (getExposure() == newExp);
	}

	@Override
	public void resetExposure() {
		if (origExposure.isPresent()) setExposure(origExposure.get());
	}

	@Override
	public boolean limitsFrames() {
		return true;
	}

}
