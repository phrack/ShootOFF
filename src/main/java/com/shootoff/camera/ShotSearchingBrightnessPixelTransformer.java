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
import javafx.geometry.Point2D;
import javafx.util.Pair;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.ShotSearcher.PixelColor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.sun.org.apache.xml.internal.security.encryption.Transforms;

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
	
	
	
	public boolean findShotWithFrame(BufferedImage frame, int frameCount, int x, int y)
	{
		logger.trace("Entered findShotWithFrame {} {}", x, y);
		
		javafx.scene.paint.Color tempcolor = javafx.scene.paint.Color.rgb(0, 0, 0);
		Shot shot = new Shot(tempcolor, x, y, 
				frameCount, config.getMarkerRadius());

		
		if (!((DeduplicationProcessor) config.getDeduplicationProcessor()).processShotLookahead(shot))
		{
			logger.info("Shot rejected by DeuplicationProcessor Lookahead {} {}", x, y);
			return false;
		}
		
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

				

				
				logger.info("Suspected shot accepted: Original Coords ({}, {}), Center ({}, {}), {}",
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
		logger.info("Shot rejected by lack of areaColor or center");
		return false;
	}
	
	protected Optional<PixelColor> detectColor(BufferedImage frame, double x, double y, double shotWidth, double shotHeight) {
		
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
				java.awt.Color currentC = new java.awt.Color(frame.getRGB(xpix, ypix));
				//java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(xpix, ypix));
				
				red_color_distance = red_color_distance+ColorDistance(currentC, Color.RED);
				green_color_distance = green_color_distance+ColorDistance(currentC, Color.GREEN);

			}
		}
		
		// Unnecessary divisions? Just use < count below
		/*red_color_distance = red_color_distance / count;
		green_color_distance = green_color_distance / count;*/
		
		// Shorter distance = smaller number
		double color_diff = red_color_distance - green_color_distance;

		if (Math.abs(color_diff) < count) {
			logger.info("Shot Processing: No color detected for suspected shot ({}, {}), "
					+ "{} - {} - {} - {}",
					x, y, red_color_distance, green_color_distance, color_diff, count);
			return Optional.empty();
		} else if (color_diff < 0) {
			logger.info("Shot Processing: Detected red shot ({}, {}), {} - {} - {} - {}",
					x, y, red_color_distance, green_color_distance, color_diff, count);
			return Optional.of(PixelColor.RED);
		} else {
			logger.info("Shot Processing: Detected green shot ({}, {}), {} - {} - {} - {}",
					x, y, red_color_distance, green_color_distance, color_diff, count);
			return Optional.of(PixelColor.GREEN);
		}
		
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
		
		logger.trace("Entering approximateCenterWithColor {} {}", x, y);
		
		
		// We need to see a certain number of dark pixels because the shot
		// does not have sharp borders (we may hit a dark pixel right away
		// even though it's not the real edge otherwise)
		int blackCount = 0;

		for (;maxY < frame.getHeight(); maxY++) {
			currentLum = PixelTransformer.calcLums(frame.getRGB((int)x, (int)maxY));
			maLum = lumsMovingAverage[(int)x][(int)maxY];
			
			logger.trace("{} {} {} {} {}", currentLum, maLum, blackCount, minY, maxY);
			if ((currentLum-maLum)<((255-maLum)/2))
				blackCount++; else blackCount = 0;
			if (blackCount == borderWidth) break;
			if (maxY-minY>maxShotDim) break;
		}
		logger.trace("minY {} maxY {}", minY, maxY);
		
		if (maxY-minY >= borderWidth) maxY -= borderWidth-1;

		double shotHeight = maxY - minY + 1;		
		double centerY = minY + (shotHeight / 2);

		logger.trace("minY {} maxY {}", minY, maxY);
		logger.trace("sH {} cY {}", shotHeight, centerY);


		double shotWidth = 0;
		for (int yy = (int)minY; yy < maxY; yy++) {
			int xx = (int)minX;
			blackCount = 0;
			
			for (; xx < frame.getWidth(); xx++) {
				currentLum = PixelTransformer.calcLums(frame.getRGB(xx, yy));
				maLum = lumsMovingAverage[xx][yy];
				
				logger.trace("{} {} {} {} {}", currentLum, maLum, blackCount, xx, yy);
				
				
				if ((currentLum-maLum)<((255-maLum)/2))
					blackCount++; else blackCount = 0;
				if (blackCount == borderWidth) break;
				if (xx-minX > maxShotDim) break;
			}
		
			double width = xx - minX + 1;
			//if (xx > borderWidth) xx -= borderWidth;
			if (width >= borderWidth) width -= borderWidth-1;
			
			
			if (width >= shotWidth) shotWidth = width;
			
			logger.trace("w {} sW {} mSD {} xx {}", width, shotWidth, minShotDim, xx);
			
		}

		double centerX = minX + (shotWidth / 2);
		
		double totalArea = shotWidth*shotHeight;
		
		// If the width and height of the shot are really small it's a false positive
		if (totalArea < minShotDim) {
			logger.warn("Suspected shot rejected: Dimensions Too Small "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return new Pair<Optional<Point2D>, Optional<PixelColor>>(Optional.empty(), Optional.empty());
			// Really big is bad too
		} else if (totalArea > maxShotDim) {
			logger.warn("Suspected shot rejected: Dimensions Too big "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return new Pair<Optional<Point2D>, Optional<PixelColor>>(Optional.empty(), Optional.empty());
		}
		
		logger.trace("SHOT: {} {} {} {} {} {}", x, y, centerX, centerY, shotWidth, shotHeight);

		
		return new Pair<Optional<Point2D>, Optional<PixelColor>>(Optional.of(new Point2D(centerX, centerY)), detectColor(frame, minX, minY, shotWidth, shotHeight));
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
			
		else if ((currentLum-lumsMovingAverage[x][y]) < 15)
			result = Optional.empty();
		else
			
			result = Optional.of(new Pixel(x,y, currentC, currentLum, lumsMovingAverage[x][y], colorDiffMovingAverage[x][y]));
		
		
        // Update the average brightness
    	lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;
		
    	// Update the color distance
    	colorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + colorDiff) / CameraManager.INIT_FRAME_COUNT;
		
		return result;

	}
	
	
	
	public boolean applyFilter(BufferedImage frame, int x, int y, LightingCondition lightCondition) {
		return false;
	}
}
