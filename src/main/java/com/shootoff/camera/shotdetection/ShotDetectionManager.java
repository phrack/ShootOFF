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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javafx.geometry.Bounds;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.Shot;
import com.shootoff.camera.arenamask.ArenaMaskManager;
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
	private int[][] colorDistanceFromRed;

	private int avgThresholdPixels = -1;

	private final static int INIT_FRAME_COUNT = 5;
	private int movingAveragePeriod = INIT_FRAME_COUNT;

	private final static int MOTION_WARNING_FRAMECOUNT = 30;
	private int MOTION_WARNING_AVG_THRESHOLD;
	private int MOTION_WARNING_THRESHOLD_PIXELS;
	private int MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG;

	// Individual pixel threshold
	private final static int MAXIMUM_LUM_VALUE = 65025;
	private final static int EXCESSIVE_BRIGHTNESS_THRESHOLD = (int) (.96 * MAXIMUM_LUM_VALUE);
	private final static int MINIMUM_BRIGHTNESS_INCREASE = (int) (.117 * MAXIMUM_LUM_VALUE);;

	// Aggregate # of pixel threshold
	private int BRIGHTNESS_WARNING_AVG_THRESHOLD;
	private final static int BRIGHTNESS_WARNING_FRAMECOUNT = 90;

	private int MAXIMUM_THRESHOLD_PIXELS_FOR_AVG;

	private int MINIMUM_SHOT_DIMENSION;

	// This is updated for every bright pixel

	Set<Pixel> brightPixels = Collections.synchronizedSet(new HashSet<Pixel>());

	// The average is then calculated here
	private int avgBrightPixels = -1;
	
	// We keep track of how many pixels we filtered due to a dynamic threshold
	// so that we keep them in the average of thresholded pixels.
	private int dynamicallyThresholded = -1;

	// This is a short circuit for our pixel-color-changer to set the bad pixels
	// red without having complicated math every pixel
	private boolean shouldShowBrightnessWarningBool = false;

	private ArenaMaskManager arenaMaskManager = null;
	private boolean usingArenaMask = false;

	public ShotDetectionManager(CameraManager cameraManager, Configuration config, CanvasManager canvasManager) {
		this.canvasManager = canvasManager;
		this.cameraManager = cameraManager;
		this.config = config;

		initializeDimensions(cameraManager.getFeedWidth(), cameraManager.getFeedHeight());
	}

	public void reInitializeDimensions() {
		initializeDimensions(cameraManager.getFeedWidth(), cameraManager.getFeedHeight());
	}

	private void initializeDimensions(int width, int height) {
		lumsMovingAverage = new int[width][height];
		colorDistanceFromRed = new int[width][height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				lumsMovingAverage[x][y] = -1;
			}
		}

		double frameSize = width * height;

		MOTION_WARNING_AVG_THRESHOLD = (int) (frameSize * .000395);
		MOTION_WARNING_THRESHOLD_PIXELS = (int) (frameSize * 0.00195);
		MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG = (int) (frameSize * 0.00195);

		// Aggregate # of pixel threshold
		BRIGHTNESS_WARNING_AVG_THRESHOLD = (int) (frameSize * .000325);

		MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = (int) (frameSize * .000976);

		MINIMUM_SHOT_DIMENSION = (int) (frameSize * .000025);

		if (usingArenaMask) arenaMaskManager.setLumsMovingAverage(lumsMovingAverage);
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}


	private Pixel updateFilter(int currentH, int currentS, int currentV, int mask, int x, int y, boolean detectShots) {
		Pixel result = null;


		int currentLum = (255 - currentS) * currentV;

		if (lumsMovingAverage[x][y] == -1) {
			lumsMovingAverage[x][y] = currentLum;
			colorDistanceFromRed[x][y] = (Math.min(currentH, Math.abs(180 - currentH)) * currentS * currentV)
					- (Math.abs(60 - currentH) * currentS * currentV);

			return result;
		}


		if (detectShots && pixelAboveExcessiveBrightnessThreshold(lumsMovingAverage[x][y])) {
			brightPixels.add(new Pixel(x, y));
		} else if (detectShots && pixelAboveThreshold(currentLum, Math.max(mask, lumsMovingAverage[x][y]))) {
			result = new Pixel(x, y, currentH, currentLum, lumsMovingAverage[x][y], colorDistanceFromRed[x][y]);
		}


		int tempColorDistanceFromRed = (Math.min(currentH, Math.abs(180 - currentH)) * currentS * currentV)
				- (Math.abs(60 - currentH) * currentS * currentV);

		// Update the average brightness
		lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (movingAveragePeriod - 1)) + currentLum)
				/ movingAveragePeriod;

		colorDistanceFromRed[x][y] = ((colorDistanceFromRed[x][y] * (movingAveragePeriod - 1))
				+ tempColorDistanceFromRed) / movingAveragePeriod;

		return result;
	}

	private boolean pixelAboveExcessiveBrightnessThreshold(int lumsMovingAverage) {
		return lumsMovingAverage > EXCESSIVE_BRIGHTNESS_THRESHOLD;
	}

	private boolean pixelAboveThreshold(int currentLum, int lumsMovingAverage) {
		final int threshold = ((MAXIMUM_LUM_VALUE - lumsMovingAverage) / 4);
		final int increase = (currentLum - lumsMovingAverage);

		int dynamic_increase = (int)((MAXIMUM_LUM_VALUE - threshold)
				* ((double)avgThresholdPixels / (double)MAXIMUM_THRESHOLD_PIXELS_FOR_AVG));

		int dynamic_threshold = threshold + dynamic_increase;

		if (increase < MINIMUM_BRIGHTNESS_INCREASE) return false;

		if (increase < dynamic_threshold) {
			if (increase > threshold) dynamicallyThresholded++;
				return false;
			}
		
		return true;

	}

	public void processFrame(Mat frame, boolean detectShots) {
		if ((cameraManager.getFrameCount()%5)==0)
			movingAveragePeriod = Math.max((int) (cameraManager.getFPS() / 5.0), INIT_FRAME_COUNT);

		Mat workingFrame = null;

		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {
			Bounds b = cameraManager.getProjectionBounds().get();
			Mat subFrame = frame.submat((int) b.getMinY(), (int) b.getMaxY(), (int) b.getMinX(), (int) b.getMaxX());
			workingFrame = subFrame;

		} else {
			workingFrame = frame;

		}

		// Must reset before every updateFilter loop
		brightPixels.clear();


		Set<Pixel> thresholdPixels = findThresholdPixelsAndUpdateFilter(workingFrame,
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

		if (detectShots && filtersInitialized) {
			updateAvgThresholdPixels(thresholdPixelsSize);

			updateAvgBrightPixels(brightPixels.size());

			if (shouldShowBrightnessWarning()) {
				cameraManager.showBrightnessWarning();

			}

			if (thresholdPixelsSize >= getMinimumShotDimension() && !isExcessiveMotion(thresholdPixelsSize)) {

				ArrayList<PixelCluster> clusters = clusterPixels(thresholdPixels);

				if (logger.isTraceEnabled()) {
					logger.trace("thresholdPixels {}", thresholdPixelsSize);
					logger.trace("clusters {}", clusters.size());
				}

				detectShots(workingFrame, clusters);
			}

			// Moved to after detectShots because otherwise we'll have changed
			// pixels in the frame that's being checked for shots
			else if (isExcessiveMotion(thresholdPixelsSize)) {
				if (shouldShowMotionWarning(thresholdPixelsSize)) cameraManager.showMotionWarning();

				byte[] blue = { (byte) 120, (byte) 255, (byte) 255 };

				for (Pixel pixel : thresholdPixels) {
					workingFrame.put(pixel.y,pixel.x,blue);
				}
			}

			if (shouldShowBrightnessWarningBool && brightPixels.size() > 0) {
				// Make the feed pixels red so the user can easily see what the
				// problem pixels are
				byte[] red = { 0, (byte) 255, (byte) 255 };

				for (Pixel pixel : brightPixels) {
					workingFrame.put(pixel.y,pixel.x,red);
				}
			}
		}
	}


	private ArrayList<PixelCluster> clusterPixels(Set<Pixel> thresholdPixels) {
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
		return thresholdPixels > MOTION_WARNING_THRESHOLD_PIXELS || avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD;
	}

	private boolean shouldShowMotionWarning(int thresholdPixels) {
		if (avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > MOTION_WARNING_FRAMECOUNT) {
			if (logger.isTraceEnabled()) logger.trace("HIGH MOTION - avgThresholdPixels {} thresholdPixels {}",
					avgThresholdPixels, thresholdPixels);

			return true;
		}

		return false;
	}

	private boolean shouldShowBrightnessWarning() {
		if (logger.isTraceEnabled()) logger.trace("avgBrightPixels {}", avgBrightPixels);

		if (avgBrightPixels >= BRIGHTNESS_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > BRIGHTNESS_WARNING_FRAMECOUNT) {
			if (logger.isTraceEnabled()) logger.trace("HIGH BRIGHTNESS - avgBrightPixels {}", avgBrightPixels);

			shouldShowBrightnessWarningBool = true;

			return true;
		}

		shouldShowBrightnessWarningBool = false;
		return false;
	}

	private boolean checkIfInitialized() {
		return cameraManager.getFrameCount() > INIT_FRAME_COUNT;
	}


	private Set<Pixel> findThresholdPixelsAndUpdateFilter(final Mat workingFrame, final boolean detectShots) {
		final int subWidth = workingFrame.cols() / SECTOR_COLUMNS;
		final int subHeight = workingFrame.rows() / SECTOR_ROWS;

		dynamicallyThresholded = 0;
		

		Set<Pixel> thresholdPixels = Collections.synchronizedSet(new HashSet<Pixel>());

		if (!cameraManager.isDetecting()) return thresholdPixels;

		int cols = workingFrame.cols();
		int channels = workingFrame.channels();

		int size = (int) (workingFrame.total() * channels);
		byte[] workingFramePrimitive = new byte[size];
		workingFrame.get(0, 0, workingFramePrimitive);

		int[] maskPrimitive = new int[workingFrame.cols() * workingFrame.rows()];
		if (usingArenaMask) {
			Mat mask = arenaMaskManager.getMask();
			mask.get(0, 0, maskPrimitive);
		}

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
					int yOffset = y * cols;
					for (Integer x = startX; x < startX + subWidth; x++) {
						int currentH = workingFramePrimitive[(yOffset + x) * channels] & 0xFF;
						int currentS = workingFramePrimitive[(yOffset + x) * channels + 1] & 0xFF;
						int currentV = workingFramePrimitive[(yOffset + x) * channels + 2] & 0xFF;


						int maskInt = 0;


						if (usingArenaMask) {

							maskInt = maskPrimitive[yOffset + x];
						}


						Pixel pixel = updateFilter(currentH, currentS, currentV, maskInt, x, y, detectShots);


						if (pixel != null) thresholdPixels.add(pixel);
					}
				}
			}
		});

		return thresholdPixels;
	}

	private void updateAvgThresholdPixels(int thresholdPixels) {
		if (avgThresholdPixels == -1)
			avgThresholdPixels = Math.min(thresholdPixels+dynamicallyThresholded, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else {

			avgThresholdPixels = (((movingAveragePeriod - 1) * avgThresholdPixels)
					+ Math.min(thresholdPixels + dynamicallyThresholded, MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG))
					/ movingAveragePeriod;
		}
		
	}

	private void updateAvgBrightPixels(int brightPixels) {
		if (avgBrightPixels == -1)
			avgBrightPixels = Math.min(brightPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
			avgBrightPixels = (((movingAveragePeriod - 1) * avgBrightPixels)
					+ Math.min(brightPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG)) / movingAveragePeriod;
	}
	

	public int getMinimumShotDimension() {
		return cameraManager.getMinimumShotDimension().isPresent() ? cameraManager.getMinimumShotDimension().get()
				: MINIMUM_SHOT_DIMENSION;
	}

	private void addShot(Mat workingFrame, PixelCluster pc) {
		Optional<javafx.scene.paint.Color> color = pc.getColorJavafx(workingFrame, colorDistanceFromRed);

		double x = pc.centerPixelX;
		double y = pc.centerPixelY;

		if (!color.isPresent()) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot Rejected By Lack Of Color Density");
			return;
		}

		Shot shot = new Shot(color.get(), x, y, 0, cameraManager.getFrameCount(), config.getMarkerRadius());

		if (!cameraManager.getDeduplicationProcessor().processShot(shot)) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot Rejected By {}",
					cameraManager.getDeduplicationProcessor().getClass().getName());
			return;
		}
		if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent()
				&& color.get().equals(config.getIgnoreLaserColor().get())) {
			if (logger.isDebugEnabled()) logger.debug("Processing Shot: Shot rejected by ignoreLaserColor {}",
					config.getIgnoreLaserColor().get());
			return;
		}


		if (logger.isInfoEnabled()) logger.info("Suspected shot accepted: Center ({}, {}), cl {} fr {}", x, y,
				color.get(), cameraManager.getFrameCount());

		if (config.isDebugShotsRecordToFiles()) {
			Mat debugFrame = new Mat();
			Imgproc.cvtColor(workingFrame, debugFrame, Imgproc.COLOR_HSV2BGR);

			String filename = String.format("shot-%d-%d_orig.png", (int) pc.centerPixelX, (int) pc.centerPixelY);
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, debugFrame);

			for (Pixel p : pc) {
				if (color.get() == javafx.scene.paint.Color.GREEN) {
					double[] greenColor = { 0, 255, 0 };
					debugFrame.put(p.y, p.x, greenColor);
				} else {
					double[] redColor = { 0, 0, 255 };
					debugFrame.put(p.y, p.x, redColor);
				}
			}

			File outputfile = new File(String.format("shot-%d-%d.png", (int) pc.centerPixelX, (int) pc.centerPixelY));
			filename = outputfile.toString();
			Highgui.imwrite(filename, debugFrame);

			if (usingArenaMask) {
				Mat mask = arenaMaskManager.getMask();
				Mat maskGrayscale = new Mat(mask.rows(), mask.cols(), CvType.CV_8UC1);
				
				for (int a = 0; a < mask.cols(); a++) {
					for (int b = 0; b < mask.rows(); b++) {
						maskGrayscale.put(b, a, mask.get(b, a)[0] / 255);
					}
				}
				
				outputfile = new File(String.format("mask-%d-%d.png", (int) pc.centerPixelX, (int) pc.centerPixelY));
				filename = outputfile.toString();
				Highgui.imwrite(filename, maskGrayscale);
			}
		}

		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {

			Bounds b = cameraManager.getProjectionBounds().get();

			canvasManager.addShot(color.get(), x + b.getMinX(), y + b.getMinY());
		} else {
			canvasManager.addShot(color.get(), x, y);
		}

	}

	public void setArenaMaskManager(final ArenaMaskManager arenaMaskManager) {
		if (arenaMaskManager != null) {
			setUsingArenaMask(true);
			this.arenaMaskManager = arenaMaskManager;
			arenaMaskManager.setLumsMovingAverage(lumsMovingAverage);
		} else {
			setUsingArenaMask(false);
		}
	}

	private void setUsingArenaMask(final boolean val) {
		usingArenaMask = val;
	}
}
