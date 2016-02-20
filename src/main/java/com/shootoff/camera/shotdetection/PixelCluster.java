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

import java.util.ArrayList;
import java.util.Optional;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelCluster extends java.util.ArrayList<Pixel> {

	private static final Logger logger = LoggerFactory.getLogger(PixelCluster.class);

	private static final long serialVersionUID = 1L;

	public double centerPixelX;
	public double centerPixelY;
	
	private final static double CURRENT_COLOR_BIAS_MULTIPLIER = 1.043;

	// We ignore fully connected pixels because they are not on the edges
	private final static int MAXIMUM_CONNECTEDNESS = 8;

	// We collect all the pixels AROUND the detected shot, not any in the shot
	// itself
	// Usually the pixels in the shot are max brightness which are biased green
	// So we look around the shot instead
	public double getColorDifference(Mat workingFrame, int[][] colorDistanceFromRed) {
		final ArrayList<Pixel> visited = new ArrayList<Pixel>();		
		int colorDistance = 0;
		int avgColorDistance = 0;
		int tempColorDistance = 0;
		
		int avgSaturation = 0;
		
		for (Pixel pixel : this) {
			if (pixel.getConnectedness() < MAXIMUM_CONNECTEDNESS) {
				for (int h = -1; h <= 1; h++)
					for (int w = -1; w <= 1; w++) {
						if (h == 0 && w == 0) continue;

						int rx = pixel.x + w;
						int ry = pixel.y + h;

						if (rx < 0 || ry < 0 || rx >= workingFrame.cols() || ry >= workingFrame.rows()) continue;

						Pixel nearPoint = new Pixel(rx, ry);
						
						if (!visited.contains(nearPoint) && !this.contains(nearPoint)) {
							
							byte[] np = { 0, 0, 0 };
							workingFrame.get(ry, rx, np);
							int npSaturation = np[1] & 0xFF;
							
							avgSaturation += npSaturation;
							
							visited.add(nearPoint);

						}
					}
			}
		}
		
		
		int pixelCount = visited.size();
		
		avgSaturation /= pixelCount;
				
		for (Pixel pixel : visited) {
			byte[] np = { 0, 0, 0 };

			workingFrame.get(pixel.y, pixel.x, np);
			int npColor = np[0] & 0xFF;
			int npSaturation = np[1] & 0xFF;
			int npLum =  np[2] & 0xFF;
			
			if (npSaturation > avgSaturation)
			{
				
				int currentCol = (int)(CURRENT_COLOR_BIAS_MULTIPLIER*(Math.min(npColor, Math.abs(180-npColor))*npLum*npSaturation));

				colorDistance += currentCol
						- colorDistanceFromRed[pixel.x][pixel.y];
				
				if (logger.isTraceEnabled())
				{
					tempColorDistance += currentCol;
					avgColorDistance += colorDistanceFromRed[pixel.x][pixel.y];
				}
			}
			
			//logger.trace("{} {} - pc {} - col {} sat {}>{} lum {} - {} {} {} - {}", pixel.x, pixel.y, pixelCount, npColor, npSaturation, avgSaturation, npLum, npSaturation>avgSaturation, 1.043*(Math.min(npColor, Math.abs(180-npColor))*npLum*npSaturation), colorDistanceFromRed[pixel.x][pixel.y], 1.043*(Math.min(npColor, Math.abs(180-npColor))*npLum*npSaturation) - colorDistanceFromRed[pixel.x][pixel.y]);

		}
		
		if (pixelCount == 0)
			return 0;
		
		if (logger.isTraceEnabled())
			logger.trace("Pixels {} Color {} avg {} sum {}", pixelCount, colorDistance/pixelCount, avgColorDistance/pixelCount, tempColorDistance/pixelCount);
		
		
		return colorDistance;
	}

	public Optional<javafx.scene.paint.Color> getColorJavafx(Mat workingFrame, int[][] colorDistanceFromRed) {
		final double colorDist = getColorDifference(workingFrame, colorDistanceFromRed);

		// Sometimes it's better to guess than to return nothing
		/*if (colorDist == 0)
			return Optional.empty();*/
		if (colorDist < 0)
			return Optional.of(javafx.scene.paint.Color.RED);
		else
			return Optional.of(javafx.scene.paint.Color.GREEN);
	}

}
