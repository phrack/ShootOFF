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
		String filename;
		File file;
		
		Mat mat = bufferedImageToMat(frame);
		
		storedMat = mat.clone();
		
		Optional<MatOfPoint2f> boardCorners = findChessboard(mat);
		
		if (!boardCorners.isPresent())
			return Optional.empty();
			
		Mat undistorted = new Mat();
		
		MatOfPoint2f estimatedPatternRect = estimatePatternRect(boardCorners.get());
		
		Optional<MatOfPoint2f> idealCorners = findIdealCorners(mat, estimatedPatternRect);
		
		if (!idealCorners.isPresent())
			return Optional.empty();
		
		
		/*if (logger.isTraceEnabled())
		{*/
			filename = String.format("calibrate-dist-%s.png",seenChessboards);
			file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, mat);
		/*}*/
		
		initializeWarpPerspective(mat, idealCorners.get());
			
		undistorted = warpPerspective(mat);
		
		
		Bounds bounds = boundingBox;
		logger.warn("bounds {} {} {} {}", boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getWidth(), boundingBox.getHeight());
		
		

		filename = String.format("calibrate-undist-%s.png",seenChessboards);
		file = new File(filename);
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
		
		return Optional.of(bounds);

	}
	
	
	
	private MatOfPoint2f estimatePatternRect(MatOfPoint2f boardCorners)
	{
		double rotationAngle = calcAngle(boardCorners);
		
		double boardBoxAngle = 0;
		
		
		MatOfPoint2f boardRect = calcBoardRectFromCorners(boardCorners);
		
		

		RotatedRect boardBox = Imgproc.minAreaRect(boardRect);
		
		Core.circle(storedMat, new Point(boardBox.boundingRect().x, boardBox.boundingRect().y), 10, new Scalar(255,0,0), -1);
		Core.circle(storedMat, new Point(boardBox.boundingRect().x+boardBox.boundingRect().width, boardBox.boundingRect().y), 10, new Scalar(255,0,0), -1);
		Core.circle(storedMat, new Point(boardBox.boundingRect().x, boardBox.boundingRect().y+boardBox.boundingRect().height), 10, new Scalar(255,0,0), -1);
		Core.circle(storedMat, new Point(boardBox.boundingRect().x+boardBox.boundingRect().width, boardBox.boundingRect().y+boardBox.boundingRect().height), 10, new Scalar(255,0,0), -1);
		
		boardBoxAngle = boardBox.size.height > boardBox.size.width ? 90.0 + boardBox.angle : boardBox.angle;
		
		Mat rotMat = getRotationMatrix(boardBox.center, boardBoxAngle);
		
		
		MatOfPoint2f rotatedRect = rotateRect(rotMat, boardRect);
		
		logger.warn("center {} angle {} width {} height {}", boardBox.center, boardBoxAngle, boardBox.size.width, boardBox.size.height);
		
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
		
		Mat unRotMat = getRotationMatrix(boardBox.center, -boardBoxAngle);
		
		MatOfPoint2f unRotatedPatternSizeRect = rotateRect(unRotMat, estimatedPatternSizeRect);
		
		return unRotatedPatternSizeRect;
		
		/*Mat unRotMat = getRotationMatrix(boardBox.center, -boardBoxAngle);
		
		
		MatOfPoint2f unRotatedPatternSizeRect = rotateRect(unRotMat, estimatedPatternSizeRect);
		

		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(0,0)[0], unRotatedPatternSizeRect.get(0,0)[1]), new Point(unRotatedPatternSizeRect.get(1,0)[0], unRotatedPatternSizeRect.get(1,0)[1]), new Scalar(0, 0, 255));
		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(1,0)[0], unRotatedPatternSizeRect.get(1,0)[1]), new Point(unRotatedPatternSizeRect.get(2,0)[0], unRotatedPatternSizeRect.get(2,0)[1]), new Scalar(0, 0, 255));
		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(3,0)[0], unRotatedPatternSizeRect.get(3,0)[1]), new Point(unRotatedPatternSizeRect.get(2,0)[0], unRotatedPatternSizeRect.get(2,0)[1]), new Scalar(0, 0, 255));
		Core.line(storedMat, new Point(unRotatedPatternSizeRect.get(3,0)[0], unRotatedPatternSizeRect.get(3,0)[1]), new Point(unRotatedPatternSizeRect.get(0,0)[0], unRotatedPatternSizeRect.get(0,0)[1]), new Scalar(0, 0, 255));
		
		
		// unRotatedPatternSizeRect is pretty good but estimateFullPatternSize should do a better job of accounting for the distortion of the rectangle
		
		// Next step is to verify contour, then use the contour information to warp the perspective
		
		RotatedRect box = Imgproc.minAreaRect(cropsrc);
		double boxAngle = box.size.height > box.size.width ? 90.0 + box.angle : box.angle;
		
		
		Point[] points = new Point[4];
		box.points(points);
		
		logger.warn("angle {} {}", rotationAngle, boxAngle);
		
		rotMat = getRotationMatrix(box.center, -boardBoxAngle);
		
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
		
		return boundingBox;*/
	}
	
	
	private double minimumDimension = 0.0;
	private MatOfPoint2f estimateFullPatternSize(MatOfPoint2f rotatedRect) {
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);
		
		double borderFactor = 0.076551;
		
		Point topLeft = new Point(rotatedRect.get(0,0)[0], rotatedRect.get(0,0)[1]);
		Point topRight = new Point(rotatedRect.get(1,0)[0], rotatedRect.get(1,0)[1]);
		Point bottomRight = new Point(rotatedRect.get(2,0)[0], rotatedRect.get(2,0)[1]);
		Point bottomLeft = new Point(rotatedRect.get(3,0)[0], rotatedRect.get(3,0)[1]);
		
		logger.warn("points {} {} {} {}", topLeft, topRight, bottomRight, bottomLeft);
		
		double topWidth = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double leftHeight = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		double bottomWidth = Math.sqrt(Math.pow(bottomRight.x - bottomLeft.x,2) + Math.pow(bottomRight.y - bottomLeft.y,2));
		double rightHeight = Math.sqrt(Math.pow(bottomRight.x - topRight.x,2) + Math.pow(bottomRight.y - topRight.y,2));
		
		//angle = int(math.atan((y1-y2)/(x2-x1))*180/math.pi)
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		//if (logger.isTraceEnabled())
		logger.warn("square size {} {} - angle {}", topWidth/(PATTERN_WIDTH-1), leftHeight/(PATTERN_HEIGHT-1), angle);
		logger.warn("square size {} {} - angle {}", bottomWidth/(PATTERN_WIDTH-1), rightHeight/(PATTERN_HEIGHT-1), angle);
		
		double squareTopWidth = (1+borderFactor)*(topWidth/(PATTERN_WIDTH-1));
		double squareTopHeight = (1+borderFactor)*(leftHeight/(PATTERN_HEIGHT-1));
		double squareBottomWidth = (1+borderFactor)*(bottomWidth/(PATTERN_WIDTH-1));
		double squareBottomHeight = (1+borderFactor)*(rightHeight/(PATTERN_HEIGHT-1));
		
		double[] newTopLeft = { topLeft.x - squareTopWidth, topLeft.y - squareTopHeight };
		double[] newBottomLeft = { bottomLeft.x - squareTopWidth, bottomLeft.y + squareTopHeight };
		double[] newTopRight = { topRight.x + squareBottomWidth, topRight.y - squareBottomHeight };
		double[] newBottomRight = { bottomRight.x + squareBottomWidth, bottomRight.y + squareBottomHeight };

		result.put(0, 0, newTopLeft);
		result.put(1, 0, newTopRight);
		result.put(2, 0, newBottomRight);
		result.put(3, 0, newBottomLeft);
		
		//double newTopWidth = Math.sqrt(Math.pow(newTopRight[0] - newTopLeft[0],2) + Math.pow(newTopRight[1] - newTopLeft[1],2));
		double newLeftHeight = Math.sqrt(Math.pow(newBottomLeft[0] - newTopLeft[0],2) + Math.pow(newBottomLeft[1] - newTopLeft[1],2));
		
		//double newBottomWidth = Math.sqrt(Math.pow(newBottomRight[0] - newBottomLeft[0],2) + Math.pow(newBottomRight[1] - newBottomLeft[1],2));
		double newRightHeight = Math.sqrt(Math.pow(newBottomRight[0] - newTopRight[0],2) + Math.pow(newBottomRight[1] - newTopRight[1],2));

		minimumDimension = newLeftHeight < newRightHeight ? newLeftHeight : newRightHeight;
		
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
	
	
	Point[] MatOfPoint2fToPoints(MatOfPoint2f mat)
	{
	    Point[] points = new Point[4];
	    points[0] = new Point(mat.get(0,0)[0], mat.get(0,0)[1]);
	    points[1] = new Point(mat.get(1,0)[0], mat.get(1,0)[1]);
	    points[2] = new Point(mat.get(2,0)[0], mat.get(2,0)[1]);
	    points[3] = new Point(mat.get(3,0)[0], mat.get(3,0)[1]);
	    
	    return points;
	}
	
	double euclideanDistance(Point pt1, Point pt2)
	{
		//logger.warn("euclideanDistance {} {} - {}", pt1, pt2, Math.sqrt(Math.pow(pt1.x-pt2.x,2) + Math.pow(pt1.y-pt2.y,2)));
		
		return Math.sqrt(Math.pow(pt1.x-pt2.x,2) + Math.pow(pt1.y-pt2.y,2));
	}
	
	Boolean nearPoints(Point[] points, Point compPt, double threshold)
	{
		for (int i = 0; i < points.length; i++)
		{
			if (euclideanDistance(points[i], compPt) < threshold)
			{
				logger.warn("nearPoints {} {}", points[i], compPt);
				return true;
			}
		}
		return false;
	}
	
	
	private Optional<Point> computeIntersect(double[] a, double[] b)
	{
	    double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
	    double x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
	    
	    double d = ((double)(x1-x2) * (y3-y4)) - ((y1-y2) * (x3-x4));
		
	    if (d>0)
	    {
	        Point pt = new Point();
	        pt.x = ((x1*y2 - y1*x2) * (x3-x4) - (x1-x2) * (x3*y4 - y3*x4)) / d;
	        pt.y = ((x1*y2 - y1*x2) * (y3-y4) - (y1-y2) * (x3*y4 - y3*x4)) / d;
	        return Optional.of(pt);
	    }
	    
	    return Optional.empty();
	}
	
	private Optional<MatOfPoint2f> findIdealCorners(Mat frame, MatOfPoint2f estimatedPatternRect)
	{
		
		// pixel distance
		// TODO: Use a dynamic threshold (Do not rely on pixel counts)
		final int TOLERANCE_THRESHOLD = (int)(minimumDimension / PATTERN_HEIGHT-1 / 5);
		
		logger.warn("tolerance threshold {}", TOLERANCE_THRESHOLD);
		
		// Grey scale conversion.
	    Mat grey = new Mat();
	    Imgproc.cvtColor(frame, grey, Imgproc.COLOR_BGR2GRAY);

	    
	    // Find edges
		Imgproc.Canny(grey, grey, 50, 150); 
		

		
		// Expand (dilate) edges for better lines
		//Imgproc.dilate(grey, grey, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
		
		Imgproc.GaussianBlur(grey, grey, new Size(3,3), 3.0);
		
		
		
		String filename = String.format("calibrate-undist-grey-lines-%s.png",seenChessboards);
		File file = new File(filename);
		filename = file.toString();
		Highgui.imwrite(filename, grey);	
		
		
		// We need to use a temp Mat because the HoughLineP command is destructive
		// TODO: Verify it is destructive, or if we even need grey afterwards (Probably only when debugging)
		Mat temp = grey.clone();
	    
	    logger.warn("estimation {} {} {} {}", estimatedPatternRect.get(0,0), estimatedPatternRect.get(1,0), estimatedPatternRect.get(2,0), estimatedPatternRect.get(3,0));
	    
	    
	    // Easier to work off of Points
	    Point[] estimatedPoints = MatOfPoint2fToPoints(estimatedPatternRect);
	    Core.circle(storedMat, estimatedPoints[0], 1, new Scalar(0, 0, 255), -1);
	    Core.circle(storedMat, estimatedPoints[1], 1, new Scalar(0, 0, 255), -1);
	    Core.circle(storedMat, estimatedPoints[2], 1, new Scalar(0, 0, 255), -1);
	    Core.circle(storedMat, estimatedPoints[3], 1, new Scalar(0, 0, 255), -1);

	    
	    // Find lines
	    // These parameters are just guesswork right now
	    Mat mLines = new Mat();
	    int threshold = 40;
	    int minLineSize = (int)(minimumDimension * .90);
	    int lineGap = TOLERANCE_THRESHOLD;
	    Imgproc.HoughLinesP(temp, mLines, 1, Math.PI/180, threshold, minLineSize, lineGap);
	    
	    
	    // Find the lines that match our estimates
	    List<double[]> verifiedLines = new ArrayList<double[]>();
	    
	    for (int x = 0; x < mLines.cols(); x++) 
	    {
	          double[] vec = mLines.get(0, x);
	          double x1 = vec[0], 
	                 y1 = vec[1],
	                 x2 = vec[2],
	                 y2 = vec[3];
	          Point start = new Point(x1, y1);
	          Point end = new Point(x2, y2);

	          if (nearPoints(estimatedPoints, start, TOLERANCE_THRESHOLD) && nearPoints(estimatedPoints, end, TOLERANCE_THRESHOLD))
	          {
	        	  Core.line(storedMat, start, end, new Scalar(255,0,0), 1);
	        	  
	        	  verifiedLines.add(vec);
	          }
	          //else
	        //	 Core.line(storedMat, start, end, new Scalar(255,255,0), 3);

	    }
	    
	    logger.warn("verifiedLines: {}", verifiedLines.size());
	    
	    // Reduce the lines to possible corners
	    List<Point> possibleCorners = new ArrayList<Point>();
	    
	    for (double[] line1 : verifiedLines)
	    {
	    	for (double[] line2 : verifiedLines)
	    	{
	    		if (line1 == line2)
	    			continue;
	    		
	    		logger.warn("compare {} {}", line1, line2);
	    		
	    		Optional<Point> intersection = computeIntersect(line1, line2);
	    		
	    		if (intersection.isPresent())
	    			possibleCorners.add(intersection.get());
	    		
	    		
	    	}
		}

	    // Reduce the possible corners to ideal corners
	    Point[] idealCorners = new Point[4]; 
	    double[] idealDistances = { TOLERANCE_THRESHOLD, TOLERANCE_THRESHOLD, TOLERANCE_THRESHOLD, TOLERANCE_THRESHOLD };

	    for (Point pt : possibleCorners)
	    {
	    	
	    	for (int i = 0; i < 4; i++)
	    	{
	    		double distance = euclideanDistance(pt, estimatedPoints[i]);
	    		
	    		if (distance < idealDistances[i])
	    		{
	    			idealDistances[i] = distance;
	    			idealCorners[i] = pt;
	    		}
	    	}
	    }
	    
	    
		filename = String.format("calibrate-contours-%s.png",seenChessboards);
		file = new File(filename);
		filename = file.toString();
		Highgui.imwrite(filename, storedMat);	
	    
	    // Verify that we have the corners we need
	    for (Point pt : idealCorners)
	    {
	    	logger.warn("idealCorners {}", pt);
	    	
	    	if (pt == null)
	    		return Optional.empty();
	    	
	    	Core.circle(storedMat, pt, 8, new Scalar(0, 255, 255), -1);
	    	
	    }
	    
	    // Sort them into the correct order
		//1st-------2nd
		// |         |
		// |         |
		// |         |
		//3rd-------4th
	    idealCorners = sortCorners(idealCorners);
	    
	    // build the MatofPoint2f
	    MatOfPoint2f sourceCorners = new MatOfPoint2f();
	    sourceCorners.alloc(4);
	    
	    for (int i = 0; i < 4; i++)
	    {
	    	sourceCorners.put(i, 0, new double[]{ idealCorners[i].x, idealCorners[i].y });
	    }
	    	
		

		
		//logger.warn("boundslines {} {} {} {}", minX, minY, maxX, maxY);
		
	    
	    return Optional.of(sourceCorners);
	}
	
	
	private Point[] sortCorners(Point[] corners)
	{
		Point[] result = new Point[4];
		
		Point center = new Point(0,0);
		for (Point corner : corners)
		{
			center.x += corner.x;
			center.y += corner.y;
		}

		center.x *= (1.0 / corners.length);
		center.y *= (1.0 / corners.length);
	    
		List<Point> top = new ArrayList<Point>();
		List<Point> bot = new ArrayList<Point>();

	    for (int i = 0; i < corners.length; i++)
	    {
	        if (corners[i].y < center.y)
	            top.add(corners[i]);
	        else
	            bot.add(corners[i]);
	    }

	    result[0] = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
	    result[1] = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
	    result[2] = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);
	    result[3] = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);
	    
	    return result;
	    
	}
	
	private void initializeWarpPerspective(final Mat frame, MatOfPoint2f sourceCorners)
	{
		MatOfPoint2f destCorners = new MatOfPoint2f();
		destCorners.alloc(4);
		
		destCorners.put(0, 0, new double[]{0,0} );
		destCorners.put(1, 0, new double[]{frame.cols(), 0});
		destCorners.put(2, 0, new double[]{0, frame.rows()});
		destCorners.put(3, 0, new double[]{frame.cols(), frame.rows()});
		
		perspMat = Imgproc.getPerspectiveTransform(sourceCorners, destCorners);
		
		boundingBox = new BoundingBox(0, 0, frame.cols(), frame.rows());
		
	}
	
	private Mat warpPerspective(final Mat frame)
	{
		Mat mat = new Mat();
		
		Imgproc.warpPerspective(frame,mat,perspMat,frame.size(), Imgproc.INTER_LINEAR);

		return mat;
	}
	
	
	
	
	/*private boolean findProjectionArea(Mat frame)
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
	}*/
	
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
	
	

	/*public Mat warpPerspective(final Mat image)
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
	}*/
	

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
		
		/*Core.circle(storedMat, topLeft, 10, new Scalar(255,0,0), -1);
		Core.circle(storedMat, topRight, 10, new Scalar(255,0,0), -1);
		Core.circle(storedMat, bottomRight, 10, new Scalar(255,0,0), -1);
		Core.circle(storedMat, bottomLeft, 10, new Scalar(255,0,0), -1);*/
		
		result.put(0, 0, topLeft.x, topLeft.y, topRight.x, topRight.y, bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y);
		
		return result;
	}
	
	/*private Mat calcMatBoundsFromDimensions(MatOfPoint2f corners)
	{
		// TODO: HANDLE UPSIDE DOWN PATTERN
		
		Mat result = new Mat(4,1,CvType.CV_32FC2);
		
		double borderFactor = 0.0666667;
		
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
	}*/
	

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