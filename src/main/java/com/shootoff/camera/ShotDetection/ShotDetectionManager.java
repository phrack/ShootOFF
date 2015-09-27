package com.shootoff.camera.ShotDetection;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

public final class ShotDetectionManager implements Runnable {

	public static final int SECTOR_COLUMNS = 3;
	public static final int SECTOR_ROWS = 3;

	private final Logger logger = LoggerFactory.getLogger(ShotDetectionManager.class);
		
	private CanvasManager canvasManager;
	private CameraManager cameraManager;
	private Configuration config;
	
	
	private final int INIT_FRAME_COUNT = 5;
	private boolean filtersInitialized = false;	

	// This is the long term storage for the MAs
	private int[][] lumsMovingAverage = new int[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];	
	private double[][] colorDiffMovingAverage = new double[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	// New data is stored here until the shot detection has finished for a frame
	// "final" does apply here, the data in the arrays can not be changed but the arrays themselves cannot be overwritten
	private final int[][] newLumsMovingAverage = new int[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	private final double[][] newColorDiffMovingAverage = new double[CameraManager.FEED_WIDTH][CameraManager.FEED_HEIGHT];
	
	private double avgThresholdPixels = -1;


	
	
	private static final int MINIMUM_SHOT_DIMENSION = 9;
	
	
	public ShotDetectionManager(CameraManager cameraManager, Configuration config,
			CanvasManager canvasManager) {
		this.canvasManager = canvasManager;
		this.cameraManager = cameraManager;
		this.config = config;
		
		

		for (int y = 0; y < CameraManager.FEED_HEIGHT; y++)
			for (int x = 0; x < CameraManager.FEED_WIDTH; x++)
			{
				lumsMovingAverage[x][y] = -1;
				colorDiffMovingAverage[x][y] = -1;
			}
	}
	
	public void run()
	{
		//Not implemented
	}


	private Optional<Pixel> updateFilter(BufferedImage frame, int x, int y, boolean detectShots) {
		
		Optional<Pixel> result = Optional.empty();
		java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
		int currentRGB = currentC.getRGB();
		
		int currentLum = Pixel.calcLums(currentRGB);
		
		double colorDiff = Pixel.colorDistance(currentC, Color.RED) - Pixel.colorDistance(currentC, Color.GREEN);
		
        if (lumsMovingAverage[x][y] == -1)
        {
        	newLumsMovingAverage[x][y] = currentLum;
        	newColorDiffMovingAverage[x][y] = colorDiff;
            return Optional.empty();

        }


		
		if (!detectShots)
			result = Optional.empty();

		if (pixelAboveThreshold(currentLum, lumsMovingAverage[x][y]))
			result = Optional.of(new Pixel(x,y, currentC, currentLum, lumsMovingAverage[x][y], colorDiffMovingAverage[x][y]));
		
		
        // Update the average brightness
		newLumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (INIT_FRAME_COUNT-1)) + currentLum) / INIT_FRAME_COUNT;
		
    	// Update the color distance
		newColorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (INIT_FRAME_COUNT-1)) + colorDiff) / INIT_FRAME_COUNT;
		
		return result;

	}
	
	
	private void applyFilter()
	{
		lumsMovingAverage = newLumsMovingAverage;
		colorDiffMovingAverage = newColorDiffMovingAverage;
	}

	
	private final int EXCESSIVE_BRIGHTNESS_THRESHOLD = 250;
	private final int MINIMUM_BRIGHTNESS_INCREASE = 10;
	
	private boolean pixelAboveThreshold(int currentLum, int lumsMovingAverage)
	{
		if (lumsMovingAverage>EXCESSIVE_BRIGHTNESS_THRESHOLD)
			return false;
		
		int threshold = (255-lumsMovingAverage)/2;
		int increase = (currentLum-lumsMovingAverage);
		
		if (increase<MINIMUM_BRIGHTNESS_INCREASE)
			return false;
		
		if (increase<threshold)
			return false;
		
		return true;
		
	}
	
	
	public boolean processFrame(BufferedImage frame, boolean detectShots) {
		BufferedImage workingCopy = null;
		
		
		if (cameraManager.isLimitingDetectionToProjection() && cameraManager.getProjectionBounds().isPresent()) {
			Bounds b = cameraManager.getProjectionBounds().get();
			BufferedImage subFrame = frame.getSubimage((int)b.getMinX(), (int)b.getMinY(),
					(int)b.getWidth(), (int)b.getHeight());
			workingCopy = subFrame;
			
		} else {
			
			workingCopy = frame;
			
		}
			
		ArrayList<Pixel> thresholdPixels = findThresholdPixelsAndUpdateFilter(workingCopy, (detectShots && filtersInitialized));
		
		logger.trace("thresholdPixels {}", thresholdPixels.size());
		
		if (checkIfInitialized())
			filtersInitialized = true;
		
		
		if (detectShots && filtersInitialized)
		{
			updateAvgThresholdPixels(thresholdPixels.size());

			
			if (shouldShowBrightnessWarning())
				cameraManager.showBrightnessWarning();
		
			// We don't show a warning if the excessive motion happens very early in the feed
			// But we still need to skip detection on those frames
			// That's why this is in two ifs
			else if (isExcessiveMotion(thresholdPixels.size()))
			{
				if (shouldShowMotionWarning(thresholdPixels.size()))
					cameraManager.showMotionWarning();
			}
			else if (thresholdPixels.size() >= getMinimumShotDimension())
			{
				ArrayList<PixelCluster> clusters = clusterPixels(workingCopy, thresholdPixels);

				detectShots(workingCopy, clusters);
			}
		}
		
		applyFilter();
		

		if (cameraManager.getDebuggerListener().isPresent()) {
			cameraManager.getDebuggerListener().get().updateDebugView(workingCopy);
		}
		
		return filtersInitialized;
	}
	
	
	private ArrayList<PixelCluster> clusterPixels(BufferedImage workingCopy, ArrayList<Pixel> thresholdPixels) {
		ArrayList<PixelCluster> clusters = new ArrayList<PixelCluster>();
		PixelClusterManager pixelClusterManager = new PixelClusterManager(thresholdPixels, this);
		pixelClusterManager.clusterPixels();
		clusters = pixelClusterManager.dumpClusters();
	
		return clusters;
	}

	
	private void detectShots(BufferedImage workingCopy, ArrayList<PixelCluster> clusters) {
		for (PixelCluster cluster : clusters)
		{
			addShot(workingCopy, cluster);
		}
	}




	private final int MOTION_WARNING_FRAMECOUNT = 30;
	private final int MOTION_WARNING_AVG_THRESHOLD = 100;
	private final int MOTION_WARNING_THRESHOLD_PIXELS = 250;
	private boolean isExcessiveMotion(int thresholdPixels) {
		
		if ((avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD || (thresholdPixels > MOTION_WARNING_THRESHOLD_PIXELS)))
			return true;

		return false;
	}
	
	private boolean shouldShowMotionWarning(int thresholdPixels) {
		
		if (cameraManager.getFrameCount() > MOTION_WARNING_FRAMECOUNT)
		{
			logger.info("HIGH MOTION - IGNORING FRAME - avgPossibleShotsDetected {} thresholdPixels {}", avgThresholdPixels, thresholdPixels);
			return true;
		}
		return false;
	}


	private final int BRIGHTNESS_WARNING_AVG_THRESHOLD = 500;
	private final int BRIGHTNESS_WARNING_FRAMECOUNT = 90;
	private boolean shouldShowBrightnessWarning() {
		if (avgThresholdPixels >= BRIGHTNESS_WARNING_AVG_THRESHOLD && cameraManager.getFrameCount() > BRIGHTNESS_WARNING_FRAMECOUNT)
			return true;
		return false;
	}


	private boolean checkIfInitialized() {
		if (cameraManager.getFrameCount()>INIT_FRAME_COUNT)
			return true;
		return false;
	}


	private ArrayList<Pixel> findThresholdPixelsAndUpdateFilter(BufferedImage workingCopy, boolean detectShots)
	{
		ArrayList<Pixel> thresholdPixels = new ArrayList<Pixel>();

		// In this loop we accomplish both MovingAverage updates AND threshold pixel detection
		Parallel.forIndex(0, workingCopy.getHeight(), 1, new Operation<Integer>()
		{

			public void perform (Integer y) {
				for (int x = 0; x < workingCopy.getWidth(); x++) {
						Optional<Pixel> pixel = updateFilter(workingCopy, x, y, detectShots);
						if(pixel.isPresent())
						{
							
							synchronized (thresholdPixels)
							{
								thresholdPixels.add(pixel.get());
							}

						}
				}

			}
		
		});
		
		return thresholdPixels;
	}
	
	
	
	
	
	
	
	private final int MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = 500;
	
	private void updateAvgThresholdPixels(int thresholdPixels)
	{
		if (avgThresholdPixels == -1)
			avgThresholdPixels = thresholdPixels;
		else
			avgThresholdPixels = (((cameraManager.getFPS()-1)*avgThresholdPixels)+Math.min(thresholdPixels,MAXIMUM_THRESHOLD_PIXELS_FOR_AVG))/cameraManager.getFPS();
	
	}
	
	public int getMinimumShotDimension()
	{
		if (cameraManager.getMinimumShotDimension().isPresent())
		{
			return cameraManager.getMinimumShotDimension().get();
		}
		return MINIMUM_SHOT_DIMENSION;
	}
	
	
	
	
	
	
	
	private void addShot(BufferedImage workingCopy, PixelCluster pc)
	{
		Optional<javafx.scene.paint.Color> color = pc.getColorJavafx(workingCopy, colorDiffMovingAverage);

		double x = pc.centerPixelX;
		double y = pc.centerPixelY;
		
		if (!color.isPresent())
			return;
		
		if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent() &&
				color.get().equals(config.getIgnoreLaserColor().get()))
			return;
		
		logger.info("Suspected shot accepted: Center ({}, {}), {}",
				x, y, color.get());
		
		
		Shot shot = new Shot(color.get(), x, y, 
				0, cameraManager.getFrameCount(), config.getMarkerRadius());
		
		
		if (config.isDebugShotsRecordToFiles() && config.getDeduplicationProcessor().processShotLookahead(shot)) {
			File outputfile = new File(String.format("shot-%d-%d_orig.png",(int)pc.centerPixelX, (int)pc.centerPixelY));
			try {
				ImageIO.write(workingCopy, "png", outputfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (Pixel p : pc)
			{
				if (color.get() == javafx.scene.paint.Color.GREEN)
					workingCopy.setRGB(p.x, p.y, 0x00FF00);
				else
					workingCopy.setRGB(p.x, p.y, 0xFF0000);
			}
			outputfile = new File(String.format("shot-%d-%d.png",(int)pc.centerPixelX, (int)pc.centerPixelY));
			try {
				ImageIO.write(workingCopy, "png", outputfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if ((cameraManager.isLimitingDetectionToProjection() || cameraManager.isCroppingFeedToProjection())
				&& cameraManager.getProjectionBounds().isPresent()) {

			Bounds b = cameraManager.getProjectionBounds().get();
			
			canvasManager.addShot(color.get(), x + b.getMinX(),
					y + b.getMinY());
		} else {
			canvasManager.addShot(color.get(), x,
					y);
		}
		

		
	}
}
