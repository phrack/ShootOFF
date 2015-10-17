package com.shootoff.camera.AutoCalibration;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.util.Callback;

import javax.imageio.ImageIO;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;


public class AutoCalibrationManager implements Runnable {
	
	private final Logger logger = LoggerFactory.getLogger(AutoCalibrationManager.class);
	
	public final static int PATTERN_WIDTH = 9;
	public final static int PATTERN_HEIGHT = 6;
	
	private Mat savedImage = new Mat();
	
	private BufferedImage frame;
	
	private int successes = 0;
	private final static int MIN_BOARDS = 5;
	
	MatOfPoint2f imageCorners = new MatOfPoint2f();
	private List<Mat> imagePoints = new ArrayList<>();
	private List<Mat> objectPoints = new ArrayList<>();
	private MatOfPoint3f obj = new MatOfPoint3f();
	
	private Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
	private Mat distCoeffs = new Mat();

	private boolean isCalibrated = false;
	
	private Callback<Optional<Bounds>, Void> callback;
	
	public void setCallback(Callback<Optional<Bounds>, Void> callback) {
		this.callback = callback;
	}

	public void setFrame(BufferedImage frame) {
		this.frame = frame;
	}

	public AutoCalibrationManager()
	{
		int numSquares = PATTERN_WIDTH * PATTERN_HEIGHT;
		for (int j = 0; j < numSquares; j++)
			obj.push_back(new MatOfPoint3f(new Point3(j / PATTERN_HEIGHT, j % PATTERN_WIDTH, 0.0f)));
	}
	
