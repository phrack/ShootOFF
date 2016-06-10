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
	
	private final static int US_LETTER_WIDTH_MM = 279;
	private final static int US_LETTER_HEIGHT_MM = 216;

	
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
	
	private int projectorResHeight = -1;
	private int projectorResWidth = -1;
	
	public PerspectiveManager()
	{
		
	}
	
	public void setCameraParams(double focalLength, double sensorWidth, double sensorHeight)
	{
		logger.trace("camera params fl {} sw {} sh {}", focalLength, sensorWidth, sensorHeight);
		
		this.focalLength = focalLength;
		this.sensorHeight = sensorHeight;
		this.sensorWidth = sensorWidth;
	}
	
	/* The real world width and height of the projector arena in the camera feed (in mm) */
	public void setProjectionSize(int width, int height)
	{
		logger.trace("projection w {} h {}", width, height);
		
		this.projectionHeight = height;
		this.projectionWidth = width;
	}
	
	/* Specify (or find with OpenCV) the number of camera pixels that are represent
	 *  a U.S. standard letter, which is 8.5 x 11 inches or 216 x 279mm
	 *  We assume that the paper is placed sideways! We could probably adjust for this though */
	public void setProjectionSizeFromLetterPaperPixels(int lwidth, int lheight)
	{
		logger.trace("letter w {} h {}", lwidth, lheight);
		
		if (cameraWidth == -1 || patternWidth == -1)
		{
			logger.error("Missing cameraWidth or patternWidth for US Letter calculation");
			return;
		}
		
		// Calculate the size of the whole camera feed using the size of the letter
		double cameraFeedWidthMM = ((double)cameraWidth / (double)lwidth) * US_LETTER_WIDTH_MM;
		double cameraFeedHeightMM = ((double)cameraHeight / (double)lheight) * US_LETTER_HEIGHT_MM;
		
		// Set the projection width/height in mm
		projectionWidth = (int) (cameraFeedWidthMM * ((double)patternWidth / (double)cameraWidth));
		projectionHeight = (int) (cameraFeedHeightMM * ((double)patternHeight / (double)cameraHeight));
	}
	
	
	
	/* The camera feed width and height (in px) */
	public void setCameraFeedSize(int width, int height)
	{
		logger.trace("camera feed w {} h {}", width, height);
		this.cameraHeight = height;
		this.cameraWidth = width;
	}
	
	/* The pattern feed width and height (in px) */
	public void setPatternSize(int width, int height)
	{
		logger.trace("pattern res w {} h {}", width, height);
		this.patternHeight = height;
		this.patternWidth = width;
	}
	
	
	
	/* Distance (in mm) camera to screen */
	public void setCameraDistance(int cameraDistance)
	{
		logger.trace("cameraDistance {}", cameraDistance);
		this.cameraDistance = cameraDistance;
	}
	
	/* Distance (in mm) camera to shooter */
	public void setShooterDistance(int shooterDistance)
	{
		logger.trace("shooterDistance {}", shooterDistance);
		this.shooterDistance = shooterDistance;
	}
	
	/* The resolution of the screen the arena is projected on
	 * Due to DPI scaling this might not correspond to the projector's
	 * resolution, but it is easier to think of that way */
	public void setProjectorResolution(int width, int height)
	{
		logger.trace("projector res w {} h {}", width, height);
		this.projectorResWidth = width;
		this.projectorResHeight = height;
	}
	
	public int getProjectionWidth()
	{
		return projectionWidth;
	}
	public int getProjectionHeight()
	{
		return projectionHeight;
	}
	
	public double getFocalLength()
	{
		return focalLength;
	}
	
	public double getSensorWidth()
	{
		return sensorWidth;
	}
	public double getSensorHeight()
	{
		return sensorHeight;
	}
	
	public void calculateUnknown()
	{
		
		int unknownCount = 0;
			
			
		double wValues[] = { focalLength, patternWidth, cameraWidth, projectionWidth, sensorWidth, cameraDistance, projectorResWidth };
		
		for (int i = 0; i < wValues.length; i++)
		{
			if (wValues[i] == -1)
			{
				logger.trace("Unknown: {}", i);
				unknownCount++;
				

			}
		}
		
		// We're okay with two unknowns if they're these two.
		if (unknownCount > 1 && (focalLength != -1 && sensorWidth != -1))
		{
			logger.error("More than one unknown");
			return;
		}	
		else if (unknownCount == 0)
		{
			logger.error("No unknown found");
			return;
		}
		
		if (projectionWidth == -1)
		{
			projectionWidth = (int) (((double)cameraDistance * (double)patternWidth * sensorWidth) / (focalLength * (double)cameraWidth)); 
			projectionHeight = (int) (((double)cameraDistance * (double)patternHeight * sensorHeight) / (focalLength * (double)cameraHeight));

			logger.trace("({} *  {} * {}) / ({} * {})", cameraDistance, patternWidth, sensorWidth, focalLength, cameraWidth);
			
			

		}
		else if (sensorWidth == -1)
		{
			// Fix focalLength at 1 since we do not know it and we can only calculate 1 unknown
			if (focalLength == -1)
				focalLength = 1;
			
			sensorWidth = ((projectionWidth * focalLength * (double)cameraWidth) / ((double)cameraDistance * (double)patternWidth)); 
			sensorHeight = ((projectionHeight * focalLength * (double)cameraHeight) / ((double)cameraDistance * (double)patternHeight)); 

			logger.trace("({} *  {} * {}) / ({} * {})", cameraDistance, patternWidth, sensorWidth, focalLength, cameraWidth);
		}
		else
		{
			logger.error("Unknown not supported");
			return;
		}
		
		pxPerMMwide = ((double)projectorResWidth / (double)projectionWidth);
		pxPerMMhigh = ((double)projectorResHeight / (double)projectionHeight);
		
		logger.trace("pW {} pH {} - pxW {} pxH {}", projectionWidth, projectionHeight, pxPerMMwide, pxPerMMhigh);
	}
	
	// Parameters in mm, return in px
	public Pair<Double, Double> calculateObjectSize(double realWidth, double realHeight, double realDistance, double desiredDistance)
	{
		if (projectionWidth == -1 || projectionHeight == -1 || shooterDistance == -1)
		{
			logger.error("projectionWidth or projectionHeight or shooterDistance unknown");
			return new Pair<Double, Double>(-1.0, -1.0);
		}
	
		// Make it appropriate size for the shooter
		double distRatio = realDistance / shooterDistance;
		
		// Make it appropriate size for the desired distance
		distRatio *= cameraDistance / desiredDistance;
		
		double adjWidthmm = realWidth * distRatio;
		double adjHeightmm = realHeight * distRatio;
		
		double adjWidthpx = adjWidthmm * pxPerMMwide;
		double adjHeightpx = adjHeightmm * pxPerMMhigh;
		
		logger.trace("rD {} dD {} sD {} dR {} - adjmm {} {} adjpx {} {}", realDistance, desiredDistance, shooterDistance, distRatio,
				adjWidthmm, adjHeightmm, adjWidthpx, adjHeightpx);
		
		return new Pair<Double, Double>(adjWidthpx, adjHeightpx);
	}
	

}
