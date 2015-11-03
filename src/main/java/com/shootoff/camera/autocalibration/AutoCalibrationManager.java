package com.shootoff.camera.autocalibration;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.util.Callback;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.xuggle.xuggler.video.ConverterFactory;


public class AutoCalibrationManager implements Runnable {
	
	private final Logger logger = LoggerFactory.getLogger(AutoCalibrationManager.class);
	
	public final static int PATTERN_WIDTH = 9;
	public final static int PATTERN_HEIGHT = 6;
	
    private Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);
	
	private BufferedImage frame;
	
	private final static int MIN_BOARDS = 3;
	
	MatOfPoint2f imageCorners = new MatOfPoint2f();

	private boolean isCalibrated = false;
	private int seenChessboards = 0;
	
	private Callback<Optional<Bounds>, Void> callback;
	
	public void setCallback(Callback<Optional<Bounds>, Void> callback) {
		this.callback = callback;
	}

	public void setFrame(BufferedImage frame) {
		this.frame = frame;
	}

	public AutoCalibrationManager()
	{
	}
	
	public Mat bufferedImageToMat(BufferedImage frame)
	{
		BufferedImage transformedFrame = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		byte[] pixels = ((DataBufferByte)transformedFrame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		
		return mat;
	}

	private void reset()
	{
		isCalibrated = false;
		seenChessboards = 0;
		warpInitialized = false;
	}

	@Override
	public void run() {
		if (isCalibrated)
		{
			reset();
		}
		
		
		Optional<Bounds> bounds = processFrame(frame);
		
		if (callback != null && bounds.isPresent())
		{
			callback.call(bounds);
		}
	}
	
	public Mat storedMat;
	public Optional<Bounds> processFrame(BufferedImage frame)
	{
		Mat mat = bufferedImageToMat(frame);
		
		storedMat = mat;
		
		Optional<MatOfPoint2f> boardCorners = findChessboard(mat);
		
		if (!boardCorners.isPresent())
			return Optional.empty();
			
		Mat undistorted = new Mat();
		
		Bounds estimatedBounds = estimateBounds(boardCorners.get());
		
		Optional<MatOfPoint> contour = findContour(mat, estimatedBounds);
		
		if (!contour.isPresent())
			return Optional.empty();
		
		
		if (logger.isTraceEnabled())
		{
			String filename = String.format("calibrate-dist-%s.png",seenChessboards);
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, mat);
		}
		
		undistorted = warpPerspective(mat);
		
		// TODO: HANDLE UPSIDE DOWN PATTERN BY WARNING USER AND NOT CALIBRATING
		
		Optional<Bounds> bounds = Optional.of(boundingBox);

		if (logger.isTraceEnabled())
		{
			String filename = String.format("calibrate-undist-%s.png",seenChessboards);
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, undistorted);
		}
		
		if (!bounds.isPresent())
			return Optional.empty();
		
		

		
		
		String filename = String.format("calibrate-undist-lines-%s.png",seenChessboards);
		File file = new File(filename);
		filename = file.toString();
		Highgui.imwrite(filename, undistorted);
		
		

		if (logger.isTraceEnabled())
		{
			/*undistorted = undistorted.submat(minY, minY+height, minX, minX+width);
			
			String filename = String.format("calibrate-undist-cropped-%s.png",seenChessboards);
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, undistorted);*/
		}
		
		isCalibrated = true;
		
		return bounds;

	}
	
	
	
	private Bounds estimateBounds(MatOfPoint2f boardCorners)
	{
		double rotationAngle = calcAngle(boardCorners);
		
		
		MatOfPoint2f boardRect = calcBoardRectFromCorners(boardCorners);
		
		RotatedRect boardBox = Imgproc.minAreaRect(boardRect);
		Mat rotMat = getRotationMatrix(boardBox.center, boardBox.angle);
		
		
		MatOfPoint2f rotatedRect = rotateRect(rotMat, boardRect);
		
		logger.warn("center {} angle {}", boardBox.center, -boardBox.angle);
		
		//Mat bounds = calcMatBoundsFromDimensions(boardCorners);
		MatOfPoint2f cropsrc = new MatOfPoint2f();
		cropsrc.alloc(4);
		cropsrc.put(0, 0, rotatedRect.get(0, 0));
		cropsrc.put(1, 0, rotatedRect.get(1, 0));
		cropsrc.put(2, 0, rotatedRect.get(3, 0));
		cropsrc.put(3, 0, rotatedRect.get(2, 0));
		

		Core.line(storedMat, new Point(rotatedRect.get(0,0)[0], rotatedRect.get(0,0)[1]), new Point(rotatedRect.get(1,0)[0], rotatedRect.get(1,0)[1]), new Scalar(0, 255, 0));
		Core.line(storedMat, new Point(rotatedRect.get(1,0)[0], rotatedRect.get(1,0)[1]), new Point(rotatedRect.get(2,0)[0], rotatedRect.get(2,0)[1]), new Scalar(0, 255, 0));
		Core.line(storedMat, new Point(rotatedRect.get(3,0)[0], rotatedRect.get(3,0)[1]), new Point(rotatedRect.get(2,0)[0], rotatedRect.get(2,0)[1]), new Scalar(0, 255, 0));
		Core.line(storedMat, new Point(rotatedRect.get(3,0)[0], rotatedRect.get(3,0)[1]), new Point(rotatedRect.get(0,0)[0], rotatedRect.get(0,0)[1]), new Scalar(0, 255, 0));
		
		
		MatOfPoint2f estimatedPatternSizeRect = estimateFullPatternSize(rotatedRect);
		
		Mat unRotMat = getRotationMatrix(boardBox.center, -boardBox.angle);
		
		
		MatOfPoint2f unRotatedPatternSizeRect = rotateRect(unRotMat, estimatedPatternSizeRect);
		

		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(0,0)[0], unRotatedPatternSizeRect.get(0,0)[1]), new Point(unRotatedPatternSizeRect.get(1,0)[0], unRotatedPatternSizeRect.get(1,0)[1]), new Scalar(255, 0, 0));
		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(1,0)[0], unRotatedPatternSizeRect.get(1,0)[1]), new Point(unRotatedPatternSizeRect.get(2,0)[0], unRotatedPatternSizeRect.get(2,0)[1]), new Scalar(255, 0, 0));
		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(3,0)[0], unRotatedPatternSizeRect.get(3,0)[1]), new Point(unRotatedPatternSizeRect.get(2,0)[0], unRotatedPatternSizeRect.get(2,0)[1]), new Scalar(255, 0, 0));
		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(3,0)[0], unRotatedPatternSizeRect.get(3,0)[1]), new Point(unRotatedPatternSizeRect.get(0,0)[0], unRotatedPatternSizeRect.get(0,0)[1]), new Scalar(255, 0, 0));
		
		
		// unRotatedPatternSizeRect is pretty good but estimateFullPatternSize should do a better job of accounting for the distortion of the rectangle
		
		// Next step is to verify contour, then use the contour information to warp the perspective
		
		RotatedRect box = Imgproc.minAreaRect(cropsrc);

		
		Point[] points = new Point[4];
		box.points(points);
		
		logger.warn("angle {} {}", rotationAngle, box.angle);
		
		rotMat = getRotationMatrix(box.center, -box.angle);
		
		for (Point point : points)
		{
			Point rpoint = rotPoint(rotMat, point);
			logger.warn("point {} {} to {} {}", point.x, point.y, rpoint.x, rpoint.y);
			
			
			Core.line(storedMat, point, rpoint, new Scalar(0, 0, 255));
		}
		
		Bounds boundingBox = new BoundingBox(box.boundingRect().x, box.boundingRect().y, box.boundingRect().width, box.boundingRect().height);
		
		String filename = String.format("calibrate-undist-estimatedBounds-%s.png",seenChessboards);
		File file = new File(filename);
		filename = file.toString();
		Highgui.imwrite(filename, storedMat);
		
		return boundingBox;
	}
	
	private MatOfPoint2f estimateFullPatternSize(MatOfPoint2f rotatedRect) {
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);
		
		double borderFactor = 0.066667;
		
		Point topLeft = new Point(rotatedRect.get(0,0)[0], rotatedRect.get(0,0)[1]);
		Point topRight = new Point(rotatedRect.get(1,0)[0], rotatedRect.get(1,0)[1]);
		Point bottomRight = new Point(rotatedRect.get(2,0)[0], rotatedRect.get(2,0)[1]);
		Point bottomLeft = new Point(rotatedRect.get(3,0)[0], rotatedRect.get(3,0)[1]);
		
		logger.warn("points {} {} {} {}", topLeft, topRight, bottomRight, bottomLeft);
		
		double width = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double height = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		//angle = int(math.atan((y1-y2)/(x2-x1))*180/math.pi)
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		//if (logger.isTraceEnabled())
		logger.warn("square size {} {} - angle {}", width/(PATTERN_WIDTH-1), height/(PATTERN_HEIGHT-1), angle);
		
		double squareWidth = (1+borderFactor)*(width/(PATTERN_WIDTH-1));
		double squareHeight = (1+borderFactor)*(height/(PATTERN_HEIGHT-1));
		
		double[] newTopLeft = { topLeft.x - squareWidth, topLeft.y - squareHeight };
		double[] newBottomLeft = { bottomLeft.x - squareWidth, bottomLeft.y + squareHeight };
		double[] newTopRight = { topRight.x + squareWidth, topRight.y - squareHeight };
		double[] newBottomRight = { bottomRight.x + squareWidth, bottomRight.y + squareHeight };

		result.put(0, 0, newTopLeft);
		result.put(1, 0, newTopRight);
		result.put(2, 0, newBottomRight);
		result.put(3, 0, newBottomLeft);
		
		return result;
	}

	private MatOfPoint2f rotateRect(Mat rotMat, MatOfPoint2f boardRect) {
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);
		for (int i = 0; i < 4; i++)
		{
			Point rPoint = rotPoint(rotMat, new Point(boardRect.get(i, 0)[0], boardRect.get(i, 0)[1]));
			double[] rPointD = new double[2];
			rPointD[0] = rPoint.x;
			rPointD[1] = rPoint.y;
			result.put(i, 0, rPointD);
		}
		return result;
	}

	private Mat getRotationMatrix(Point center, double rotationAngle)
	{
		return Imgproc.getRotationMatrix2D( center, rotationAngle, 1.0 );
	}
	
	private Optional<MatOfPoint> findContour(Mat frame, Bounds estimatedBounds)
	{
	    Mat grey = new Mat();
	    Mat temp;
	    Imgproc.cvtColor(frame, grey, Imgproc.COLOR_BGR2GRAY);

		Imgproc.Canny(grey, grey, 50, 150); 

	    temp = grey.clone();
	    
	    logger.warn("boundslines {} {} {} {}", estimatedBounds.getMinX(), estimatedBounds.getMinY(), estimatedBounds.getMaxX(), estimatedBounds.getMaxY());
	    
		final Mat hierarchy = new Mat();
        final List<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();
        Imgproc.findContours(temp, contoursList, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint contour : contoursList) {

        	Rect rect = Imgproc.boundingRect( contour );
        	if (rect.width >= estimatedBounds.getWidth() && rect.height >= estimatedBounds.getHeight())
        	{
        		logger.warn("contour {} {} - {} {} {} {}", contour.rows(), contour.cols(), rect.x, rect.y, rect.x+rect.width, rect.y+rect.height);

        		return Optional.of(contour);
        	}
        }
        //Imgproc.drawContours(undistorted, contoursList, -1, new Scalar(255, 0, 0));
		
		String filename = String.format("calibrate-undist-grey-lines-%s.png",seenChessboards);
		File file = new File(filename);
		filename = file.toString();
		Highgui.imwrite(filename, grey);			
		
		//logger.warn("boundslines {} {} {} {}", minX, minY, maxX, maxY);
		
	    
	    return Optional.empty();
	}
	
	
	
	
	
	
	
	
	
	
	private boolean findProjectionArea(Mat frame)
	{
		boolean result = false;
		
		int maxX = 0, maxY = 0, minX = CameraManager.FEED_WIDTH, minY = CameraManager.FEED_HEIGHT;
		
	    Mat grey = new Mat();
	    Mat temp;
	    Imgproc.cvtColor(frame, grey, Imgproc.COLOR_BGR2GRAY);

		Imgproc.Canny(grey, grey, 50, 150); 

	    temp = grey.clone();
	    
	    // use actual contour bounds and estimate of pattern dimensions to transform
		
		final Mat hierarchy = new Mat();
        final List<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();
        Imgproc.findContours(temp, contoursList, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint contour : contoursList) {
        	result = false;

        	Rect rect = Imgproc.boundingRect( contour );
        	if (rect.width > boundingBox.getWidth() && rect.height > boundingBox.getHeight())
        	{
        		logger.warn("contour {} {} - {} {} {} {}", contour.rows(), contour.cols(), rect.x, rect.y, rect.x+rect.width, rect.y+rect.height);
        		Core.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y+rect.height), new Scalar(255,0,0));
        		
        		minX = rect.x;
        		minY = rect.y;
        		maxX = rect.x+rect.width;
        		maxY = rect.y+rect.height;
        		
                boundingBox = new BoundingBox(minX, minY, rect.width, rect.height);
                result = true;
        	}
        }
        //Imgproc.drawContours(undistorted, contoursList, -1, new Scalar(255, 0, 0));
		
		String filename = String.format("calibrate-undist-grey-lines-%s.png",seenChessboards);
		File file = new File(filename);
		filename = file.toString();
		Highgui.imwrite(filename, grey);			
		
		logger.warn("boundslines {} {} {} {}", minX, minY, maxX, maxY);
		
		return result;
	}
	
	public BufferedImage matToBufferedImage(Mat matBGR){  
	      int width = matBGR.width(), height = matBGR.height(), channels = matBGR.channels() ;  
	      byte[] sourcePixels = new byte[width * height * channels];  
	      matBGR.get(0, 0, sourcePixels);  
 
	      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);  
	      final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();  
	      System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);  

	      return image;  
	}  
	
	public Optional<MatOfPoint2f> findChessboardBufferedImage(BufferedImage frame)
	{
		return findChessboard(bufferedImageToMat(frame));
	}
	
	private final TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
	private Optional<MatOfPoint2f> findChessboard(Mat mat)
	{
		
		Mat grayImage = new Mat();

	    Imgproc.cvtColor(mat, grayImage, Imgproc.COLOR_BGR2GRAY);
	    
	    MatOfPoint2f imageCorners = new MatOfPoint2f();
	    
		boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners, Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_ADAPTIVE_THRESH);
		if (found) 
		{

			
			// optimization
			Imgproc.cornerSubPix(grayImage, imageCorners, new Size(5, 5), new Size(-1, -1), term);
			
			if (logger.isTraceEnabled())
			{
				String filename = String.format("calibrate-marked-%s.png",seenChessboards);
				File file = new File(filename);
				filename = file.toString();
				Highgui.imwrite(filename, mat);
			}
			
			return Optional.of(imageCorners);
		}
		return Optional.empty();
	}
	
	private MatOfPoint2f rotatedCorners = null;
	private Mat perspMat = null;
	private Bounds boundingBox = null;
	private boolean warpInitialized = false;
	private double rotationAngle = 0.0;
	Point realCenter = null;
	Mat rotMatrix = null;
	
	

	public Mat warpPerspective(final Mat image)
	{
		Mat mat = image.clone();
		
		if (!warpInitialized)
		{
			warpInitialized = true;
			
			Point realCenter = new Point(image.rows()/2, image.cols()/2);
			
			// Get the rotation matrix with the specifications above
			rotatedCorners = new MatOfPoint2f();
			rotatedCorners.alloc(imageCorners.rows());			
			rotationAngle = calcAngle(imageCorners);
			logger.debug("calcAngle {}", rotationAngle);
			
			rotMatrix = Imgproc.getRotationMatrix2D( realCenter, rotationAngle, 1.0 );
			for (int i = 0; i < imageCorners.rows(); i++)
			{
				Point newpt = rotPoint(rotMatrix, new Point(imageCorners.get(i, 0)[0], imageCorners.get(i, 0)[1]));
				logger.trace("old pt x {} y {} - new pt x {} y {}", imageCorners.get(i,0)[0], imageCorners.get(i,0)[1], newpt.x, newpt.y);
				rotatedCorners.put(i, 0, newpt.x, newpt.y);
			}
			
			Imgproc.warpAffine( mat, mat, rotMatrix, mat.size() );
			

			//1st-------2nd
			// |         |
			// |         |
			// |         |
			//3rd-------4th
			Mat bounds = calcMatBoundsFromDimensions(rotatedCorners);
			MatOfPoint2f cropsrc = new MatOfPoint2f();
			cropsrc.alloc(4);
			cropsrc.put(0, 0, bounds.get(0, 0));
			cropsrc.put(1, 0, bounds.get(1, 0));
			cropsrc.put(2, 0, bounds.get(3, 0));
			cropsrc.put(3, 0, bounds.get(2, 0));
			

			
			RotatedRect box = Imgproc.minAreaRect(cropsrc);

			
			boundingBox = new BoundingBox(box.boundingRect().x, box.boundingRect().y, box.boundingRect().width, box.boundingRect().height);
			
			logger.warn("orig bounds - {} {} {} {}", boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getWidth(), boundingBox.getHeight());
			
			findProjectionArea(mat);
			
			logger.warn("ratios - {} - expected {}", boundingBox.getWidth()/boundingBox.getHeight(), (9.0/7.0));

			MatOfPoint2f cropdst = new MatOfPoint2f();
			cropdst.alloc(4);
			cropdst.put(0, 0, boundingBox.getMinX(), boundingBox.getMinY(),
					boundingBox.getMinX()+boundingBox.getWidth(), boundingBox.getMinY(),
					boundingBox.getMinX(), boundingBox.getMinY()+boundingBox.getHeight(),
					boundingBox.getMinX()+boundingBox.getWidth(), boundingBox.getMinY()+boundingBox.getHeight());
			
			if (logger.isWarnEnabled())
			{
				logger.warn("bounds {} - {} - {}", bounds.get(0, 0), cropsrc.get(0, 0), cropdst.get(0, 0));
				logger.warn("bounds {} - {} - {}", bounds.get(1, 0), cropsrc.get(1, 0), cropdst.get(1, 0));
				logger.warn("bounds {} - {} - {}", bounds.get(2, 0), cropsrc.get(2, 0), cropdst.get(2, 0));
				logger.warn("bounds {} - {} - {}", bounds.get(3, 0), cropsrc.get(3, 0), cropdst.get(3, 0));
			
				logger.warn("sizes {} {} - {} {} {}", cropsrc.size(), cropdst.size(), box.angle, box.center, box.size);
			}
			
			perspMat = Imgproc.getPerspectiveTransform(cropsrc, cropdst);
			
			Imgproc.warpPerspective(mat,mat,perspMat,mat.size(), Imgproc.INTER_LINEAR);
			
			
		}
		else
		{
		
			Imgproc.warpAffine( mat, mat, rotMatrix, mat.size() );
		
			Imgproc.warpPerspective(mat,mat,perspMat,mat.size(), Imgproc.INTER_LINEAR);
		}
			
		return mat;
	}
	

	private Point rotPoint(Mat rot_mat, Point point)
	{
		Point rp = new Point();
		rp.x = rot_mat.get(0, 0)[0] * point.x + rot_mat.get(0, 1)[0] * point.y +  rot_mat.get(0, 2)[0];
		rp.y = rot_mat.get(1, 0)[0] * point.x + rot_mat.get(1, 1)[0] * point.y +  rot_mat.get(1, 2)[0];

		return rp;
	}
	
	private double calcAngle(MatOfPoint2f corners)
	{
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		return angle;
	}
	
	
	private MatOfPoint2f calcBoardRectFromCorners(MatOfPoint2f corners)
	{
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);
		
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		Point bottomRight = new Point(corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		result.put(0, 0, topLeft.x, topLeft.y, topRight.x, topRight.y, bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y);
		
		return result;
	}
	
	private Mat calcMatBoundsFromDimensions(MatOfPoint2f corners)
	{
		// TODO: HANDLE UPSIDE DOWN PATTERN
		
		Mat result = new Mat(4,1,CvType.CV_32FC2);
		
		double borderFactor = 0.066667;
		
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		Point bottomRight = new Point(corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		logger.warn("points {} {} {} {}", topLeft, topRight, bottomRight, bottomLeft);
		
		double width = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double height = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		//angle = int(math.atan((y1-y2)/(x2-x1))*180/math.pi)
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		//if (logger.isTraceEnabled())
		logger.warn("square size {} {} - angle {}", width/(PATTERN_WIDTH-1), height/(PATTERN_HEIGHT-1), angle);
		
		double squareWidth = (1+borderFactor)*(width/(PATTERN_WIDTH-1));
		double squareHeight = (1+borderFactor)*(height/(PATTERN_HEIGHT-1));
		
		double[] newTopLeft = { topLeft.x - squareWidth, topLeft.y - squareHeight };
		double[] newBottomLeft = { bottomLeft.x - squareWidth, bottomLeft.y + squareHeight };
		double[] newTopRight = { topRight.x + squareWidth, topRight.y - squareHeight };
		double[] newBottomRight = { bottomRight.x + squareWidth, bottomRight.y + squareHeight };

		result.put(0, 0, newTopLeft[0], newTopLeft[1], newTopRight[0], newTopRight[1], newBottomRight[0], newBottomRight[1], newBottomLeft[0], newBottomLeft[1]);
		
		return result;
	}
	

	public BufferedImage undistortFrame(BufferedImage frame, int frameCount) {
		if (!isCalibrated)
		{
			logger.warn("undistortFrame called when isCalibrated is false");
			return null;
		}
		
		Mat mat = bufferedImageToMat(frame);
		
		frame = matToBufferedImage(warpPerspective(mat));
		
		return frame;
	}
}