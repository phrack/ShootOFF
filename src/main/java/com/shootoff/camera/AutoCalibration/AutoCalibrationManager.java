package com.shootoff.camera.AutoCalibration;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.util.Callback;

import javax.imageio.ImageIO;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.camera.CameraIntrinsics;
import org.openimaj.image.camera.calibration.CameraCalibration;
import org.openimaj.image.camera.calibration.CameraCalibrationZhang;
import org.openimaj.image.camera.calibration.ChessboardCornerFinder;
import org.openimaj.image.camera.calibration.FastChessboardDetector;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.util.pair.IndependentPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;


public class AutoCalibrationManager implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(AutoCalibrationManager.class);
	
	public final static int PATTERN_WIDTH = 9;
	public final static int PATTERN_HEIGHT = 6;
	
	private FastChessboardDetector fastFinder = new FastChessboardDetector(PATTERN_WIDTH, PATTERN_HEIGHT);
	private ChessboardCornerFinder cbFinder = new ChessboardCornerFinder(PATTERN_WIDTH, PATTERN_HEIGHT, ChessboardCornerFinder.Options.ADAPTIVE_THRESHOLD);

	
	private BufferedImage frame;
	
	private Callback<List<Point2dImpl>, Void> callback;
	
	public void setCallback(Callback<List<Point2dImpl>, Void> callback) {
		this.callback = callback;
	}

	public void setFrame(BufferedImage frame) {
		this.frame = frame;
	}

	public AutoCalibrationManager()
	{

	}
	
	public void test()
	{
		BufferedImage testFrame;
		try {
			testFrame = ImageIO.read(
					AutoCalibrationManager.class.getClassLoader().getResourceAsStream("pattern.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

	    ColorSpace grayCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	    ColorConvertOp grayConvertOp = new ColorConvertOp(grayCS, null);
	    BufferedImage grayScale = grayConvertOp.filter(testFrame, null);
		
		
		FImage oiFrame = null;
		oiFrame = ImageUtilities.createFImage(grayScale);
		
		findChessboard(oiFrame);
		
		
		File outputfile = new File("test.png");
		try {
			ImageIO.write(grayScale, "png", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public AutoCalibrationManager(BufferedImage frame)
	{
		this.frame = frame;
	}
	
	public List<Point2dImpl> findChessboardBufferedImage()
	{
		FImage oiFrame = null;
		oiFrame = ImageUtilities.createFImage(frame);
		
		return findChessboard(oiFrame);
	}
	
	public List<Point2dImpl> findChessboardBufferedImage(BufferedImage frame)
	{
		/*BufferedImage grayScale = new BufferedImage(frame.getWidth(),
				frame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);*/
		
	    ColorSpace grayCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	    ColorConvertOp grayConvertOp = new ColorConvertOp(grayCS, null);
	    BufferedImage grayScale = grayConvertOp.filter(frame, null);
		
		
		
		FImage oiFrame = null;
		oiFrame = ImageUtilities.createFImage(grayScale);
		
		List<Point2dImpl> corners =  findChessboard(oiFrame);
		
		if (corners == null)
			return null;
		
		/*MBFImage mbfFrame = ImageUtilities.createMBFImage(frame, false);
		
		ChessboardCornerFinder.drawChessboardCorners(mbfFrame, 9, 6, corners, true);
		
		frame = ImageUtilities.createBufferedImage(mbfFrame);

		File outputfile = new File("test.png");
		try {
			ImageIO.write(frame, "png", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		return corners;
	}
	
	
	private List<Point2dImpl> findChessboard(FImage oiFrame)
	{
		
		/*fastFinder.analyseImage(oiFrame);
		
		if (!fastFinder.chessboardDetected())
			return null;
		*/

		cbFinder.analyseImage(oiFrame);
		
		if (cbFinder.isFound())
		{
			List<Point2dImpl> corners = cbFinder.getCorners();
			
			logger.warn("found {} {}", corners.get(0).x, corners.get(0).y);

			calibrate(oiFrame, corners);
			
			return corners;
		}
		else
		{
			logger.trace("not found");
			
			return null;
		}
	}
	
	public void calibrate(FImage oiFrame, List<Point2dImpl> corners)
	{
		
		List<List<? extends IndependentPair<? extends Point2d, ? extends Point2d>>> listCorners = new ArrayList<List<? extends IndependentPair<? extends Point2d, ? extends Point2d>>>();
		
		
        int i, j, k;
        
        ArrayList<IndependentPair<? extends Point2d, ? extends Point2d>> firstFrameSquares = new ArrayList<IndependentPair<? extends Point2d, ? extends Point2d>>();


        
        /*for (k = 0; k < 2; k++)
        {
                for (i = 0; i < (k == 0 ? PATTERN_HEIGHT : PATTERN_WIDTH); i++)
                {
                        final Point2dImpl a = k == 0 ? corners.get(i * PATTERN_WIDTH) : corners.get(i);
                        final Point2dImpl b = k == 0 ? corners.get((i + 1) * PATTERN_WIDTH - 1) :
                        	corners.get((PATTERN_HEIGHT - 1) * PATTERN_WIDTH + i);

                        IndependentPair<? extends Point2d, ? extends Point2d> square = new IndependentPair<Point2dImpl,Point2dImpl>(a,b);
                        
                        logger.warn("square {} {} {} {} - {} {}", a.x, a.y, b.x, b.y, b.x-a.x, b.y-a.y);
                        
                        listCorners.add(firstFrameSquares);
                        
                }
        }*/
		
		logger.warn("found {} {}", corners.get(0).x, corners.get(0).y);
		
		/*CameraCalibration calibrator = new CameraCalibration(listCorners, oiFrame.width, oiFrame.height);
		
		CameraIntrinsics cameraIntrinsics  = calibrator.getIntrisics();*/
		
	}
	
	public Optional<Bounds> calcBounds(List<Point2dImpl> corners)
	{
		
		double minX = CameraManager.FEED_WIDTH, minY = CameraManager.FEED_HEIGHT;
        double biggestWidth = 0;
        double biggestHeight = 0;
        
        for (int k = 0; k < PATTERN_HEIGHT-1; k++)
        {
            for (int j = 0; j < PATTERN_WIDTH-1; j++)
            {
            	int anum = (k*(PATTERN_WIDTH))+(j);
            	int bnum = ((k+1)*(PATTERN_WIDTH))+(j+1);
            	
            	
            	final Point2dImpl a = corners.get(anum);
            	final Point2dImpl b = corners.get(bnum);
            	
            	double squareWidth = b.x-a.x;
            	double squareHeight = b.y-a.y;
            	
            	if (squareWidth<0 || squareHeight<0)
            	{
            		logger.warn("Calibration pattern is upside down!");
            		return Optional.empty();
            	}

            	logger.trace("{} {} - {} {}", anum, bnum, squareWidth, squareHeight);

            	
            	if (squareWidth > biggestWidth)
            		biggestWidth = squareWidth;
            	if (squareHeight > biggestHeight)
            		biggestHeight = squareHeight;
            	
            	if (a.x < minX)
            		minX = a.x;
            	if (a.y < minY)
            		minY = a.y;
            	
            }
            
        }
        
    	logger.warn("square {} {}", biggestWidth, biggestHeight);
		
    	minX = minX - biggestWidth;
    	minY = minY - biggestHeight;
		
		
		double width = biggestWidth * (PATTERN_WIDTH+1);
		double height = biggestHeight * (PATTERN_HEIGHT+1);
		
		if (minX<0 || minY<0 || width>CameraManager.FEED_WIDTH || height>CameraManager.FEED_HEIGHT)
			return Optional.empty();
		

		logger.warn("Calibrating to {} {} with width {} height {}", minX, minY, width, height);
		
		
		return Optional.of(new BoundingBox(minX, minY, width, height));
	}

	@Override
	public void run() {
		
		List<Point2dImpl> result = findChessboardBufferedImage(frame);
		
		if (callback != null && result != null)
		{
			callback.call(result);
		}
	}

}
