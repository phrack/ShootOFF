package com.shootoff.camera;

public interface CameraErrorView {
	public void showMissingCameraError(Camera webcam);
	public void showFPSWarning(Camera webcam, double fps);
	public void showBrightnessWarning(Camera webcam);
}