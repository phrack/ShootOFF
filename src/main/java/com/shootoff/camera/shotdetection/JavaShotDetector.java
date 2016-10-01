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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javafx.scene.paint.Color;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.GlobalExecutorPool;
import org.openimaj.util.parallel.Parallel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.config.Configuration;

public final class JavaShotDetector extends FrameProcessingShotDetector {
	private static final Logger logger = LoggerFactory.getLogger(JavaShotDetector.class);

	public static final int SECTOR_COLUMNS = 3;
	public static final int SECTOR_ROWS = 3;

	// These assume BGR format
	private static final byte[] BLUE_MAT_PIXEL = { (byte) 255, (byte) 0, (byte) 0 };
	private static final byte[] RED_MAT_PIXEL = { 0, (byte) 0, (byte) 255 };

	private final CameraManager cameraManager;

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
	private final Set<Pixel> brightPixels = Collections.synchronizedSet(new HashSet<Pixel>());

	// The average is then calculated here
	private int avgBrightPixels = -1;

	// We keep track of how many pixels we filtered due to a dynamic threshold
	// so that we keep them in the average of thresholded pixels.
	private int dynamicallyThresholded = -1;

	// This is a short circuit for our pixel-color-changer to set the bad pixels
	// red without having complicated math every pixel
	private boolean shouldShowBrightnessWarningBool = false;

	final PixelClusterManager pixelClusterManager;

	public static boolean isSystemSupported() {
		return true;
	}

	public JavaShotDetector(final CameraManager cameraManager, final CameraView cameraView) {
		super(cameraManager, cameraView);

		GlobalExecutorPool.getPool().setRejectedExecutionHandler((r, p) -> {
			if (!p.isShutdown()) {
				logger.error("Shot detection thread was rejected but GlobalExecutorPool was not shutdown");
			}
		});

		this.cameraManager = cameraManager;

		setFrameSize(cameraManager.getFeedWidth(), cameraManager.getFeedHeight());

		this.pixelClusterManager = new PixelClusterManager(cameraManager.getFeedWidth(), cameraManager.getFeedHeight());
	}

	@Override
	public void setFrameSize(final int width, final int height) {
		lumsMovingAverage = new int[width][height];
		colorDistanceFromRed = new int[width][height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				lumsMovingAverage[x][y] = -1;
			}
		}

		final double frameSize = width * height;

		MOTION_WARNING_AVG_THRESHOLD = (int) (frameSize * .000395);
		MOTION_WARNING_THRESHOLD_PIXELS = (int) (frameSize * 0.00195);
		MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG = (int) (frameSize * 0.00195);

		// Aggregate # of pixel threshold
		BRIGHTNESS_WARNING_AVG_THRESHOLD = (int) (frameSize * .000325);

		MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = (int) (frameSize * .000976);

		MINIMUM_SHOT_DIMENSION = (int) (frameSize * .000025);
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	private Pixel updateFilter(int currentH, int currentS, int currentV, int x, int y, boolean detectShots) {
		Pixel result = null;

		final int currentLum = (255 - currentS) * currentV;

		if (lumsMovingAverage[x][y] == -1) {
			lumsMovingAverage[x][y] = currentLum;
			colorDistanceFromRed[x][y] = (Math.min(currentH, Math.abs(180 - currentH)) * currentS * currentV)
					- (Math.abs(60 - currentH) * currentS * currentV);

			return result;
		}

		if (detectShots && pixelAboveExcessiveBrightnessThreshold(lumsMovingAverage[x][y])) {
			brightPixels.add(new Pixel(x, y));
		} else if (detectShots && pixelAboveThreshold(currentLum, lumsMovingAverage[x][y])) {
			result = new Pixel(x, y, currentH, currentLum, lumsMovingAverage[x][y], colorDistanceFromRed[x][y]);
		}

		final int tempColorDistanceFromRed = (Math.min(currentH, Math.abs(180 - currentH)) * currentS * currentV)
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
		final int increase = (currentLum - lumsMovingAverage);

		if (increase < MINIMUM_BRIGHTNESS_INCREASE)
			return false;

		// (var >> 2) equivalent to (var / 4)
		final int threshold = (MAXIMUM_LUM_VALUE - lumsMovingAverage) >> 2;

		final int dynamic_increase = (int) ((MAXIMUM_LUM_VALUE - threshold)
				* ((double) avgThresholdPixels / (double) MAXIMUM_THRESHOLD_PIXELS_FOR_AVG));

		final int dynamic_threshold = threshold + dynamic_increase;

		if (increase < dynamic_threshold) {
			if (increase > threshold)
				dynamicallyThresholded++;
			return false;
		}

		return true;
	}