	public Mat BufferedImageToMat(BufferedImage frame)
	{
		byte[] pixels = ((DataBufferByte)frame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		
		return mat;
	}


	@Override
	public void run() {
		Optional<Bounds> bounds = processFrame(frame);
		
		if (callback != null && bounds.isPresent())
		{
			callback.call(bounds);
		}
	}
	
	public Optional<Bounds> processFrame(BufferedImage frame)
	{
		if (!isCalibrated)
			collectBoards(frame);
		if (isCalibrated)
		{ 	
			Mat mat = BufferedImageToMat(frame);
			
			Mat undistorted = new Mat();
			
			

						 
			imageCorners = (MatOfPoint2f) imagePoints.get(imagePoints.size()-1);
			
			

			
			undistorted = undistort(mat, intrinsic, distCoeffs, imageCorners);
			
			
			Optional<Bounds> bounds = calcBoundsFromDimensions(rotatedCorners);
			

						
			
			

			


			//mat.copyTo(undistorted);
			
			/*MatOfPoint2f approxCurve = new MatOfPoint2f();
	        double approxDistance = Imgproc.arcLength(dst, true)*0.02;
	        Imgproc.approxPolyDP(dst, approxCurve, approxDistance, true);

	        //Convert back to MatOfPoint
	        MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

	        Rect rect = Imgproc.boundingRect(points);*/
			
			//logger.warn("{} {} {} {} - {} {}", rect.x, rect.y, rect.width, rect.height);
			
	        
	        //undistorted = undistorted.submat(rect);
			
			String filename = String.format("calibrate-undist-%s.png",successes);
			File file = new File(filename);
			filename = file.toString();
			Imgcodecs.imwrite(filename, undistorted);
			
			if (!bounds.isPresent())
				return Optional.empty();
			
			
			int minX = (int) bounds.get().getMinX();
			int minY = (int) bounds.get().getMinY();
			int width = (int) bounds.get().getWidth();
			int height = (int) bounds.get().getHeight();
			
			logger.warn("{} {} {} {}", minX, minY, width, height);
			
			frame = frame.getSubimage(minX, minY, width, height);
			
			File outputfile = new File(String.format("calibrate-cropped-%s.png",successes));
			try {
				ImageIO.write(frame, "png", outputfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return bounds;
			
		}
		
		return Optional.empty();
	}
	
	
	private BufferedImage matToBufferedImage(Mat mat) throws IOException {
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, matOfByte);
		byte[] byteArray = matOfByte.toArray();
		InputStream in = new ByteArrayInputStream(byteArray);
		return ImageIO.read(in);
	}

	public void collectBoards(BufferedImage frame)
	{
		boolean found = findChessboardBufferedImage(frame);
		
		if (!found)
			return;
		
		successes++;
		
		// reach the correct number of images needed for the calibration
		if (successes == MIN_BOARDS)
		{
			
			logger.warn("got enough, calibrating");
			calibrateCamera();
		}
		else
		{
			File outputfile = new File(String.format("calibrate-%s.png",successes));
			try {
				ImageIO.write(frame, "png", outputfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void calibrateCamera() {
		// init needed variables according to OpenCV docs
		List<Mat> rvecs = new ArrayList<>();
		List<Mat> tvecs = new ArrayList<>();
		
		logger.warn("{}", savedImage.size());

		// calibrate!
		Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
		
		logger.warn("{}", rvecs);
		logger.warn("{}", tvecs);
		
		logger.warn("{}", intrinsic);
		logger.warn("{}", distCoeffs);

		this.isCalibrated  = true;
		
		logger.warn("calibrated");
	}

	public boolean findChessboardBufferedImage()
	{
		byte[] pixels = ((DataBufferByte)frame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		
		return findChessboard(mat);
	}
	
	public boolean findChessboardBufferedImage(BufferedImage frame)
	{
		byte[] pixels = ((DataBufferByte)frame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		
		return findChessboard(mat);
	}
	
	
	private boolean findChessboard(Mat mat)
	{
		
		Mat grayImage = new Mat();

	    Imgproc.cvtColor(mat, grayImage, Imgproc.COLOR_BGR2GRAY);
	    

	    Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);

	    MatOfPoint2f imageCorners = new MatOfPoint2f();
	    
		boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners, Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_ADAPTIVE_THRESH);
		if (found) 
		{

			
			// optimization
			TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
			Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11, 11), new Size(-1, -1), term);
			// save the current frame for further elaborations
			grayImage.copyTo(this.savedImage);
			// show the chessboard inner corners on screen
			Calib3d.drawChessboardCorners(mat, boardSize, imageCorners, found);
			
			try {
				frame = matToBufferedImage(mat);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.imagePoints.add(imageCorners);
			this.objectPoints.add(obj);			
			
			String filename = String.format("calibrate-marked-%s.png",successes);
			File file = new File(filename);
			filename = file.toString();
			Imgcodecs.imwrite(filename, mat);
			
			
			logger.warn("found {}", found);
		}
		return found;
	}
	
	private Mat map1 = new Mat();
	private Mat map2 = new Mat();
	private boolean mapInitialized = false;
	private MatOfPoint2f translatedCorners = new MatOfPoint2f();
	private MatOfPoint2f rotatedCorners = new MatOfPoint2f();
	private Mat perspMat = new Mat();
	public Mat undistort(final Mat image,final Mat cameraMatrix,final Mat distCoeffs,final MatOfPoint2f imageCorners){
		

		
		Mat mat=new Mat();
		
		final Mat newCameraMtx=Calib3d.getOptimalNewCameraMatrix(cameraMatrix,distCoeffs,image.size(),0);
		
		if (!mapInitialized)
		{
			mapInitialized = true;
			
			Imgproc.initUndistortRectifyMap(newCameraMtx, distCoeffs, new Mat(), newCameraMtx, image.size(), CvType.CV_32FC1, map1, map2);

			
			translatedCorners.alloc(imageCorners.rows());
			
			logger.warn("remap {} {} - {} {}", imageCorners.rows(), imageCorners.cols());
			for (int i = 0; i < imageCorners.rows(); i++)
			{
				logger.warn("{} - {}", i, imageCorners.get(i, 0));
			
				
				logger.warn("{} {}", map2.get((int)imageCorners.get(i, 0)[0], (int)imageCorners.get(i, 0)[1]), map1.get((int)imageCorners.get(i, 0)[0], (int)imageCorners.get(i, 0)[1]));
				
				double newx = map2.get((int)imageCorners.get(i, 0)[0], (int)imageCorners.get(i, 0)[1])[0];
				double newy = map1.get((int)imageCorners.get(i, 0)[0], (int)imageCorners.get(i, 0)[1])[0];
				
				translatedCorners.put(i, 0, newx, newy);
				
				logger.warn("newx {} newy {}", newx, newy);
				
			}
			
			
			
			Point realCenter = new Point(image.rows()/2, image.cols()/2);
			
			double scale = 1.0;

			// Get the rotation matrix with the specifications above
			
			rotatedCorners.alloc(translatedCorners.rows());
			
			Mat rot_mat = Imgproc.getRotationMatrix2D( realCenter, calcAngle(translatedCorners), scale );
			for (int i = 0; i < translatedCorners.rows(); i++)
			{
				Point newpt = rotPoint(rot_mat, new Point(translatedCorners.get(i, 0)[0], translatedCorners.get(i, 0)[1]));
				logger.warn("old pt x {} y {} - new pt x {} y {}", translatedCorners.get(i,0)[0], translatedCorners.get(i,0)[1], newpt.x, newpt.y);
				rotatedCorners.put(i, 0, newpt.x, newpt.y);
			}

			
			Mat cropsrc = calcMatBoundsFromDimensions(rotatedCorners);
			Mat cropdst = new Mat(4,1,CvType.CV_32FC2);
			cropdst.put(0, 0, 0, 0, image.cols(), 0, image.cols(), image.rows(), 0, image.rows());
			
			perspMat = Imgproc.getPerspectiveTransform(cropsrc, cropdst);
		}
	
		
		Imgproc.remap(image, mat, map1, map2, Imgproc.INTER_LINEAR);
		

	
		logger.warn("translatedCorners {} {}", translatedCorners.rows(), translatedCorners.cols());
		
		Imgproc.undistort(image,mat,cameraMatrix,distCoeffs,cameraMatrix);

		//Calib3d.drawChessboardCorners(mat, new Size(PATTERN_WIDTH, PATTERN_HEIGHT), dst, true);

		//Calib3d.drawChessboardCorners(mat, new Size(PATTERN_WIDTH, PATTERN_HEIGHT), imageCorners, true);

		
		//Imgproc.undistortPoints(imageCorners, dst, intrinsic, distCoeffs);
		
		
		//
		//
		  
		  
		//mat = warpPerspective(mat, dst);
		
		logger.warn("calcAngle {} calcCenter {}", calcAngle(translatedCorners), calcCenter(translatedCorners));
		
		rotateImage(mat, calcAngle(translatedCorners), calcCenter(translatedCorners));
		

		
		Calib3d.drawChessboardCorners(mat, new Size(PATTERN_WIDTH, PATTERN_HEIGHT), rotatedCorners, true);
		
		

		// TODO: Re-enable to crop
		//Imgproc.warpPerspective(mat,mat,perspMat,mat.size());
		
		
		return mat;
	}
	
	/*public Mat warpPerspective(final Mat image, MatOfPoint2f corners)
	{
		Mat src = new Mat(4,1,CvType.CV_32FC2);
		Mat dst = new Mat(4,1,CvType.CV_32FC2);
		
		dst.put(0, 0, 0, 0, image.cols(), 0, image.cols(), image.rows(), 0, image.rows());
		
		logger.warn("{} {} {} {} {} {} {} {}", 0, 0, image.cols(), 0, image.cols(), image.rows(), 0, image.rows());

		src.put(0, 0, corners.get(0,0)[0], corners.get(0,0)[1], corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1],
				corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1],
				corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1] );
		
		Mat newsrc = new Mat(4,1,CvType.CV_32FC2);
		
		//Imgproc.resize(src, newsrc, new Size(), 1.3, 1, Imgproc.INTER_NEAREST);
		
		newsrc = calcBoundsFromDimensions(corners);
		
		logger.warn("{} {} {} {} {} {} {} {}", corners.get(0,0)[0], corners.get(0,0)[1], corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1],
				corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1],
				corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		Mat perspMat = Imgproc.getPerspectiveTransform(src, dst);
		
		Mat changed = new Mat();
		
		changed = image.clone();
		
		//Imgproc.line(changed, new Point(newsrc.get(0,0)[0], newsrc.get(0,0)[1]), new Point(newsrc.get(3,0)[0], newsrc.get(3,0)[1]), new Scalar(0,0,255));
				
		//Imgproc.warpPerspective(image,changed,perspMat,image.size());
		
		
		rotateImage(changed, calcAngle(corners), calcCenter(corners));
		
		return changed;
	}*/
	
/*Point2f rotPoint(const Mat &R, const Point2f &p)
{
    Point2f rp;
    rp.x = (float)(R.at<double>(0,0)*p.x + R.at<double>(0,1)*p.y + R.at<double>(0,2));
    rp.y = (float)(R.at<double>(1,0)*p.x + R.at<double>(1,1)*p.y + R.at<double>(1,2));
    return rp;
}*/
	
	private Point rotPoint(Mat rot_mat, Point point)
	{
		Point rp = new Point();
		rp.x = rot_mat.get(0, 0)[0] * point.x + rot_mat.get(0, 1)[0] * point.y +  rot_mat.get(0, 2)[0];
		rp.y = rot_mat.get(1, 0)[0] * point.x + rot_mat.get(1, 1)[0] * point.y +  rot_mat.get(1, 2)[0];

		return rp;
	}
	
	private void rotateImage(Mat frame, double angle, Point center)
	{
		Point realCenter = new Point(frame.rows()/2, frame.cols()/2);
		
		   double scale = 1.0;

		   /// Get the rotation matrix with the specifications above
		   Mat rot_mat = Imgproc.getRotationMatrix2D( realCenter, angle, scale );

		   /// Rotate the warped image
		   Imgproc.warpAffine( frame, frame, rot_mat, frame.size() );
	}
	
	private double calcAngle(MatOfPoint2f corners)
	{
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		return angle;
	}
	
	private Point calcCenter(MatOfPoint2f corners)
	{
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		Point bottomRight = new Point(corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		double width = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double height = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		logger.warn("center {} {}", topLeft.x+(width/2), topLeft.y+(height/2));
		
		return new Point(topLeft.x+(width/2), topLeft.y+(height/2));
	}
	
	private Optional<Bounds> calcBoundsFromDimensions(MatOfPoint2f corners)
	{
		Mat mat = calcMatBoundsFromDimensions(corners);
		
		return Optional.of(new BoundingBox(mat.get(0, 0)[0], mat.get(0, 0)[1], mat.get(1, 0)[0]-mat.get(0, 0)[0], mat.get(2, 0)[1]-mat.get(1, 0)[1]));
	}
	
	private Mat calcMatBoundsFromDimensions(MatOfPoint2f corners)
	{
		Mat result = new Mat(4,1,CvType.CV_32FC2);
		
		double borderFactor = .05;
		
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		Point bottomRight = new Point(corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		double width = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double height = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		//angle = int(math.atan((y1-y2)/(x2-x1))*180/math.pi)
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		logger.warn("square size {} {} - angle {}", width/PATTERN_WIDTH, height/PATTERN_HEIGHT, angle);
		double squareWidth = (1+borderFactor)*(width/PATTERN_WIDTH);
		double squareHeight = (1+borderFactor)*(height/PATTERN_HEIGHT);
		
		double[] newTopLeft = { topLeft.x - squareWidth, topLeft.y - squareHeight };
		double[] newBottomLeft = { bottomLeft.x - squareWidth, bottomLeft.y + squareHeight };
		double[] newTopRight = { topRight.x + squareWidth, topRight.y - squareHeight };
		double[] newBottomRight = { bottomRight.x + squareWidth, bottomRight.y + squareHeight };

		//RotatedRect rect = new RotatedRect(topLeft, topRight, bottomRight);
		
		result.put(0, 0, newTopLeft[0], newTopLeft[1], newTopRight[0], newTopRight[1], newBottomRight[0], newBottomRight[1], newBottomLeft[0], newBottomLeft[1]);
		
		return result;
	}
	
	
	public Optional<Bounds> calcBounds(MatOfPoint2f corners)
	{
		double borderFactor = .05;
		
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double biggestWidth = 0;
        double biggestHeight = 0;
        
        for (int k = 0; k < PATTERN_HEIGHT-1; k++)
        {
            for (int j = 0; j < PATTERN_WIDTH-1; j++)
            {
            	
            	int anum = (k*(PATTERN_WIDTH))+(j);
            	int bnum = ((k+1)*(PATTERN_WIDTH))+(j+1);
            	            	
            	double ax = corners.get(anum, 0)[0];
            	double ay = corners.get(anum, 0)[1];
            	
            	
            	double bx = corners.get(bnum, 0)[0];
            	double by = corners.get(bnum, 0)[1];
            	
            	logger.warn("{} {} - {} {} {} {}", anum, bnum, ax, ay, bx, by);

            	double squareWidth = Math.abs(bx-ax);
            	double squareHeight = Math.abs(by-ay);
            	
            	if (squareWidth<0 || squareHeight<0)
            	{
            		logger.warn("Calibration pattern is upside down!");
            		return Optional.empty();
            	}

            	logger.trace("{} {} - {} {}", j, k, squareWidth, squareHeight);

            	
            	if (squareWidth > biggestWidth)
            		biggestWidth = squareWidth;
            	if (squareHeight > biggestHeight)
            		biggestHeight = squareHeight;
            	
            	if (ax < minX)
            		minX = ax;
            	if (ay < minY)
            		minY = ay;
            	
            }
            
        }
        
    	logger.warn("square {} {}", biggestWidth, biggestHeight);
		
    	minX = minX - biggestWidth*(1+2*borderFactor);
    	minY = minY - biggestHeight*(1+2*borderFactor);
		
		
		double width = biggestWidth * (PATTERN_WIDTH+1) * (1+borderFactor);
		double height = biggestHeight * (PATTERN_HEIGHT+1) * (1+borderFactor);
		
		logger.warn("Calibrating to {} {} with width {} height {}", minX, minY, width, height);
		
		if (minX<0 || minY<0 || minX+width>CameraManager.FEED_WIDTH || minY+height>CameraManager.FEED_HEIGHT)
			return Optional.empty();
		


		
		
		return Optional.of(new BoundingBox(minX, minY, width, height));
	}

	public void undistortFrame(BufferedImage frame) {
		if (!isCalibrated)
		{
			logger.warn("undistortFrame called when isCalibrated is false");
			return;
		}
		
		Mat mat = BufferedImageToMat(frame);
		
		try {
			frame = matToBufferedImage(undistort(mat, intrinsic, distCoeffs, imageCorners));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}