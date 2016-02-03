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

	private double lumsMovingAverageAcrossFrame = 0;
	private double lumsMovingAverageAcrossFrameCurrentCalc = 0;
	
	private double[][] lumsMovingAverage;
	private double[][] colorDiffMovingAverage;

	private double avgThresholdPixels = -1;

	private final static int INIT_FRAME_COUNT = 5;
	private int movingAveragePeriod = INIT_FRAME_COUNT;

	private final static int MOTION_WARNING_FRAMECOUNT = 30;
	private int MOTION_WARNING_AVG_THRESHOLD = 100;
	private int MOTION_WARNING_THRESHOLD_PIXELS = 600;
	private int MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG = 600;

	// Individual pixel threshold
	private final static  int EXCESSIVE_BRIGHTNESS_THRESHOLD = 245;
	private final static  int MINIMUM_BRIGHTNESS_INCREASE = 255 - EXCESSIVE_BRIGHTNESS_THRESHOLD;

	// Aggregate # of pixel threshold
	private int BRIGHTNESS_WARNING_AVG_THRESHOLD = 100;
	private final static int BRIGHTNESS_WARNING_FRAMECOUNT = 90;

	private int MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = 300;


	private int MINIMUM_SHOT_DIMENSION = 6;

	// This is updated for every bright pixel
	ArrayList<Pixel> brightPixels = new ArrayList<Pixel>();

	// The average is then calculated here
	private double avgBrightPixels = -1;

	// This is a short circuit for our pixel-color-changer to set the bad pixels
	// red
	// without having complicated math every pixel
	private boolean shouldShowBrightnessWarningBool = false;
	private BufferedImage currentFullFrame;
	


	public ShotDetectionManager(CameraManager cameraManager, Configuration config, CanvasManager canvasManager) {
		//((ch.qos.logback.classic.Logger)
		//logger).setLevel(ch.qos.logback.classic.Level.DEBUG);

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
		lumsMovingAverage = new double[width][height];
		colorDiffMovingAverage = new double[width][height];


		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++) {
				lumsMovingAverage[x][y] = -1;
				colorDiffMovingAverage[x][y] = -1;
			}
		}
		
		double frameSize = width*height;
		
		MOTION_WARNING_AVG_THRESHOLD = (int) (frameSize * .000325);
		MOTION_WARNING_THRESHOLD_PIXELS = (int) (frameSize * 0.00195);
		MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG = (int) (frameSize * 0.00195);

		// Aggregate # of pixel threshold
		BRIGHTNESS_WARNING_AVG_THRESHOLD = (int) (frameSize * .000325);

		MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = (int) (frameSize * .000976);

		MINIMUM_SHOT_DIMENSION = (int) (frameSize * .00001953125);
		
		
		
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	private Optional<Pixel> updateFilter(BufferedImage frame, int x, int y, boolean detectShots) {

		
		
		Optional<Pixel> result = Optional.empty();
		java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
		int currentRGB = currentC.getRGB();

		double currentLum = Pixel.calcLums(currentRGB);
		double colorDiff = Pixel.colorDistance(currentC, Color.RED) - Pixel.colorDistance(currentC, Color.GREEN);
		

		if (lumsMovingAverage[x][y] == -1) {
			
			lumsMovingAverage[x][y] = Math.min(currentLum, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
			colorDiffMovingAverage[x][y] = Math.min(colorDiff, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
			

			return result;

		}

		if (!detectShots) result = Optional.empty();

		if (pixelAboveExcessiveBrightnessThreshold((int)lumsMovingAverage[x][y])) {
			brightPixels.add(new Pixel(x, y));


		}

		else if (pixelAboveThreshold(currentLum,lumsMovingAverage[x][y])) result = Optional
				.of(new Pixel(x, y, currentC, currentLum, lumsMovingAverage[x][y], colorDiffMovingAverage[x][y]));

		
		// Update the average brightness
		lumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (double)(movingAveragePeriod - 1)) + (double)currentLum)
				/ (double)movingAveragePeriod;

		// Update the color distance
		colorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (movingAveragePeriod - 1)) + colorDiff)
				/ movingAveragePeriod;
		
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
		currentFullFrame.setRGB(drawX, drawY, rgb);
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

		// int dynamic_increase = 0;
		// if (avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD ||
		// avgThresholdPixels > BRIGHTNESS_WARNING_AVG_THRESHOLD)
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


	public void processFrame(BufferedImage frame, boolean detectShots) {
		movingAveragePeriod = Math.max((int)(cameraManager.getFPS()/5.0),INIT_FRAME_COUNT);
		
		
		// This is the FULL, ORIGINAL FRAME passed from CameraManager
		currentFullFrame = frame;

		BufferedImage workingFrame = null;

		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {
			Bounds b = cameraManager.getProjectionBounds().get();
			BufferedImage subFrame = frame.getSubimage((int) b.getMinX(), (int) b.getMinY(), (int) b.getWidth(),
					(int) b.getHeight());
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

	private void detectShots(BufferedImage workingCopy, ArrayList<PixelCluster> clusters) {
		for (PixelCluster cluster : clusters) {
			addShot(workingCopy, cluster);
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
			logger.debug("HIGH MOTION - avgThresholdPixels {} thresholdPixels {}", avgThresholdPixels, thresholdPixels);

			return true;
		}
		return false;
	}

	private boolean shouldShowBrightnessWarning() {

		if (logger.isTraceEnabled()) logger.trace("avgBrightPixels {}", avgBrightPixels);

		if (avgBrightPixels >= BRIGHTNESS_WARNING_AVG_THRESHOLD
				&& cameraManager.getFrameCount() > BRIGHTNESS_WARNING_FRAMECOUNT) {
			logger.debug("HIGH BRIGHTNESS - avgBrightPixels {}", avgBrightPixels);

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

	private List<Pixel> findThresholdPixelsAndUpdateFilter(BufferedImage workingFrame, boolean detectShots) {
		final int subWidth = workingFrame.getWidth() / SECTOR_COLUMNS;
		final int subHeight = workingFrame.getHeight() / SECTOR_ROWS;


		List<Pixel> thresholdPixels = Collections.synchronizedList( new ArrayList<Pixel>() );


		if (!cameraManager.isDetecting()) return thresholdPixels;
		
		lumsMovingAverageAcrossFrameCurrentCalc = 0;

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
						

						lumsMovingAverageAcrossFrameCurrentCalc += lumsMovingAverage[x][y];

						if (pixel.isPresent())
								thresholdPixels.add(pixel.get());
					}
				}				
			}
		});
		
		lumsMovingAverageAcrossFrameCurrentCalc /= (workingFrame.getWidth()*workingFrame.getHeight());

		lumsMovingAverageAcrossFrame = lumsMovingAverageAcrossFrameCurrentCalc;
		
		return thresholdPixels;
	}
	
	public double getLumsMovingAverageAcrossFrame()
	{
		return lumsMovingAverageAcrossFrame;
	}

	private void updateAvgThresholdPixels(int thresholdPixels) {
		if (avgThresholdPixels == -1)
			avgThresholdPixels = Math.min(thresholdPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
			avgThresholdPixels = (((movingAveragePeriod - 1) * avgThresholdPixels)
					+ Math.min(thresholdPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_MOTION_AVG)) / movingAveragePeriod;

	}

	private void updateAvgBrightPixels(int brightPixels) {
		if (avgBrightPixels == -1)
			avgBrightPixels = Math.min(brightPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
			avgBrightPixels = (((movingAveragePeriod - 1) * avgBrightPixels)
					+ Math.min(brightPixels, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG)) / movingAveragePeriod;
	}

	public int getMinimumShotDimension() {
		if (cameraManager.getMinimumShotDimension().isPresent()) {
			return cameraManager.getMinimumShotDimension().get();
		}
		return MINIMUM_SHOT_DIMENSION;
	}

	private void addShot(BufferedImage workingCopy, PixelCluster pc) {
		Optional<javafx.scene.paint.Color> color = pc.getColorJavafx(workingCopy, colorDiffMovingAverage);

		double x = pc.centerPixelX;
		double y = pc.centerPixelY;

		if (!color.isPresent()) return;

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
			
			File outputfile = new File(
					String.format("shot-%d-%d_orig.png", (int) pc.centerPixelX, (int) pc.centerPixelY));
			try {
				ImageIO.write(workingCopy, "png", outputfile);
			} catch (IOException e) {
				logger.error("Error writing original shot detection image", e);
			}
			

			
			
			for (Pixel p : pc) {
				if (color.get() == javafx.scene.paint.Color.GREEN)
					workingCopy.setRGB(p.x, p.y, 0x00FF00);
				else
					workingCopy.setRGB(p.x, p.y, 0xFF0000);
			}
			outputfile = new File(String.format("shot-%d-%d.png", (int) pc.centerPixelX, (int) pc.centerPixelY));
			try {
				ImageIO.write(workingCopy, "png", outputfile);
			} catch (IOException e) {
				logger.error("Error writing processed shot detection image", e);
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
}