	/**
	 * Use and HSV copy of the current camera frame to detect shots and use a
	 * BGR copy to draw bright pixels as red and high motion pixels as blue. The
	 * BGR copy is what ShootOFF shows
	 * 
	 * @param frameHSV
	 * 
	 * @param frameBGR
	 *            a blue, green, red copy of the current frame for drawing
	 *            bright/high motion pixels
	 * @param detectShots
	 *            whether or not to detect a shot
	 */
	@Override
	public void processFrame(final Mat frameBGR, final boolean detectShots) {
		updateMovingAveragePeriod();

		// Must reset before every updateFilter loop
		brightPixels.clear();
		

		// Create a hue, saturation, value copy of the current frame used to
		// detect
		// the shots. The BGR version is just used by this implementation to
		// show
		// the user where bright/high motion pixels are
		final Mat frameHSV = new Mat();
		Imgproc.cvtColor(frameBGR, frameHSV, Imgproc.COLOR_BGR2HSV);

		final Set<Pixel> thresholdPixels = findThresholdPixelsAndUpdateFilter(frameHSV,
				(detectShots && filtersInitialized));

		int thresholdPixelsSize = thresholdPixels.size();

		if (logger.isTraceEnabled()) {
			if (thresholdPixelsSize >= 1)
				logger.trace("thresholdPixels {} getMinimumShotDimension {}", thresholdPixelsSize,
						getMinimumShotDimension());

			for (final Pixel pixel : thresholdPixels) {
				logger.trace("thresholdPixel {} {} - from array {} from pixel cur {} avg {}", pixel.x, pixel.y,
						lumsMovingAverage[pixel.x][pixel.y], pixel.getCurrentLum(), pixel.getLumAverage());
			}
		}

		if (!filtersInitialized)
			filtersInitialized = checkIfInitialized();

		if (detectShots && filtersInitialized) {
			updateAvgThresholdPixels(thresholdPixelsSize);

			updateAvgBrightPixels(brightPixels.size());

			if (shouldShowBrightnessWarning()) {
				cameraManager.showBrightnessWarning();
			}

			if (thresholdPixelsSize >= getMinimumShotDimension() && !isExcessiveMotion(thresholdPixelsSize)) {
				final Set<PixelCluster> clusters = pixelClusterManager.clusterPixels(thresholdPixels,
						getMinimumShotDimension());

				if (logger.isTraceEnabled()) {
					logger.trace("thresholdPixels {}", thresholdPixelsSize);
					logger.trace("clusters {}", clusters.size());
				}

				detectShots(frameHSV, clusters);
			}

			// Moved to after detectShots because otherwise we'll have changed
			// pixels in the frame that's being checked for shots
			else if (isExcessiveMotion(thresholdPixelsSize)) {
				if (shouldShowMotionWarning(thresholdPixelsSize))
					cameraManager.showMotionWarning();

				for (final Pixel pixel : thresholdPixels) {
					frameBGR.put(pixel.y, pixel.x, BLUE_MAT_PIXEL);
				}
			}

			if (shouldShowBrightnessWarningBool && !brightPixels.isEmpty()) {
				// Make the feed pixels red so the user can easily see what the
				// problem pixels are
				synchronized(brightPixels) {
					for (final Pixel pixel : brightPixels) {
						frameBGR.put(pixel.y, pixel.x, RED_MAT_PIXEL);
					}
				}
			}
		}
	}

	private void updateMovingAveragePeriod() {
		if (cameraManager.getFrameCount() % 5 == 0)
			movingAveragePeriod = Math.max((int) (cameraManager.getFPS() / 5.0), INIT_FRAME_COUNT);
	}

	private void detectShots(final Mat workingFrame, final Set<PixelCluster> clusters) {
		for (final PixelCluster cluster : clusters) {
			addShot(workingFrame, cluster);
		}
	}

	private boolean isExcessiveMotion(final int thresholdPixels) {
		return thresholdPixels > MOTION_WARNING_THRESHOLD_PIXELS || avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD;
	}

	private boolean shouldShowMotionWarning(final int thresholdPixels) {
		final boolean showWarning = avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > MOTION_WARNING_FRAMECOUNT;

		if (showWarning && logger.isTraceEnabled())
			logger.trace("HIGH MOTION - avgThresholdPixels {} thresholdPixels {}", avgThresholdPixels, thresholdPixels);

		return showWarning;
	}

