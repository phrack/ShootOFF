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
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.video.ConverterFactory;


public class AutoCalibrationManager implements Runnable {
	
	private final Logger logger = LoggerFactory.getLogger(AutoCalibrationManager.class);
	
	public final static int PATTERN_WIDTH = 9;
	public final static int PATTERN_HEIGHT = 6;
	
    private Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);

	
	//private Mat savedImage = new Mat();
	
	private BufferedImage frame;
	
	//private int successes = 0;
	private final static int MIN_BOARDS = 3;
	
	MatOfPoint2f imageCorners = new MatOfPoint2f();
	//private List<Mat> imagePoints = new ArrayList<>();
	//private List<Mat> objectPoints = new ArrayList<>();
	//private MatOfPoint3f obj = new MatOfPoint3f();
	
	//private Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
	//private Mat distCoeffs = new Mat();

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
		/*int numSquares = PATTERN_WIDTH * PATTERN_HEIGHT;
		for (int j = 0; j < numSquares; j++)
			obj.push_back(new MatOfPoint3f(new Point3(j / PATTERN_HEIGHT, j % PATTERN_WIDTH, 0.0f)));*/
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
				Imgcodecs.imwrite(filename, mat);
			}
			
			undistorted = warpPerspective(mat, imageCorners);
			
			// TODO: HANDLE UPSIDE DOWN PATTERN BY WARNING USER AND NOT CALIBRATING
			// UPSIDE DOWN PATTERN CURRENTLY BREAKS EVERYTHING
			
			Optional<Bounds> bounds = Optional.of(boundingBox);
			
			if (logger.isTraceEnabled())
			{
				String filename = String.format("calibrate-undist-%s.png",seenChessboards);
				File file = new File(filename);
				filename = file.toString();
				Imgcodecs.imwrite(filename, undistorted);
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
				Imgcodecs.imwrite(filename, undistorted);
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

	/*public void collectBoards(BufferedImage frame)
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
	}*/
	
	/*private void calibrateCamera() {
		// init needed variables according to OpenCV docs
		List<Mat> rvecs = new ArrayList<>();
		List<Mat> tvecs = new ArrayList<>();
		
		Mat reprojectionErrors = new Mat();
		
		distCoeffs = Mat.zeros(8, 1, CvType.CV_64F);
		intrinsic = Mat.eye(3, 3, CvType.CV_64F);
		//intrinsic.put(0, 0, 1.0);
		
		logger.warn("{}", savedImage.size());
		
		objectPoints = new ArrayList<Mat>();
		
		objectPoints.add(Mat.zeros((int) boardSize.area(), 1, CvType.CV_32FC3));
		calcBoardCornerPositions(objectPoints.get(0), boardSize);
		for (int i = 1; i < imagePoints.size(); i++) {
			objectPoints.add(objectPoints.get(0));
		}

		// calibrate!
		Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs,
				Calib3d.CALIB_ZERO_TANGENT_DIST
	            | Calib3d.CALIB_FIX_PRINCIPAL_POINT 
	            | Calib3d.CALIB_FIX_K4
	            | Calib3d.CALIB_FIX_K5);
		
		boolean calibrated = Core.checkRange(intrinsic) && Core.checkRange(distCoeffs);
		double rms = computeReprojectionErrors(intrinsic, distCoeffs, objectPoints, imagePoints, rvecs, tvecs, reprojectionErrors);
		logger.warn("Is calibration in range: {}, Cal Error:{}", calibrated, rms);


		this.isCalibrated  = true;
		
		logger.warn("calibrated");
	}*/
	
	/*public static double computeReprojectionErrors(Mat cameraMatrix, Mat distCoeffs, List<Mat> objectPoints,List<Mat> cornersBuffer, List<Mat> rvecs, List<Mat> tvecs, Mat perViewErrors) {
		MatOfPoint2f cornersProjected = new MatOfPoint2f();
		double totalError = 0;
		double error;
		float viewErrors[] = new float[objectPoints.size()];

		MatOfDouble distortionCoefficients = new MatOfDouble(distCoeffs);
		int totalPoints = 0;
		for (int i = 0; i < objectPoints.size(); i++) {
			MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
			Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i), cameraMatrix, distortionCoefficients, cornersProjected);
			error = Core.norm(cornersBuffer.get(i), cornersProjected, Core.NORM_L2);

			int n = objectPoints.get(i).rows();
			viewErrors[i] = (float) Math.sqrt(error * error / n);
			totalError += error * error;
			totalPoints += n;
		}
		perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
		perViewErrors.put(0, 0, viewErrors);

		return Math.sqrt(totalError / totalPoints);
	}*/
	
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
			// save the current frame for further elaborations
			//grayImage.copyTo(this.savedImage);
			// show the chessboard inner corners on mat
			//Calib3d.drawChessboardCorners(mat, boardSize, imageCorners, found);
			
			/*try {
				frame = matToBufferedImage(mat);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			//this.imagePoints.add(imageCorners.clone());
			
			//imageCorners.release();
		
			
			//this.objectPoints.add(obj);			
			
			if (logger.isTraceEnabled())
			{
				String filename = String.format("calibrate-marked-%s.png",seenChessboards);
				File file = new File(filename);
				filename = file.toString();
				Imgcodecs.imwrite(filename, mat);
			}
			
			
			logger.debug("found {}", found);
		}
		return found;
	}
	
	/*public static final double SQUARE_SIZE = 40;
	public static void calcBoardCornerPositions(Mat corners, Size boardSize) {
		final int cn = 3;
		int size = (int) boardSize.width * (int) boardSize.height;
		float positions[] = new float[size * cn];

		for (int i = 0; i < boardSize.height; i++) {
			for (int j = 0; j < boardSize.width * cn; j += cn) {
				positions[(int) (i * boardSize.width * cn + j + 0)] = (2 * (j / cn) + i % 2) * (float) SQUARE_SIZE;
				positions[(int) (i * boardSize.width * cn + j + 1)] = i * (float) SQUARE_SIZE;
				positions[(int) (i * boardSize.width * cn + j + 2)] = 0;
			}
		}
		corners.create(size, 1, CvType.CV_32FC3);
		corners.put(0, 0, positions);
	}*/
	
	/*private Mat map1 = new Mat();
	private Mat map2 = new Mat();
	private boolean mapInitialized = false;
	private MatOfPoint2f translatedCorners = new MatOfPoint2f();
	private RotatedRect box = null;
	private Mat newCameraMtx = new Mat();
	private Size boundSize = null;*/
	
	private MatOfPoint2f rotatedCorners = new MatOfPoint2f();
	private Mat perspMat = new Mat();
	//MatOfPoint2f cropsrc = null;
	//MatOfPoint2f cropdst = null;


	private Bounds boundingBox = null;
	private boolean warpInitialized = false;
	private double rotationAngle = 0.0;
	Point realCenter = null;
	
	

	public Mat warpPerspective(final Mat image, MatOfPoint2f corners)
	{
		Mat mat=new Mat(image.rows(),image.cols(),CvType.CV_32FC1);
		
		if (!warpInitialized)
		{
			warpInitialized = true;
			
			Point realCenter = new Point(image.rows()/2, image.cols()/2);
			
			double scale = 1.0;

			// Get the rotation matrix with the specifications above
			
			rotatedCorners.alloc(imageCorners.rows());
			
			logger.debug("calcAngle {} calcCenter {}", calcAngle(imageCorners), calcCenter(imageCorners));
			
			rotationAngle = calcAngle(imageCorners);
			
			Mat rot_mat = Imgproc.getRotationMatrix2D( realCenter, rotationAngle, scale );
			for (int i = 0; i < imageCorners.rows(); i++)
			{
				Point newpt = rotPoint(rot_mat, new Point(imageCorners.get(i, 0)[0], imageCorners.get(i, 0)[1]));
				logger.debug("old pt x {} y {} - new pt x {} y {}", imageCorners.get(i,0)[0], imageCorners.get(i,0)[1], newpt.x, newpt.y);
				rotatedCorners.put(i, 0, newpt.x, newpt.y);
			}

			
			/*Mat cropsrc = calcMatBoundsFromDimensions(rotatedCorners);
			Mat cropdst = new Mat(4,1,CvType.CV_32FC2);
			cropdst.put(0, 0, 0, 0, image.cols(), 0, image.cols(), image.rows(), 0, image.rows());
			perspMat = Imgproc.getPerspectiveTransform(cropsrc, cropdst);*/
			
			
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
			
			//boundSize = new Size(image.cols(), image.rows());
			
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
		
		mat = rotateImage(mat, rotationAngle, realCenter);
		
		logger.trace("rotatedCorners {}", rotatedCorners.get(PATTERN_WIDTH-1, 0));
		

		//Calib3d.drawChessboardCorners(mat, new Size(PATTERN_WIDTH, PATTERN_HEIGHT), rotatedCorners, true);
		
		Imgproc.warpPerspective(mat,mat,perspMat,mat.size(), Imgproc.INTER_LINEAR);
		
		
		return mat;
	}
	
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
	
	private Mat rotateImage(Mat mat, double angle, Point center)
	{
		
		Mat result = new Mat();
	
		
		Point realCenter = new Point(mat.rows()/2, mat.cols()/2);
	
		double scale = 1.0;
		
		logger.trace("rotateImage {} {} {}", realCenter, angle, scale);

		/// Get the rotation matrix with the specifications above
		Mat rot_mat = Imgproc.getRotationMatrix2D( realCenter, angle, scale );

		/// Rotate the warped image
		Imgproc.warpAffine( mat, result, rot_mat, mat.size() );
	   
		return result;
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
		//Point bottomRight = new Point(corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[0], corners.get(PATTERN_WIDTH*PATTERN_HEIGHT-1,0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[0], corners.get(PATTERN_WIDTH*(PATTERN_HEIGHT-1),0)[1]);
		
		double width = Math.sqrt(Math.pow(topRight.x - topLeft.x,2) + Math.pow(topRight.y - topLeft.y,2));
		double height = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x,2) + Math.pow(bottomLeft.y - topLeft.y,2));
		
		logger.debug("center {} {}", topLeft.x+(width/2), topLeft.y+(height/2));
		
		return new Point(topLeft.x+(width/2), topLeft.y+(height/2));
	}
	
	/*private Optional<Bounds> calcBoundsFromDimensions(MatOfPoint2f corners)
	{
		Mat mat = calcMatBoundsFromDimensions(corners);
		

		
		return Optional.of(new BoundingBox(mat.get(0, 0)[0], mat.get(0, 0)[1], mat.get(1, 0)[0]-mat.get(0, 0)[0], mat.get(2, 0)[1]-mat.get(1, 0)[1]));
	}*/
	
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
		
		logger.debug("square size {} {} - angle {}", width/PATTERN_WIDTH, height/PATTERN_HEIGHT, angle);
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
	
	
	/*public Optional<Bounds> calcBounds(MatOfPoint2f corners)
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
	}*/

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