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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class ShotSearcher implements Runnable {
	public static final int SECTOR_COLUMNS = 3;
	public static final int SECTOR_ROWS = 3;

	private final Logger logger = LoggerFactory.getLogger(ShotSearcher.class);
	private final Configuration config;
	private final CanvasManager canvasManager;
	private final boolean[][] sectorStatuses;
	private final BufferedImage currentFrame;
	private final BufferedImage grayScale;
	private final Optional<Bounds> projectionBounds;
	private final boolean cropped;
	private final Optional<ShotRecorder> shotRecorder;

	private int borderWidth = 3; // px
	private int minShotDim = 6; // px

	public ShotSearcher(Configuration config, CanvasManager canvasManager, boolean[][] sectorStatuses,
			BufferedImage currentFrame, BufferedImage grayScale, Optional<Bounds> projectionBounds,
			boolean cropped, Optional<ShotRecorder> shotRecorder) {
		this.config = config;
		this.canvasManager = canvasManager;
		this.sectorStatuses = sectorStatuses;
		this.currentFrame = currentFrame;
		this.grayScale = grayScale;
		this.projectionBounds = projectionBounds;
		this.cropped = cropped;
		this.shotRecorder = shotRecorder;
	}

	public void setCenterApproxBorderSize(int width) {
		borderWidth = width;
	}

	public void setMinimumShotDimension(int minDim) {
		minShotDim = minDim;
	}

	@Override
	public void run() {
		// Split the image into x columns and y rows, and search
		// each independently
		int subWidth = grayScale.getWidth() / SECTOR_COLUMNS;
		int subHeight = grayScale.getHeight() / SECTOR_ROWS;

		for (int startY = 0, sectorY = 0; sectorY < SECTOR_ROWS;
				startY += subHeight, sectorY++) {
			for (int startX = 0, sectorX = 0; sectorX < SECTOR_COLUMNS;
					startX += subWidth, sectorX++) {

				// Don't detect a shot in a sector that is turned off
				if (sectorStatuses[sectorY][sectorX])
					findShot(startX, startX + subWidth, startY, startY + subHeight);
			}
		}
	}

	private void findShot(int startX, int endX, int startY, int endY) {
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				if ((grayScale.getRGB(x, y) & 0xFF) > config.getLaserIntensity()) {
					Optional<Color> areaColor = detectColor(x, y);
					if (areaColor.isPresent()) {
						if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent() &&
								areaColor.get().equals(config.getIgnoreLaserColor().get()))
									continue;

						Optional<Point2D> center = approximateCenter(x, y);

						if (center.isPresent()) {
							logger.debug("Suspected shot accepted: Original Coords ({}, {}), Center ({}, {}), {}",
									x, y, center.get().getX(),
									center.get().getY(), areaColor.get());

							if (shotRecorder.isPresent()) shotRecorder.get().recordShot();
							
							if (cropped && projectionBounds.isPresent()) {
								Bounds b = projectionBounds.get();
								canvasManager.addShot(areaColor.get(), center.get().getX() + b.getMinX(),
										center.get().getY() + b.getMinY(), shotRecorder);
							} else {
								canvasManager.addShot(areaColor.get(), center.get().getX(),
										center.get().getY(), shotRecorder);
							}
							return;
						}
					}
				}
			}
		}
	}
	
	private enum PixelColor {
		RED, GREEN, NONE
	}
	
	private PixelColor getPixelColor(int rgb) {
		float[] hsb = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
		
		boolean nearWhite = hsb[1] < 0.1 && hsb[2] > 0.9;
		boolean nearBlack = hsb[2] < 0.1;
		
		if (!nearWhite && !nearBlack) {
		    float deg = hsb[0]*360;
		    if (deg >= 0 && deg <  50) return PixelColor.RED;
		    else if (deg >=  90 && deg < 180) return PixelColor.GREEN;
		    else if (deg >= 200) return PixelColor.RED;
		}
		
		return PixelColor.NONE;
	}

	private Optional<Color> detectColor(int x, int y) {
		int redCount = 0;
		int greenCount = 0;
		
		// Count the colors of pixels in a small patch of pixels that should mostly be shot pixels
		int colorPatchWidth = minShotDim;
		if (x + minShotDim > currentFrame.getWidth()) {
			colorPatchWidth = currentFrame.getWidth() - x;
		}
		
		int colorPatchHeight = minShotDim;
		if (y + minShotDim > currentFrame.getHeight()) {
			colorPatchHeight = currentFrame.getHeight() - y;
		}
		
		int[] rgbs = currentFrame.getRGB(x, y, colorPatchWidth, colorPatchHeight, null, 0, minShotDim);
		
		for (int rgb : rgbs) {
			PixelColor c = getPixelColor(rgb);
			
			if (c == PixelColor.RED) {
				redCount++;
			} else if (c == PixelColor.GREEN) {
				greenCount++;
			}
		}
		
		// More than one pixel must be a specific color otherwise the shot is likely just noise
		if (Math.abs(redCount - greenCount) < 2) {
			logger.trace("Shot Processing: No color detected for suspected shot ({}, {}), "
					+ "redCount = {}, greenCount = {}",
					x, y, redCount, greenCount);
			return Optional.empty();
		} else if (redCount > greenCount) {
			logger.trace("Shot Processing: Detected red shot ({}, {}), redCount = {}, greenCount = {}",
					x, y, redCount, greenCount);
			return Optional.of(Color.RED);
		} else {
			logger.trace("Shot Processing: Detected green shot ({}, {}), redCount = {}, greenCount = {}",
					x, y, redCount, greenCount);
			return Optional.of(Color.GREEN);
		}
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
		double maxY = y;

		// We need to see a certain number of dark pixels because the shot
		// does not have sharp borders (we may hit a dark pixel right away
		// even though it's not the real edge otherwise)
		int blackCount = 0;

		for (;maxY < grayScale.getHeight(); maxY++) {
			if ((grayScale.getRGB((int)x, (int)maxY) & 0xFF) <= config.getLaserIntensity())
				blackCount++; else blackCount = 0;
			if (blackCount == borderWidth) break;
		}

		blackCount = 0;
		if (minY >= borderWidth) minY -= borderWidth;
		double shotHeight = maxY - minY;
		double centerY = minY + (shotHeight / 2);

		double shotWidth = 0;
		for (int yy = (int)minY; yy < maxY; yy++) {
			int xx = (int)minX;
			for (; xx < grayScale.getWidth(); xx++) {
				if ((grayScale.getRGB((int)xx, (int)yy) & 0xFF) <= config.getLaserIntensity())
					blackCount++; else blackCount = 0;
				if (blackCount == borderWidth) break;
			}
		
			if (xx > borderWidth) xx -= borderWidth;
			double width = xx - minX;
			if (width >= shotWidth && width < minShotDim) shotWidth = width;
		}

		double centerX = minX + (shotWidth / 2);
		
		// If the width and height of the shot are really small it's a false positive
		if ((shotWidth < minShotDim && shotHeight < minShotDim) || 
				shotWidth == 0 || shotHeight == 0) {
			logger.debug("Suspected shot rejected: Dimensions Too Small "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return Optional.empty();
			// Really big is bad too
		} else if (shotWidth > minShotDim * 3 || shotHeight > minShotDim * 3) {
			logger.debug("Suspected shot rejected: Dimensions Too big "
					+ "(x={}, y={}, width={} height={} min={})", x, y, shotWidth, shotHeight, minShotDim);
			return Optional.empty();
		}

		return Optional.of(new Point2D(centerX, centerY));
	}
}