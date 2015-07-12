package com.shootoff.gui;

import java.awt.image.BufferedImage;

public interface ThresholdListener {
	public void updateThreshold(BufferedImage thresholdImg, byte[][] mask);
}
