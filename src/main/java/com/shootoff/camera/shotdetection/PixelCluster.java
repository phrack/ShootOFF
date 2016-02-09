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


package com.shootoff.camera.shotdetection;

import java.awt.image.BufferedImage;
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
	

	private final static double COLOR_THRESHOLD_PER_PIXEL = .000391;

	// We ignore fully connected pixels because they are not on the edges
	private final static int MAXIMUM_CONNECTEDNESS = 8;

	// We collect all the pixels AROUND the detected shot, not any in the shot
	// itself
	// Usually the pixels in the shot are max brightness which are biased green
	// So we look around the shot instead
	public double getColorDifference(Mat workingFrame, int[][] colorAngleMovingAverage, int[][] colorChromaMovingAverage) {
		final ArrayList<Pixel> visited = new ArrayList<Pixel>();

		double avgSin = 0;
		double avgCos = 0;
		int pixelCount = 0;
		
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

							double npColor = (workingFrame.get(ry, rx)[0] * (Math.PI/90.0));
							int npSaturation = (int) workingFrame.get(ry,rx)[1];
							int npLum = (int) workingFrame.get(ry,rx)[2];
							double npChroma = (((double)npSaturation/255.0)*((double)npLum/255.0));
							
							double cAMARadians = colorAngleMovingAverage[rx][ry] * (Math.PI/90);
							
							double sin = (((npChroma * Math.sin(npColor) * 1.0 - (double)colorChromaMovingAverage[rx][ry]/255.0 * Math.sin(cAMARadians * 1.0))) / 2.0);
							double cos = (((npChroma * Math.cos(npColor) * 1.0 - (double)colorChromaMovingAverage[rx][ry]/255.0 * Math.cos(cAMARadians * 1.0))) / 2.0);

							//logger.warn("{} {} np {} {} {} {} - {} {} - {} {}", rx, ry, npColor, npSaturation, npLum, npChroma, colorChromaMovingAverage[rx][ry]/255.0, cAMARadians, sin, cos);
							//logger.warn("{} {} np {} {} - {} {}", rx, ry, workingFrame.get(ry, rx)[0], npChroma, colorAngleMovingAverage[rx][ry], colorChromaMovingAverage[rx][ry]/255.0);
							
							
							avgSin += sin;
							avgCos += cos;
							pixelCount++;
							
							visited.add(nearPoint);

							if (logger.isTraceEnabled())
								logger.trace("Visiting pixel {} {} - {} - {}", rx, ry, npColor,
										cAMARadians, colorChromaMovingAverage[rx][ry]);
						}
					}
			}
		}
		
		double resultAngle = Math.atan2(avgSin/(double)pixelCount, avgCos/(double)pixelCount);
		resultAngle = (resultAngle / Math.PI*180) + (resultAngle > 0 ? 0 : 360);
		
		logger.warn("Done visiting - {} - {} {} - {}", pixelCount, avgSin, avgCos, resultAngle);

		//red
		if (resultAngle < 70 || resultAngle > 240)
			return -1;
		//green
		else
			return 1;
	}

	public Optional<javafx.scene.paint.Color> getColorJavafx(Mat workingFrame, int[][] colorDiffMovingAverage, int[][] colorChromaMovingAverage) {
		final double colorDist = getColorDifference(workingFrame, colorDiffMovingAverage, colorChromaMovingAverage);
		
		double colorThreshold = (workingFrame.cols() * workingFrame.rows() * COLOR_THRESHOLD_PER_PIXEL);

		//if (logger.isDebugEnabled())
			logger.warn("getcolorjavafx {} {} - {} - {}", centerPixelX, centerPixelY, colorDist, (colorDist < 0));
		
		

		//if (Math.abs(colorDist) < colorThreshold) {
		//	return Optional.empty();
		if (colorDist < 0) {
			return Optional.of(javafx.scene.paint.Color.RED);
		} else {
			return Optional.of(javafx.scene.paint.Color.GREEN);
		}
	}

}
