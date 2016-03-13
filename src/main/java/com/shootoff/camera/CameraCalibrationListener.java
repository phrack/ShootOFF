package com.shootoff.camera;

import com.shootoff.camera.arenamask.ArenaMaskManager;

import javafx.geometry.Bounds;

public interface CameraCalibrationListener {
	public void calibrate(Bounds bounds, boolean calibratedFromCanvas);
	public void setArenaBackground(String resourceFilename);
	public void setArenaMaskManager(ArenaMaskManager arenaMaskManager);
}
