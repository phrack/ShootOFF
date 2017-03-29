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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.shot.ShotColor;

public class PixelCluster extends HashSet<Pixel> {
	private static final Logger logger = LoggerFactory.getLogger(PixelCluster.class);

	private static final boolean debugColorsToFile = false;

	private static final long serialVersionUID = 1L;

	public double centerPixelX;
	public double centerPixelY;

	private final static double CURRENT_COLOR_BIAS_MULTIPLIER = .8;

	// We ignore fully connected pixels because they are not on the edges
	private final static int MAXIMUM_CONNECTEDNESS = 8;

	// We collect all the pixels AROUND the detected shot
	// Usually the pixels in the shot are max brightness which are biased green
	// So we look around the shot instead
	@SuppressWarnings("unused")
	public int getColorDifference(final Mat workingFrame, final int[][] colorDistanceFromRed) {

		Mat traceMat = null;
		if (logger.isTraceEnabled() && debugColorsToFile) {
			traceMat = Mat.zeros(workingFrame.size(), workingFrame.type());
		}

		final Map<Pixel, byte[]> visited = new HashMap<>();
		int avgSaturation = 0;
		int avgLum = 0;

		for (final Pixel pixel : this) {
			if (pixel.getConnectedness() < MAXIMUM_CONNECTEDNESS) {
				for (int h = -1; h <= 1; h++) {
					for (int w = -1; w <= 1; w++) {
						if (h == 0 && w == 0) continue;

						final int rx = pixel.x + w;
						final int ry = pixel.y + h;

						if (rx < 0 || ry < 0 || rx >= workingFrame.cols() || ry >= workingFrame.rows()) continue;

						final Pixel nearPoint = new Pixel(rx, ry);

						// && !this.contains(nearPoint)
						if (!visited.containsKey(nearPoint)) {
							final byte[] np = { 0, 0, 0 };
							workingFrame.get(ry, rx, np);

							final int npSaturation = np[1] & 0xFF;
							avgSaturation += npSaturation;

							final int npLum = np[2] & 0xFF;
							avgLum += npLum;

							visited.put(nearPoint, np);
						}
					}
				}
			}
		}

		final int pixelCount = visited.size();
		if (pixelCount == 0) return 0;

		avgSaturation /= pixelCount;
		avgLum /= pixelCount;

		int redSum = 0;
		int greenSum = 0;
		int colorDistance = 0;
		final int colorDistanceFromRedSum = 0;
		int avgColorDistance = 0;
		int tempColorDistance = 0;

		for (final Entry<Pixel, byte[]> pixelEntry : visited.entrySet()) {

			final byte[] np = pixelEntry.getValue();

			if (logger.isTraceEnabled() && debugColorsToFile) {

				System.out.println(String.format("x %d y %d pc %d - %d %d %d - %d - %d", (int) centerPixelX,
						(int) centerPixelY, pixelCount, np[0] & 0xFF, np[1], np[2] & 0xFF, avgSaturation, avgLum));
			}

			final int npSaturation = np[1] & 0xFF;
			final int npLum = np[2] & 0xFF;

			if (npSaturation > avgSaturation && npLum < avgLum) {
				final int npColor = np[0] & 0xFF;

				final int thisDFromRed = Math.min(npColor, Math.abs(180 - npColor)) * npLum * npSaturation;
				final int thisDFromGreen = Math.abs(60 - npColor) * npLum * npSaturation;

				redSum += thisDFromRed;
				greenSum += thisDFromGreen;

				final int currentCol = thisDFromRed - thisDFromGreen;

				final Pixel pixel = pixelEntry.getKey();

				// logger.trace("red {} green {} diff {} CDFR {}", thisDFromRed,
				// thisDFromGreen, currentCol,
				// colorDistanceFromRed[pixel.x][pixel.y]);

				colorDistance += currentCol
						- (int) (CURRENT_COLOR_BIAS_MULTIPLIER * colorDistanceFromRed[pixel.x][pixel.y]);

				if (logger.isTraceEnabled() && debugColorsToFile) {
					traceMat.put(pixelEntry.getKey().y, pixelEntry.getKey().x,
							workingFrame.get(pixelEntry.getKey().y, pixelEntry.getKey().x));

					// logger.trace("pixel cD {} cC {} cD {}", colorDistance,
					// currentCol, CURRENT_COLOR_BIAS_MULTIPLIER *
					// colorDistanceFromRed[pixel.x][pixel.y]);

					tempColorDistance += currentCol;
					avgColorDistance += colorDistanceFromRed[pixel.x][pixel.y];
				}
			}
		}

		if (logger.isTraceEnabled() && debugColorsToFile) {
			System.out.println(String.format("%d, %d, %d, %d, %d, %b", colorDistance / pixelCount,
					avgColorDistance / pixelCount, tempColorDistance / pixelCount, redSum / pixelCount,
					greenSum / pixelCount, colorDistance > 0));

			System.out.println(String.format("x %d y %d pc %d", (int) centerPixelX, (int) centerPixelY, pixelCount));

			final Mat testMat = new Mat();
			Imgproc.cvtColor(traceMat, testMat, Imgproc.COLOR_HSV2BGR);

			String filename = String.format("shot-colors-%d-%d.png", (int) centerPixelX, (int) centerPixelY);
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, testMat);
		}

		return colorDistance / pixelCount;
	}

	public Optional<ShotColor> getColor(final Mat workingFrame, final int[][] colorDistanceFromRed) {
		final int colorDist = getColorDifference(workingFrame, colorDistanceFromRed);

		// Sometimes it's better to guess than to return nothing
		if (colorDist < 1000)
			return Optional.of(ShotColor.RED);
		else
			return Optional.of(ShotColor.GREEN);
	}
}
