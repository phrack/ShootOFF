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

package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamCompositeDriver;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamAuth;
import com.github.sarxos.webcam.ds.ipcam.IpCamDevice;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import com.xuggle.xuggler.video.ConverterFactory;

/**
 * A facade class to decouple webcam interaction implementation from webcam use
 * throughout ShootOFF. This class should be re-written if a new webcam library
 * is being used.
 * 
 * @author phrack
 */
public class Camera {
	// These are used in a hack to get this code to work on Mac.
	// On Mac several of the webcam-capture API's can only be
	// called on the main thread before a JavaFX thread is started
	// or the library will hopeless hang and take ShootOFF with it.
	// Our solution is to cache the things we need that will hang
	// the program on start-up. This has the side effect that the
	// cameras that are known when ShootOFF starts are the only
	// ones it will ever know on Mac.
	private static final boolean isMac;
	private static final Webcam defaultWebcam;
	private static final List<Camera> knownWebcams;

	private static final Logger logger = LoggerFactory.getLogger(Camera.class);

	private static final List<Camera> openCameras = Collections.synchronizedList(new ArrayList<>());

	private final VideoCapture camera;
	private final int cameraIndex;
	private final Webcam ipcam;
	private final boolean isIpCam;

	public static class CompositeDriver extends WebcamCompositeDriver {
		public CompositeDriver() {
			super();
			add(new WebcamDefaultDriver());
			add(new IpCamDriver());
		}
	}

	static {
		Webcam.setDriver(new CompositeDriver());
		final String os = System.getProperty("os.name");

		if (os != null && os.equals("Mac OS X")) {
			isMac = true;
			defaultWebcam = Webcam.getDefault();

			knownWebcams = new ArrayList<Camera>();

			for (final Webcam w : Webcam.getWebcams()) {
				knownWebcams.add(new Camera(w.getName()));
			}

		} else {
			isMac = false;
			defaultWebcam = null;
			knownWebcams = null;
		}
	}

