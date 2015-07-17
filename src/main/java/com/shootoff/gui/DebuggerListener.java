package com.shootoff.gui;

import java.awt.image.BufferedImage;

import com.shootoff.camera.LightingCondition;

public interface DebuggerListener {
	public void updateDebugView(BufferedImage thresholdImg);
	public void updateFeedData(double fps, LightingCondition lightingCondition);
}