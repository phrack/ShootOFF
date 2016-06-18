package com.shootoff.camera;

import java.util.Optional;

import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;

public interface CameraCalibrationListener {
	public void calibrate(Bounds arenaBounds, Optional<Dimension2D> perspectivePaperDims, boolean calibratedFromCanvas);

	public void setArenaBackground(String resourceFilename);
}
