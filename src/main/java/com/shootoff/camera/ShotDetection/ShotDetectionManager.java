package com.shootoff.camera.ShotDetection;

import java.awt.Color;
import java.awt.Graphics;
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

public final class ShotDetectionManager {

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
	
	private final int MOTION_WARNING_FRAMECOUNT = 30;
	private final int MOTION_WARNING_AVG_THRESHOLD = 100;
	private final int MOTION_WARNING_THRESHOLD_PIXELS = 250;
	
	// Individual pixel threshold
	private final int EXCESSIVE_BRIGHTNESS_THRESHOLD = 245;
	private final int MINIMUM_BRIGHTNESS_INCREASE = 255-EXCESSIVE_BRIGHTNESS_THRESHOLD;
	
	// Aggregate # of pixel threshold
	private final int BRIGHTNESS_WARNING_AVG_THRESHOLD = 100;
	private final int BRIGHTNESS_WARNING_FRAMECOUNT = 90;
	// This is updated for every bright pixel
	private int brightPixels = 0;
	// The average is then calculated here
	private double avgBrightPixels = -1;

	// This is a short circuit for our pixel-color-changer to set the bad pixels red
	// without having complicated math every pixel
	private boolean shouldShowBrightnessWarningBool = false;
	private BufferedImage currentFullFrame;
	
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


