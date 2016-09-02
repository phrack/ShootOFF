package com.shootoff.camera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamCompositeDriver;
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamDevice;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.cameratypes.IpCamera;
import com.shootoff.camera.cameratypes.WebcamCaptureCamera;

public final class CameraFactory {
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
	public static void openCameraRemove(Camera camera) {
		openCameras.remove(camera);
	}
	public static void openCamerasAdd(Camera camera) {
		openCameras.add(camera);
	}
	public static boolean isMac() {
		return isMac;
	}
}
