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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Optional;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.util.Pair;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.ShotSearcher.PixelColor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class ShotSearchingBrightnessPixelTransformer extends ShotSearcher implements PixelTransformer {
	
	protected int minShotDim = 9; // px
	
	
	public ShotSearchingBrightnessPixelTransformer(Configuration config,
			CanvasManager canvasManager, boolean[][] sectorStatuses,
			BufferedImage currentFrame, Optional<Bounds> projectionBounds, boolean cropped) {
		super(config, canvasManager, sectorStatuses, currentFrame, null,
				projectionBounds, cropped);


		for (int x = 0; x < CameraManager.FEED_WIDTH; x++)
			for (int y = 0; y < CameraManager.FEED_HEIGHT; y++)
				lumsMovingAverage[x][y] = -1;


	}

	private final Logger logger = LoggerFactory.getLogger(ShotSearchingBrightnessPixelTransformer.class);
	
	private final static int BRIGHTNESS_INDEX = 2;
	
	
	private final static float GREEN_HUE_LOW = .18f;
	private final static float GREEN_HUE_HIGH = .45f;
	
	private final static float BLUE_HUE_LOW = .5f;
	private final static float BLUE_HUE_HIGH = .78f;

	private final BufferedImage colorMovingAverage = new BufferedImage(CameraManager.FEED_WIDTH,
			CameraManager.FEED_HEIGHT, BufferedImage.TYPE_INT_RGB);
	private final int[][] lumsMovingAverage = new int[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];

	
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
	
	
	
	public boolean findShotWithFrame(BufferedImage frame, int x, int y)
	{
			//logger.warn("Entered findShot {} {}", x, y);
			
			javafx.scene.paint.Color tempcolor = javafx.scene.paint.Color.rgb(0, 0, 0);
			Shot shot = new Shot(tempcolor, x, y, 
					CameraManager.TESTING_framecount, config.getMarkerRadius());

			
			if (!((DeduplicationProcessor) config.getDeduplicationProcessor()).processShotLookahead(shot))
				return false;

			
			Pair<Optional<Point2D>, Optional<PixelColor>> pair = approximateCenterWithColor(frame, x, y);
			
			Optional<Point2D> center = pair.getKey();
			
			Optional<PixelColor> areaColor = pair.getValue();
			
			
			if (areaColor.isPresent()) {

				if (center.isPresent()) {
					
					if (areaColor.get() == PixelColor.RED)
						tempcolor = javafx.scene.paint.Color.RED;
					if (areaColor.get() == PixelColor.GREEN)
						tempcolor = javafx.scene.paint.Color.GREEN;
					if (areaColor.get() == PixelColor.BLUE)
						tempcolor = javafx.scene.paint.Color.BLUE;

					
					if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent() &&
							tempcolor.equals(config.getIgnoreLaserColor().get()))
								return false;

					

					
					logger.debug("Suspected shot accepted: Original Coords ({}, {}), Center ({}, {}), {}",
							x, y, center.get().getX(),
							center.get().getY(), areaColor.get());

					if (cropped && projectionBounds.isPresent()) {
						Bounds b = projectionBounds.get();
						
						canvasManager.addShot(tempcolor, center.get().getX() + b.getMinX(),
								center.get().getY() + b.getMinY());
					} else {
						canvasManager.addShot(tempcolor, center.get().getX(),
								center.get().getY());
					}
					return true;
				}
			}
		return false;
	}
	
	protected Optional<PixelColor> detectColor(double x, double y, double shotWidth, double shotHeight) {
		int redCount = 0;
		int greenCount = 0;
		
		//logger.warn("SIZE {} {} {} {}", x, y, shotWidth, shotHeight);

		float redavg=0;
		float greenavg=0;
		
		float redadvavg=0;
		float greenadvavg=0;
		
		double red_color_distance=0;
		double green_color_distance=0;

		int startx = (int) x;
		int starty = (int) y;
		int endx = (int)(x+shotWidth-1);
		int endy = (int)(y+shotHeight-1);
		
		int count = (endx-startx)*(endy-starty);
	

		for (int xpix = startx; xpix < endx; xpix++)
		{

			for (int ypix = starty; ypix < endy; ypix++)
			{
				java.awt.Color currentC = new java.awt.Color(currentFrame.getRGB(xpix, ypix));
				//java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(xpix, ypix));
				
				/*float IDEAL_R_AVERAGE = 171;
				float dr = IDEAL_R_AVERAGE / averageC.getRed();
				float db = 1 - (dr - 1);
				if (averageC.getRed() < IDEAL_R_AVERAGE && dr < 2f) {
					float r = currentC.getRed() * dr;
					if (r > 255) r = 255;
					if (r < 0) r = 0;
					float b = currentC.getBlue() * db;
					if (b > 255) b = 255;
					if (b < 0) b = 0;
					//currentC = new Color((int)r, currentC.getGreen(), (int)b);
				}*/
				
				/*float redratio = (float)currentC.getRed()/(float)averageC.getRed();
				float greenratio = (float)currentC.getGreen()/(float)averageC.getGreen();
				float blueratio = (float)currentC.getBlue()/(float)averageC.getBlue();
				
				
				float gbratio = (greenratio+blueratio)/2;
				float rbratio = (redratio+blueratio)/2;
				
				redadvavg = redadvavg+(redratio-gbratio);
				greenadvavg = greenadvavg+(greenratio-rbratio);
				
				redavg += redratio;
				greenavg += greenratio;
				*/
				
				red_color_distance = red_color_distance+ColorDistance(currentC, Color.RED);
				green_color_distance = green_color_distance+ColorDistance(currentC, Color.GREEN);
				
				/*PixelColor c = getPixelColor(currentC.getRGB());
				if (c == PixelColor.RED)
					redCount++;
				else if (c == PixelColor.GREEN)
					greenCount++;
				 */
			}
		}
		
		/*redavg = redavg / count;
		greenavg = greenavg / count;
		
		redadvavg = redadvavg / count;
		greenadvavg = greenadvavg / count;
		
		float diff = redavg - greenavg;
		
		float diffadv = redadvavg-greenadvavg;
		*/
		red_color_distance = red_color_distance / count;
		green_color_distance = green_color_distance / count;
		
		double color_diff = red_color_distance - green_color_distance;

		if (Math.abs(color_diff) < 1) {
			logger.warn("Shot Processing: No color detected for suspected shot ({}, {}), "
					+ "redCount = {}, greenCount = {} - {} {} - {} {} - {} - {}",
					x, y, redCount, greenCount, redavg, greenavg, redadvavg, greenadvavg, color_diff, count);
			return Optional.empty();
		} else if (color_diff < 0) {
			logger.warn("Shot Processing: Detected red shot ({}, {}), redCount = {}, greenCount = {} - {} {} - {} {} - {} - {}",
					x, y, redCount, greenCount, redavg, greenavg, redadvavg, greenadvavg, color_diff, count);
			return Optional.of(PixelColor.RED);
		} else {
			logger.warn("Shot Processing: Detected green shot ({}, {}), redCount = {}, greenCount = {} - {} {} - {} {} - {} - {}",
					x, y, redCount, greenCount, redavg, greenavg, redadvavg, greenadvavg, color_diff, count);
			return Optional.of(PixelColor.GREEN);
		}
		
		/*if (redCount == greenCount) {
			logger.warn("Shot Processing: No color detected for suspected shot ({}, {}), "
					+ "redCount = {}, greenCount = {} - {} {} - {} {} -{ }",
					x, y, redCount, greenCount, redavg, greenavg, redadvavg, greenadvavg, count);
			return Optional.empty();
		} else if (redCount > 0) {
			logger.warn("Shot Processing: Detected red shot ({}, {}), redCount = {}, greenCount = {} - {} {} - {} {} -{ }",
					x, y, redCount, greenCount, redavg, greenavg, redadvavg, greenadvavg, count);
			return Optional.of(PixelColor.RED);
		} else {
			logger.warn("Shot Processing: Detected green shot ({}, {}), redCount = {}, greenCount = {} - {} {} - {} {} -{ }",
					x, y, redCount, greenCount, redavg, greenavg, redadvavg, greenadvavg, count);
			return Optional.of(PixelColor.GREEN);
		}*/
	}
	
	public double ColorDistance(Color c1, Color c2)
	{
	    return Math.sqrt(Math.pow(c1.getRed()-c2.getRed(),2) + Math.pow(c1.getGreen()-c2.getGreen(),2) + Math.pow(c1.getBlue()-c2.getBlue(),2));
	} 
	
	
	/**
	 * Find the approximate center of the shot given initial coordinates.
	 *
	 * @param x	initial x coordinate of the shot location
	 * @param y initial y coordinate of the shot location
	 * @return the approximate center of the shot
	 */
	protected Pair<Optional<Point2D>, Optional<PixelColor>> approximateCenterWithColor(BufferedImage frame, double x, double y) {
		double minX = x, minY = y;
		double maxY = y;
		
		int currentLum;
		int maLum;
		
		// We need to see a certain number of dark pixels because the shot
		// does not have sharp borders (we may hit a dark pixel right away
		// even though it's not the real edge otherwise)
		int blackCount = 0;

		for (;maxY < frame.getHeight(); maxY++) {
			currentLum = PixelTransformer.calcLums(frame.getRGB((int)x, (int)maxY));
			maLum = lumsMovingAverage[(int)x][(int)maxY];
			
			//logger.warn("{} {} {} {} {}", currentLum, maLum, blackCount, minY, maxY);
			if ((currentLum-maLum)<((255-maLum)/2))
				blackCount++; else blackCount = 0;
			if (blackCount == borderWidth) break;
			if (maxY-minY>minShotDim*3) break;
		}
		//logger.warn("minY {} maxY {}", minY, maxY);
		
		if (maxY-minY >= borderWidth) maxY -= borderWidth-1;

		double shotHeight = maxY - minY + 1;		
		double centerY = minY + (shotHeight / 2);


		//logger.warn("minY {} maxY {}", minY, maxY);
		//logger.warn("sH {} cY {}", shotHeight, centerY);


		double shotWidth = 0;
		for (int yy = (int)minY; yy < maxY; yy++) {
			int xx = (int)minX;
			blackCount = 0;
			
			for (; xx < frame.getWidth(); xx++) {
				currentLum = PixelTransformer.calcLums(frame.getRGB(xx, yy));
				maLum = lumsMovingAverage[xx][yy];
				
				//logger.warn("{} {} {} {} {}", currentLum, maLum, blackCount, xx, yy);
				
				
				if ((currentLum-maLum)<((255-maLum)/2))
					blackCount++; else blackCount = 0;
				if (blackCount == borderWidth) break;
				if (xx-minX > minShotDim*3) break;
			}
		
			double width = xx - minX + 1;
			//if (xx > borderWidth) xx -= borderWidth;
			if (width >= borderWidth) width -= borderWidth-1;
			
			
			if (width >= shotWidth) shotWidth = width;
			
			//logger.warn("w {} sW {} mSD {} xx {}", width, shotWidth, minShotDim, xx);
			
		}

		double centerX = minX + (shotWidth / 2);
		
		double totalArea = shotWidth*shotHeight;
		
		// If the width and height of the shot are really small it's a false positive
		if (totalArea < minShotDim) {
			logger.warn("Suspected shot rejected: Dimensions Too Small "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return new Pair<Optional<Point2D>, Optional<PixelColor>>(Optional.empty(), Optional.empty());
			// Really big is bad too
		} else if (totalArea > minShotDim * 7) {
			logger.warn("Suspected shot rejected: Dimensions Too big "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return new Pair<Optional<Point2D>, Optional<PixelColor>>(Optional.empty(), Optional.empty());
		}
		
		logger.warn("SHOT: {} {} {} {} {} {}", x, y, centerX, centerY, shotWidth, shotHeight);

		
		return new Pair<Optional<Point2D>, Optional<PixelColor>>(Optional.of(new Point2D(centerX, centerY)), detectColor(minX, minY, shotWidth, shotHeight));
	}
	
	public void updateFilter(BufferedImage frame, int x, int y)
	{
		System.exit(1);
	}
	
	public boolean updateFilter(BufferedImage frame, int x, int y, boolean pixelTransformerInitialized) {
		java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
		int currentRGB = currentC.getRGB();
		
		int currentLum = PixelTransformer.calcLums(currentRGB);
		
        if (lumsMovingAverage[x][y] == -1)
        {
            lumsMovingAverage[x][y] = currentLum;
            //colorMovingAverage.setRGB(x,y, currentRGB);
            return false;

        }

        // Update the average brightness
    	lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;
		// Update the average color
		//int rgb = colorMovingAverage.getRGB(x,y);
		
		/*double red = Math.pow(((rgb >> 16) & 0xFF),2);
		double blue = Math.pow(((rgb >> 8) & 0xFF),2);
		double green = Math.pow((rgb & 0xFF),2);
		
		double currentred = Math.pow(((currentRGB >> 16) & 0xFF),2);
		double currentblue = Math.pow(((currentRGB >> 8) & 0xFF),2);
		double currentgreen = Math.pow((currentRGB & 0xFF),2);*/
		
		//sqrt((R1^2+R2^2)/2),sqrt((G1^2+G2^2)/2),sqrt((B1^2+B2^2)/2)
		/*int averageRed = (int) Math.sqrt((red * (CameraManager.INIT_FRAME_COUNT-1) + currentred) / CameraManager.INIT_FRAME_COUNT);
		int averageGreen = (int) Math.sqrt((green * (CameraManager.INIT_FRAME_COUNT-1) + currentgreen) / CameraManager.INIT_FRAME_COUNT);
		int averageBlue = (int) Math.sqrt((blue * (CameraManager.INIT_FRAME_COUNT-1) + currentblue) / CameraManager.INIT_FRAME_COUNT);
		*/
		
		/*int averageRed = ((((rgb >> 16) & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + ((currentRGB >> 16) & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageGreen = ((((rgb >> 8) & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + ((currentRGB >> 8) & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageBlue = (((rgb & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + (currentRGB & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;

		rgb = ((255 & 0xFF) << 24) |
                 ((averageRed & 0xFF) << 16) |
                 ((averageGreen & 0xFF) << 8)  |
                 ((averageBlue & 0xFF) << 0);
		*/
		
		//rgb =  ( ((((currentRGB) ^ (colorMovingAverage.getRGB(x,y))) & 0xfffefefe) >> 1) + ((currentRGB) & (colorMovingAverage.getRGB(x,y))) );
		
		//colorMovingAverage.setRGB(x, y, rgb);
		
		
		if (!pixelTransformerInitialized)
			return false;

		if ((currentLum-lumsMovingAverage[x][y])<((255-lumsMovingAverage[x][y])/2) || lumsMovingAverage[x][y]>250)
			return false;
			
		if ((currentLum-lumsMovingAverage[x][y]) < 15)
			return false;

		/*java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(x, y));
		float redratio = (float)currentC.getRed()/(float)averageC.getRed();
		float greenratio = (float)currentC.getGreen()/(float)averageC.getGreen();
		float blueratio = (float)currentC.getBlue()/(float)averageC.getBlue();
		
		float gbratio = (greenratio+blueratio)/2;
		float rbratio = (redratio+blueratio)/2;
		
		float redadv = redratio-gbratio;
		float greenadv = greenratio-rbratio;
		
		if ((redratio > 1.25 && redadv>.005) || (greenratio > 1.25 && greenadv>.005))
			return true;*/
		
		return true;

	}
	
	
	
	public boolean applyFilter(BufferedImage frame, int x, int y, LightingCondition lightCondition) {
		return false;
	}
}