	private Optional<Pixel> updateFilter(BufferedImage frame, int x, int y, boolean detectShots) {
		
		Optional<Pixel> result = Optional.empty();
		java.awt.Color currentC = new java.awt.Color(frame.getRGB(x, y));
		int currentRGB = currentC.getRGB();
		
		int currentLum = Pixel.calcLums(currentRGB);
		
		double colorDiff = Pixel.colorDistance(currentC, Color.RED) - Pixel.colorDistance(currentC, Color.GREEN);
		
        if (lumsMovingAverage[x][y] == -1)
        {
        	newLumsMovingAverage[x][y] = Math.min(currentLum, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
        	newColorDiffMovingAverage[x][y] = Math.min(colorDiff, MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
            return Optional.empty();

        }


		
		if (!detectShots)
			result = Optional.empty();
		
		if (pixelAboveExcessiveBrightnessThreshold(lumsMovingAverage[x][y]))
		{
			brightPixels++;
			
			// Make the feed pixels red so the user can easily see what the problem pixels are
			if (shouldShowBrightnessWarningBool)
				drawOnCurrentFrame(x,y,0xFF0000);
		}

		else if (pixelAboveThreshold(currentLum, lumsMovingAverage[x][y]))
			result = Optional.of(new Pixel(x,y, currentC, currentLum, lumsMovingAverage[x][y], colorDiffMovingAverage[x][y]));
		
		
        // Update the average brightness
		newLumsMovingAverage[x][y] = ((lumsMovingAverage[x][y] * (INIT_FRAME_COUNT-1)) + currentLum) / INIT_FRAME_COUNT;
		
    	// Update the color distance
		newColorDiffMovingAverage[x][y] = ((colorDiffMovingAverage[x][y] * (INIT_FRAME_COUNT-1)) + colorDiff) / INIT_FRAME_COUNT;
		
		return result;

	}
	
	
	// This function ADJUSTS X AND Y FOR LIMITING DETECTION BOUNDS
	private void drawOnCurrentFrame(int x, int y, int rgb)
	{
		int drawX = x;
		int drawY = y;
		if (cameraManager.isLimitingDetectionToProjection() && cameraManager.getProjectionBounds().isPresent()) {
			drawX = (int) (cameraManager.getProjectionBounds().get().getMinX()+x);
			drawY = (int) (cameraManager.getProjectionBounds().get().getMinY()+y);
		}
		currentFullFrame.setRGB(drawX, drawY, rgb);
	}
	
	
	private void applyFilter()
	{
		lumsMovingAverage = newLumsMovingAverage;
		colorDiffMovingAverage = newColorDiffMovingAverage;
	}

	

	private boolean pixelAboveExcessiveBrightnessThreshold(int lumsMovingAverage)
	{
		if (lumsMovingAverage>EXCESSIVE_BRIGHTNESS_THRESHOLD)
			return true;
		return false;
	}
	
	
	
	

	private boolean pixelAboveThreshold(int currentLum, int lumsMovingAverage)
	{
		int threshold = (255-lumsMovingAverage)/2;
		int increase = (currentLum-lumsMovingAverage);
		
		int dynamic_threshold = threshold+(int)((255-threshold)*(avgThresholdPixels/(double)MAXIMUM_THRESHOLD_PIXELS_FOR_AVG));
		
		
		if (increase<MINIMUM_BRIGHTNESS_INCREASE)
			return false;
		
		if (increase<dynamic_threshold)
			return false;
		
		return true;
		
	}
	
	public static BufferedImage deepCopy(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	public boolean processFrame(BufferedImage frame, boolean detectShots) {
		// This is the FULL, ORIGINAL FRAME passed from CameraManager
		currentFullFrame = frame;
		
		BufferedImage workingCopy = null;
		
		
		if (cameraManager.isLimitingDetectionToProjection() && cameraManager.getProjectionBounds().isPresent()) {
			Bounds b = cameraManager.getProjectionBounds().get();
			BufferedImage subFrame = frame.getSubimage((int)b.getMinX(), (int)b.getMinY(),
					(int)b.getWidth(), (int)b.getHeight());
			workingCopy = deepCopy(subFrame);
			
		} else {
			
			workingCopy = deepCopy(frame);
			
		}
		
		// Must reset before every updateFilter loop
		brightPixels = 0;
			
		ArrayList<Pixel> thresholdPixels = findThresholdPixelsAndUpdateFilter(workingCopy, (detectShots && filtersInitialized));
		
		logger.trace("thresholdPixels {}", thresholdPixels.size());
		
		if (checkIfInitialized())
			filtersInitialized = true;
		
		
		if (detectShots && filtersInitialized)
		{
			updateAvgThresholdPixels(thresholdPixels.size());
			
			updateAvgBrightPixels(brightPixels);

			
			if (shouldShowBrightnessWarning())
			{
				cameraManager.showBrightnessWarning();

			}
		
			// We don't show a warning if the excessive motion happens very early in the feed
			// But we still need to skip detection on those frames
			// That's why this is in two ifs
			if (isExcessiveMotion(thresholdPixels.size()))
			{
				if (shouldShowMotionWarning(thresholdPixels.size()))
					cameraManager.showMotionWarning();
				
				for (Pixel pixel : thresholdPixels)
				{
					drawOnCurrentFrame(pixel.x, pixel.y, 0x0000FF);
				}
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





	private boolean isExcessiveMotion(int thresholdPixels) {
		
		if ((avgThresholdPixels > MOTION_WARNING_AVG_THRESHOLD || (thresholdPixels > MOTION_WARNING_THRESHOLD_PIXELS)))
			return true;

		return false;
	}
	
	private boolean shouldShowMotionWarning(int thresholdPixels) {
		
		if (cameraManager.getFrameCount() > MOTION_WARNING_FRAMECOUNT)
		{
			logger.debug("HIGH MOTION - IGNORING FRAME - avgThresholdPixels {} thresholdPixels {}", avgThresholdPixels, thresholdPixels);

			return true;
		}
		return false;
	}



	private boolean shouldShowBrightnessWarning() {
		logger.trace("avgBrightPixels {}", avgBrightPixels);
		
		
		if (avgBrightPixels >= BRIGHTNESS_WARNING_AVG_THRESHOLD && cameraManager.getFrameCount() > BRIGHTNESS_WARNING_FRAMECOUNT)
		{
			logger.debug("HIGH BRIGHTNESS - avgBrightPixels {}", avgBrightPixels);
			
			shouldShowBrightnessWarningBool = true;
			
			return true;
		}
		
		shouldShowBrightnessWarningBool = false;
		return false;
	}


	private boolean checkIfInitialized() {
		if (cameraManager.getFrameCount()>INIT_FRAME_COUNT)
			return true;
		return false;
	}


	private ArrayList<Pixel> findThresholdPixelsAndUpdateFilter(BufferedImage workingCopy, boolean detectShots)
	{
		final int subWidth = workingCopy.getWidth() / SECTOR_COLUMNS;
		final int subHeight = workingCopy.getHeight() / SECTOR_ROWS;
		
		final boolean[][] sectorstatuses = cameraManager.getSectorStatuses();
		
		
		ArrayList<Pixel> thresholdPixels = new ArrayList<Pixel>();

		// In this loop we accomplish both MovingAverage updates AND threshold pixel detection
		Parallel.forIndex(0, (SECTOR_ROWS*SECTOR_COLUMNS), 1, new Operation<Integer>()
		{

			public void perform (Integer sector) {
				Integer sectorX = (sector%SECTOR_COLUMNS);
				Integer sectorY = (int)Math.floor((sector/SECTOR_ROWS));
				
				Integer startX = subWidth*sectorX;
				Integer startY = subHeight*sectorY;
				
				logger.trace("{} - {} {} - {} {}", sector, sectorX, sectorY, startX, startY);
				
				if (!sectorstatuses[sectorY][sectorX])
					return;
				
				for (Integer y = startY; y < startY + subHeight; y++)
				{
					
					for (Integer x = startX; x < startX + subWidth; x++)
					{

						Optional<Pixel> pixel = Optional.empty();
						
						pixel = updateFilter(workingCopy, x, y, detectShots);
						

						if(pixel.isPresent())
						{
							
							synchronized (thresholdPixels)
							{
								thresholdPixels.add(pixel.get());
							}

						}
						
					}
				}

			}
		
		});
		
		return thresholdPixels;
	}
	
	
	
	
	
	
	
	private final int MAXIMUM_THRESHOLD_PIXELS_FOR_AVG = 200;
	
	private void updateAvgThresholdPixels(int thresholdPixels)
	{
		if (avgThresholdPixels == -1)
			avgThresholdPixels = Math.min(thresholdPixels,MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
			avgThresholdPixels = (((cameraManager.getFPS()-1)*avgThresholdPixels)+Math.min(thresholdPixels,MAXIMUM_THRESHOLD_PIXELS_FOR_AVG))/cameraManager.getFPS();
	
	}
	
	private void updateAvgBrightPixels(int brightPixels)
	{
		if (avgBrightPixels == -1)
			avgBrightPixels = Math.min(brightPixels,MAXIMUM_THRESHOLD_PIXELS_FOR_AVG);
		else
			avgBrightPixels = (((cameraManager.getFPS()-1)*avgBrightPixels)+Math.min(brightPixels,MAXIMUM_THRESHOLD_PIXELS_FOR_AVG))/cameraManager.getFPS();
	
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
