package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class MovingAveragePixelTransformer implements PixelTransformer {
	private final static int BRIGHTNESS_INDEX = 2;
	
	private final BufferedImage colorMovingAverage = new BufferedImage(CameraManager.FEED_WIDTH,
			CameraManager.FEED_HEIGHT, BufferedImage.TYPE_INT_RGB);
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_HEIGHT][CameraManager.FEED_WIDTH];
	//private final float[][] lumsBrighterMovingAverage = new float[CameraManager.FEED_HEIGHT][CameraManager.FEED_WIDTH];
	
	public void updatePixel(int x, int y, Color c) {
		int currentLum = calcLums(c);
		
		// Update the average brightness
        if (lumsMovingAverage[y][x] == 0)
            lumsMovingAverage[y][x] = currentLum;
	
		/*lumsMovingAverage[y][x] = ((lumsMovingAverage[y][x] * (CameraManager.INIT_FRAME_COUNT-1)) +
				currentLum) / CameraManager.INIT_FRAME_COUNT;
		
		// Update the average brightness change if the pixel got brighter
		float percentBrighter = 1 - ((float)lumsMovingAverage[y][x] / (float)currentLum);
		
		if (percentBrighter > 0) {
			lumsBrighterMovingAverage[y][x] = ((lumsBrighterMovingAverage[y][x] * (CameraManager.INIT_FRAME_COUNT-1)) + percentBrighter) / 
					CameraManager.INIT_FRAME_COUNT;
		}*/

		// Update the average color
		Color maC = new Color(colorMovingAverage.getRGB(x,y));
		int averageRed = ((maC.getRed() * (CameraManager.INIT_FRAME_COUNT-1)) + c.getRed()) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageGreen = ((maC.getGreen() * (CameraManager.INIT_FRAME_COUNT-1)) + c.getGreen()) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageBlue = ((maC.getBlue() * (CameraManager.INIT_FRAME_COUNT-1)) + c.getBlue()) / 
				CameraManager.INIT_FRAME_COUNT;

		Color newAverage = new Color(averageRed, averageGreen, averageBlue);
		colorMovingAverage.setRGB(x, y, newAverage.getRGB());
	}

	private int calcLums(Color c) {
		return (c.getRed() + c.getRed() + c.getRed() +
				c.getBlue() +
				c.getGreen() + c.getGreen() + c.getGreen() + c.getGreen()) >> 3;
	}
	
	private boolean isRedBrighter(Color currentC, Color averageC) {
		// We only care if current red is brighter than normal
		if (currentC.getRed() < averageC.getRed()) return false;
		
		//System.out.println("color: current rgb" + currentC.getRed() + "," + currentC.getGreen() + "," + currentC.getBlue() + " average red: "+ averageC.getRed());
		
		float percentRedBigger = 1 - ((float)averageC.getRed() / (float)currentC.getRed());
		
		// Current red must be at least 10% bigger than normal and it should be larger or
		// equal to all other components
		return percentRedBigger >= .17f && currentC.getRed() >= averageC.getGreen() && currentC.getRed() >= averageC.getBlue();
	}

	public void generateTransformation(BufferedImage frame) {
		for (int x = 0; x < frame.getWidth(); x++) {
			for (int y = 0; y < frame.getHeight(); y++) {
				int maLum = lumsMovingAverage[y][x];

				Color currentC = new Color(frame.getRGB(x, y));
				//int currentLum = calcLums(currentC);
				

				/*if (x == 627 && y == 168) {
					isRedBrighter(currentC, new Color(colorMovingAverage.getRGB(x, y)));
				}*/
				
				// We only care about dimming pixels that are brighter than average
				 if (maLum > CameraManager.IDEAL_LUM) {
					 // If the current pixels is brighter than normal and it's not because
					 // red grew by quit a bit, dim the pixel. If it is brighter and red
					 // grew by quite a bit it might be a shot
					 if (!isRedBrighter(currentC, new Color(colorMovingAverage.getRGB(x, y)))) {
	                        float[] hsbvals = Color.RGBtoHSB(currentC.getRed(), currentC.getGreen(), currentC.getBlue(), null);
	                        hsbvals[BRIGHTNESS_INDEX] *= CameraManager.IDEAL_LUM / (float)maLum;
	                        frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));
				 	}
				 }
				
				
				/*if (maLum > CameraManager.IDEAL_LUM) {
					float percentBrighter = 1 - ((float)maLum / (float)currentLum);
					
					if (percentBrighter < .2f) {
						float[] hsbvals = Color.RGBtoHSB(currentC.getRed(), currentC.getGreen(), currentC.getBlue(), null);
						hsbvals[BRIGHTNESS_INDEX] *= CameraManager.IDEAL_LUM / (float)currentLum;
						frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));
					}
				}*/
			}
		}
	}
}
