package com.shootoff.camera.shotdetection;

import java.io.File;

import org.opencv.core.Mat;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.config.Configuration;

import javafx.scene.paint.Color;

public class NativeShotDetector extends FrameProcessingShotDetector {
	private final CameraManager cameraManager;
	private final Configuration config;

	public static boolean loadNativeShotDetector() {
		if (!isSystemSupported())
			return false;

		File lib = new File(System.mapLibraryName("NativeShotDetector"));
		System.load(lib.getAbsolutePath());

		return true;
	}

	public static boolean isSystemSupported() {
		// TODO: Remove this flag when no longer loading dummy detector
		boolean USE_NATIVE_DETECTION = false;

		final String os = System.getProperty("os.name");
		// Do to an oddity in the JVM, this is actually the JRE bitness,
		// which is what we want in this case
		final String arch = System.getProperty("os.arch");

		// Only support 64-bit Windows and Linux
		return USE_NATIVE_DETECTION && arch.contains("64") && (os.contains("Windows") || os.contains("Linux"));
	}

	public NativeShotDetector(final CameraManager cameraManager, final Configuration config,
			final CameraView cameraView) {
		super(cameraManager, config, cameraView);

		this.cameraManager = cameraManager;
		this.config = config;
	}

	private native void analyzeFrame(long frameBGR);

	@Override
	public void processFrame(Mat frameBGR, boolean isDetecting) {
		if (isDetecting)
			analyzeFrame(frameBGR.getNativeObjAddr());
	}

	@Override
	public void setFrameSize(int width, int height) {
		// TODO: Should this be a noop for native shot detection?
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
		Color c = Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb >> 8) & 0xFF, 1.0);

		super.addShot(c, x, y, true);
	}
	
	@Override
	protected boolean handlesBounds() {
		return false;
	}
}
