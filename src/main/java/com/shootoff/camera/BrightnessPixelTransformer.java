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
import java.util.Optional;

import javafx.geometry.Bounds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class BrightnessPixelTransformer implements PixelTransformer {
	
	private final Logger logger = LoggerFactory.getLogger(BrightnessPixelTransformer.class);
	
	private final static int BRIGHTNESS_INDEX = 2;
	
	private final BufferedImage colorMovingAverage = new BufferedImage(CameraManager.FEED_WIDTH,
			CameraManager.FEED_HEIGHT, BufferedImage.TYPE_INT_RGB);
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_HEIGHT][CameraManager.FEED_WIDTH];
	
	private boolean initialized = false;


	
	public void updateFilter(BufferedImage frame, int x, int y) {
		int currentRGB = frame.getRGB(x,y);
		int currentLum = PixelTransformer.calcLums(currentRGB);
		
        if (this.initialized == false)
        {
            lumsMovingAverage[x][y] = currentLum;
            colorMovingAverage.setRGB(x,y, currentRGB);
            
            if (x == CameraManager.FEED_WIDTH-1 && y == CameraManager.FEED_HEIGHT-1)
            	this.initialized = true;

            return;
        }
        
		/*if (x == 236 && y == 169)
		{
			Color currentC = new Color(currentRGB);
			Color averageC = new Color(colorMovingAverage.getRGB(x, y));
			logger.warn("updateFilter {} {} {} - {} {} {} - {} {} {} - {}", currentC.getRed(), currentC.getGreen(), currentC.getBlue(),
					averageC.getRed(), averageC.getGreen(), averageC.getBlue(),
					((float)currentC.getRed()/(float)averageC.getRed()), ((float)currentC.getGreen()/(float)averageC.getGreen()), ((float)currentC.getBlue()/(float)averageC.getBlue()),
					lumsMovingAverage[x][y]);
		}*/
        
        // Update the average brightness
        lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;

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

	public boolean applyFilter(BufferedImage frame, int x, int y, LightingCondition lightCondition) {

		 return false;
	}
}
