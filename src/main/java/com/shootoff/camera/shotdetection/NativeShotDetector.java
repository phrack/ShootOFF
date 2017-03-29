package com.shootoff.camera.shotdetection;

import java.io.File;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.Frame;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.util.SystemInfo;

public class NativeShotDetector extends FrameProcessingShotDetector {
	private final CameraManager cameraManager;

	public static boolean loadNativeShotDetector() {
		if (!isSystemSupported()) return false;

		final File lib = new File(System.mapLibraryName("NativeShotDetector"));
		System.load(lib.getAbsolutePath());

		return true;
	}

	public static boolean isSystemSupported() {
		// TODO: Remove this flag when no longer loading dummy detector
		boolean USE_NATIVE_DETECTION = false;

		// Do to an oddity in the JVM, this is actually the JRE bitness,
		// which is what we want in this case
		final String arch = System.getProperty("os.arch");

		// Only support 64-bit Windows and Linux
		return USE_NATIVE_DETECTION && arch.contains("64") && (SystemInfo.isWindows() || SystemInfo.isLinux());
	}

	public NativeShotDetector(final CameraManager cameraManager, final CameraView cameraView) {
		super(cameraManager, cameraView);

		this.cameraManager = cameraManager;
	}

	private native void analyzeFrame(long frameBGR);

	@Override
	public void processFrame(Frame frame, boolean isDetecting) {
		if (isDetecting) analyzeFrame(frame.getOriginalMat().getNativeObjAddr());
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
	 * @param timestamp
	 *            the timestamp of the new shot, not adjusted for the shot timer
	 * @param rgb
	 *            the rgb color of the new shot
	 */
	public void foundShot(int x, int y, long timestamp, int rgb) {
		// final Color c = Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb
		// >> 8) & 0xFF, 1.0);
		// TODO: Handle colors

		super.addShot(ShotColor.RED, x, y, timestamp, true);
	}

	@Override
	protected boolean handlesBounds() {
		return false;
	}
}
