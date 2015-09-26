/*
7 * ShootOFF - Software for Laser Dry Fire Training
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

import java.awt.image.BufferedImage;


import java.util.Optional;

import javafx.geometry.Bounds;
import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class ShotSearchingBrightnessPixelTransformer extends ShotSearcher implements PixelTransformer {
	
	protected int minShotDim = 12; // px
	
	protected int maxShotDim = minShotDim*5; // px
	
	public ShotSearchingBrightnessPixelTransformer(Configuration config,
			CanvasManager canvasManager, boolean[][] sectorStatuses,
			BufferedImage currentFrame, Optional<Bounds> projectionBounds, boolean cropped) {
		super(config, canvasManager, sectorStatuses, null, null,
				projectionBounds, cropped);


		for (int x = 0; x < CameraManager.FEED_WIDTH; x++)
			for (int y = 0; y < CameraManager.FEED_HEIGHT; y++)
				lumsMovingAverage[x][y] = -1;


	}

	private final Logger logger = LoggerFactory.getLogger(ShotSearchingBrightnessPixelTransformer.class);
	
	private final static int BRIGHTNESS_INDEX = 2;
	
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	private final double[][] colorDiffMovingAverage = new double[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	@Override
	public void run() {
		//**These tests don't work without lum information**
		
		// Split the image into x columns and y rows, and search
		// each independently
		/*int subWidth = currentFrame.getWidth() / SECTOR_COLUMNS;
		int subHeight = currentFrame.getHeight() / SECTOR_ROWS;

		for (int startY = 0, sectorY = 0; sectorY < SECTOR_ROWS;
				startY += subHeight, sectorY++) {
			for (int startX = 0, sectorX = 0; sectorX < SECTOR_COLUMNS;
					startX += subWidth, sectorX++) {

				// Don't detect a shot in a sector that is turned off
				if (sectorStatuses[sectorY][sectorX])
				{
					for (int sectorPointX = startX; sectorPointX < subWidth; sectorPointX++)
						for (int sectorPointY = startY; sectorPointY < subHeight; sectorPointY++)
								findShotWithFrame(currentFrame, sectorPointX, sectorPointY);
				}
			}
		}*/
	}
	
	

	
	public void updateFilter(BufferedImage frame, int x, int y)
	{
		System.exit(1);
	}
	
	public Optional<Pixel> updateFilter(BufferedImage frame, int x, int y, boolean pixelTransformerInitialized) {
		
		Optional<Pixel> result = Optional.empty();
		java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
		int currentRGB = currentC.getRGB();
		
		int currentLum = PixelTransformer.calcLums(currentRGB);
		
		double colorDiff = Pixel.colorDistance(currentC, Color.RED) - Pixel.colorDistance(currentC, Color.GREEN);
		
        if (lumsMovingAverage[x][y] == -1)
        {
            lumsMovingAverage[x][y] = currentLum;
            colorDiffMovingAverage[x][y] = colorDiff;
            return Optional.empty();

        }


		
		if (!pixelTransformerInitialized)
			result = Optional.empty();

		else if ((currentLum-lumsMovingAverage[x][y])<((255-lumsMovingAverage[x][y])/2) || lumsMovingAverage[x][y]>250)
			result = Optional.empty();
			
		else if ((currentLum-lumsMovingAverage[x][y]) < 10)
			result = Optional.empty();
		else
			
			result = Optional.of(new Pixel(x,y, currentC, currentLum, lumsMovingAverage[x][y], colorDiffMovingAverage[x][y]));
		
		
        // Update the average brightness
    	lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;
		
    	// Update the color distance
    	colorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + colorDiff) / CameraManager.INIT_FRAME_COUNT;
		
    	/*if (lumsMovingAverage[x][y] > 245)
    	{
    		logger.warn("PIXEL TOO BRIGHT {} {}", x, y);
    	}*/
    	
    	
    	
		return result;

	}
	
	
	
	public boolean applyFilter(BufferedImage frame, int x, int y, LightingCondition lightCondition) {
		return false;
	}
}