	public static Camera registerIpCamera(String cameraName, URL cameraURL, Optional<String> username,
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

			return new Camera(ipcam);
		} catch (WebcamException we) {
			Throwable cause = we.getCause();

			if (cause instanceof UnknownHostException) {
				throw (UnknownHostException) cause;
			}

			logger.error("Error cocnnecting to webcam", we);
			throw we;
		}
	}

	public static boolean unregisterIpCamera(final String cameraName) {
		return IpCamDeviceRegistry.unregister(cameraName);
	}

	// For testing
	protected Camera() {
		camera = null;
		cameraIndex = -1;
		ipcam = null;
		isIpCam = false;
	}

	private Camera(final String cameraName) {
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
		this.ipcam = null;
		this.isIpCam = false;
	}

	private Camera(final IpCamDevice ipcam) {
		camera = null;
		cameraIndex = -1;
		isIpCam = true;

		for (final Camera webcam : getWebcams()) {
			if (webcam.getName().equals(ipcam.getName())) {
				this.ipcam = webcam.getWebcam();
				return;
			}
		}

		this.ipcam = null;
	}

	protected Webcam getWebcam() {
		return ipcam;
	}

	public static Optional<Camera> getDefault() {
		Camera defaultCam;

		if (isMac) {
			if (defaultWebcam == null) return Optional.empty();
			
			defaultCam = new Camera(defaultWebcam.getName());
		} else {
			final Webcam cam = Webcam.getDefault();

			if (cam == null) {
				defaultCam = null;
			} else {
				defaultCam = new Camera(cam.getName());
			}
		}

		return Optional.ofNullable(defaultCam);
	}

	public static List<Camera> getWebcams() {
		if (isMac) return knownWebcams;

		final List<Camera> webcams = new ArrayList<Camera>();

		for (Webcam w : Webcam.getWebcams()) {
			Camera c = new Camera(w.getName());

			// If we already have an open instance of the camera
			// go ahead and reuse it in this list as opposed to
			// the newly created camera
			int i = openCameras.indexOf(c);
			if (i >= 0) {
				webcams.add(openCameras.get(i));
			} else {
				webcams.add(c);
			}
		}

		return webcams;
	}

	public Mat getFrame() {
		final Mat frame = new Mat();
		if (!camera.read(frame) || frame.size().height == 0) return null;

		return frame;
	}

	public BufferedImage getImage() {
		if (isIpCam) {
			return ipcam.getImage();
		} else {
			Mat frame = getFrame();

			if (frame == null) {
				return null;
			} else {
				return matToBufferedImage(getFrame());
			}
		}
	}

	public boolean isIpCam() {
		return isIpCam;
	}

	public boolean open() {
		boolean open;

		if (isIpCam) {
			try {
				open = ipcam.open();
			} catch (WebcamException we) {
				open = false;
			}
		} else {
			open = camera.open(cameraIndex);
			// Set the max FPS to 60. If we don't set this it defaults
			// to 30, which unnecessarily hampers higher end cameras
			camera.set(5, 60);
		}

		if (open) openCameras.add(this);

		return open;
	}

	public Dimension getViewSize() {
		if (isIpCam) {
			return ipcam.getViewSize();
		} else {
			return new Dimension((int) camera.get(Highgui.CV_CAP_PROP_FRAME_WIDTH),
					(int) camera.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT));
		}
	}

	public boolean close() {
		if (isIpCam) {
			if (isMac) {
				new Thread(() -> {
					ipcam.close();
				}, "CloseMacOSXWebcam").start();
				return true;
			} else {
				return ipcam.close();
			}
		} else {
			camera.release();
			openCameras.remove(this);
			return true;
		}
	}

	public String getName() {
		if (isIpCam) {
			return ipcam.getName();
		} else {
			return Webcam.getWebcams().get(cameraIndex).getName();
		}
	}

	public boolean isOpen() {
		if (isIpCam) {
			return ipcam.isOpen();
		} else {
			return camera.isOpened();
		}
	}

	public boolean isLocked() {
		if (isIpCam) {
			return ipcam.getLock().isLocked();
		} else {
			return isOpen();
		}
	}

	public boolean isImageNew() {
		if (isIpCam) {
			return ipcam.isImageNew();
		} else {
			return true;
		}
	}

	public void setViewSize(final Dimension size) {
		if (isIpCam) {
			try {
				ipcam.setCustomViewSizes(new Dimension[] { size });

				ipcam.setViewSize(size);
			} catch (IllegalArgumentException e) {
				logger.error(String.format("Failed to set dimensions for camera: camera.getName() = %s", getName()), e);
			}
		} else {
			camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, size.getWidth());
			camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, size.getHeight());
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ipcam == null) ? 0 : ipcam.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Camera other = (Camera) obj;
		if (!this.getName().equals(other.getName())) return false;
		return true;
	}

	public static BufferedImage matToBufferedImage(Mat matBGR) {
		BufferedImage image = new BufferedImage(matBGR.width(), matBGR.height(), BufferedImage.TYPE_3BYTE_BGR);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		matBGR.get(0, 0, targetPixels);

		return image;
	}

	public static Mat bufferedImageToMat(BufferedImage frame) {
		BufferedImage transformedFrame = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		byte[] pixels = ((DataBufferByte) transformedFrame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);

		return mat;
	}

	public static Mat colorTransfer(Mat source, Mat target) {
		Mat src = new Mat();
		Mat dst = new Mat();

		Imgproc.cvtColor(source, src, Imgproc.COLOR_BGR2Lab);
		Imgproc.cvtColor(target, dst, Imgproc.COLOR_BGR2Lab);

		ArrayList<Mat> src_channels = new ArrayList<Mat>();
		ArrayList<Mat> dst_channels = new ArrayList<Mat>();
		Core.split(src, src_channels);
		Core.split(dst, dst_channels);

		for (int i = 0; i < 3; i++) {
			MatOfDouble src_mean = new MatOfDouble(), src_std = new MatOfDouble();
			MatOfDouble dst_mean = new MatOfDouble(), dst_std = new MatOfDouble();
			Core.meanStdDev(src_channels.get(i), src_mean, src_std);
			Core.meanStdDev(dst_channels.get(i), dst_mean, dst_std);

			dst_channels.get(i).convertTo(dst_channels.get(i), CvType.CV_64FC1);
			Core.subtract(dst_channels.get(i), dst_mean, dst_channels.get(i));
			Core.divide(dst_std, src_std, dst_std);
			Core.multiply(dst_channels.get(i), dst_std, dst_channels.get(i));
			Core.add(dst_channels.get(i), src_mean, dst_channels.get(i));
			dst_channels.get(i).convertTo(dst_channels.get(i), CvType.CV_8UC1);
		}

		Core.merge(dst_channels, dst);

		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_Lab2BGR);

		return dst;
	}
}
