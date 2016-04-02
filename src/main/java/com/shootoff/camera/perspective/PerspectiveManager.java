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

package com.shootoff.camera.perspective;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;

/*
 * 
 * distance to object (mm) =
 * 
 *  focal length (mm) * real height of the object (mm) * image height (pixels)
 * ---------------------------------------------------------------------------
 *                  object height (pixels) * sensor height (mm)
 * 
 */




public class PerspectiveManager {
	private static final Logger logger = LoggerFactory.getLogger(PerspectiveManager.class);
	
	// Should be put in a resource file
	public final static double C270_FOCAL_LENGTH = 4.0;
	public final static double C270_SENSOR_WIDTH = 3.58;
	public final static double C270_SENSOR_HEIGHT = 2.02;
	
	// All in millimeters
	private double focalLength = -1;
	private double sensorHeight = -1;
	private double sensorWidth = -1;
	private int cameraDistance = -1;
	private int shooterDistance = -1;
	
	private double pxPerMMhigh = -1;
	private double pxPerMMwide = -1;
	
	// All in pixels
	private int projectionHeight = -1;
	private int projectionWidth = -1;
	private int cameraHeight = -1;
	private int cameraWidth = -1;
	private int patternHeight = -1;
	private int patternWidth = -1;
	
	public PerspectiveManager()
	{
		
	}
	
	public void setCameraParams(double focalLength, double sensorWidth, double sensorHeight)
	{
		this.focalLength = focalLength;
		this.sensorHeight = sensorHeight;
		this.sensorWidth = sensorWidth;
	}
	
	/* The width and height of the projector arena in the camera feed */
	public void setProjectionSize(int width, int height)
	{
		this.projectionHeight = height;
		this.projectionWidth = width;
	}
	
	/* The camera feed width and height */
	public void setCameraFeedSize(int width, int height)
	{
		this.cameraHeight = height;
		this.cameraWidth = width;
	}
	
	/* The pattern feed width and height */
	public void setPatternSize(int width, int height)
	{
		this.patternHeight = height;
		this.patternWidth = width;
	}
	
	
	
	/* Distance (in mm) camera to screen */
	public void setCameraDistance(int cameraDistance)
	{
		this.cameraDistance = cameraDistance;
	}
	
	/* Distance (in mm) camera to shooter */
	public void setShooterDistance(int shooterDistance)
	{
		this.shooterDistance = shooterDistance;
	}
	
	public int getProjectionWidth()
	{
		return projectionWidth;
	}
	public int getProjectionHeight()
	{
		return projectionHeight;
	}
	
	public void calculateUnknown()
	{
		
		double wValues[] = { focalLength, patternWidth, cameraWidth, projectionWidth, sensorWidth };
		
		boolean foundUnknown = false;
		for (int i = 0; i < wValues.length; i++)
		{
			if (wValues[i] == -1)
			{
				if (foundUnknown)
				{
					logger.error("More than one unknown");
					return;
				}
				foundUnknown = true;
			}
		}
		
		if (!foundUnknown)
		{
			logger.error("No unknown found");
			return;
		}
		
		if (projectionWidth == -1)
		{
			projectionWidth = (int) ((focalLength * patternWidth * cameraWidth) / (cameraDistance * sensorWidth)); 
			projectionHeight = (int) ((focalLength * patternHeight * cameraHeight) / (cameraDistance * sensorHeight)); 
			
			pxPerMMwide = ((double)cameraWidth / (double)projectionWidth) * (1/sensorWidth);
			pxPerMMhigh = ((double)cameraHeight / (double)projectionHeight) * (1/sensorHeight);
			
			logger.debug("pW {} pH {} - pxW {} pxH {}", projectionWidth, projectionHeight, pxPerMMwide, pxPerMMhigh);
		}
	}
	
	// Parameters in mm, return in px
	public Pair<Double, Double> calculateObjectSize(double realWidth, double realHeight, double realDistance, double desiredDistance)
	{
		if (projectionWidth == -1 || projectionHeight == -1 || shooterDistance == -1)
		{
			logger.error("projectionWidth or projectionHeight or shooterDistance unknown");
			return new Pair<Double, Double>(-1.0, -1.0);
		}
				
		double distRatio = realDistance / desiredDistance;
		distRatio *= shooterDistance / desiredDistance;
		
		double adjWidthmm = realWidth * distRatio;
		double adjHeightmm = realHeight * distRatio;
		
		double adjWidthpx = adjWidthmm * pxPerMMwide;
		double adjHeightpx = adjHeightmm * pxPerMMhigh;
		
		logger.debug("dD/rD {} sD/dD {} dR {} - adjmm {} {} adjpx {} {}", desiredDistance/realDistance, shooterDistance/desiredDistance, distRatio,
												adjWidthmm, adjHeightmm, adjWidthpx, adjHeightpx);
		
		return new Pair<Double, Double>(adjWidthpx, adjHeightpx);
	}
	

}
