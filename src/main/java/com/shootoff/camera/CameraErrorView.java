package com.shootoff.camera;

import com.shootoff.camera.cameratypes.Camera;

/**
 * View to display camera and shot detection errors on a GUI.
 * 
 * @author phrack
 */
public interface CameraErrorView {
	public void showCameraLockError(Camera webcam, boolean allCamerasFailed);

	public void showMissingCameraError(Camera webcam);

	public void showFPSWarning(Camera webcam, double fps);

	public void showBrightnessWarning(Camera webcam);
}