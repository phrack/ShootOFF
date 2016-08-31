package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamCompositeDriver;
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamDevice;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.xuggle.xuggler.video.ConverterFactory;

public abstract class Camera {
	public abstract Mat getFrame();

	public abstract BufferedImage getImage();

	public abstract boolean open();

	public abstract boolean isOpen();

	public abstract boolean close();

	public abstract String getName();

	public abstract boolean isLocked();

	public abstract boolean isImageNew();

	public abstract void setViewSize(Dimension size);

	public abstract Dimension getViewSize();

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
		return result;
	}

	
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Camera other = (Camera) obj;
		if (!this.getName().equals(other.getName())) return false;
		return true;
	}
	
	
	// Cameras that are not discovered by webcam-capture can be registered here
	public static List<Camera> registeredCameras = new ArrayList<Camera>();
	public static void registerCamera(Camera camera)
	{
		registeredCameras.add(camera);
	}

	public static class CompositeDriver extends WebcamCompositeDriver {
		public CompositeDriver() {
			super();
			add(new WebcamDefaultDriver());
			add(new IpCamDriver());
		}
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
	
	// These are used in a hack to get this code to work on Mac.
	// On Mac several of the webcam-capture API's can only be
	// called on the main thread before a JavaFX thread is started
	// or the library will hopeless hang and take ShootOFF with it.
	// Our solution is to cache the things we need that will hang
	// the program on start-up. This has the side effect that the
	// cameras that are known when ShootOFF starts are the only
	// ones it will ever know on Mac.
	static boolean isMac = false;
	static Webcam defaultWebcam = null;
	static List<Camera> knownWebcams;
	static List<Camera> openCameras = Collections.synchronizedList(new ArrayList<>());

	static {
		Webcam.setDriver(new CompositeDriver());
		final String os = System.getProperty("os.name");

		if (os != null && os.equals("Mac OS X")) {
			isMac = true;
			knownWebcams = new ArrayList<Camera>();
			
			for (final Webcam w : Webcam.getWebcams()) {
				Camera c = null;
				if (w.getDevice() instanceof IpCamDevice)
					c = new IpCamera(w);
				else
					c = new WebcamCaptureCamera(w.getName());
				
				knownWebcams.add(c);
			}

		} else {
			isMac = false;
			defaultWebcam = null;
			knownWebcams = null;
		}
	}

	public static Optional<Camera> getDefault() {
		Camera defaultCam;

		if (isMac) {
			if (defaultWebcam == null) return Optional.empty();
			
			defaultCam = new WebcamCaptureCamera(defaultWebcam.getName());
		} else {
			final Webcam cam = Webcam.getDefault();
			
			if (cam == null) {
				defaultCam = null;
			} else {
				
				defaultCam = new WebcamCaptureCamera(cam.getName());
			}
		}

		return Optional.ofNullable(defaultCam);
	}
	public static List<Camera> getWebcams() {
		if (isMac) return knownWebcams;

		final List<Camera> webcams = new ArrayList<Camera>();

		for (Webcam w : Webcam.getWebcams()) {
			Camera c = null;
			if (w.getDevice() instanceof IpCamDevice)
				c = new IpCamera(w);
			else
				c = new WebcamCaptureCamera(w.getName());

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
		
		webcams.addAll(registeredCameras);

		return webcams;
	}

}