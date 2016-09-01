package com.shootoff.camera.cameratypes;

import org.opencv.core.Mat;

public interface FrameListener {
	public void newFrame(Mat frame);
}