	private boolean shouldShowBrightnessWarning() {
		if (logger.isTraceEnabled())
			logger.trace("avgBrightPixels {}", avgBrightPixels);

		if (avgBrightPixels >= BRIGHTNESS_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > BRIGHTNESS_WARNING_FRAMECOUNT) {
			if (logger.isTraceEnabled())
				logger.trace("HIGH BRIGHTNESS - avgBrightPixels {}", avgBrightPixels);

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
		dynamicallyThresholded = 0;

		final Set<Pixel> thresholdPixels = Collections.synchronizedSet(new HashSet<Pixel>());

		if (!cameraManager.isDetecting())
			return thresholdPixels;

		final int subWidth = workingFrame.cols() / SECTOR_COLUMNS;
		final int subHeight = workingFrame.rows() / SECTOR_ROWS;

		final int cols = workingFrame.cols();
		final int channels = workingFrame.channels();

		final int size = (int) (workingFrame.total() * channels);
		final byte[] workingFramePrimitive = new byte[size];
		workingFrame.get(0, 0, workingFramePrimitive);

		// In this loop we accomplish both MovingAverage updates AND threshold
		// pixel detection
		Parallel.forIndex(0, (SECTOR_ROWS * SECTOR_COLUMNS), 1, new Operation<Integer>() {
			public void perform(Integer sector) {
				final int sectorX = sector.intValue() % SECTOR_COLUMNS;
				final int sectorY = sector.intValue() / SECTOR_ROWS;

				if (!cameraManager.isSectorOn(sectorX, sectorY))
					return;

				final int startX = subWidth * sectorX;
				final int startY = subHeight * sectorY;

				for (int y = startY; y < startY + subHeight; y++) {
					final int yOffset = y * cols;
					for (int x = startX; x < startX + subWidth; x++) {
						// If the thread is interrupted it's likely because the
						// thread pool
						// is being shutdown with shutdownNow. Thus cancel
						// searching
						// for a shot in the current frame.
						if (Thread.currentThread().isInterrupted()) {
							logger.trace("Shot detection sieve interrupted");
							return;
						}

						final int currentH = workingFramePrimitive[(yOffset + x) * channels] & 0xFF;
						final int currentS = workingFramePrimitive[(yOffset + x) * channels + 1] & 0xFF;
						final int currentV = workingFramePrimitive[(yOffset + x) * channels + 2] & 0xFF;

						final Pixel pixel = updateFilter(currentH, currentS, currentV, x, y, detectShots);

						if (pixel != null)
							thresholdPixels.add(pixel);
					}
				}
			}
		});

		return thresholdPixels;
	}

	private void updateAvgThresholdPixels(final int thresholdPixels) {
		if (avgThresholdPixels == -1)
			avgThresholdPixels = Math.min(thresholdPixels + dynamicallyThresholded, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else {
			avgThresholdPixels = (((movingAveragePeriod - 1) * avgThresholdPixels)
					+ Math.min(thresholdPixels + dynamicallyThresholded, MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG))
					/ movingAveragePeriod;
		}
	}

	private void updateAvgBrightPixels(final int brightPixels) {
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
		final Optional<Color> color = pc.getColor(workingFrame, colorDistanceFromRed);

		if (!color.isPresent()) {
			if (logger.isDebugEnabled())
				logger.debug("Processing Shot: Shot Rejected By Lack Of Color Density");
			return;
		}

		final double x = pc.centerPixelX;
		final double y = pc.centerPixelY;

		if (super.addShot(color.get(), x, y, true) && Configuration.getConfig().isDebugShotsRecordToFiles()) {
			final Mat debugFrame = new Mat();
			Imgproc.cvtColor(workingFrame, debugFrame, Imgproc.COLOR_HSV2BGR);

			String filename = String.format("shot-%d-%d-%d_orig.png", cameraManager.getFrameCount(),
					(int) pc.centerPixelX, (int) pc.centerPixelY);
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, debugFrame);

			for (final Pixel p : pc) {
				if (javafx.scene.paint.Color.GREEN.equals(color.get())) {
					final double[] greenColor = { 0, 255, 0 };
					debugFrame.put(p.y, p.x, greenColor);
				} else {
					final double[] redColor = { 0, 0, 255 };
					debugFrame.put(p.y, p.x, redColor);
				}
			}

			final File outputfile = new File(String.format("shot-%d-%d-%d.png", cameraManager.getFrameCount(),
					(int) pc.centerPixelX, (int) pc.centerPixelY));
			filename = outputfile.toString();
			Highgui.imwrite(filename, debugFrame);
		}
	}

	@Override
	protected boolean handlesBounds() {
		return true;
	}
}
