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


import java.util.ArrayList;
import java.util.Optional;

import javafx.geometry.Bounds;

import java.awt.Color;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.analysis.colour.CIEDE2000;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.feature.global.AvgBrightness;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
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
		super(config, canvasManager, sectorStatuses, currentFrame,
				null, projectionBounds, cropped);


		for (int x = 0; x < CameraManager.FEED_WIDTH; x++)
			for (int y = 0; y < CameraManager.FEED_HEIGHT; y++)
				lumsMovingAverage[x][y] = -1;


	}

	private final Logger logger = LoggerFactory.getLogger(ShotSearchingBrightnessPixelTransformer.class);
	
	private final static int BRIGHTNESS_INDEX = 2;
	
	private int[][] lumsMovingAverage = new int[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	private double[][] colorDiffMovingAverage = new double[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	
	private final int[][] newLumsMovingAverage = new int[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	private final double[][] newColorDiffMovingAverage = new double[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	public double[][] getColorDiffMovingAverage() {
		return colorDiffMovingAverage;
	}




	@Override
	public void run() {
		
		//test(currentFrame);
		
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
		newLumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;
		
    	// Update the color distance
		newColorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + colorDiff) / CameraManager.INIT_FRAME_COUNT;
		
    	/*if (lumsMovingAverage[x][y] > 245)
    	{
    		logger.warn("PIXEL TOO BRIGHT {} {}", x, y);
    	}*/
    	
    	
    	
		return result;

	}
	
	public void applyFilter()
	{
		lumsMovingAverage = newLumsMovingAverage;
		colorDiffMovingAverage = newColorDiffMovingAverage;
	}
	
	/*static public Float[] GREEN = { 87.7370334735442f, -86.1884340941196f, 83.1861435450368f } ;
	static public Float[] RED = { 53.2328817858425f, 80.1053270902018f, 67.2227819454362f };
	public ArrayList<Pixel> updateDataAndGetPossibleShots(BufferedImage frame, boolean pixelTransformerInitialized)
	{
		ArrayList<Pixel> possibleShots = new ArrayList<Pixel>();
		
		final MBFImage mbfImage = Transforms.RGB_TO_CIELab(ImageUtilities.createMBFImage(frame, false));
		
		
		Parallel.forIndex(0, frame.getHeight(), 1, new Operation<Integer>()
		{

		public void perform (Integer y) {
			for (int x = 0; x < frame.getWidth(); x++) {
				java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
				int currentRGB = currentC.getRGB();
				
				int currentLum = PixelTransformer.calcLums(currentRGB);
				
		    	float deltaEg = deltaE_green(mbfImage.getPixel(x, y));
		    	float deltaEr = deltaE_red(mbfImage.getPixel(x, y));
		    	//logger.trace("{} {} {} {}", mbfImage.getPixel(x, y), GREEN, deltaEg, deltaEr);
		    	//avgDistance += (deltaEr - deltaEg);
		    	
		    	float colorDiff = (deltaEr - deltaEg);
		    	
		    	if (lumsMovingAverage[x][y] == -1)
		    	{
		    		colorDiffMovingAverage[x][y] = colorDiff;
		    		lumsMovingAverage[x][y] = currentLum;
		    	}
		    	else
		    	{
		    		
		    		if (pixelTransformerInitialized && (currentLum-lumsMovingAverage[x][y])>=((255-lumsMovingAverage[x][y])/2) && lumsMovingAverage[x][y]<=250 && (currentLum-lumsMovingAverage[x][y])>=10)
		    		{
		    			synchronized (possibleShots)
		    			{
		    				possibleShots.add(new Pixel(x,y, currentC, currentLum, lumsMovingAverage[x][y], colorDiffMovingAverage[x][y], mbfImage.getPixel(x,y)));
		    			}
		    		}
		    		
		    		
		    		
		        	lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;
		    		colorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + colorDiff) / CameraManager.INIT_FRAME_COUNT;
		    	}
		    }
		}
		});
		
		return possibleShots;
	}
	

	
	public static float deltaE_green(Float[] c1)
	{
		return CIEDE2000.calculateDeltaE(c1, GREEN);
	}
	
	public static float deltaE_red(Float[] c1)
	{
		return CIEDE2000.calculateDeltaE(c1, RED);
	}
	
	public void test(BufferedImage frame)
	{
		Float[] GREEN = { 87.7370334735442f, -86.1884340941196f, 83.1861435450368f } ;
		Float[] RED = { 53.2328817858425f, 80.1053270902018f, 67.2227819454362f };
		
		MBFImage mbfImage = ImageUtilities.createMBFImage(frame, false);
		
		AvgBrightness avgb = new AvgBrightness();
		avgb.analyseImage(mbfImage);
		logger.warn("{}", avgb.getBrightness());
		
		
		mbfImage = Transforms.RGB_TO_CIELab(mbfImage);
		
		double avgDistance = 0;
		int i = 0;
		
		
		for (int y=325; y<338; y++) {
		    for(int x=403; x<415; x++) {
		    	float deltaEg = CIEDE2000.calculateDeltaE(mbfImage.getPixel(x, y), GREEN);
		    	float deltaEr = CIEDE2000.calculateDeltaE(mbfImage.getPixel(x, y), RED);
		    	logger.warn("{} {} {} {}", mbfImage.getPixel(x, y), GREEN, deltaEg, deltaEr);
		    	avgDistance += (deltaEr - deltaEg);
		    	
		    	i++;
		    }
		}
		avgDistance /= i;
		
		logger.warn("{}", avgDistance);
	}*/
	
	public boolean applyFilter(BufferedImage frame, int x, int y, LightingCondition lightCondition) {
		return false;
	}
}
