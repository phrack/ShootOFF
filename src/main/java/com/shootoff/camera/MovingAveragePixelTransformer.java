package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovingAveragePixelTransformer implements PixelTransformer {
	public static final int MOVING_AVERAGE_LENGTH = 5;

	//private final BufferedImage movingAverageBuffer = new BufferedImage(CameraManager.FEED_WIDTH,
	//		CameraManager.FEED_HEIGHT, BufferedImage.TYPE_INT_RGB);

	private final Logger logger = LoggerFactory.getLogger(MovingAveragePixelTransformer.class);
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_HEIGHT][CameraManager.FEED_WIDTH];

	public void updatePixel(int x, int y, Color c) {
		if (x == 10 && y == 10)
			System.out.println("Current average: " + lumsMovingAverage[y][x]);
		lumsMovingAverage[y][x] = ((lumsMovingAverage[y][x] * (MOVING_AVERAGE_LENGTH-1)) +
				calcLums(c)) / MOVING_AVERAGE_LENGTH;
		if (x == 10 && y == 10)
			System.out.println("New average: " + lumsMovingAverage[y][x] + " " + calcLums(c));
		/*Color maC = new Color(movingAverageBuffer.getRGB(x,y));
		int averageRed = ((maC.getRed() * (MOVING_AVERAGE_LENGTH-1)) + c.getRed()) / MOVING_AVERAGE_LENGTH;
		int averageGreen = ((maC.getGreen() * (MOVING_AVERAGE_LENGTH-1)) + c.getGreen()) / MOVING_AVERAGE_LENGTH;
		int averageBlue = ((maC.getBlue() * (MOVING_AVERAGE_LENGTH-1)) + c.getBlue()) / MOVING_AVERAGE_LENGTH;

		Color newAverage = new Color(averageRed, averageGreen, averageBlue);
		movingAverageBuffer.setRGB(x, y, newAverage.getRGB());*/
	}

	private int calcLums(Color c) {
		return (c.getRed() + c.getRed() + c.getRed() +
				c.getBlue() +
				c.getGreen() + c.getGreen() + c.getGreen() + c.getGreen()) >> 3;
	}

	public void generateTransformation(BufferedImage frame) {
		for (int x = 0; x < frame.getWidth(); x++) {
			for (int y = 0; y < frame.getHeight(); y++) {
				/*Color maC = new Color(movingAverageBuffer.getRGB(x, y));
				int maLum = (maC.getRed() + maC.getRed() + maC.getRed() +
						maC.getBlue() +
						maC.getGreen() + maC.getGreen() + maC.getGreen() + maC.getGreen()) >> 3;*/
				int maLum = lumsMovingAverage[y][x];

				Color frameC = new Color(frame.getRGB(x, y));
				int frameLum = calcLums(frameC);

				if (frameLum <= maLum)
				{
					// logger.warn("test: {}  {}", frameLum, maLum);

					//logger.warn("hot pixel: " + x + " " + y);

					//float factor = CameraManager.IDEAL_LUM / frameLum;

					//logger.warn("hot pixel: " + factor);

					/*int newRed = (int) (c.getRed() * factor);
					int newGreen = (int) (c.getGreen() * factor);
					int newBlue = (int) (c.getBlue() * factor);*/

					//logger.warn("old: " + c.getRed() + " " + c.getGreen() + " " + c.getBlue());
					//logger.warn("new: " + newRed + " " + newGreen + " " + newBlue);


					/*float[] hsbvals = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
					hsbvals[2] = hsbvals[2] * factor;
					frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));*/

					frame.setRGB(x, y, new Color(0,0,0).getRGB());
				}
			}
		}
	}


}
