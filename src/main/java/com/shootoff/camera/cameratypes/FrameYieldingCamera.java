package com.shootoff.camera.cameratypes;

public abstract class FrameYieldingCamera extends Camera {
	public abstract void startYieldFrames(FrameListener frameListener);
	public abstract void stopYieldFrames();
}
