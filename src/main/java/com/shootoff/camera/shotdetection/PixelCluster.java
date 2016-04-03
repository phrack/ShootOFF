/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

package com.shootoff.camera.shotdetection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.paint.Color;

public class PixelCluster extends HashSet<Pixel> {
	private static final Logger logger = LoggerFactory.getLogger(PixelCluster.class);

	private static final long serialVersionUID = 1L;

	public double centerPixelX;
	public double centerPixelY;

	private final static double CURRENT_COLOR_BIAS_MULTIPLIER = .5;

	// We ignore fully connected pixels because they are not on the edges
	private final static int MAXIMUM_CONNECTEDNESS = 8;

	// We collect all the pixels AROUND the detected shot, not any in the shot
	// itself
	// Usually the pixels in the shot are max brightness which are biased green
	// So we look around the shot instead
	public double getColorDifference(final Mat workingFrame, final int[][] colorDistanceFromRed) {
		final Map<Pixel, byte[]> visited = new HashMap<Pixel, byte[]>();
		int avgSaturation = 0;

		for (final Pixel pixel : this) {
			if (pixel.getConnectedness() < MAXIMUM_CONNECTEDNESS) {
				for (int h = -1; h <= 1; h++) {
					for (int w = -1; w <= 1; w++) {
						if (h == 0 && w == 0) continue;

						final int rx = pixel.x + w;
						final int ry = pixel.y + h;

						if (rx < 0 || ry < 0 || rx >= workingFrame.cols() || ry >= workingFrame.rows()) continue;

						final Pixel nearPoint = new Pixel(rx, ry);

						if (!visited.containsKey(nearPoint) && !this.contains(nearPoint)) {
							byte[] np = { 0, 0, 0 };
							workingFrame.get(ry, rx, np);
							final int npSaturation = np[1] & 0xFF;

							avgSaturation += npSaturation;

							visited.put(nearPoint, np);
						}
					}
				}
			}
		}

		final int pixelCount = visited.size();
		if (pixelCount == 0) return 0;

		avgSaturation /= pixelCount;

		int colorDistance = 0;
		int avgColorDistance = 0;
		int tempColorDistance = 0;

		for (final Entry<Pixel, byte[]> pixelEntry : visited.entrySet()) {
			byte[] np = pixelEntry.getValue();
			final int npSaturation = np[1] & 0xFF;

			if (npSaturation > avgSaturation) {
				final int npColor = np[0] & 0xFF;
				final int npLum = np[2] & 0xFF;

				final int thisDFromRed = Math.min(npColor, Math.abs(180 - npColor)) * npLum * npSaturation;
				final int thisDFromGreen = Math.abs(60 - npColor) * npLum * npSaturation;

				final int currentCol = thisDFromRed - thisDFromGreen;

				final Pixel pixel = pixelEntry.getKey();
				colorDistance += currentCol
						- (int) (CURRENT_COLOR_BIAS_MULTIPLIER * colorDistanceFromRed[pixel.x][pixel.y]);

				if (logger.isTraceEnabled()) {
					tempColorDistance += currentCol;
					avgColorDistance += colorDistanceFromRed[pixel.x][pixel.y];
				}
			}
		}

		if (logger.isTraceEnabled()) logger.trace("Pixels {} Color {} avg {} sum {}", pixelCount,
				colorDistance / pixelCount, avgColorDistance / pixelCount, tempColorDistance / pixelCount);

		return colorDistance;
	}

	public Optional<Color> getColor(final Mat workingFrame, final int[][] colorDistanceFromRed) {
		final double colorDist = getColorDifference(workingFrame, colorDistanceFromRed);

		// Sometimes it's better to guess than to return nothing
		if (colorDist < 0)
			return Optional.of(Color.RED);
		else
			return Optional.of(Color.GREEN);
	}
}
