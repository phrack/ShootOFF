/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BrightnessPixelTransformer implements PixelTransformer {
	private final static int BRIGHTNESS_INDEX = 2;
	
	private final BufferedImage colorMovingAverage = new BufferedImage(CameraManager.FEED_WIDTH,
			CameraManager.FEED_HEIGHT, BufferedImage.TYPE_INT_RGB);
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_HEIGHT][CameraManager.FEED_WIDTH];
	
	public void updateFilter(int x, int y, int currentRGB) {
		int currentLum = PixelTransformer.calcLums(currentRGB);
		
        if (lumsMovingAverage[y][x] == 0)
        {
            lumsMovingAverage[y][x] = currentLum;
            colorMovingAverage.setRGB(x,y, currentRGB);

            return;
        }
        
        // Update the average brightness
        lumsMovingAverage[y][x] = ((lumsMovingAverage[y][x] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;

		// Update the average color
		int rgb = colorMovingAverage.getRGB(x,y);
		int averageRed = ((((rgb >> 16) & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + ((currentRGB >> 16) & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageGreen = ((((rgb >> 8) & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + ((currentRGB >> 8) & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageBlue = (((rgb & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + (currentRGB & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;

		rgb = ((255 & 0xFF) << 24) |
                 ((averageRed & 0xFF) << 16) |
                 ((averageGreen & 0xFF) << 8)  |
                 ((averageBlue & 0xFF) << 0);
		colorMovingAverage.setRGB(x, y, rgb);
	}
	
	private boolean isRedBrighter(Color currentC, Color averageC, LightingCondition lightCondition) {
		// We only care if current red is brighter than normal
		if (currentC.getRed() < averageC.getRed()) return false;
		
		float percentRedBigger = 1 - ((float)averageC.getRed() / (float)currentC.getRed());
		
		// The pixel is redder than normal and looks red in general
		float threshold;
		if (lightCondition == LightingCondition.VERY_BRIGHT) {
			threshold = .15f;
		} else if (lightCondition == LightingCondition.BRIGHT) {
			threshold = .41f;
		} else {
			threshold = .25f;
		}
		
		return percentRedBigger >= threshold && currentC.getRed() >= averageC.getBlue();
	}
	
	private boolean isGreenBrighter(Color currentC, Color averageC, LightingCondition lightCondition) {
		// We only care if current green is brighter than normal
		if (currentC.getGreen() < averageC.getGreen()) return false;
		
		float percentGreenBigger = 1 - ((float)averageC.getGreen() / (float)currentC.getGreen());
		
		// The pixel is greener than normal and looks greener in general
		float threshold;
		if (lightCondition == LightingCondition.VERY_BRIGHT) {
			threshold = .25f;
		} else if (lightCondition == LightingCondition.BRIGHT) {
			threshold = .65f;
		} else {
			threshold = .50f;
		}
		return percentGreenBigger >= threshold  && currentC.getGreen() >= averageC.getBlue();
	}

	public void applyFilter(BufferedImage frame, int x, int y, LightingCondition lightCondition) {
		int maLum = lumsMovingAverage[y][x];

		Color currentC = new Color(frame.getRGB(x, y));

		// We only care about dimming pixels that are brighter than average
		 if (maLum > CameraManager.IDEAL_LUM) {
			 // If the current pixels is brighter than normal and it's not because
			 // red grew by quit a bit, dim the pixel. If it is brighter and red
			 // grew by quite a bit it might be a shot
			 if (!isRedBrighter(currentC, new Color(colorMovingAverage.getRGB(x, y)), lightCondition) && 
					 !isGreenBrighter(currentC, new Color(colorMovingAverage.getRGB(x, y)), lightCondition)) {
                    float[] hsbvals = Color.RGBtoHSB(currentC.getRed(), currentC.getGreen(), currentC.getBlue(), null);
                    hsbvals[BRIGHTNESS_INDEX] *= CameraManager.IDEAL_LUM / (float)maLum;
                    frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));
		 	}
		 }
	}
}
