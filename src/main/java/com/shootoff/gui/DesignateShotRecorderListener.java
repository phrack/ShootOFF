package com.shootoff.gui;

public interface DesignateShotRecorderListener {
	public void registerShotRecorder(String webcamName);

	public void unregisterShotRecorder(String webcamName);
}
