package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;

public interface PixelTransformer {
	public void updatePixel(int x, int y, Color c);
	public void generateTransformation(BufferedImage frame);
}
