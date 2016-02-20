/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

	private final static int PATTERN_WIDTH = 9;
	private final static int PATTERN_HEIGHT = 6;
	private final static Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);

	private Callback<Void, Void> callback;

	private CameraManager cameraManager;

	private boolean calculateFrameDelay;

	public void setCallback(Callback<Void, Void> callback) {
		this.callback = callback;
	}

	public AutoCalibrationManager(CameraManager cameraManager, boolean calculateFrameDelay) {
		// ((ch.qos.logback.classic.Logger)
		// logger).setLevel(ch.qos.logback.classic.Level.DEBUG);

		this.cameraManager = cameraManager;
		this.calculateFrameDelay = calculateFrameDelay;
	}

	// Stores the transformation matrix
	private Mat perspMat = null;

	public Mat getPerspMat() {
		return perspMat;
	}

	// Stores the bounding box we'll pass back to CameraManager
	private Bounds boundingBox = null;

	private boolean warpInitialized = false;
	private boolean isCalibrated = false;

	// We use this to constrain the hough lines algorithm
	private double minimumDimension = 0.0;

	// Edge is 11 pixels wide. Squares are 168 pixels wide.
	// 11/168 = 0.06547619047619047619047619047619
	// Maybe I should have made it divisible...
	private static final double BORDER_FACTOR = 0.065476;

	private static final double CANNY_THRESHOLD_1 = 50;
	private static final double CANNY_THRESHOLD_2 = 150;
	private static final double GAUSSIANBLUR_SIGMA = 3.0;

	private static final double HOUGHLINES_RHO = 1;

	private static final double HOUGHLINES_THETA = Math.PI / 180;

	private static final int HOUGHLINES_THRESHOLD = 40;
	private Size gaussianBlurSize;

	private long frameTimestampBeforeFrameChange;

	private Bounds boundsResult = null;
	private long frameDelayResult;

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
	}

	public void processFrame(BufferedImage frame) {

		if (boundsResult == null) {
			Optional<Bounds> bounds = calibrateFrame(frame);

			if (bounds.isPresent()) {
				boundsResult = bounds.get();

				if (!calculateFrameDelay) {
					if (callback != null) {
						callback.call(null);
					}
				} else {
					checkForFrameChange(frame);
					frameTimestampBeforeFrameChange = cameraManager.getCurrentFrameTimestamp();
					cameraManager.setArenaBackground(null);
				}
			}
		} else {
			Optional<Long> frameDelay = checkForFrameChange(frame);

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

	private Optional<Long> checkForFrameChange(BufferedImage frame) {
		Mat mat;

		synchronized (frame) {
			undistortFrame(frame);
			mat = Camera.bufferedImageToMat(frame);
		}
		;

		double[] pixel = getFrameDelayPixel(mat);

		// Initialize
		if (patternLuminosity[0] == -1) {
			patternLuminosity = pixel;
			return Optional.empty();
		}

		Mat tempMat = new Mat(1, 2, CvType.CV_8UC3);
		tempMat.put(0, 0, patternLuminosity);
		tempMat.put(0, 1, pixel);

		// logger.debug("checkForFrameChange {}", pixel);

		Imgproc.cvtColor(tempMat, tempMat, Imgproc.COLOR_BGR2HSV);

		// logger.debug("checkForFrameChange {} {}", tempMat.get(0,0),
		// tempMat.get(0,1));

		if (tempMat.get(0, 1)[2] < .9 * tempMat.get(0, 0)[2]) {
			// logger.debug("checkForFrameChange {} {} - frameDelay {}",
			// cameraManager.getCurrentFrameTimestamp(),
			// frameTimestampBeforeFrameChange,
			// cameraManager.getCurrentFrameTimestamp()-
			// frameTimestampBeforeFrameChange);

			return Optional.of(cameraManager.getCurrentFrameTimestamp() - frameTimestampBeforeFrameChange);
		}
		return Optional.empty();
	}

	private double[] getFrameDelayPixel(Mat mat) {
		double squareHeight = boundsResult.getHeight() / (double) (PATTERN_HEIGHT + 1);
		double squareWidth = boundsResult.getWidth() / (double) (PATTERN_WIDTH + 1);

		int secondSquareCenterX = (int) (boundsResult.getMinX() + (squareWidth * 1.5));
		int secondSquareCenterY = (int) (boundsResult.getMinY() + (squareHeight * .5));

		return mat.get(secondSquareCenterY, secondSquareCenterX);
	}

	public Optional<Bounds> calibrateFrame(BufferedImage frame) {
		Mat mat;

		synchronized (frame) {
			mat = Camera.bufferedImageToMat(frame);
		}
		;

		// For debugging
		Mat traceMat = null;
		if (logger.isTraceEnabled()) {
			traceMat = mat.clone();
		}

		initializeSize(frame.getWidth(), frame.getHeight());

		// Step 1: Find the chessboard corners
		Optional<MatOfPoint2f> boardCorners = findChessboard(mat);

		if (!boardCorners.isPresent()) return Optional.empty();

		// Step 2: Estimate the pattern corners
		MatOfPoint2f estimatedPatternRect = estimatePatternRect(traceMat, boardCorners.get());

		// Step 3: Use Hough Lines to find the actual corners
		Optional<MatOfPoint2f> idealCorners = findIdealCorners(mat, estimatedPatternRect);

		if (!idealCorners.isPresent()) return Optional.empty();

		if (logger.isTraceEnabled()) {
			String filename = String.format("calibrate-dist.png");
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, traceMat);
		}

		// Step 4: Initialize the warp matrix and bounding box
		initializeWarpPerspective(mat, idealCorners.get());

		if (boundingBox.getMinX() < 0 || boundingBox.getMinY() < 0
				|| boundingBox.getWidth() > cameraManager.getFeedWidth()
				|| boundingBox.getHeight() > cameraManager.getFeedHeight())
			return Optional.empty();

		if (logger.isDebugEnabled()) logger.debug("bounds {} {} {} {}", boundingBox.getMinX(), boundingBox.getMinY(),
				boundingBox.getWidth(), boundingBox.getHeight());

		Mat undistorted = warpPerspective(mat);

		if (logger.isTraceEnabled()) {

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

		Mat warpedBoardCorners = warpCorners(boardCorners.get());
		findColors(undistorted, warpedBoardCorners);

		isCalibrated = true;

		double squareHeight = boundingBox.getHeight() / (double) (PATTERN_HEIGHT + 1);
		double squareWidth = boundingBox.getWidth() / (double) (PATTERN_WIDTH + 1);

		int secondSquareCenterX = (int) (boundingBox.getMinX() + (squareWidth * 1.5));
		int secondSquareCenterY = (int) (boundingBox.getMinY() + (squareHeight * .5));

		if (logger.isDebugEnabled()) logger.debug("pF getFrameDelayPixel x {} y {} p {}", secondSquareCenterX,
				secondSquareCenterY, undistorted.get(secondSquareCenterY, secondSquareCenterX));

		return Optional.of(boundingBox);

	}

	private void findColors(Mat frame, Mat warpedBoardCorners) {
		Point rCenter = findChessBoardSquareCenter(frame, warpedBoardCorners, 2, 3);
		Point gCenter = findChessBoardSquareCenter(frame, warpedBoardCorners, 2, 5);
		Point bCenter = findChessBoardSquareCenter(frame, warpedBoardCorners, 2, 7);

		if (logger.isDebugEnabled()) {
			logger.debug("findColors {} {} {}", rCenter, gCenter, bCenter);
			logger.debug("findColors r {} {} {} {}", (int) rCenter.y - 10, (int) rCenter.y + 10, (int) rCenter.x - 10,
					(int) rCenter.x + 10);
		}

		Scalar rMeanColor = Core.mean(
				frame.submat((int) rCenter.y - 10, (int) rCenter.y + 10, (int) rCenter.x - 10, (int) rCenter.x + 10));
		Scalar gMeanColor = Core.mean(
				frame.submat((int) gCenter.y - 10, (int) gCenter.y + 10, (int) gCenter.x - 10, (int) gCenter.x + 10));
		Scalar bMeanColor = Core.mean(
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

		if (logger.isDebugEnabled()) logger.debug("meanColor {} {} {}", rMeanColor, gMeanColor, bMeanColor);
	}

	private void initializeSize(int width, int height) {
		// If smaller or equal to 800x600, use 3,3
		// Otherwise 5,5 seems to work well up past 1280x720 at least
		// This is a fudge but I'm not trying to perfect resolution handling

		// Must be odd numbers
		if (width * height <= 480000)
			gaussianBlurSize = new Size(3, 3);
		else
			gaussianBlurSize = new Size(5, 5);
	}

	public BufferedImage undistortFrame(BufferedImage frame) {
		if (!isCalibrated) {
			logger.warn("undistortFrame called when isCalibrated is false");
			return frame;
		}

		Mat mat = Camera.bufferedImageToMat(frame);

		frame = Camera.matToBufferedImage(warpPerspective(mat));

		return frame;
	}

	RotatedRect boundsRect;

	private MatOfPoint2f estimatePatternRect(Mat traceMat, MatOfPoint2f boardCorners) {

		// Turn the chessboard into corners
		MatOfPoint2f boardRect = calcBoardRectFromCorners(boardCorners);

		// We use this to calculate the angle
		RotatedRect boardBox = Imgproc.minAreaRect(boardRect);
		double boardBoxAngle = boardBox.size.height > boardBox.size.width ? 90.0 + boardBox.angle : boardBox.angle;

		// This is the board corners with the angle eliminated
		Mat unRotMat = getRotationMatrix(massCenterMatOfPoint2f(boardRect), boardBoxAngle);
		MatOfPoint2f unRotatedRect = rotateRect(unRotMat, boardRect);

		if (logger.isTraceEnabled()) logger.trace("center {} angle {} width {} height {}", boardBox.center,
				boardBoxAngle, boardBox.size.width, boardBox.size.height);

		// This is the estimated projection area that has minimum angle (Not
		// rotated)
		MatOfPoint2f estimatedPatternSizeRect = estimateFullPatternSize(unRotatedRect);

		// This is what we'll use as the transformation target and bounds given
		// back to the cameramanager
		boundsRect = Imgproc.minAreaRect(estimatedPatternSizeRect);

		if (logger.isDebugEnabled()) logger.debug("boundsRect {} {} {} {}", boundsRect.boundingRect().x,
				boundsRect.boundingRect().y, boundsRect.boundingRect().x + boundsRect.boundingRect().width,
				boundsRect.boundingRect().y + boundsRect.boundingRect().height);

		// We now rotate the estimation back to the original angle to use for
		// transformation source
		Mat rotMat = getRotationMatrix(massCenterMatOfPoint2f(estimatedPatternSizeRect), -boardBoxAngle);

		MatOfPoint2f rotatedPatternSizeRect = rotateRect(rotMat, estimatedPatternSizeRect);

		if (logger.isTraceEnabled()) {
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
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);

		// Get the sources as points
		Point topLeft = new Point(rect.get(0, 0)[0], rect.get(0, 0)[1]);
		Point topRight = new Point(rect.get(1, 0)[0], rect.get(1, 0)[1]);
		Point bottomRight = new Point(rect.get(2, 0)[0], rect.get(2, 0)[1]);
		Point bottomLeft = new Point(rect.get(3, 0)[0], rect.get(3, 0)[1]);

		logger.trace("points {} {} {} {}", topLeft, topRight, bottomRight, bottomLeft);

		// We need the heights and widths to estimate the square sizes

		double topWidth = Math.sqrt(Math.pow(topRight.x - topLeft.x, 2) + Math.pow(topRight.y - topLeft.y, 2));
		double leftHeight = Math.sqrt(Math.pow(bottomLeft.x - topLeft.x, 2) + Math.pow(bottomLeft.y - topLeft.y, 2));
		double bottomWidth = Math
				.sqrt(Math.pow(bottomRight.x - bottomLeft.x, 2) + Math.pow(bottomRight.y - bottomLeft.y, 2));
		double rightHeight = Math
				.sqrt(Math.pow(bottomRight.x - topRight.x, 2) + Math.pow(bottomRight.y - topRight.y, 2));

		if (logger.isTraceEnabled()) {
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

		// Calculate the new heights (We don't need the widths but I'll leave
		// the code here commented out)

		// double newTopWidth = Math.sqrt(Math.pow(newTopRight[0] -
		// newTopLeft[0],2) + Math.pow(newTopRight[1] - newTopLeft[1],2));
		// double newBottomWidth = Math.sqrt(Math.pow(newBottomRight[0] -
		// newBottomLeft[0],2) + Math.pow(newBottomRight[1] -
		// newBottomLeft[1],2));
		double newLeftHeight = Math
				.sqrt(Math.pow(newBottomLeft[0] - newTopLeft[0], 2) + Math.pow(newBottomLeft[1] - newTopLeft[1], 2));
		double newRightHeight = Math.sqrt(
				Math.pow(newBottomRight[0] - newTopRight[0], 2) + Math.pow(newBottomRight[1] - newTopRight[1], 2));

		// The minimum dimension is always from the height because the pattern
		// is shorter on that side
		// Technically it is possible that the pattern is super stretched out,
		// but in that case I think we're
		// better off failing
		minimumDimension = newLeftHeight < newRightHeight ? newLeftHeight : newRightHeight;

		return result;
	}

	// Given a rotation matrix and a quadrilateral, rotate the points
	private MatOfPoint2f rotateRect(Mat rotMat, MatOfPoint2f boardRect) {
		MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);
		for (int i = 0; i < 4; i++) {
			Point rPoint = rotPoint(rotMat, new Point(boardRect.get(i, 0)[0], boardRect.get(i, 0)[1]));
			double[] rPointD = new double[2];
			rPointD[0] = rPoint.x;
			rPointD[1] = rPoint.y;
			result.put(i, 0, rPointD);
		}
		return result;
	}

	private Mat getRotationMatrix(Point center, double rotationAngle) {
		return Imgproc.getRotationMatrix2D(center, rotationAngle, 1.0);
	}

	// Use probabilistic Hough Lines algorithm to calculate the ideal corners of
	// the pattern
	private Optional<MatOfPoint2f> findIdealCorners(Mat frame, MatOfPoint2f estimatedPatternRect) {

		Mat traceMat = null;
		if (logger.isTraceEnabled()) {
			traceMat = frame.clone();
		}

		// pixel distance, dynamic because we want to allow any resolution or
		// distance from pattern
		final int toleranceThreshold = (int) (minimumDimension / (double) (PATTERN_HEIGHT - 1) / 2.0);

		if (logger.isTraceEnabled())
			logger.trace("tolerance threshold {} minimumDimension {}", toleranceThreshold, minimumDimension);

		// Grey scale conversion.
		Mat grey = new Mat();
		Imgproc.cvtColor(frame, grey, Imgproc.COLOR_BGR2GRAY);

		// Find edges
		Imgproc.Canny(grey, grey, CANNY_THRESHOLD_1, CANNY_THRESHOLD_2);

		// Blur the lines, otherwise the lines algorithm does not consider them
		Imgproc.GaussianBlur(grey, grey, gaussianBlurSize, GAUSSIANBLUR_SIGMA);

		if (logger.isTraceEnabled()) {
			String filename = String.format("calibrate-undist-grey-lines.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, grey);
		}

		if (logger.isDebugEnabled()) logger.debug("estimation {} {} {} {}", estimatedPatternRect.get(0, 0),
				estimatedPatternRect.get(1, 0), estimatedPatternRect.get(2, 0), estimatedPatternRect.get(3, 0));

		// Easier to work off of Points
		Point[] estimatedPoints = matOfPoint2fToPoints(estimatedPatternRect);

		if (logger.isTraceEnabled()) {
			Core.circle(traceMat, estimatedPoints[0], 1, new Scalar(0, 0, 255), -1);
			Core.circle(traceMat, estimatedPoints[1], 1, new Scalar(0, 0, 255), -1);
			Core.circle(traceMat, estimatedPoints[2], 1, new Scalar(0, 0, 255), -1);
			Core.circle(traceMat, estimatedPoints[3], 1, new Scalar(0, 0, 255), -1);
		}

		// Find lines
		// These parameters are just guesswork right now
		final Mat mLines = new Mat();
		final int minLineSize = (int) (minimumDimension * .90);
		final int lineGap = toleranceThreshold;

		// Do it
		Imgproc.HoughLinesP(grey, mLines, HOUGHLINES_RHO, HOUGHLINES_THETA, HOUGHLINES_THRESHOLD, minLineSize, lineGap);

		// Find the lines that match our estimates
		List<double[]> verifiedLines = new ArrayList<double[]>();

		for (int x = 0; x < mLines.cols(); x++) {
			double[] vec = mLines.get(0, x);
			double x1 = vec[0], y1 = vec[1], x2 = vec[2], y2 = vec[3];
			Point start = new Point(x1, y1);
			Point end = new Point(x2, y2);

			if (nearPoints(estimatedPoints, start, toleranceThreshold)
					&& nearPoints(estimatedPoints, end, toleranceThreshold)) {
				verifiedLines.add(vec);

				if (logger.isTraceEnabled()) {
					Core.line(traceMat, start, end, new Scalar(255, 0, 0), 1);
				}

			}

		}

		if (logger.isTraceEnabled()) logger.trace("verifiedLines: {}", verifiedLines.size());

		// Reduce the lines to possible corners
		List<Point> possibleCorners = new ArrayList<Point>();

		for (double[] line1 : verifiedLines) {
			for (double[] line2 : verifiedLines) {
				if (line1 == line2) continue;

				// logger.trace("compare {} {}", line1, line2);

				Optional<Point> intersection = computeIntersect(line1, line2);

				if (intersection.isPresent()) possibleCorners.add(intersection.get());

			}
		}

		// Reduce the possible corners to ideal corners
		Point[] idealCorners = new Point[4];
		double[] idealDistances = { toleranceThreshold, toleranceThreshold, toleranceThreshold, toleranceThreshold };

		for (Point pt : possibleCorners) {

			for (int i = 0; i < 4; i++) {
				double distance = euclideanDistance(pt, estimatedPoints[i]);

				if (distance < idealDistances[i]) {
					idealDistances[i] = distance;
					idealCorners[i] = pt;
				}
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("idealDistances {} {} {} {}", idealDistances[0], idealDistances[1], idealDistances[2],
					idealDistances[3]);

			String filename = String.format("calibrate-lines.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, traceMat);
		}

		// Verify that we have the corners we need
		for (Point pt : idealCorners) {
			if (pt == null) return Optional.empty();

			if (logger.isTraceEnabled()) {
				logger.trace("idealCorners {}", pt);
				Core.circle(traceMat, pt, 1, new Scalar(0, 255, 255), -1);
			}

		}

		if (logger.isTraceEnabled()) {
			String filename = String.format("calibrate-lines-with-corners.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, traceMat);
		}

		// Sort them into the correct order
		// 1st-------2nd
		// | |
		// | |
		// | |
		// 3rd-------4th
		idealCorners = sortCorners(idealCorners);

		// build the MatofPoint2f
		MatOfPoint2f sourceCorners = new MatOfPoint2f();
		sourceCorners.alloc(4);

		for (int i = 0; i < 4; i++) {
			sourceCorners.put(i, 0, new double[] { idealCorners[i].x, idealCorners[i].y });
		}

		return Optional.of(sourceCorners);
	}

	// Given 4 corners, use the mass center to arrange the corners into correct
	// order
	private Point[] sortCorners(Point[] corners) {
		Point[] result = new Point[4];

		Point center = new Point(0, 0);
		for (Point corner : corners) {
			center.x += corner.x;
			center.y += corner.y;
		}

		center.x *= (1.0 / corners.length);
		center.y *= (1.0 / corners.length);

		List<Point> top = new ArrayList<Point>();
		List<Point> bot = new ArrayList<Point>();

		for (int i = 0; i < corners.length; i++) {
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

	/*
	 * The one time calculation of the transformations.
	 * 
	 * After this is done, the transformation is just applied
	 */
	private void initializeWarpPerspective(final Mat frame, MatOfPoint2f sourceCorners) {
		MatOfPoint2f destCorners = new MatOfPoint2f();
		destCorners.alloc(4);

		// 1st-------2nd
		// | |
		// | |
		// | |
		// 3rd-------4th
		destCorners.put(0, 0, new double[] { boundsRect.boundingRect().x, boundsRect.boundingRect().y });
		destCorners.put(1, 0, new double[] { boundsRect.boundingRect().x + boundsRect.boundingRect().width,
				boundsRect.boundingRect().y });
		destCorners.put(2, 0, new double[] { boundsRect.boundingRect().x,
				boundsRect.boundingRect().y + boundsRect.boundingRect().height });
		destCorners.put(3, 0, new double[] { boundsRect.boundingRect().x + boundsRect.boundingRect().width,
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

			Mat mat = new Mat();

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

		if (warpInitialized) {

			Mat mat = new Mat();

			Core.transform(imageCorners, mat, perspMat);

			return mat;
		} else {
			logger.warn("warpCorners called when warpInitialized is false - {} {} - {}", perspMat, boundingBox,
					isCalibrated);

			return null;
		}

	}

	private final TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);

	private Optional<MatOfPoint2f> findChessboard(Mat mat) {

		Mat grayImage = new Mat();

		Imgproc.cvtColor(mat, grayImage, Imgproc.COLOR_BGR2GRAY);

		MatOfPoint2f imageCorners = new MatOfPoint2f();

		boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners,
				Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_ADAPTIVE_THRESH);
		if (found) {
			// optimization
			Imgproc.cornerSubPix(grayImage, imageCorners, new Size(5, 5), new Size(-1, -1), term);

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

	private Point findChessBoardSquareCenter(Mat frame, Mat corners, int row, int col) {
		if (row >= PATTERN_HEIGHT - 1 || col >= PATTERN_WIDTH - 1) {
			logger.warn("findChessBoardSquareColor invalid row or col {} {}", row, col);
			return null;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("findChessBoardSquareColor {}", corners.size());

			logger.trace("findChessBoardSquareColor {} {}", (row * PATTERN_WIDTH - 1) + col,
					((row + 1) * PATTERN_WIDTH - 1) + col + 1);
		}

		Point topLeft = new Point(corners.get((row * PATTERN_WIDTH - 1) + col, 0)[0],
				corners.get((row * PATTERN_WIDTH - 1) + col, 0)[1]);
		Point bottomRight = new Point(corners.get(((row + 1) * PATTERN_WIDTH - 1) + col + 1, 0)[0],
				corners.get(((row + 1) * PATTERN_WIDTH - 1) + col + 1, 0)[1]);

		Point result = new Point((topLeft.x + bottomRight.x) / 2, (topLeft.y + bottomRight.y) / 2);

		if (logger.isTraceEnabled()) logger.warn("findChessBoardSquareColor {} {} {}", topLeft, bottomRight, result);

		return result;
	}

	private Point[] matOfPoint2fToPoints(MatOfPoint2f mat) {
		Point[] points = new Point[4];
		points[0] = new Point(mat.get(0, 0)[0], mat.get(0, 0)[1]);
		points[1] = new Point(mat.get(1, 0)[0], mat.get(1, 0)[1]);
		points[2] = new Point(mat.get(2, 0)[0], mat.get(2, 0)[1]);
		points[3] = new Point(mat.get(3, 0)[0], mat.get(3, 0)[1]);

		return points;
	}

	private double euclideanDistance(Point pt1, Point pt2) {
		// if (logger.isTraceEnabled())
		// logger.trace("euclideanDistance {} {} - {}", pt1, pt2,
		// Math.sqrt(Math.pow(pt1.x - pt2.x, 2) + Math.pow(pt1.y - pt2.y, 2)));

		return Math.sqrt(Math.pow(pt1.x - pt2.x, 2) + Math.pow(pt1.y - pt2.y, 2));
	}

	// Given a list of points, a point, and a threshold
	// finds out if point is within euclidean distance of
	// any of the points in the list
	private Boolean nearPoints(Point[] points, Point compPt, double threshold) {
		for (int i = 0; i < points.length; i++) {
			if (euclideanDistance(points[i], compPt) < threshold) {
				logger.trace("nearPoints {} {}", points[i], compPt);
				return true;
			}
		}
		return false;
	}

	// Calculate the intersection of two lines
	// Works even if the lines don't cross
	private Optional<Point> computeIntersect(double[] a, double[] b) {
		double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
		double x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];

		double d = ((double) (x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));

		if (d > 0) {
			Point pt = new Point();
			pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
			pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
			return Optional.of(pt);
		}

		return Optional.empty();
	}

	// Given a rotation matrix, rotates a point
	private Point rotPoint(Mat rot_mat, Point point) {
		Point rp = new Point();
		rp.x = rot_mat.get(0, 0)[0] * point.x + rot_mat.get(0, 1)[0] * point.y + rot_mat.get(0, 2)[0];
		rp.y = rot_mat.get(1, 0)[0] * point.x + rot_mat.get(1, 1)[0] * point.y + rot_mat.get(1, 2)[0];

		return rp;
	}

	private Point massCenterMatOfPoint2f(MatOfPoint2f map) {
		Moments moments = Imgproc.moments(map);
		Point centroid = new Point();
		centroid.x = moments.get_m10() / moments.get_m00();
		centroid.y = moments.get_m01() / moments.get_m00();
		return centroid;
	}

}