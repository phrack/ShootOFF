package com.shootoff.camera;

import com.shootoff.camera.cameratypes.Camera;

/**
 * View to display camera and shot detection errors on a GUI.
 * 
 * @author phrack
 */
public interface CameraErrorView {
	public static final String MISSING_ERROR = "ShootOFF can no longer communicate with the webcam %s. Was it "
			+ "unplugged?";
	public static final String FPS_WARNING = "The FPS from %s has dropped to %f, which is too low for reliable "
			+ "shot detection. Some shots may be missed. You may be able to raise the FPS by closing other "
			+ "applications.";
	public static final String BRIGHTNESS_WARNING = "The camera %s is streaming frames that are very bright. "
			+ " This will increase the odds of shots falsely being detected. For best results, please do any "
			+ "mix of the following:%n%n "
			+ "-Turn off auto white balance and auto focus on your webcam and reduce the brightness%n"
			+ "-Remove any bright light sources in the camera's view%n"
			+ "-Turn down your projector's brightness and contrast";
	
	public void showCameraLockError(Camera webcam, boolean allCamerasFailed);

	public void showMissingCameraError(Camera webcam);

	public void showFPSWarning(Camera webcam, double fps);

	public void showBrightnessWarning(Camera webcam);
}