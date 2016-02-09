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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import javafx.geometry.Bounds;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public final class ShotDetectionManager {

	public static final int SECTOR_COLUMNS = 3;
	public static final int SECTOR_ROWS = 3;

	private static final Logger logger = LoggerFactory.getLogger(ShotDetectionManager.class);

	private CanvasManager canvasManager;
	private CameraManager cameraManager;

	private Configuration config;

	private boolean filtersInitialized = false;

	
	private int[][] lumsMovingAverage;
	private int[][] colorAngleMovingAverage;
	private int[][] colorChromaMovingAverage;

	private int avgThresholdPixels = -1;

	private final static int INIT_FRAME_COUNT = 5;
	private int movingAveragePeriod = INIT_FRAME_COUNT;

	private final static int MOTION_WARNING_FRAMECOUNT = 30;
	private int MOTION_WARNING_AVG_THRESHOLD = 100;
	private int MOTION_WARNING_THRESHOLD_PIXELS = 600;
	private int MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG = 600;

	// Individual pixel threshold
	private final static  int EXCESSIVE_BRIGHTNESS_THRESHOLD = 245;
	private final static  int MINIMUM_BRIGHTNESS_INCREASE = 25;

	// Aggregate # of pixel threshold
	private int BRIGHTNESS_WARNING_AVG_THRESHOLD = 100;
	private final static int BRIGHTNESS_WARNING_FRAMECOUNT = 90;

	private int MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = 300;


	private int MINIMUM_SHOT_DIMENSION = 6;

	// This is updated for every bright pixel
	ArrayList<Pixel> brightPixels = new ArrayList<Pixel>();

	// The average is then calculated here
	private int avgBrightPixels = -1;

	// This is a short circuit for our pixel-color-changer to set the bad pixels
	// red
	// without having complicated math every pixel
	private boolean shouldShowBrightnessWarningBool = false;
	private Mat currentFullFrame;
	


	public ShotDetectionManager(CameraManager cameraManager, Configuration config, CanvasManager canvasManager) {
		((ch.qos.logback.classic.Logger)
		logger).setLevel(ch.qos.logback.classic.Level.DEBUG);

		this.canvasManager = canvasManager;
		this.cameraManager = cameraManager;
		this.config = config;

		initializeDimensions(cameraManager.getFeedWidth(), cameraManager.getFeedHeight());
	}
	
	public void reInitializeDimensions()
	{
		initializeDimensions(cameraManager.getFeedWidth(), cameraManager.getFeedHeight());
	}
	
	private void initializeDimensions(int width, int height)
	{
		lumsMovingAverage = new int[width][height];
		colorAngleMovingAverage = new int[width][height];
		colorChromaMovingAverage = new int[width][height];

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++) {
				lumsMovingAverage[x][y] = -1;
				colorAngleMovingAverage[x][y] = -1;
				colorChromaMovingAverage[x][y] = -1;
			}
		}
		
		double frameSize = width*height;
		
		MOTION_WARNING_AVG_THRESHOLD = (int) (frameSize * .000325);
		MOTION_WARNING_THRESHOLD_PIXELS = (int) (frameSize * 0.00195);
		MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG = (int) (frameSize * 0.00195);

		// Aggregate # of pixel threshold
		BRIGHTNESS_WARNING_AVG_THRESHOLD = (int) (frameSize * .000325);

		MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = (int) (frameSize * .000976);

		MINIMUM_SHOT_DIMENSION = (int) (frameSize * .000025);
		
		logger.warn("MINIMUM_SHOT_DIMENSION {}", MINIMUM_SHOT_DIMENSION);
		
		
		
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	private Optional<Pixel> updateFilter(Mat workingFrame, int x, int y, boolean detectShots) {

		
		
		Optional<Pixel> result = Optional.empty();
		int currentH = (int) workingFrame.get(y,x)[0];
		double currentHRadians = workingFrame.get(y,x)[0] * (Math.PI/90);
		int currentS = (int) workingFrame.get(y,x)[1];
		int currentV = (int) workingFrame.get(y,x)[2];
		int currentChroma = (int)
				(
				 ((double)currentS/255.0) *
				 ((double)currentV/255.0) *
				 255.0
				);
		
		int currentLum = (int) 
				(
						(1.0-(double)(currentS/255.0))*
						((double)(currentV/255.0))*
						255.0
				);

		if (lumsMovingAverage[x][y] == -1) {
			
			lumsMovingAverage[x][y] = Math.min(currentLum, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
			colorAngleMovingAverage[x][y] = currentH;
			colorChromaMovingAverage[x][y] = currentChroma;

			return result;

		}

		
		
		double valueForThreshold = currentLum;
		

		if (cameraManager.curFrameMask != null)
		{
			valueForThreshold = cameraManager.curFrameMask.get(y,x)[0]; 
			
		}

		
		if (!detectShots) result = Optional.empty();
		else if (pixelAboveExcessiveBrightnessThreshold(lumsMovingAverage[x][y])) {
			brightPixels.add(new Pixel(x, y));


		}

		else if (pixelAboveThreshold(valueForThreshold,lumsMovingAverage[x][y])) result = Optional
				.of(new Pixel(x, y, currentH, valueForThreshold, lumsMovingAverage[x][y], colorAngleMovingAverage[x][y]));

		
		// Update the average brightness
		lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (movingAveragePeriod - 1)) + currentLum)
				/ movingAveragePeriod;

		// Update the color angle
		double sin = 
				(
				 (
						(
						 ((double)currentChroma/255.0 * Math.sin(currentHRadians)) +
						 ((double)(movingAveragePeriod - 1) * (double)colorChromaMovingAverage[x][y]/255.0 * Math.sin((double)colorAngleMovingAverage[x][y]*(Math.PI/180)))
						)
				 ) /
				 (double)movingAveragePeriod
				);
		double cos = 
				(
				 (
				  (
				   ((double)currentChroma/255.0 * Math.cos(currentHRadians)) +
				   ((double)(movingAveragePeriod - 1) * (double)colorChromaMovingAverage[x][y]/255.0 * Math.cos((double)colorAngleMovingAverage[x][y]*(Math.PI/180)))
				  )
				 ) / 
				 (double)movingAveragePeriod
				);
		double angle = Math.atan2(sin, cos);
		angle = (angle / Math.PI*90) + (angle > 0 ? 0 : 180);

		colorAngleMovingAverage[x][y] = (int)angle;
		
		// Update the color chroma
		colorChromaMovingAverage[x][y] = ((colorChromaMovingAverage[x][y] * (movingAveragePeriod - 1)) + currentChroma)
				/ movingAveragePeriod;

		/*if (x==140&&y==189)
		{	
			logger.debug("P1 {} {} {}", sin, cos, angle);
			logger.debug("Pixel {} {} {} {} {} - {} - {} - {} - {} {}", currentH, currentS, currentV, currentLum, currentChroma, valueForThreshold, currentLum-valueForThreshold, angle, colorAngleMovingAverage[x][y], colorChromaMovingAverage[x][y]);
		}*/

		
		return result;

	}

	// This function ADJUSTS X AND Y FOR LIMITING DETECTION BOUNDS
	private void drawOnCurrentFrame(int x, int y, int rgb) {
		int drawX = x;
		int drawY = y;
		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {
			drawX = (int) (cameraManager.getProjectionBounds().get().getMinX() + x);
			drawY = (int) (cameraManager.getProjectionBounds().get().getMinY() + y);
		}
		//TODO: currentFullFrame.setRGB(drawX, drawY, rgb);
	}

	/*private void deepCopy(int[][] source, int[][] destination) {
		if (!cameraManager.isDetecting()) return;

		Parallel.forIndex(0, source.length, 1, new Operation<Integer>() {
			public void perform(Integer i) {
				final int[] aMatrix = source[i];
				final int aLength = aMatrix.length;
				System.arraycopy(aMatrix, 0, destination[i], 0, aLength);
			}
		});
	}

	private void deepCopy(double[][] source, double[][] destination) {
		if (!cameraManager.isDetecting()) return;

		Parallel.forIndex(0, source.length, 1, new Operation<Integer>() {
			public void perform(Integer i) {
				double[] aMatrix = source[i];
				int aLength = aMatrix.length;
				System.arraycopy(aMatrix, 0, destination[i], 0, aLength);
			}
		});
	}*/


	private boolean pixelAboveExcessiveBrightnessThreshold(int lumsMovingAverage) {
		return lumsMovingAverage > EXCESSIVE_BRIGHTNESS_THRESHOLD;
	}

	private boolean pixelAboveThreshold(double currentLum, double lumsMovingAverage) {
		final double threshold = ((255.0 - lumsMovingAverage) / 4.0);
		final double increase = (currentLum - lumsMovingAverage);

		double dynamic_increase = ((255.0 - threshold)
				* (avgThresholdPixels / MAXIMUM_THRESHOLD_PIXELS_FOR_AVG));


		double dynamic_threshold = threshold + dynamic_increase;

		if (increase < MINIMUM_BRIGHTNESS_INCREASE) return false;

		if (increase < dynamic_threshold) return false;

		return true;

	}

	public BufferedImage deepCopy(BufferedImage source) {
		BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = b.getGraphics();
		boolean drawImageComplete = g.drawImage(source, 0, 0, null);

		if (!drawImageComplete) {

			if (logger.isErrorEnabled()) logger.error("deepCopy drawImageComplete false");
		}
		g.dispose();
		return b;
	}


	public void processFrame(Mat frame, boolean detectShots) {
		movingAveragePeriod = Math.max((int)(cameraManager.getFPS()/5.0),INIT_FRAME_COUNT);
		
		// This is the FULL, ORIGINAL FRAME passed from CameraManager
		currentFullFrame = frame;

		Mat workingFrame = null;

		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {
			Bounds b = cameraManager.getProjectionBounds().get();
			Mat subFrame = frame.submat((int) b.getMinY(), (int) b.getMaxY(),
					(int) b.getMinX(), (int) b.getMaxX());
			workingFrame = subFrame;

		} else {
			workingFrame = frame;


		}
		
		// Must reset before every updateFilter loop
		brightPixels.clear();


		List<Pixel> thresholdPixels = findThresholdPixelsAndUpdateFilter(workingFrame,
				(detectShots && filtersInitialized));
		
		int thresholdPixelsSize = thresholdPixels.size();

		if (logger.isTraceEnabled()) {
			if (thresholdPixelsSize >= 1) logger.trace("thresholdPixels {} getMinimumShotDimension {}",
					thresholdPixelsSize, getMinimumShotDimension());

			for (Pixel pixel : thresholdPixels) {
				logger.trace("thresholdPixel {} {} - from array {} from pixel cur {} avg {}", pixel.x, pixel.y,
						lumsMovingAverage[pixel.x][pixel.y], pixel.getCurrentLum(), pixel.getLumAverage());
			}
		}


		if (!filtersInitialized) filtersInitialized = checkIfInitialized();
		
		//if (thresholdPixelsSize >= 1) logger.debug("detectShots {} filtersInitialized {}", detectShots, filtersInitialized);

		if (detectShots && filtersInitialized) {
			updateAvgThresholdPixels(thresholdPixelsSize);

			updateAvgBrightPixels(brightPixels.size());

			if (shouldShowBrightnessWarning()) {
				cameraManager.showBrightnessWarning();

			}

			if (thresholdPixelsSize >= getMinimumShotDimension() && !isExcessiveMotion(thresholdPixelsSize))
			{

				ArrayList<PixelCluster> clusters = clusterPixels(thresholdPixels);

				if (logger.isTraceEnabled())
				{
					logger.trace("thresholdPixels {}", thresholdPixelsSize);
					logger.trace("clusters {}", clusters.size());
				}
				
				detectShots(workingFrame, clusters);
			}
			
			// Moved to after detectShots because otherwise we'll have changed pixels in the frame that's being checked for shots
			else if (isExcessiveMotion(thresholdPixelsSize)) {
				if (shouldShowMotionWarning(thresholdPixelsSize)) cameraManager.showMotionWarning();
				
				for (Pixel pixel : thresholdPixels) {
					drawOnCurrentFrame(pixel.x, pixel.y, 0x0000FF);
				}
			}
			
			if (shouldShowBrightnessWarningBool && brightPixels.size() > 0)
			{
				// Make the feed pixels red so the user can easily see what the
				// problem pixels are
				for (Pixel pixel : brightPixels) {
						drawOnCurrentFrame(pixel.x, pixel.y, 0xFF0000);
				}
			}
			
		}
	}

	private ArrayList<PixelCluster> clusterPixels(List<Pixel> thresholdPixels) {
		PixelClusterManager pixelClusterManager = new PixelClusterManager(thresholdPixels, this);
		pixelClusterManager.clusterPixels();
		ArrayList<PixelCluster> clusters = pixelClusterManager.dumpClusters();

		return clusters;
	}

	private void detectShots(Mat workingFrame, ArrayList<PixelCluster> clusters) {
		for (PixelCluster cluster : clusters) {
			addShot(workingFrame, cluster);
		}
	}

	private boolean isExcessiveMotion(int thresholdPixels) {

		if (thresholdPixels > MOTION_WARNING_THRESHOLD_PIXELS
				|| avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD) return true;

		return false;
	}

	private boolean shouldShowMotionWarning(int thresholdPixels) {

		if (avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > MOTION_WARNING_FRAMECOUNT) {
			logger.trace("HIGH MOTION - avgThresholdPixels {} thresholdPixels {}", avgThresholdPixels, thresholdPixels);

			return true;
		}
		return false;
	}

	private boolean shouldShowBrightnessWarning() {

		if (logger.isTraceEnabled()) logger.trace("avgBrightPixels {}", avgBrightPixels);

		if (avgBrightPixels >= BRIGHTNESS_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > BRIGHTNESS_WARNING_FRAMECOUNT) {
			logger.trace("HIGH BRIGHTNESS - avgBrightPixels {}", avgBrightPixels);

			shouldShowBrightnessWarningBool = true;

			return true;
		}

		shouldShowBrightnessWarningBool = false;
		return false;
	}

	private boolean checkIfInitialized() {
		if (cameraManager.getFrameCount() > INIT_FRAME_COUNT) return true;

		return false;
	}

	private List<Pixel> findThresholdPixelsAndUpdateFilter(Mat workingFrame, boolean detectShots) {
		final int subWidth = workingFrame.cols() / SECTOR_COLUMNS;
		final int subHeight = workingFrame.rows() / SECTOR_ROWS;


		List<Pixel> thresholdPixels = Collections.synchronizedList( new ArrayList<Pixel>() );


		if (!cameraManager.isDetecting()) return thresholdPixels;
		
		// In this loop we accomplish both MovingAverage updates AND threshold
		// pixel detection
		Parallel.forIndex(0, (SECTOR_ROWS * SECTOR_COLUMNS), 1, new Operation<Integer>() {

			public void perform(Integer sector) {
				int sectorX = (sector.intValue() % SECTOR_COLUMNS);
				int sectorY = sector.intValue() / SECTOR_ROWS;

				int startX = subWidth * sectorX;
				int startY = subHeight * sectorY;
				
				if (!cameraManager.isSectorOn(sectorX, sectorY)) return;
				
				for (Integer y = startY; y < startY + subHeight; y++) {
					for (Integer x = startX; x < startX + subWidth; x++) {
						Optional<Pixel> pixel = updateFilter(workingFrame, x, y, detectShots);
						
						if (pixel.isPresent())
								thresholdPixels.add(pixel.get());
					}
				}				
			}
		});
		
		return thresholdPixels;
	}


	private void updateAvgThresholdPixels(int thresholdPixels) {
		if (avgThresholdPixels == -1)
			avgThresholdPixels = Math.min(thresholdPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
		{
			int fps = (int)cameraManager.getFPS();
			avgThresholdPixels = (((fps - 1) * avgThresholdPixels)
					+ Math.min(thresholdPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG)) / fps;
		}

	}

	private void updateAvgBrightPixels(int brightPixels) {
		if (avgBrightPixels == -1)
			avgBrightPixels = Math.min(brightPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
		{
			int fps = (int)cameraManager.getFPS();
			avgBrightPixels = (((fps - 1) * avgBrightPixels)
					+ Math.min(brightPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG)) / fps;
		}
	}

	public int getMinimumShotDimension() {
		if (cameraManager.getMinimumShotDimension().isPresent()) {
			return cameraManager.getMinimumShotDimension().get();
		}
		return MINIMUM_SHOT_DIMENSION;
	}

	private void addShot(Mat workingFrame, PixelCluster pc) {
		Optional<javafx.scene.paint.Color> color = pc.getColorJavafx(workingFrame, colorAngleMovingAverage, colorChromaMovingAverage);

		double x = pc.centerPixelX;
		double y = pc.centerPixelY;

		if (!color.isPresent()) {
			logger.debug("Processing Shot: Shot Rejected By Lack Of Color Density");
			return;
		}

		Shot shot = new Shot(color.get(), x, y, 0, cameraManager.getFrameCount(), config.getMarkerRadius());

		if (!cameraManager.getDeduplicationProcessor().processShot(shot)) {
			logger.debug("Processing Shot: Shot Rejected By {}",
					cameraManager.getDeduplicationProcessor().getClass().getName());
			return;
		}
		if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent()
				&& color.get().equals(config.getIgnoreLaserColor().get())) {
			logger.debug("Processing Shot: Shot rejected by ignoreLaserColor {}", config.getIgnoreLaserColor().get());
			return;
		}

		logger.info("Suspected shot accepted: Center ({}, {}), {}", x, y, color.get());

		if (config.isDebugShotsRecordToFiles()) {
			Mat debugFrame = new Mat();
			Imgproc.cvtColor(workingFrame, debugFrame, Imgproc.COLOR_HSV2BGR);
			
			String filename = String.format("shot-%d-%d_orig.png", (int) pc.centerPixelX, (int) pc.centerPixelY);
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, debugFrame);
			

			
			for (Pixel p : pc) {
				if (color.get() == javafx.scene.paint.Color.GREEN)
				{
					double[] greenColor = { 0, 255, 0 };
					debugFrame.put(p.y, p.x, greenColor);
				}
				else
				{
					double[] redColor = { 0, 0, 255 };
					debugFrame.put(p.y, p.x, redColor);
				}
			}
			File outputfile = new File(String.format("shot-%d-%d.png", (int) pc.centerPixelX, (int) pc.centerPixelY));
			filename = outputfile.toString();
			Highgui.imwrite(filename, debugFrame);
		}

		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {

			Bounds b = cameraManager.getProjectionBounds().get();

			canvasManager.addShot(color.get(), x + b.getMinX(), y + b.getMinY());

		} else {
			canvasManager.addShot(color.get(), x, y);
		}

	}
}
