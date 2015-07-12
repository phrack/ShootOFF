package com.shootoff.gui;

import java.awt.image.BufferedImage;

public interface DebuggerListener {
	public void updateThreshold(BufferedImage thresholdImg, byte[][] mask);
	public void updateFPS(double fps);
}
