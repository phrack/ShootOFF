package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovingAveragePixelTransformer implements PixelTransformer {
	

	//private final BufferedImage movingAverageBuffer = new BufferedImage(CameraManager.FEED_WIDTH,
	//		CameraManager.FEED_HEIGHT, BufferedImage.TYPE_INT_RGB);

	private final Logger logger = LoggerFactory.getLogger(MovingAveragePixelTransformer.class);
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_HEIGHT][CameraManager.FEED_WIDTH];

	private float biggestFactor = 0f;
	
	public void updatePixel(int x, int y, Color c) {
		//if (x == 10 && y == 10)
		//	System.out.println("Current average: " + lumsMovingAverage[y][x]);
		lumsMovingAverage[y][x] = ((lumsMovingAverage[y][x] * (CameraManager.INIT_FRAME_COUNT-1)) +
				calcLums(c)) / CameraManager.INIT_FRAME_COUNT;
		//if (x == 10 && y == 10)
		//	System.out.println("New average: " + lumsMovingAverage[y][x] + " " + calcLums(c));
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
				int maLum = lumsMovingAverage[y][x];

				Color frameC = new Color(frame.getRGB(x, y));
				int frameLum = calcLums(frameC);
				
				if (x == 119 && y == 143) {
					System.out.println(frameLum + " " + maLum + " " + (1 - ((float)maLum / (float)frameLum)));
				}
				
				if (maLum > CameraManager.IDEAL_LUM) {
					float percent = ((float)maLum / (float)frameLum);
					float percentBrighter = 1 - percent;
					
					if (percentBrighter < .2f) {
						float[] hsbvals = Color.RGBtoHSB(frameC.getRed(), frameC.getGreen(), frameC.getBlue(), null);
						hsbvals[2] *= CameraManager.IDEAL_LUM / (float)frameLum;
						frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));
					}
				}
				
				
				/*if (frameLum >= maLum)
				{
					// Pixel darker than average or same so we don't care, just black it out
					frame.setRGB(x, y, new Color(0,0,0).getRGB());
				} else {
					// Pixel is brighter than average, but by how much?
					float percent = ((float)maLum / (float)frameLum);
					float percentBrighter = 1 - percent;
					
					// Step down pixel unless it has gotten more than 5% brighter
					// % determined using binary search and nature scene
					
					// Also step down the average otherwise the average acts as a lower
					// bound for how low we can go and we want to be able to hit rock bottom (black)
					if (maLum > CameraManager.IDEAL_LUM && percentBrighter < 0.5) {
						float[] hsbvals = Color.RGBtoHSB(frameC.getRed(), frameC.getGreen(), frameC.getBlue(), null);
						hsbvals[2] *= percentBrighter;
						//lumsMovingAverage[y][x] *= percent;
						//frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));
					}
				}*/
			}
		}
	}
}
