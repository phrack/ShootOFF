/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.camera.autocalibration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.util.Callback;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import ch.qos.logback.classic.Level;

import com.shootoff.camera.Camera;
import com.shootoff.camera.CameraManager;

public class AutoCalibrationManager {
	private static final Logger logger = LoggerFactory.getLogger(AutoCalibrationManager.class);

	private static final int PATTERN_WIDTH = 9;
	private static final int PATTERN_HEIGHT = 6;
	private static final double PAPER_MARGIN_WIDTH = 1.048;
	private static final double PAPER_MARGIN_HEIGHT = 1.063;
	private static final Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);

	private Callback<Void, Void> callback;

	private CameraManager cameraManager;

	private final boolean calculateFrameDelay;

	// Stores the transformation matrix
	private Mat perspMat = null;

	// Stores the bounding box we'll pass back to CameraManager
	private Bounds boundingBox = null;

	private boolean warpInitialized = false;
	private boolean isCalibrated = false;

	// Edge is 11 pixels wide. Squares are 168 pixels wide.
	// 11/168 = 0.06547619047619047619047619047619
	// Maybe I should have made it divisible...
	private static final double BORDER_FACTOR = 0.065476;

	private long frameTimestampBeforeFrameChange;

	private Bounds boundsResult = null;
	private long frameDelayResult;

	private final TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 60, 0.0001);

	/* Paper Pattern */
	private Optional<Dimension2D> paperDimensions = Optional.empty();

	public Optional<Dimension2D> getPaperDimensions() {
		return paperDimensions;
	}

	public void setPaperDimensions(Optional<Dimension2D> paperDimensions) {
		this.paperDimensions = paperDimensions;
	}

	public AutoCalibrationManager(final CameraManager cameraManager, final boolean calculateFrameDelay) {
		this.cameraManager = cameraManager;
		this.calculateFrameDelay = calculateFrameDelay;
	}

	public void setCallback(final Callback<Void, Void> callback) {
		this.callback = callback;
	}

	public Mat getPerspMat() {
		return perspMat;
	}

	public Bounds getBoundsResult() {
		if (boundsResult == null)
			logger.error("getBoundsResult called when boundsResult==null, isCalibrated {}", isCalibrated);

		return boundsResult;
	}

	public long getFrameDelayResult() {
		return frameDelayResult;
	}

	public void reset() {
		isCalibrated = false;
		warpInitialized = false;
		boundsResult = null;

		paperDimensions = Optional.empty();
	}
	
	// Converts to BW mat from bufferedimage
	public void preProcessFrame(final BufferedImage frame, final Mat mat)
	{
		final Mat matTemp;

		synchronized (frame) {
			matTemp = Camera.bufferedImageToMat(frame);
		}
		Imgproc.cvtColor(matTemp, mat, Imgproc.COLOR_BGR2GRAY);
				
		if (logger.isTraceEnabled())
		{
			String filename = String.format("grayscale.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, mat);
		}

	}

	public void processFrame(final BufferedImage frame) {
		if (boundsResult == null) {
			
			
			Mat mat = new Mat();
			preProcessFrame(frame, mat);

			
			// TODO: Make a master function that finds all chessboard corners, then just returns them as a list
			// Instead of all this garbage intertwined with paper and projector calibration

			// Step 1: Find the chessboard corners
			Optional<MatOfPoint2f> boardCorners = findChessboard(mat);

			if (!boardCorners.isPresent()) return;

			// THIS FUNCTION ALSO BLANKS THE PAPER PATTERN IN mat
			// Which the function description tells you, so this is a second warning
			Optional<Dimension2D> newPaperDimensions = findPaperPattern(boardCorners.get(), mat, null);
			
			if (!paperDimensions.isPresent() && newPaperDimensions.isPresent())
			{
				paperDimensions = newPaperDimensions;
				
				logger.debug("Found paper dimensions {}", paperDimensions.get());
			}

			if (newPaperDimensions.isPresent())
				boardCorners = findChessboard(mat);
			
			if (!boardCorners.isPresent()) return;
			
		
			Optional<Bounds> bounds = calibrateFrame(boardCorners.get(), mat);

			if (bounds.isPresent()) {
				boundsResult = bounds.get();

				if (calculateFrameDelay) {
					logger.debug("Checking frame delay");

					checkForFrameChange(mat);
					frameTimestampBeforeFrameChange = cameraManager.getCurrentFrameTimestamp();
					cameraManager.setArenaBackground(null);
				} else {
					if (callback != null) {
						callback.call(null);
					}
				}
			}
		} else {
			final Optional<Long> frameDelay = checkForFrameChange(Camera.bufferedImageToMat(frame));

			if (frameDelay.isPresent()) {
				frameDelayResult = frameDelay.get();

				logger.debug("frameDelayResult {}", frameDelayResult);

				if (callback != null) {
					callback.call(null);
				}
			}
		}
	}

	private double[] patternLuminosity = { -1, -1, -1 };

	private Optional<Long> checkForFrameChange(Mat mat) {
		mat = undistortFrame(mat);

		final double[] pixel = getFrameDelayPixel(mat);

		// Initialize
		if (patternLuminosity[0] == -1) {
			patternLuminosity = pixel;
			return Optional.empty();
		}

		final Mat tempMat = new Mat(1, 2, CvType.CV_8UC3);
		tempMat.put(0, 0, patternLuminosity);
		tempMat.put(0, 1, pixel);

		Imgproc.cvtColor(tempMat, tempMat, Imgproc.COLOR_BGR2HSV);

		if (tempMat.get(0, 1)[2] < .9 * tempMat.get(0, 0)[2]) {
			return Optional.of(cameraManager.getCurrentFrameTimestamp() - frameTimestampBeforeFrameChange);
		}

		return Optional.empty();
	}

	private double[] getFrameDelayPixel(Mat mat) {
		final double squareHeight = boundsResult.getHeight() / (double) (PATTERN_HEIGHT + 1);
		final double squareWidth = boundsResult.getWidth() / (double) (PATTERN_WIDTH + 1);

		final int secondSquareCenterX = (int) (boundsResult.getMinX() + (squareWidth * 1.5));
		final int secondSquareCenterY = (int) (boundsResult.getMinY() + (squareHeight * .5));

		return mat.get(secondSquareCenterY, secondSquareCenterX);
	}

	public Optional<Bounds> calibrateFrame(MatOfPoint2f boardCorners, Mat mat) {

		// For debugging
		Mat traceMat = null;
		if (logger.isTraceEnabled()) {
			traceMat = mat.clone();
		}

		// Turn the chessboard into corners
		final MatOfPoint2f boardRect = calcBoardRectFromCorners(boardCorners);
		
		// Estimate the pattern corners
		MatOfPoint2f estimatedPatternRect = estimatePatternRect(traceMat, boardRect);
		
		// More definitively find corners using goodFeaturesToTrack
		final MatOfPoint corners = findCorners(boardRect, mat, estimatedPatternRect);

		if (corners.total() != 4) return Optional.empty();
		

		// Creates sorted cornerArray for warp perspective
		MatOfPoint2f corners2f = sortPointsForWarpPerspective(boardRect, corners);

		if (logger.isTraceEnabled()) {
			String filename = String.format("calibrate-dist.png");
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, traceMat);
		}

		// Initialize the warp matrix and bounding box
		initializeWarpPerspective(mat, corners2f);

		if (boundingBox.getMinX() < 0 || boundingBox.getMinY() < 0
				|| boundingBox.getWidth() > cameraManager.getFeedWidth()
				|| boundingBox.getHeight() > cameraManager.getFeedHeight()) {
			return Optional.empty();
		}

		if (logger.isDebugEnabled()) logger.debug("bounds {} {} {} {}", boundingBox.getMinX(), boundingBox.getMinY(),
				boundingBox.getWidth(), boundingBox.getHeight());



		if (logger.isTraceEnabled()) {
			final Mat undistorted = warpPerspective(mat);

			String filename = String.format("calibrate-undist.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, undistorted);

			Mat undistortedCropped = undistorted.submat((int) boundingBox.getMinY(), (int) boundingBox.getMaxY(),
					(int) boundingBox.getMinX(), (int) boundingBox.getMaxX());

			filename = String.format("calibrate-undist-cropped.png");
			file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, undistortedCropped);
		}

		Mat warpedBoardCorners = warpCorners(boardCorners);

		isCalibrated = true;

		if (calculateFrameDelay) {
			final Mat undistorted = warpPerspective(mat);
			
			findColors(undistorted, warpedBoardCorners);

			final double squareHeight = boundingBox.getHeight() / (double) (PATTERN_HEIGHT + 1);
			final double squareWidth = boundingBox.getWidth() / (double) (PATTERN_WIDTH + 1);

			int secondSquareCenterX = (int) (boundingBox.getMinX() + (squareWidth * 1.5));
			int secondSquareCenterY = (int) (boundingBox.getMinY() + (squareHeight * .5));

			if (logger.isDebugEnabled()) logger.debug("pF getFrameDelayPixel x {} y {} p {}", secondSquareCenterX,
					secondSquareCenterY, undistorted.get(secondSquareCenterY, secondSquareCenterX));

		}

		return Optional.of(boundingBox);
	}

	private MatOfPoint2f sortPointsForWarpPerspective(final MatOfPoint2f boardRect, final MatOfPoint corners) {
		Point[] cornerArray = new Point[4];
		Double[] cornerED = new Double[4];
		Point[] boardRectArray = boardRect.toArray();
		for (int i = 0; i < 4; i++) cornerED[i] = -1.0;
		for (Point cpt : corners.toArray())
		{
			for (int i = 0; i < 4; i++)
			{
				
				double tempED = euclideanDistance(cpt, boardRectArray[i]);
				if (cornerED[i] == -1.0 || tempED < cornerED[i])
				{
					cornerArray[i] = cpt;
					cornerED[i] = tempED;
				}
			}
		}
		
		MatOfPoint2f corners2f = new MatOfPoint2f();
		corners2f.fromArray(cornerArray);
		return corners2f;
	}

	private MatOfPoint findCorners(MatOfPoint2f boardRect, Mat mat, MatOfPoint2f estimatedPatternRect) {
		// Turn mat BW for corner discovery
		
		// This is dynamic per OTSU algorithm
		Imgproc.threshold(mat, mat, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);


		
		MatOfPoint corners = new MatOfPoint();
		
		Mat mask = Mat.zeros(mat.size(), CvType.CV_8UC1);
		
		final Point[] estimatedPoints = estimatedPatternRect.toArray();
		
		// Establishes a search region
		long region = mat.total() / 19200;
		
		// This gets rid of the checkerboard pattern on the corners
		// Otherwise the corner detection will pick up the wrong corner potentially
		for (Point pt : boardRect.toList())
		{
			Mat tempMask = new Mat();
			tempMask.create(mat.rows()+2, mat.cols()+2, CvType.CV_8UC1);
			Point upwards = new Point(pt.x-5, pt.y-5);
			Point downwards = new Point(pt.x-5, pt.y+5);
			Imgproc.floodFill(mat, tempMask, upwards, new Scalar(255));
			Imgproc.floodFill(mat, tempMask, downwards, new Scalar(255));
		}
		
		// Create a mask containing the search region for each corner
		for (Point pt : estimatedPoints)
		{
			Point leftpt = new Point(pt.x-region, pt.y-region);
			Point rightpt = new Point(pt.x+region, pt.y+region);
			
			Core.rectangle(mask, leftpt, rightpt, new Scalar(255),-1);
			
			
		}
				
		// Finds the corners
		Imgproc.goodFeaturesToTrack(mat, corners, 4, .01, region, mask, 3, false, .04);
		
		if (logger.isTraceEnabled())
		{
			String filename = String.format("mask.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, mask);
			
			Mat tempMat = new Mat(mat.size(), CvType.CV_8UC3);
			Imgproc.cvtColor(mat, tempMat, Imgproc.COLOR_GRAY2BGR);
			
			for (Point corner : corners.toList())
			{
				Core.circle(tempMat, corner, 1, new Scalar(0, 0, 255), -1);	
			}
			
			filename = String.format("corners.png");
			file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, tempMat);

		}
		return corners;
	}

	/**
	 * Perspective pattern discovery
	 * 
	 * Works similar to arena calibration but does not try to identify the
	 * outline of the projection area We are only concerned with size, not
	 * alignment or angle
	 * 
	 * This function blanks out the pattern that it discovers in the Mat it is
	 * provided. This is so that the pattern is not discovered by future pattern
	 * discovery, e.g. auto-calibration
	 * 
	 * workingMat should be null for all external callers unless there is some
	 * need to work off a different Mat than is having patterns blanked out by
	 * this function
	 */
	public Optional<Dimension2D> findPaperPattern(MatOfPoint2f boardCorners, Mat mat, Mat workingMat) {

		if (workingMat == null) workingMat = mat.clone();

		final RotatedRect rect = getPaperPatternDimensions(workingMat, boardCorners);

		// OpenCV gives us the checkerboard corners, not the outside dimension
		// So this estimates where the outside corner would be, plus a fudge
		// factor for the edge of the paper
		// Printer margins are usually a quarter inch on each edge
		double rect_width = rect.size.width,rect_height = rect.size.height;
		double width = rect_width,height = rect_height;
		
		// Flip them if its sideways
		if (height > width)
		{
			width = rect_height;
			height = rect_width;
			rect_height = width;
			rect_width = height;
		}

		width = ((double) width * ((double) (PATTERN_WIDTH + 1) / (double) (PATTERN_WIDTH - 1))
				* PAPER_MARGIN_WIDTH * 1+(BORDER_FACTOR/PATTERN_WIDTH));
		height = ((double) height
				* ((double) (PATTERN_HEIGHT + 1) / (double) (PATTERN_HEIGHT - 1)) * PAPER_MARGIN_HEIGHT * 1+(BORDER_FACTOR/PATTERN_HEIGHT));

		final double PAPER_PATTERN_SIZE_THRESHOLD = .25;
		if (width > PAPER_PATTERN_SIZE_THRESHOLD * workingMat.cols()
				|| height > PAPER_PATTERN_SIZE_THRESHOLD * workingMat.rows()) {
			logger.trace("Pattern too big to be paper, must be projection, setting blank {}", rect);
			
			blankRotatedRect(workingMat, rect);
			
			if (logger.isTraceEnabled())
			{
				String filename = String.format("blanked-box.png");
				File file = new File(filename);
				filename = file.toString();
				Highgui.imwrite(filename, workingMat);

			}

			final Optional<MatOfPoint2f> boardCornersNew = findChessboard(workingMat);

			if (!boardCornersNew.isPresent()) return Optional.empty();
			
			logger.trace("Found new pattern, attempting findPaperPattern {}", boardCornersNew.get());

			return findPaperPattern(boardCornersNew.get(), mat, workingMat);

		}

		if (logger.isTraceEnabled()) {
			logger.trace("pattern width {} height {}", rect_width, rect_height);

			logger.trace("paper width {} height {}", width, height);

		}

		blankRotatedRect(mat, rect);

		return Optional.of(new Dimension2D(width, height));
	}

	// What a stupid function, can't be the best way
	private void blankRotatedRect(Mat mat, final RotatedRect rect) {
		Mat tempMat = Mat.zeros(mat.size(), CvType.CV_8UC1);
		
		Point points[] = new Point[4];
		rect.points(points);
		for(int i=0; i<4; ++i){
		    Core.line(tempMat, points[i], points[(i+1)%4], new Scalar(255,255,255));
		}
		
		Mat tempMask = Mat.zeros((mat.rows()+2), (mat.cols()+2), CvType.CV_8UC1);
		Imgproc.floodFill(tempMat, tempMask, rect.center, new Scalar(255,255,255), null, new Scalar(0,0,0), new Scalar(254,254,254), 4);

		if (logger.isTraceEnabled())
		{
			String filename = String.format("poly.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, tempMat);
		}
		
		mat.setTo(new Scalar(0,0,0), tempMat);
	}

	private RotatedRect getPaperPatternDimensions(Mat traceMat, MatOfPoint2f boardCorners) {
		final MatOfPoint2f boardRect2f = calcBoardRectFromCorners(boardCorners);	
		return Imgproc.minAreaRect(boardRect2f);
		
	}

	private void findColors(Mat frame, Mat warpedBoardCorners) {
		final Point rCenter = findChessBoardSquareCenter(warpedBoardCorners, 2, 3);
		final Point gCenter = findChessBoardSquareCenter(warpedBoardCorners, 2, 5);
		final Point bCenter = findChessBoardSquareCenter(warpedBoardCorners, 2, 7);

		if (logger.isTraceEnabled()) {
			logger.trace("findColors {} {} {}", rCenter, gCenter, bCenter);
			logger.trace("findColors r {} {} {} {}", (int) rCenter.y - 10, (int) rCenter.y + 10, (int) rCenter.x - 10,
					(int) rCenter.x + 10);
		}

		final Scalar rMeanColor = Core.mean(
				frame.submat((int) rCenter.y - 10, (int) rCenter.y + 10, (int) rCenter.x - 10, (int) rCenter.x + 10));
		final Scalar gMeanColor = Core.mean(
				frame.submat((int) gCenter.y - 10, (int) gCenter.y + 10, (int) gCenter.x - 10, (int) gCenter.x + 10));
		final Scalar bMeanColor = Core.mean(
				frame.submat((int) bCenter.y - 10, (int) bCenter.y + 10, (int) bCenter.x - 10, (int) bCenter.x + 10));

		if (logger.isTraceEnabled()) {
			String filename = String.format("rColor.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, frame.submat((int) rCenter.y - 10, (int) rCenter.y + 10, (int) rCenter.x - 10,
					(int) rCenter.x + 10));

			filename = String.format("gColor.png");
			file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, frame.submat((int) gCenter.y - 10, (int) gCenter.y + 10, (int) gCenter.x - 10,
					(int) gCenter.x + 10));

			filename = String.format("bColor.png");
			file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, frame.submat((int) bCenter.y - 10, (int) bCenter.y + 10, (int) bCenter.x - 10,
					(int) bCenter.x + 10));
		}

		if (logger.isTraceEnabled()) logger.trace("meanColor {} {} {}", rMeanColor, gMeanColor, bMeanColor);
	}


	public BufferedImage undistortFrame(BufferedImage frame) {
		if (isCalibrated) {
			final Mat mat = Camera.bufferedImageToMat(frame);

			frame = Camera.matToBufferedImage(warpPerspective(mat));
		} else {
			logger.warn("undistortFrame called when isCalibrated is false");
		}

		return frame;
	}
	
	// MUST BE IN BGR pixel format.
	public Mat undistortFrame(Mat mat) {
		if (!isCalibrated) {
			logger.warn("undistortFrame called when isCalibrated is false");
			return mat;
		}

		return warpPerspective(mat);
	}

	private RotatedRect boundsRect;

	private MatOfPoint2f estimatePatternRect(Mat traceMat, MatOfPoint2f boardRect) {


		// We use this to calculate the angle
		final RotatedRect boardBox = Imgproc.minAreaRect(boardRect);
		final double boardBoxAngle = boardBox.size.height > boardBox.size.width ? 90.0 + boardBox.angle
				: boardBox.angle;

		// This is the board corners with the angle eliminated
		final Mat unRotMat = getRotationMatrix(massCenterMatOfPoint2f(boardRect), boardBoxAngle);
		final MatOfPoint2f unRotatedRect = rotateRect(unRotMat, boardRect);

		// This is the estimated projection area that has minimum angle (Not
		// rotated)
		final MatOfPoint2f estimatedPatternSizeRect = estimateFullPatternSize(unRotatedRect);

		// This is what we'll use as the transformation target and bounds given
		// back to the cameramanager
		boundsRect = Imgproc.minAreaRect(estimatedPatternSizeRect);

		// We now rotate the estimation back to the original angle to use for
		// transformation source
		final Mat rotMat = getRotationMatrix(massCenterMatOfPoint2f(estimatedPatternSizeRect), -boardBoxAngle);

		final MatOfPoint2f rotatedPatternSizeRect = rotateRect(rotMat, estimatedPatternSizeRect);

		if (logger.isTraceEnabled()) {
			logger.trace("center {} angle {} width {} height {}", boardBox.center, boardBoxAngle, boardBox.size.width,
					boardBox.size.height);

			logger.debug("boundsRect {} {} {} {}", boundsRect.boundingRect().x, boundsRect.boundingRect().y,
					boundsRect.boundingRect().x + boundsRect.boundingRect().width,
					boundsRect.boundingRect().y + boundsRect.boundingRect().height);

			Core.circle(traceMat, new Point(boardRect.get(0, 0)[0], boardRect.get(0, 0)[1]), 1, new Scalar(255, 0, 0),
					-1);
			Core.circle(traceMat, new Point(boardRect.get(1, 0)[0], boardRect.get(1, 0)[1]), 1, new Scalar(255, 0, 0),
					-1);
			Core.circle(traceMat, new Point(boardRect.get(2, 0)[0], boardRect.get(2, 0)[1]), 1, new Scalar(255, 0, 0),
					-1);
			Core.circle(traceMat, new Point(boardRect.get(3, 0)[0], boardRect.get(3, 0)[1]), 1, new Scalar(255, 0, 0),
					-1);

			Core.line(traceMat, new Point(unRotatedRect.get(0, 0)[0], unRotatedRect.get(0, 0)[1]),
					new Point(unRotatedRect.get(1, 0)[0], unRotatedRect.get(1, 0)[1]), new Scalar(0, 255, 0));
			Core.line(traceMat, new Point(unRotatedRect.get(1, 0)[0], unRotatedRect.get(1, 0)[1]),
					new Point(unRotatedRect.get(2, 0)[0], unRotatedRect.get(2, 0)[1]), new Scalar(0, 255, 0));
			Core.line(traceMat, new Point(unRotatedRect.get(3, 0)[0], unRotatedRect.get(3, 0)[1]),
					new Point(unRotatedRect.get(2, 0)[0], unRotatedRect.get(2, 0)[1]), new Scalar(0, 255, 0));
			Core.line(traceMat, new Point(unRotatedRect.get(3, 0)[0], unRotatedRect.get(3, 0)[1]),
					new Point(unRotatedRect.get(0, 0)[0], unRotatedRect.get(0, 0)[1]), new Scalar(0, 255, 0));

			Core.line(traceMat, new Point(estimatedPatternSizeRect.get(0, 0)[0], estimatedPatternSizeRect.get(0, 0)[1]),
					new Point(estimatedPatternSizeRect.get(1, 0)[0], estimatedPatternSizeRect.get(1, 0)[1]),
					new Scalar(255, 255, 0));
			Core.line(traceMat, new Point(estimatedPatternSizeRect.get(1, 0)[0], estimatedPatternSizeRect.get(1, 0)[1]),
					new Point(estimatedPatternSizeRect.get(2, 0)[0], estimatedPatternSizeRect.get(2, 0)[1]),
					new Scalar(255, 255, 0));
			Core.line(traceMat, new Point(estimatedPatternSizeRect.get(3, 0)[0], estimatedPatternSizeRect.get(3, 0)[1]),
					new Point(estimatedPatternSizeRect.get(2, 0)[0], estimatedPatternSizeRect.get(2, 0)[1]),
					new Scalar(255, 255, 0));
			Core.line(traceMat, new Point(estimatedPatternSizeRect.get(3, 0)[0], estimatedPatternSizeRect.get(3, 0)[1]),
					new Point(estimatedPatternSizeRect.get(0, 0)[0], estimatedPatternSizeRect.get(0, 0)[1]),
					new Scalar(255, 255, 0));

			Core.line(traceMat, new Point(rotatedPatternSizeRect.get(0, 0)[0], rotatedPatternSizeRect.get(0, 0)[1]),
					new Point(rotatedPatternSizeRect.get(1, 0)[0], rotatedPatternSizeRect.get(1, 0)[1]),
					new Scalar(255, 255, 0));
			Core.line(traceMat, new Point(rotatedPatternSizeRect.get(1, 0)[0], rotatedPatternSizeRect.get(1, 0)[1]),
					new Point(rotatedPatternSizeRect.get(2, 0)[0], rotatedPatternSizeRect.get(2, 0)[1]),
					new Scalar(255, 255, 0));
			Core.line(traceMat, new Point(rotatedPatternSizeRect.get(3, 0)[0], rotatedPatternSizeRect.get(3, 0)[1]),
					new Point(rotatedPatternSizeRect.get(2, 0)[0], rotatedPatternSizeRect.get(2, 0)[1]),
					new Scalar(255, 255, 0));
			Core.line(traceMat, new Point(rotatedPatternSizeRect.get(3, 0)[0], rotatedPatternSizeRect.get(3, 0)[1]),
					new Point(rotatedPatternSizeRect.get(0, 0)[0], rotatedPatternSizeRect.get(0, 0)[1]),
					new Scalar(255, 255, 0));
		}

		return rotatedPatternSizeRect;
	}

	/*
	 * This function takes a rectangular region representing the chessboard
	 * inner corners and estimates the corners of the full pattern image
	 */
	private MatOfPoint2f estimateFullPatternSize(MatOfPoint2f rect) {
		// Result Mat
		final MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);

		// Get the sources as points
		final Point topLeft = new Point(rect.get(0, 0)[0], rect.get(0, 0)[1]);
		final Point topRight = new Point(rect.get(1, 0)[0], rect.get(1, 0)[1]);
		final Point bottomRight = new Point(rect.get(2, 0)[0], rect.get(2, 0)[1]);
		final Point bottomLeft = new Point(rect.get(3, 0)[0], rect.get(3, 0)[1]);

		// We need the heights and widths to estimate the square sizes

		final double topWidth = Math.sqrt(Math.pow(topRight.x - topLeft.x, 2) + Math.pow(topRight.y - topLeft.y, 2));
		final double leftHeight = Math
				.sqrt(Math.pow(bottomLeft.x - topLeft.x, 2) + Math.pow(bottomLeft.y - topLeft.y, 2));
		final double bottomWidth = Math
				.sqrt(Math.pow(bottomRight.x - bottomLeft.x, 2) + Math.pow(bottomRight.y - bottomLeft.y, 2));
		final double rightHeight = Math
				.sqrt(Math.pow(bottomRight.x - topRight.x, 2) + Math.pow(bottomRight.y - topRight.y, 2));

		if (logger.isTraceEnabled()) {
			logger.trace("points {} {} {} {}", topLeft, topRight, bottomRight, bottomLeft);

			double angle = Math.atan((topRight.y - topLeft.y) / (topRight.x - topLeft.x)) * 180 / Math.PI;
			double angle2 = Math.atan((bottomRight.y - bottomLeft.y) / (bottomRight.x - bottomLeft.x)) * 180 / Math.PI;

			logger.trace("square size {} {} - angle {}", topWidth / (PATTERN_WIDTH - 1),
					leftHeight / (PATTERN_HEIGHT - 1), angle);
			logger.trace("square size {} {} - angle {}", bottomWidth / (PATTERN_WIDTH - 1),
					rightHeight / (PATTERN_HEIGHT - 1), angle2);
		}

		// Estimate the square widths, that is what we base the estimate of the
		// real corners on

		double squareTopWidth = (1 + BORDER_FACTOR) * (topWidth / (PATTERN_WIDTH - 1));
		double squareLeftHeight = (1 + BORDER_FACTOR) * (leftHeight / (PATTERN_HEIGHT - 1));
		double squareBottomWidth = (1 + BORDER_FACTOR) * (bottomWidth / (PATTERN_WIDTH - 1));
		double squareRightHeight = (1 + BORDER_FACTOR) * (rightHeight / (PATTERN_HEIGHT - 1));

		// The estimations
		double[] newTopLeft = { topLeft.x - squareTopWidth, topLeft.y - squareLeftHeight };
		double[] newBottomLeft = { bottomLeft.x - squareBottomWidth, bottomLeft.y + squareLeftHeight };
		double[] newTopRight = { topRight.x + squareTopWidth, topRight.y - squareRightHeight };
		double[] newBottomRight = { bottomRight.x + squareBottomWidth, bottomRight.y + squareRightHeight };

		// Populate the result
		result.put(0, 0, newTopLeft);
		result.put(1, 0, newTopRight);
		result.put(2, 0, newBottomRight);
		result.put(3, 0, newBottomLeft);

		return result;
	}

	// Given a rotation matrix and a quadrilateral, rotate the points
	private MatOfPoint2f rotateRect(Mat rotMat, MatOfPoint2f boardRect) {
		final MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);
		for (int i = 0; i < 4; i++) {
			final Point rPoint = rotPoint(rotMat, new Point(boardRect.get(i, 0)[0], boardRect.get(i, 0)[1]));
			final double[] rPointD = new double[2];
			rPointD[0] = rPoint.x;
			rPointD[1] = rPoint.y;
			result.put(i, 0, rPointD);
		}
		return result;
	}

	private Mat getRotationMatrix(final Point center, final double rotationAngle) {
		return Imgproc.getRotationMatrix2D(center, rotationAngle, 1.0);
	}

	/*
	 * The one time calculation of the transformations.
	 * 
	 * After this is done, the transformation is just applied
	 */
	private void initializeWarpPerspective(final Mat frame, final MatOfPoint2f sourceCorners) {
		final MatOfPoint2f destCorners = new MatOfPoint2f();
		destCorners.alloc(4);

		destCorners.put(0, 0, new double[] { boundsRect.boundingRect().x, boundsRect.boundingRect().y });
		destCorners.put(1, 0, new double[] { boundsRect.boundingRect().x + boundsRect.boundingRect().width,
				boundsRect.boundingRect().y });
		destCorners.put(3, 0, new double[] { boundsRect.boundingRect().x,
				boundsRect.boundingRect().y + boundsRect.boundingRect().height });
		destCorners.put(2, 0, new double[] { boundsRect.boundingRect().x + boundsRect.boundingRect().width,
				boundsRect.boundingRect().y + boundsRect.boundingRect().height });

		if (logger.isDebugEnabled()) {
			logger.debug("initializeWarpPerspective {} {} {} {}", sourceCorners.get(0, 0), sourceCorners.get(1, 0),
					sourceCorners.get(2, 0), sourceCorners.get(3, 0));
			logger.debug("initializeWarpPerspective {} {} {} {}", destCorners.get(0, 0), destCorners.get(1, 0),
					destCorners.get(2, 0), destCorners.get(3, 0));
		}

		perspMat = Imgproc.getPerspectiveTransform(sourceCorners, destCorners);

		int width = boundsRect.boundingRect().width;
		int height = boundsRect.boundingRect().height;

		// Make them divisible by two for video recording purposes
		if ((width & 1) == 1) width++;
		if ((height & 1) == 1) height++;

		boundingBox = new BoundingBox(boundsRect.boundingRect().x, boundsRect.boundingRect().y, width, height);

		warpInitialized = true;

		if (logger.isTraceEnabled()) {
			Mat debugFrame = frame.clone();

			Core.circle(debugFrame, new Point(sourceCorners.get(0, 0)[0], sourceCorners.get(0, 0)[1]), 1,
					new Scalar(255, 0, 255), -1);
			Core.circle(debugFrame, new Point(sourceCorners.get(1, 0)[0], sourceCorners.get(1, 0)[1]), 1,
					new Scalar(255, 0, 255), -1);
			Core.circle(debugFrame, new Point(sourceCorners.get(2, 0)[0], sourceCorners.get(2, 0)[1]), 1,
					new Scalar(255, 0, 255), -1);
			Core.circle(debugFrame, new Point(sourceCorners.get(3, 0)[0], sourceCorners.get(3, 0)[1]), 1,
					new Scalar(255, 0, 255), -1);

			Core.circle(debugFrame, new Point(destCorners.get(0, 0)[0], destCorners.get(0, 0)[1]), 1,
					new Scalar(255, 0, 0), -1);
			Core.circle(debugFrame, new Point(destCorners.get(1, 0)[0], destCorners.get(1, 0)[1]), 1,
					new Scalar(255, 0, 0), -1);
			Core.circle(debugFrame, new Point(destCorners.get(2, 0)[0], destCorners.get(2, 0)[1]), 1,
					new Scalar(255, 0, 0), -1);
			Core.circle(debugFrame, new Point(destCorners.get(3, 0)[0], destCorners.get(3, 0)[1]), 1,
					new Scalar(255, 0, 0), -1);

			Core.line(debugFrame, new Point(boundingBox.getMinX(), boundingBox.getMinY()),
					new Point(boundingBox.getMaxX(), boundingBox.getMinY()), new Scalar(0, 255, 0));
			Core.line(debugFrame, new Point(boundingBox.getMinX(), boundingBox.getMinY()),
					new Point(boundingBox.getMinX(), boundingBox.getMaxY()), new Scalar(0, 255, 0));
			Core.line(debugFrame, new Point(boundingBox.getMaxX(), boundingBox.getMaxY()),
					new Point(boundingBox.getMaxX(), boundingBox.getMinY()), new Scalar(0, 255, 0));
			Core.line(debugFrame, new Point(boundingBox.getMaxX(), boundingBox.getMaxY()),
					new Point(boundingBox.getMinX(), boundingBox.getMaxY()), new Scalar(0, 255, 0));

			String filename = String.format("calibrate-transformation.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, debugFrame);
		}
	}

	// initializeWarpPerspective MUST BE CALLED first
	private Mat warpPerspective(final Mat frame) {
		if (warpInitialized) {
			final Mat mat = new Mat();
			Imgproc.warpPerspective(frame, mat, perspMat, frame.size(), Imgproc.INTER_LINEAR);

			return mat;
		} else {
			logger.warn("warpPerspective called when warpInitialized is false - {} {} - {}", perspMat, boundingBox,
					isCalibrated);

			return frame;
		}
	}

	// initializeWarpPerspective MUST BE CALLED first
	private Mat warpCorners(MatOfPoint2f imageCorners) {
		Mat mat = null;

		if (warpInitialized) {
			mat = new Mat();
			Core.transform(imageCorners, mat, perspMat);
		} else {
			logger.warn("warpCorners called when warpInitialized is false - {} {} - {}", perspMat, boundingBox,
					isCalibrated);
		}

		return mat;
	}

	public Optional<MatOfPoint2f> findChessboard(Mat mat) {
		
		MatOfPoint2f imageCorners = new MatOfPoint2f();

		boolean found = Calib3d.findChessboardCorners(mat, boardSize, imageCorners,
				Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_NORMALIZE_IMAGE);

		logger.trace("found {}", found);

		if (found) {
			
			// optimization
			Imgproc.cornerSubPix(mat, imageCorners, new Size(1, 1), new Size(-1, -1), term);

			return Optional.of(imageCorners);
		}
		return Optional.empty();
	}

	// converts the chessboard corners into a quadrilateral
	private MatOfPoint2f calcBoardRectFromCorners(MatOfPoint2f corners) {
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);

		Point topLeft = new Point(corners.get(0, 0)[0], corners.get(0, 0)[1]);
		Point topRight = new Point(corners.get(PATTERN_WIDTH - 1, 0)[0], corners.get(PATTERN_WIDTH - 1, 0)[1]);
		Point bottomRight = new Point(corners.get(PATTERN_WIDTH * PATTERN_HEIGHT - 1, 0)[0],
				corners.get(PATTERN_WIDTH * PATTERN_HEIGHT - 1, 0)[1]);
		Point bottomLeft = new Point(corners.get(PATTERN_WIDTH * (PATTERN_HEIGHT - 1), 0)[0],
				corners.get(PATTERN_WIDTH * (PATTERN_HEIGHT - 1), 0)[1]);

		result.put(0, 0, topLeft.x, topLeft.y, topRight.x, topRight.y, bottomRight.x, bottomRight.y, bottomLeft.x,
				bottomLeft.y);

		return result;
	}

	private Point findChessBoardSquareCenter(Mat corners, int row, int col) {
		if (row >= PATTERN_HEIGHT - 1 || col >= PATTERN_WIDTH - 1) {
			logger.warn("findChessBoardSquareColor invalid row or col {} {}", row, col);
			return null;
		}

		final Point topLeft = new Point(corners.get((row * PATTERN_WIDTH - 1) + col, 0)[0],
				corners.get((row * PATTERN_WIDTH - 1) + col, 0)[1]);
		final Point bottomRight = new Point(corners.get(((row + 1) * PATTERN_WIDTH - 1) + col + 1, 0)[0],
				corners.get(((row + 1) * PATTERN_WIDTH - 1) + col + 1, 0)[1]);

		final Point result = new Point((topLeft.x + bottomRight.x) / 2, (topLeft.y + bottomRight.y) / 2);

		if (logger.isTraceEnabled()) {
			logger.trace("findChessBoardSquareColor {}", corners.size());

			logger.trace("findChessBoardSquareColor {} {}", (row * PATTERN_WIDTH - 1) + col,
					((row + 1) * PATTERN_WIDTH - 1) + col + 1);
			logger.trace("findChessBoardSquareColor {} {} {}", topLeft, bottomRight, result);
		}

		return result;
	}
	
	private double euclideanDistance(final Point pt1, final Point pt2) {
		return Math.sqrt(Math.pow(pt1.x - pt2.x, 2) + Math.pow(pt1.y - pt2.y, 2));
	}



	// Given a rotation matrix, rotates a point
	private Point rotPoint(final Mat rot_mat, final Point point) {
		final Point rp = new Point();
		rp.x = rot_mat.get(0, 0)[0] * point.x + rot_mat.get(0, 1)[0] * point.y + rot_mat.get(0, 2)[0];
		rp.y = rot_mat.get(1, 0)[0] * point.x + rot_mat.get(1, 1)[0] * point.y + rot_mat.get(1, 2)[0];

		return rp;
	}

	private Point massCenterMatOfPoint2f(final MatOfPoint2f map) {
		final Moments moments = Imgproc.moments(map);
		final Point centroid = new Point();
		centroid.x = moments.get_m10() / moments.get_m00();
		centroid.y = moments.get_m01() / moments.get_m00();
		return centroid;
	}
}