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
	
	public ShotSearchingBrightnessPixelTransformer(Configuration config,
			CanvasManager canvasManager, boolean[][] sectorStatuses,
			Optional<Bounds> projectionBounds, boolean cropped) {
		super(config, canvasManager, sectorStatuses, null, null,
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
	
	public boolean findShotWithFrame(BufferedImage frame, int x, int y)
	{
			//logger.warn("Entered findShot {} {}", x, y);
			
			javafx.scene.paint.Color tempcolor = javafx.scene.paint.Color.rgb(0, 0, 0);
			Shot shot = new Shot(tempcolor, x, y, 
					CameraManager.TESTING_framecount, config.getMarkerRadius());

			
			if (!((DeduplicationProcessor) config.getDeduplicationProcessor()).processShotLookahead(shot))
				return false;
			
			Pair<Optional<Point2D>, Optional<Color>> pair = approximateCenterWithColor(frame, x, y);
			
			Optional<Point2D> center = pair.getKey();
			
			Optional<Color> areaColor = pair.getValue();
			
			
			if (areaColor.isPresent()) {
				if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent() &&
						areaColor.get().equals(config.getIgnoreLaserColor().get()))
							return false;

				

				if (center.isPresent()) {
					tempcolor = javafx.scene.paint.Color.rgb(areaColor.get().getRed(), areaColor.get().getGreen(), areaColor.get().getBlue());
					
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
	
	protected Optional<Color> detectColor(double x, double y, double shotWidth, double shotHeight) {
		int redCount = 0;
		int greenCount = 0;
		
		//logger.warn("SIZE {} {} {} {}", x, y, shotWidth, shotHeight);

		float redavg=0;
		float greenavg=0;
		int startx = Math.max((int)(x-shotWidth/2), 0);
		int starty = Math.max((int)(y-shotHeight/2), 0);
		int endx = Math.min((int)(x+shotWidth/2), CameraManager.FEED_WIDTH);
		int endy = Math.min((int)(y+shotHeight/2), CameraManager.FEED_HEIGHT);
		
		int count = (endx-startx)*(endy-starty);
	

		for (int xpix = startx; xpix < endx; xpix++)
		{

			for (int ypix = starty; ypix < endy; ypix++)
			{
				java.awt.Color currentC = new java.awt.Color(currentFrame.getRGB(xpix, ypix));
				java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(xpix, ypix));
				
				float redratio = (float)currentC.getRed()/(float)averageC.getRed();
				float greenratio = (float)currentC.getGreen()/(float)averageC.getGreen();
				float blueratio = (float)currentC.getBlue()/(float)averageC.getBlue();
				
				float gbratio = (greenratio+blueratio)/2;
				float rbratio = (redratio+blueratio)/2;
				
				float redadv = redratio-gbratio;
				float greenadv = greenratio-rbratio;
				
				//logger.warn("pixel {} {} {} {}", xpix, ypix, redadv, greenadv);
				
				redavg += redratio;
				greenavg += greenratio;

				/*PixelColor c = getPixelColor(xpix, ypix);
				
				if (c == PixelColor.RED) {
					redCount++;
				} else if (c == PixelColor.GREEN) {
					greenCount++;
				}*/
			}
		}
		
		redavg = redavg / count;
		greenavg = greenavg / count;
		
		float diff = redavg - greenavg;

		if (Math.abs(diff) < .01) {
			logger.warn("Shot Processing: No color detected for suspected shot ({}, {}), "
					+ "redCount = {}, greenCount = {}",
					x, y, redavg, greenavg);
			return Optional.of(Color.BLUE);
		} else if (diff > 0) {
			logger.warn("Shot Processing: Detected red shot ({}, {}), redCount = {}, greenCount = {}",
					x, y, redavg, greenavg);
			return Optional.of(Color.RED);
		} else {
			logger.warn("Shot Processing: Detected green shot ({}, {}), redCount = {}, greenCount = {}",
					x, y, redavg, greenavg);
			return Optional.of(Color.GREEN);
		}
	}
	
	protected PixelColor getPixelColor(int x, int y) {
		java.awt.Color currentC = new java.awt.Color(currentFrame.getRGB(x, y));

		java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(x, y));
		
		float redratio = (float)currentC.getRed()/(float)averageC.getRed();
		float greenratio = (float)currentC.getGreen()/(float)averageC.getGreen();
		float blueratio = (float)currentC.getBlue()/(float)averageC.getBlue();
		
		float gbratio = (greenratio+blueratio)/2;
		float rbratio = (redratio+blueratio)/2;
		
		float redadv = redratio-gbratio;
		float greenadv = greenratio-rbratio;
		
		logger.warn("getPixelColor {} {} {} {}", x, y, redadv, greenadv);
		
		if (redadv>.002)
			return PixelColor.RED;
		if (greenadv>.002)
			return PixelColor.GREEN;
		
		return PixelColor.NONE;
	}
	
	
	/**
	 * Find the approximate center of the shot given initial coordinates.
	 *
	 * @param x	initial x coordinate of the shot location
	 * @param y initial y coordinate of the shot location
	 * @return the approximate center of the shot
	 */
	protected Pair<Optional<Point2D>, Optional<Color>> approximateCenterWithColor(BufferedImage frame, double x, double y) {
		double minX = x, minY = y;
		double maxY = y;
		
		int currentLum;
		int maLum;
		
		
		if (x==439 && y == 264)
			logger.warn("bw {}", borderWidth);
		
		
		// We need to see a certain number of dark pixels because the shot
		// does not have sharp borders (we may hit a dark pixel right away
		// even though it's not the real edge otherwise)
		int blackCount = 0;

		for (;maxY < frame.getHeight(); maxY++) {
			currentLum = PixelTransformer.calcLums(frame.getRGB((int)x, (int)maxY));
			maLum = lumsMovingAverage[(int)x][(int)maxY];
			
			if (x==439 && y == 264)
				logger.warn("{} {} {} {} {}", currentLum, maLum, blackCount, minY, maxY);
			if ((currentLum-maLum)<((255-maLum)/2))
				blackCount++; else blackCount = 0;
			if (blackCount == borderWidth) break;
		}
		if (x==439 && y == 264)
			logger.warn("minY {} maxY {}", minY, maxY);
		
		if (maxY-minY >= borderWidth) maxY -= borderWidth-1;

		double shotHeight = maxY - minY + 1;		
		double centerY = minY + (shotHeight / 2);

		if (x==439 && y == 264)
		{
			logger.warn("minY {} maxY {}", minY, maxY);
			logger.warn("sH {} cY {}", shotHeight, centerY);
		}

		double shotWidth = 0;
		for (int yy = (int)minY; yy < maxY; yy++) {
			int xx = (int)minX;
			blackCount = 0;
			
			for (; xx < frame.getWidth(); xx++) {
				currentLum = PixelTransformer.calcLums(frame.getRGB(xx, yy));
				maLum = lumsMovingAverage[xx][yy];
				
				if (x==439 && y == 264)
					logger.warn("{} {} {} {} {}", currentLum, maLum, blackCount, xx, yy);
				
				
				if ((currentLum-maLum)<((255-maLum)/2))
					blackCount++; else blackCount = 0;
				if (blackCount == borderWidth) break;
			}
		
			double width = xx - minX + 1;
			//if (xx > borderWidth) xx -= borderWidth;
			if (width >= borderWidth) width -= borderWidth-1;
			
			
			if (width >= shotWidth) shotWidth = width;
			
			if (x==439 && y == 264)
				logger.warn("w {} sW {} mSD {} xx {}", width, shotWidth, minShotDim, xx);
			
		}

		double centerX = minX + (shotWidth / 2);
		
		double totalArea = shotWidth*shotHeight;
		
		// If the width and height of the shot are really small it's a false positive
		if (totalArea < minShotDim) {
			logger.warn("Suspected shot rejected: Dimensions Too Small "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return new Pair<Optional<Point2D>, Optional<Color>>(Optional.empty(), Optional.empty());
			// Really big is bad too
		} else if (totalArea > minShotDim * 6) {
			logger.warn("Suspected shot rejected: Dimensions Too big "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return new Pair<Optional<Point2D>, Optional<Color>>(Optional.empty(), Optional.empty());
		}
		
		logger.warn("SHOT: {} {} {} {} {} {}", x, y, centerX, centerY, shotWidth, shotHeight);

		
		return new Pair<Optional<Point2D>, Optional<Color>>(Optional.of(new Point2D(centerX, centerY)), detectColor(centerX, centerY, shotWidth, shotHeight));
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
            colorMovingAverage.setRGB(x,y, currentRGB);
            return false;

        }

        // Update the average brightness
    	lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (CameraManager.INIT_FRAME_COUNT-1)) + currentLum) / CameraManager.INIT_FRAME_COUNT;
		// Update the average color
		int rgb = colorMovingAverage.getRGB(x,y);
		
		double red = Math.pow(((rgb >> 16) & 0xFF),2);
		double blue = Math.pow(((rgb >> 8) & 0xFF),2);
		double green = Math.pow((rgb & 0xFF),2);
		
		double currentred = Math.pow(((currentRGB >> 16) & 0xFF),2);
		double currentblue = Math.pow(((currentRGB >> 8) & 0xFF),2);
		double currentgreen = Math.pow((currentRGB & 0xFF),2);
		
		//sqrt((R1^2+R2^2)/2),sqrt((G1^2+G2^2)/2),sqrt((B1^2+B2^2)/2)
		int averageRed = (int) Math.sqrt((red * (CameraManager.INIT_FRAME_COUNT-1) + currentred) / CameraManager.INIT_FRAME_COUNT);
		int averageGreen = (int) Math.sqrt((green * (CameraManager.INIT_FRAME_COUNT-1) + currentgreen) / CameraManager.INIT_FRAME_COUNT);
		int averageBlue = (int) Math.sqrt((blue * (CameraManager.INIT_FRAME_COUNT-1) + currentblue) / CameraManager.INIT_FRAME_COUNT);

		
		/*int averageRed = ((((rgb >> 16) & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + ((currentRGB >> 16) & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageGreen = ((((rgb >> 8) & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + ((currentRGB >> 8) & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;
		int averageBlue = (((rgb & 0xFF) * (CameraManager.INIT_FRAME_COUNT-1)) + (currentRGB & 0xFF)) / 
				CameraManager.INIT_FRAME_COUNT;*/

		rgb = ((255 & 0xFF) << 24) |
                 ((averageRed & 0xFF) << 16) |
                 ((averageGreen & 0xFF) << 8)  |
                 ((averageBlue & 0xFF) << 0);
		colorMovingAverage.setRGB(x, y, rgb);
		
		
		if (!pixelTransformerInitialized)
			return false;

		if ((currentLum-lumsMovingAverage[x][y])<((255-lumsMovingAverage[x][y])/2) || lumsMovingAverage[x][y]>250)
			return false;
			
		if ((currentLum-lumsMovingAverage[x][y]) < 10)
			return false;

		java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(x, y));
		float redratio = (float)currentC.getRed()/(float)averageC.getRed();
		float greenratio = (float)currentC.getGreen()/(float)averageC.getGreen();
		float blueratio = (float)currentC.getBlue()/(float)averageC.getBlue();
		
		float gbratio = (greenratio+blueratio)/2;
		float rbratio = (redratio+blueratio)/2;
		
		float redadv = redratio-gbratio;
		float greenadv = greenratio-rbratio;
		
		if ((redratio > 1.25 && redadv>.005) || (greenratio > 1.25 && greenadv>.005))
			return true;
		
		return false;

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
		int maLum = lumsMovingAverage[x][y];

		int currentLum = PixelTransformer.calcLums(frame.getRGB(x, y));

		if ((currentLum-maLum)<((255-maLum)/2) || maLum>250)
			return false;
		
		if ((currentLum-maLum) < 10)
			return false;
		
		//float[] hsbvals = Color.RGBtoHSB(currentC.getRed(), currentC.getGreen(), currentC.getBlue(), null);

		java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
		java.awt.Color averageC = new java.awt.Color(colorMovingAverage.getRGB(x, y));
		
		float redratio = (float)currentC.getRed()/(float)averageC.getRed();
		float greenratio = (float)currentC.getGreen()/(float)averageC.getGreen();
		float blueratio = (float)currentC.getBlue()/(float)averageC.getBlue();
		
		float gbratio = (greenratio+blueratio)/2;
		float rbratio = (redratio+blueratio)/2;
		
		float redadv = redratio-gbratio;
		float greenadv = greenratio-rbratio;

		
		/*if (x==377 && y==274)
		{
			logger.warn("applyFilter{} {} {} - {} {} {} - {} {} {} - {} {} - {} {} - {}  - {}", 
					CameraManager.TESTING_framecount, x, y,
					((float)currentC.getRed()/(float)averageC.getRed()), ((float)currentC.getGreen()/(float)averageC.getGreen()), ((float)currentC.getBlue()/(float)averageC.getBlue()),
					redratio, greenratio, blueratio,
					gbratio, rbratio,
					redadv, greenadv,
					hsbvals[0],
					lumsMovingAverage[x][y]);
		}*/

		if ((redratio > 1.25 && redadv>.005) || (greenratio > 1.25 && greenadv>.005)) {
			
			/*logger.warn("applyFilter{} {} {} - {} {} {} - {} {} {} - {} {} - {} {} - {} - {}", 
				CameraManager.TESTING_framecount, x, y,
				((float)currentC.getRed()/(float)averageC.getRed()), ((float)currentC.getGreen()/(float)averageC.getGreen()), ((float)currentC.getBlue()/(float)averageC.getBlue()),
				redratio, greenratio, blueratio,
				gbratio, rbratio,
				redadv, greenadv,
				hsbvals[0],
				lumsMovingAverage[x][y]);*/
			//findShotWithFrame(frame, x, y);
			return true;
		}
		/*// We only care about dimming pixels that are brighter than average
		 if (maLum > CameraManager.IDEAL_LUM) {
			 // If the current pixels is brighter than normal and it's not because
			 // red grew by quit a bit, dim the pixel. If it is brighter and red
			 // grew by quite a bit it might be a shot
			 if (!isRedBrighter(currentC, new Color(colorMovingAverage.getRGB(x, y)), lightCondition) && 
					 !isGreenBrighter(currentC, new Color(colorMovingAverage.getRGB(x, y)), lightCondition)) {
                    float[] hsbvals = Color.RGBtoHSB(currentC.getRed(), currentC.getGreen(), currentC.getBlue(), null);
                    hsbvals[BRIGHTNESS_INDEX] *= (CameraManager.IDEAL_LUM / (float)maLum);
                    frame.setRGB(x, y, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));
		 	}
		 }*/
		return false;
	}
}
