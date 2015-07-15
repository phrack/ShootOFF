package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;

public interface PixelTransformer {
	public void updateFilter(int x, int y, Color c);
	public void applyFilter(BufferedImage frame, LightingCondition lightCondition);
}
