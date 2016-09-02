package com.shootoff.camera.cameratypes;

import org.opencv.core.Mat;

public interface CameraEventListener {
	public void newFrame(Mat frame);
	
	public void newFPS(double cameraFPS);

	public void cameraClosed();

	public void setFeedResolution(int width, int height);
}
