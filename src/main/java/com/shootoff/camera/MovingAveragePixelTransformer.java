package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class MovingAveragePixelTransformer implements PixelTransformer {
	public static int FEED_WIDTH;
	public static int FEED_HEIGHT;

	BufferedImage movingAverageBuffer;
	public static final int MOVING_AVERAGE_LENGTH = 5;

	protected MovingAveragePixelTransformer(int feed_height, int feed_width)
	{
		FEED_WIDTH = feed_width;
		FEED_HEIGHT = feed_height;
		movingAverageBuffer = new BufferedImage(FEED_HEIGHT, FEED_WIDTH, BufferedImage.TYPE_INT_RGB);
	}

	public void updatePixel(int x, int y, Color c) {
		int rgb = ((movingAverageBuffer.getRGB(x,y) * (MOVING_AVERAGE_LENGTH-1)) + c.getRGB()) / MOVING_AVERAGE_LENGTH;
		movingAverageBuffer.setRGB(x, y, rgb);
	}



	public BufferedImage generateTransformation(BufferedImage frame) {
		return frame;
	}


}
