package com.shootoff.camera;

import javafx.geometry.Bounds;

public interface CameraCalibrationListener {
	public void calibrate(Bounds bounds, boolean calibratedFromCanvas);
	public void setArenaBackground(String resourceFilename);
}
