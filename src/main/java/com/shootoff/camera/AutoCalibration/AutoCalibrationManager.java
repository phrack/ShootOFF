package com.shootoff.camera.AutoCalibration;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.util.Callback;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public Optional<Bounds> processFrame(BufferedImage frame)
	{
		boolean found = findChessboardBufferedImage(frame);
		
		if (found && seenChessboards < MIN_BOARDS)
		{
			seenChessboards++;
		}
		else if (found)
		{ 	
			Mat mat = bufferedImageToMat(frame);
			
			Mat undistorted = new Mat();
			
			
			if (logger.isTraceEnabled())
			{
				String filename = String.format("calibrate-dist-%s.png",seenChessboards);
				File file = new File(filename);
				filename = file.toString();
				Highgui.imwrite(filename, mat);
			}
			
			undistorted = warpPerspective(mat, imageCorners);
			
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
			
			
			int minX = (int) bounds.get().getMinX();
			int minY = (int) bounds.get().getMinY();
			int width = (int) bounds.get().getWidth();
			int height = (int) bounds.get().getHeight();
			
			logger.debug("bounds {} {} {} {}", bounds.get().getMinX(), bounds.get().getMinY(), bounds.get().getWidth(), bounds.get().getHeight());
			
			if (logger.isTraceEnabled())
			{
				undistorted = undistorted.submat(minY, minY+height, minX, minX+width);
				
				String filename = String.format("calibrate-undist-cropped-%s.png",seenChessboards);
				File file = new File(filename);
				filename = file.toString();
				Highgui.imwrite(filename, undistorted);
			}
			
			isCalibrated = true;
			
			return bounds;
			
		}
		
		return Optional.empty();
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
	
	public boolean findChessboardBufferedImage(BufferedImage frame)
	{
		return findChessboard(bufferedImageToMat(frame));
	}
	
	private final TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
	private boolean findChessboard(Mat mat)
	{
		
		Mat grayImage = new Mat();

	    Imgproc.cvtColor(mat, grayImage, Imgproc.COLOR_BGR2GRAY);
	    
	    imageCorners = new MatOfPoint2f();
	    
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
			
			logger.debug("found {}", found);
		}
		return found;
	}
	
	private MatOfPoint2f rotatedCorners = null;
	private Mat perspMat = null;
	private Bounds boundingBox = null;
	private boolean warpInitialized = false;
	private double rotationAngle = 0.0;
	Point realCenter = null;
	Mat rotMatrix = null;
	
	

	public Mat warpPerspective(final Mat image, MatOfPoint2f corners)
	{
		Mat mat=new Mat(image.rows(),image.cols(),CvType.CV_32FC1);
		
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
			MatOfPoint2f cropdst = new MatOfPoint2f();
			cropdst.alloc(4);
			cropdst.put(0, 0, box.boundingRect().x, box.boundingRect().y,
					box.boundingRect().x+box.boundingRect().width, box.boundingRect().y,
					box.boundingRect().x, box.boundingRect().y+box.boundingRect().height,
					box.boundingRect().x+box.boundingRect().width, box.boundingRect().y+box.boundingRect().height);
			
			boundingBox = new BoundingBox(box.boundingRect().x, box.boundingRect().y, box.boundingRect().width, box.boundingRect().height);
			
			realCenter = new Point(image.rows()/2, image.cols()/2);
			
			if (logger.isDebugEnabled())
			{
				logger.debug("bounds {} - {} - {}", bounds.get(0, 0), cropsrc.get(0, 0), cropdst.get(0, 0));
				logger.debug("bounds {} - {} - {}", bounds.get(1, 0), cropsrc.get(1, 0), cropdst.get(1, 0));
				logger.debug("bounds {} - {} - {}", bounds.get(2, 0), cropsrc.get(2, 0), cropdst.get(2, 0));
				logger.debug("bounds {} - {} - {}", bounds.get(3, 0), cropsrc.get(3, 0), cropdst.get(3, 0));
			
				logger.debug("sizes {} {} - {} {} {}", cropsrc.size(), cropdst.size(), box.angle, box.center, box.size);
			}
			
			perspMat = Imgproc.getPerspectiveTransform(cropsrc, cropdst);
		}
		
		mat = image.clone();
		
		Imgproc.warpAffine( mat, mat, rotMatrix, mat.size() );
		
		Imgproc.warpPerspective(mat,mat,perspMat,mat.size(), Imgproc.INTER_LINEAR);
		
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
	
	private Mat calcMatBoundsFromDimensions(MatOfPoint2f corners)
	{
		// TODO: HANDLE UPSIDE DOWN PATTERN
		
		Mat result = new Mat(4,1,CvType.CV_32FC2);
		
		double borderFactor = .36;
		
		Point topLeft = new Point(corners.get(0,0)[0], corners.get(0,0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH-1,0)[0], corners.get(PATTERN_WIDTH-1,0)[1]);
		Point bottomRight = new Point(corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		double width = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double height = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		//angle = int(math.atan((y1-y2)/(x2-x1))*180/math.pi)
		double angle = Math.atan((topRight.y-topLeft.y)/(topRight.x-topLeft.x))*180/Math.PI;
		
		if (logger.isTraceEnabled())
			logger.trace("square size {} {} - angle {}", width/PATTERN_WIDTH, height/PATTERN_HEIGHT, angle);
		
		double squareWidth = (1+borderFactor)*(width/PATTERN_WIDTH);
		double squareHeight = (1+borderFactor)*(height/PATTERN_HEIGHT);
		
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
		
		frame = matToBufferedImage(warpPerspective(mat, imageCorners));
		
		return frame;
	}
}