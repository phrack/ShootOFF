package com.shootoff.camera.AutoCalibration;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
	
	//private Callback<List<Point2dImpl>, Void> callback;
	
	/*public void setCallback(Callback<List<Point2dImpl>, Void> callback) {
		this.callback = callback;
	}*/

	public void setFrame(BufferedImage frame) {
		this.frame = frame;
	}

	public AutoCalibrationManager()
	{
		int numSquares = PATTERN_WIDTH * PATTERN_HEIGHT;
		for (int j = 0; j < numSquares; j++)
			obj.push_back(new MatOfPoint3f(new Point3(j / PATTERN_WIDTH, j % PATTERN_HEIGHT, 0.0f)));
	}
	
	public Mat BufferedImageToMat(BufferedImage frame)
	{
		byte[] pixels = ((DataBufferByte)frame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		
		return mat;
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
		
		Mat mat = BufferedImageToMat(frame);
		
		Mat grayImage = new Mat();

	    Imgproc.cvtColor(mat, grayImage, Imgproc.COLOR_BGR2GRAY);

	    Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);
	    
		boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners, 0);

		String filename = "test.png";
		File file = new File(filename);
		filename = file.toString();
		Imgcodecs.imwrite(filename, grayImage);
		
		
		logger.warn("found {}", found);
	}

	@Override
	public void run() {
		test();
		
	}
	
	public void processFrame(BufferedImage frame)
	{
		if (!isCalibrated)
			collectBoards(frame);
		else
		{
			Mat mat = BufferedImageToMat(frame);
			
			Mat undistorted = new Mat();
			Imgproc.undistort(mat, undistorted, intrinsic, distCoeffs);

			try {
				frame = matToBufferedImage(undistorted);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
	}
	
	private void calibrateCamera() {
		// init needed variables according to OpenCV docs
		List<Mat> rvecs = new ArrayList<>();
		List<Mat> tvecs = new ArrayList<>();
		intrinsic.put(0, 0, 1);
		intrinsic.put(1, 1, 1);
		// calibrate!
		Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
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
	    
		boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
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
			
			String filename = "test.png";
			File file = new File(filename);
			filename = file.toString();
			Imgcodecs.imwrite(filename, mat);
			
			
			logger.warn("found {}", found);
		}
		return found;
	}
	
	/*public void calibrate(FImage oiFrame, List<Point2dImpl> corners)
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
        }
		
		logger.warn("found {} {}", corners.get(0).x, corners.get(0).y);
		
		//CameraCalibration calibrator = new CameraCalibration(listCorners, oiFrame.width, oiFrame.height);
		
		//CameraIntrinsics cameraIntrinsics  = calibrator.getIntrisics();
		
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
*/
}
