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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class ShotSearcher implements Runnable {
	private final int BLACK_PIXEL = 0;
	private final int WHITE_PIXEL = 1;
	private final int MIN_SHOT_DIM = 7; // px
	
	private final Logger logger = LoggerFactory.getLogger(ShotSearcher.class);
	private final Configuration config;
	private final CanvasManager canvasManager;
	private final BufferedImage currentFrame;
	private final byte[][] shotFrame;
	
	public ShotSearcher(Configuration config, CanvasManager canvasManager, 
			BufferedImage currentFrame, byte[][] shotFrame) {
		this.config = config;
		this.canvasManager = canvasManager;
		this.currentFrame = currentFrame;
		this.shotFrame = shotFrame;
	}
	
	@Override
	public void run() {
		// Split the image into 3 columns and 3 rows, and search
		// each independently
		int sub_width = shotFrame[0].length / 3;
		int sub_height = shotFrame.length / 3;
		
		for (int y_start = 0; y_start <= shotFrame.length - sub_height; 
				y_start += sub_height) {
			for (int x_start = 0; x_start <= shotFrame[0].length - sub_width; 
					x_start += sub_width) {
				
				findShot(x_start, x_start + sub_width, y_start, y_start + sub_height);
			}
		}
	}
	
	private void findShot(int x_start, int x_end, int y_start, int y_end) {
		for (int x = x_start; x < x_end; x++) {
			for (int y = y_start; y < y_end; y++) {
				if (shotFrame[y][x] == WHITE_PIXEL) {
					Optional<Color> areaColor = detectColor(x, y);
					if (areaColor.isPresent()) {
						if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent() &&
								areaColor.get().equals(config.getIgnoreLaserColor().get()))
									continue; 
						
						Optional<Point2D> center = approximateCenter(x, y);
						
						if (center.isPresent()) {
							logger.debug("Suspected shot accepted: Original Coords ({}, {}), Center ({}, {})", 
									x, y, center.get().getX(),
									center.get().getY());
							
							canvasManager.addShot(areaColor.get(), center.get().getX(), 
									center.get().getY());
							return;
						}
					}
				}
			}
		}
	}
	
	private Optional<Color> detectColor(int x, int y) {
		int rgb = currentFrame.getRGB(x, y);
		double r = getRed(rgb);
		double g = getGreen(rgb);
		double b = getBlue(rgb);
		
		final int colorDetectionRadius = 5;
		int pixelsSeen = 1;
		
		// Average colorDetectionRadius pixels left
		for (int x_offset = x; x_offset > 0 && x - x_offset < colorDetectionRadius; 
				x_offset--) {
			
			rgb = currentFrame.getRGB(x_offset, y);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
		
		// Average colorDetectionRadius pixels right
		for (int x_offset = x; 
				x_offset < currentFrame.getWidth() && x_offset - x < colorDetectionRadius; 
				x_offset++) {
			
			rgb = currentFrame.getRGB(x_offset, y);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
			
		// Average colorDetectionRadius pixels up
		for (int y_offset = y; 
				y_offset < currentFrame.getHeight() && y_offset - y < colorDetectionRadius; 
				y_offset++) {
			
			rgb = currentFrame.getRGB(x, y_offset);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
			
		// Average colorDetectionRadius pixels down
		for (int y_offset = y; 
				y_offset > 0 && y - y_offset < colorDetectionRadius; 
				y_offset--) {
			
			rgb = currentFrame.getRGB(x, y_offset);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
		
		r /= (double)pixelsSeen;
		g /= (double)pixelsSeen;
		b /= (double)pixelsSeen;
		
        // We only detect a color if the largest component is at least
        // 5% bigger than the other components. This is based on the
        // heuristic that noise tends to have color values that are very
        // similar
		final double PDIFF_THRESHOLD = 1.05;
		
        if (g == 0 || b == 0) 
            return Optional.empty();

        if ((r / g) > PDIFF_THRESHOLD && (r / b) > PDIFF_THRESHOLD)
            return Optional.of(Color.RED);
            
        if (r == 0 || b == 0)
        	return Optional.empty();

        if ((g / r) > PDIFF_THRESHOLD && (g / b) > PDIFF_THRESHOLD)
            return Optional.of(Color.GREEN);

		
		return Optional.empty();
	}
	
	private int getRed(int rgb) {
		return (rgb & 0x00ff0000) >> 16;
	}
	
	private int getGreen(int rgb) {
		return (rgb & 0x0000ff00) >> 8;
	}
	
	private int getBlue(int rgb) {
		return (rgb & 0x000000ff) >> 0;
	}		
	
	/**
	 * Find the approximate center of the shot given initial coordinates. 
	 * 
	 * @param x	initial x coordinate of the shot location
	 * @param y initial y coordinate of the shot location
	 * @return the approximate center of the shot
	 */
	private Optional<Point2D> approximateCenter(double x, double y) {
		double minX = x, minY = y;
		double maxX = x, maxY = y;	
		
		// We need to see a certain number of black pixels because the shot
		// does not have sharp borders (we may hit a black pixel right away
		// even though it's not the read edge otherwise)
		final int BORDER_WIDTH = 3;
		int blackCount = 0;
	
		for (;maxY < shotFrame.length; maxY++) {
			if (shotFrame[(int)maxY][(int)maxX] == BLACK_PIXEL) blackCount++; else blackCount = 0;
			if (blackCount == BORDER_WIDTH) break;
		}
		
		blackCount = 0;
		minY -= BORDER_WIDTH;
		double shotHeight = maxY - minY;
		double centerY = minY + (shotHeight / 2);
		
		for (;maxX < shotFrame[0].length; maxX++) {
			if (shotFrame[(int)centerY][(int)maxX] == BLACK_PIXEL) blackCount++; else blackCount = 0;
			if (blackCount == BORDER_WIDTH) break;
		}
		
		maxX -= BORDER_WIDTH;
		
		double shotWidth = maxX - minX;
		double centerX = minX + (shotWidth / 2);
		
		// If the width and height of the shot are really small it's a false positive
		if (shotWidth < MIN_SHOT_DIM && shotHeight < MIN_SHOT_DIM) {
			logger.debug("Suspected shot rejected: Dimensions Too Small "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, MIN_SHOT_DIM);
			return Optional.empty();
		}

		return Optional.of(new Point2D(centerX, centerY));
	}
}