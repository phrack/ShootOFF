package com.shootoff.gui;

import java.awt.image.BufferedImage;

public interface DebuggerListener {
	public void updateDebugView(BufferedImage thresholdImg);
	public void updateFPS(double fps);
}
