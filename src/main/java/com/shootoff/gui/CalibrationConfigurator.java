package com.shootoff.gui;

public interface CalibrationConfigurator {
	public CalibrationOption getSelectedCalibrationOption();

	public void toggleCalibrating();

	public void disableShotDetection(int msDuration);
}
