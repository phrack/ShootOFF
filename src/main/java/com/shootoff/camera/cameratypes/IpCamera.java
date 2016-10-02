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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.ds.ipcam.IpCamAuth;
import com.github.sarxos.webcam.ds.ipcam.IpCamDevice;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;

public class IpCamera extends CalculatedFPSCamera {
	private static final Logger logger = LoggerFactory.getLogger(IpCamera.class);
	private final Webcam ipcam;

	private AtomicBoolean closing = new AtomicBoolean(false);

	public IpCamera(final Webcam ipcam) {
		this.ipcam = ipcam;
	}

	protected Webcam getWebcam() {
		return this.ipcam;
	}

	public static IpCamera registerIpCamera(String cameraName, URL cameraURL, Optional<String> username,
			Optional<String> password)
			throws MalformedURLException, URISyntaxException, UnknownHostException, TimeoutException {
		// These are here because webcam-capture wraps this exception in a
		// WebcamException if the
		// URL has a syntax issue. We don't want to use webcam-capture classes
		// outside of this
		// class, thus to handle this error we need to artificially cause it
		// earlier if it is
		// going to be a problem.
		cameraURL.toURI();

		try {
			IpCamDevice ipcam;
			if (username.isPresent() && password.isPresent()) {
				IpCamAuth auth = new IpCamAuth(username.get(), password.get());
				ipcam = IpCamDeviceRegistry.register(new IpCamDevice(cameraName, cameraURL, IpCamMode.PUSH, auth));
			} else {
				ipcam = IpCamDeviceRegistry.register(new IpCamDevice(cameraName, cameraURL, IpCamMode.PUSH));
			}

			// If a camera can't be reached, webcam capture seems to freeze
			// indefinitely. This is done
			// to add an artificial timeout.
			Thread t = new Thread(() -> ipcam.getResolution(), "GetIPcamResolution");
			t.start();
			final int ipcamTimeout = 6000;
			try {
				t.join(ipcamTimeout);
			} catch (InterruptedException e) {
				logger.error("Error connecting to webcam", e);
			}

			if (t.isAlive()) {
				IpCamDeviceRegistry.unregister(cameraName);
				throw new TimeoutException();
			}

			return new IpCamera(Webcam.getWebcamByName(cameraName));
		} catch (WebcamException we) {
			Throwable cause = we.getCause();

			if (cause instanceof UnknownHostException) {
				throw (UnknownHostException) cause;
			}

			logger.error("Error connecting to webcam", we);
			throw we;
		}
	}

	public static boolean unregisterIpCamera(final String cameraName) {
		return IpCamDeviceRegistry.unregister(cameraName);
	}

	public Mat getMatFrame() {

		return Camera.bufferedImageToMat(getBufferedImage());
	}

	public BufferedImage getBufferedImage() {
		currentFrameTimestamp = System.currentTimeMillis();
		frameCount++;
		return ipcam.getImage();
	}

	public synchronized boolean open() {
		if (isOpen() && !closing.get()) return true;

		closing.set(false);
		boolean open = false;
		try {
			open = ipcam.open();
		} catch (WebcamException we) {
			open = false;
		}
		return open;
	}

	public boolean isOpen() {
		return ipcam.isOpen();
	}

	public synchronized void close() {
		if (!isOpen() || closing.get()) return;

		if (cameraEventListener.isPresent()) cameraEventListener.get().cameraClosed();

		closing.set(true);

		if (CameraFactory.isMac()) {
			new Thread(() -> {
				ipcam.close();
			}, "CloseMacOSXWebcam").start();
			return;
		} else {
			ipcam.close();
			return;
		}
	}

	public String getName() {
		return ipcam.getName();
	}

	public boolean isLocked() {
		return ipcam.getLock().isLocked();
	}

	private boolean isImageNew() {
		return ipcam.isImageNew();
	}

	public void setViewSize(Dimension size) {
		try {
			ipcam.setCustomViewSizes(new Dimension[] { size });

			ipcam.setViewSize(size);
		} catch (IllegalArgumentException e) {
			logger.error(String.format("Failed to set dimensions for camera: camera.getName() = %s", getName()), e);
		}
	}

	public Dimension getViewSize() {
		return ipcam.getViewSize();
	}

	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final CameraView cameraView) {
		if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, cameraView);
		else
			return null;
	}

	public void run() {
		while (isOpen() && !closing.get()) {
			if (!isImageNew()) continue;

			if (cameraEventListener.isPresent()) cameraEventListener.get().newFrame(getMatFrame());

			if (((int) (getFrameCount() % Math.min(getFPS(), 5)) == 0) && cameraState != CameraState.CALIBRATING) {
				estimateCameraFPS();
			}

		}

		if (!closing.get()) close();
	}

	public boolean supportsExposureAdjustment() {
		return false;
	}

	public boolean decreaseExposure() {
		return false;
	}

	public void resetExposure() {
		return;
	}

	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((ipcam == null) ? 0 : ipcam.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		IpCamera other = (IpCamera) obj;
		if (ipcam == null) {
			if (other.ipcam != null) return false;
		} else if (!ipcam.equals(other.ipcam)) return false;
		return true;
	}

	public boolean limitsFrames() {
		return false;
	}
}
