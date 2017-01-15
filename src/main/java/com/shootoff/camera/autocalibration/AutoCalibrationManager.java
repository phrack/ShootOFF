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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.photo.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraCalibrationListener;
import com.shootoff.camera.Frame;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.config.Configuration;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;

public class AutoCalibrationManager {
	private static final Logger logger = LoggerFactory.getLogger(AutoCalibrationManager.class);

	private static final int PATTERN_WIDTH = 9;
	private static final int PATTERN_HEIGHT = 6;

	// These are slightly fudged
	// 11/10.65 = 1.032. With 1/4" margins this should be 11/10.5, but OpenCV
	// reads the pattern a bit big
	private static final double PAPER_MARGIN_WIDTH = 1.032;
	// 8.5/8.25 = 1.030.
	private static final double PAPER_MARGIN_HEIGHT = 1.03;
	private static final Size boardSize = new Size(PATTERN_WIDTH, PATTERN_HEIGHT);

	private final CameraCalibrationListener calibrationListener;

	private final Camera camera;

	// Stores the transformation matrix
	private Mat perspMat = null;

	// Stores the bounding box we'll pass back to CameraManager
	private Bounds boundingBox = null;

	private RotatedRect boundsRect;

	private boolean warpInitialized = false;
	private boolean isCalibrated = false;

	// Edge is 11 pixels wide. Squares are 168 pixels wide.
	// 11/168 = 0.06547619047619047619047619047619
	// Maybe I should have made it divisible...
	private static final double BORDER_FACTOR = 0.065476;

	private final TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 60, 0.0001);

	/* Paper Pattern */

	public Optional<Dimension2D> getPaperDimensions() {
		return ((StepFindPaperPattern) stepFindPaperPattern).paperDimensions;
	}

	protected AutoCalStep stepFindBounds = null;
	protected AutoCalStep stepFindDelay = null;
	protected AutoCalStep stepFindPaperPattern = null;
	protected AutoCalStep stepAdjustExposure = null;
	List<AutoCalStep> steps = new ArrayList<>();

	public AutoCalibrationManager(final CameraCalibrationListener calibrationListener, final Camera camera,
			final boolean calculateFrameDelay) {
		this.camera = camera;
		this.calibrationListener = calibrationListener;

		stepFindBounds = new StepFindBounds();
		stepFindDelay = new StepFindDelay(calculateFrameDelay);
		stepFindPaperPattern = new StepFindPaperPattern();
		stepAdjustExposure = new StepAdjustExposure();

		steps.add(stepFindBounds);
		steps.add(stepFindDelay);
		steps.add(stepFindPaperPattern);
		steps.add(stepAdjustExposure);
	}

	public Mat getPerspMat() {
		return perspMat;
	}

	public Bounds getBoundsResult() {
		if (((StepFindBounds) stepFindBounds).boundsResult == null)
			logger.error("getBoundsResult called when boundsResult==null, isCalibrated {}", isCalibrated);

		return ((StepFindBounds) stepFindBounds).boundsResult;
	}

	public void reset() {
		isCalibrated = false;
		warpInitialized = false;
		boundsRect = null;
		boundingBox = null;
		perspMat = null;
		for (final AutoCalStep step : steps)
			if (step.enabled()) step.reset();
	}

	public Mat preProcessFrame(final Mat mat) {
		if (mat.channels() == 1) return mat.clone();

		final Mat newMat = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);

		Imgproc.cvtColor(mat, newMat, Imgproc.COLOR_BGR2GRAY);

		if (logger.isTraceEnabled()) {
			String filename = String.format("grayscale.png");
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, newMat);
		}

		return newMat;
	}

	private boolean isFinished() {
		for (final AutoCalStep step : steps)
			if (step.enabled() && !step.completed()) return false;
		return true;
	}

	public void processFrame(final Frame frame) {
		final Mat grayMat = preProcessFrame(frame.getOriginalMat());
		for (final AutoCalStep step : steps) {
			if (step.enabled() && !step.completed()) {
				step.process(new Frame(grayMat, frame.getTimestamp()));
				break;
			}
		}
		if (isFinished()) calibrationListener.calibrate(((StepFindBounds) stepFindBounds).boundsResult,
				((StepFindPaperPattern) stepFindPaperPattern).paperDimensions, false,
				((StepFindDelay) stepFindDelay).frameDelayResult);

	}

	// FOR TESTS ONLY
	public Mat prepTestFrame(BufferedImage frame) {
		final Mat mat = preProcessFrame(Camera.bufferedImageToMat(frame));
		Imgproc.equalizeHist(mat, mat);
		return mat;
	}

	interface AutoCalStep {
		void reset();

		boolean enabled();

		boolean completed();

		void process(Frame frame);
	}

	class StepFindBounds implements AutoCalStep {
		public Bounds boundsResult = null;
		private static final long minimumInterval = 250;
		private long lastFrameCheck = 0;

		@Override
		public void reset() {
			boundsResult = null;
			lastFrameCheck = 0;
		}

		@Override
		public boolean enabled() {
			return true;
		}

		@Override
		public boolean completed() {
			return !(boundsResult == null);
		}

		@Override
		public void process(Frame frame) {
			if (frame.getTimestamp() - lastFrameCheck < minimumInterval) return;

			lastFrameCheck = frame.getTimestamp();

			Imgproc.equalizeHist(frame.getOriginalMat(), frame.getOriginalMat());

			final List<MatOfPoint2f> listPatterns = findPatterns(frame.getOriginalMat(), true);

			if (listPatterns.isEmpty()) return;

			final Optional<Dimension2D> paperRes = findPaperPattern(frame.getOriginalMat(), listPatterns);
			if (paperRes.isPresent())
				((StepFindPaperPattern) stepFindPaperPattern).addPaperDimensions(paperRes.get(), true);

			if (listPatterns.isEmpty()) return;

			// Technically there could still be more than one pattern
			// or even a pattern that is much too small
			// But damn if we're gonna fix every problem the user gives us
			final Optional<Bounds> bounds = calibrateFrame(listPatterns.get(0), frame.getOriginalMat());

			if (bounds.isPresent()) {
				boundsResult = bounds.get();

			} else {
				boundsResult = null;
			}
		}
	}

	class StepFindDelay implements AutoCalStep {
		private long frameTimestampBeforeFrameChange = -1;
		private boolean calculateFrameDelay = false;
		public long frameDelayResult = -1;
		private double patternLuminosity = -1;

		public StepFindDelay(boolean calculateFrameDelay) {
			this.calculateFrameDelay = calculateFrameDelay;
		}

		@Override
		public void reset() {
			patternLuminosity = 0;
			frameDelayResult = -1;
			frameTimestampBeforeFrameChange = -1;
		}

		@Override
		public boolean enabled() {
			return calculateFrameDelay;
		}

		@Override
		public boolean completed() {
			return frameDelayResult > -1;
		}

		private boolean inStepTwo() {
			// We're in step two if frameTimestampBeforeFrameChange > -1
			// AND step two is not complete
			return !(frameTimestampBeforeFrameChange == -1 || completed());
		}

		@Override
		public void process(Frame frame) {
			if (!inStepTwo()) {
				calibrationListener.setArenaBackground(null);

				checkForFrameChange(frame);
				frameTimestampBeforeFrameChange = frame.getTimestamp();
			} else {
				final Optional<Long> frameDelay = checkForFrameChange(frame);

				if (frameDelay.isPresent()) {
					frameDelayResult = frameDelay.get();

					logger.debug("Step Two: frameDelayResult {}", frameDelayResult);

					calibrationListener.setArenaBackground(null);

				}
			}
		}

		private Optional<Long> checkForFrameChange(Frame frame) {
			frame = undistortFrame(frame);

			final double pixel = getFrameDelayPixel(frame.getOriginalMat());

			// Initialize
			if (patternLuminosity == 0) {
				patternLuminosity = pixel;
				return Optional.empty();
			}

			final long change = frame.getTimestamp() - frameTimestampBeforeFrameChange;

			logger.debug("{} {} {}", pixel, patternLuminosity, change);

			if (pixel < .9 * patternLuminosity) {
				return Optional.of(change);
			} else if (change > 250) {
				return Optional.of(-1L);
			}

			return Optional.empty();
		}

		private double getFrameDelayPixel(Mat mat) {
			final double squareHeight = getBoundsResult().getHeight() / (PATTERN_HEIGHT + 1);
			final double squareWidth = getBoundsResult().getWidth() / (PATTERN_WIDTH + 1);

			final int secondSquareCenterX = (int) (getBoundsResult().getMinX() + (squareWidth * 1.5));
			final int secondSquareCenterY = (int) (getBoundsResult().getMinY() + (squareHeight * .5));

			return mat.get(secondSquareCenterY, secondSquareCenterX)[0];
		}

	}

	class StepFindPaperPattern implements AutoCalStep {
		public Optional<Dimension2D> paperDimensions = Optional.empty();
		private int stepThreeAttempts = 0;
		private final static int STEP_THREE_MAX_ATTEMPTS = 3;

		@Override
		public void reset() {
			paperDimensions = Optional.empty();
			stepThreeAttempts = 0;
		}

		@Override
		public boolean completed() {
			return (stepThreeAttempts >= STEP_THREE_MAX_ATTEMPTS);
		}

		@Override
		public boolean enabled() {
			return true;
		}

		@Override
		public void process(Frame frame) {
			stepThreeAttempts++;

			calibrationListener.setArenaBackground(null);

			frame = undistortFrame(frame);

			final List<MatOfPoint2f> listPatterns = findPatterns(frame.getOriginalMat(), true);

			if (listPatterns.isEmpty()) return;

			final Optional<Dimension2D> paperRes = findPaperPattern(frame.getOriginalMat(), listPatterns);

			if (paperRes.isPresent()) {
				addPaperDimensions(paperRes.get(), false);

				stepThreeAttempts = STEP_THREE_MAX_ATTEMPTS;
			} else {
				stepThreeAttempts++;
			}

			logger.trace("stepThree {}", completed());

		}

		public void addPaperDimensions(Dimension2D newPaperDimensions, boolean averagePatterns) {
			if (!paperDimensions.isPresent() || !averagePatterns) {
				paperDimensions = Optional.of(newPaperDimensions);

				logger.trace("Found paper dimensions {}", paperDimensions.get());
			} else if (paperDimensions.isPresent()) {
				paperDimensions = Optional.of(averageDimensions(paperDimensions.get(), newPaperDimensions));
				logger.trace("Averaged paper dimensions {}", paperDimensions.get());
			}
		}

	}

	class StepAdjustExposure implements AutoCalStep {
		private static final int TARGET_THRESH = 80;
		private static final int SAMPLE_DELAY = 100;
		private static final int NUM_TRIES = 6;
		private boolean completed = false;
		private int tries = 0;
		private boolean patternSet = false;
		private long lastSample = 0;
		private double origMean = 0;

		@Override
		public void reset() {
			completed = false;
			patternSet = false;
			lastSample = 0;
			origMean = 0;
			tries = 0;
		}

		@Override
		public boolean completed() {
			return completed;
		}

		@Override
		public boolean enabled() {
			return Configuration.getConfig().autoAdjustExposure() && camera.supportsExposureAdjustment();
		}

		@Override
		public void process(Frame frame) {
			if (!patternSet) {
				calibrationListener.setArenaBackground("white.png");
				patternSet = true;
				lastSample = System.currentTimeMillis();
				return;
			}

			if (completed || (System.currentTimeMillis() - lastSample) < SAMPLE_DELAY) return;

			final Scalar mean = Core.mean(frame.getOriginalMat());
			if (origMean == 0) origMean = mean.val[0];

			logger.trace("{} {}", mean.val[0], TARGET_THRESH);

			if (mean.val[0] > TARGET_THRESH) {
				if (!camera.decreaseExposure()) completed = true;
			} else {
				completed = true;
			}

			if (logger.isTraceEnabled()) {
				String filename = String.format("exposure-%d.png", lastSample);
				final File file = new File(filename);
				filename = file.toString();
				Highgui.imwrite(filename, frame.getOriginalMat());
			}

			tries++;
			if (tries == NUM_TRIES) completed = true;

			if (completed) {
				if (mean.val[0] > origMean * .95 || mean.val[0] < .6 * TARGET_THRESH) {
					camera.resetExposure();
					logger.info("Failed to adjust exposure, mean originally {} lowest {}", origMean, mean.val[0]);
				} else {
					logger.info("Exposure lowered to {} mean from {}", mean.val[0], origMean);
				}
			}

			lastSample = System.currentTimeMillis();
		}

	}

	private List<MatOfPoint2f> findPatterns(Mat mat, boolean findMultiple) {
		final List<MatOfPoint2f> patternList = new ArrayList<>();

		int count = 0;
		while (true) {
			final Optional<MatOfPoint2f> boardCorners = findChessboard(mat);

			if (boardCorners.isPresent()) {
				patternList.add(boardCorners.get());

				if (!findMultiple) break;

				final RotatedRect rect = getPatternDimensions(boardCorners.get());

				blankRotatedRect(mat, rect);

				if (logger.isTraceEnabled()) {
					String filename = String.format("blanked-box-%d.png", count);
					final File file = new File(filename);
					filename = file.toString();
					Highgui.imwrite(filename, mat);

				}

				// Shortcut to not try to find three+ patterns
				// We never should see more than two but maybe that'll change
				// in the future
				findMultiple = false;

			} else {
				break;
			}
			count++;
		}
		return patternList;
	}

	private Dimension2D averageDimensions(Dimension2D d2d1, Dimension2D d2d2) {
		return new Dimension2D((d2d1.getWidth() + d2d2.getWidth()) / 2, (d2d1.getHeight() + d2d2.getHeight()) / 2);
	}

	public Optional<Bounds> calibrateFrame(MatOfPoint2f boardCorners, Mat mat) {

		// For debugging
		Mat traceMat = null;
		if (logger.isTraceEnabled()) {
			if (mat.channels() == 3)
				traceMat = mat.clone();
			else {
				traceMat = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC3);
				Imgproc.cvtColor(mat, traceMat, Imgproc.COLOR_GRAY2BGR);
			}
		}

		// Turn the chessboard into corners
		final MatOfPoint2f boardRect = calcBoardRectFromCorners(boardCorners);

		// Estimate the pattern corners
		final Optional<MatOfPoint2f> estimatedPatternRect = estimatePatternRect(traceMat, boardRect);
		
		if (!estimatedPatternRect.isPresent())
			return Optional.empty();

		// More definitively find corners using goodFeaturesToTrack
		final Optional<Point[]> corners = findCorners(boardRect, mat, estimatedPatternRect.get());

		if (!corners.isPresent()) return Optional.empty();

		// Creates sorted cornerArray for warp perspective
		final MatOfPoint2f corners2f = sortPointsForWarpPerspective(boardRect, corners.get());

		if (logger.isTraceEnabled()) {
			String filename = String.format("calibrate-dist.png");
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, traceMat);
		}

		// Initialize the warp matrix and bounding box
		initializeWarpPerspective(mat, corners2f);

		if (boundingBox.getMinX() < 0 || boundingBox.getMinY() < 0
				|| boundingBox.getWidth() > camera.getViewSize().getWidth()
				|| boundingBox.getHeight() > camera.getViewSize().getHeight()) {
			return Optional.empty();
		}

		if (logger.isTraceEnabled()) logger.trace("bounds {} {} {} {}", boundingBox.getMinX(), boundingBox.getMinY(),
				boundingBox.getWidth(), boundingBox.getHeight());

		if (logger.isTraceEnabled()) {
			final Mat undistorted = warpPerspective(mat);

			String filename = String.format("calibrate-undist.png");
			File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, undistorted);

			final Mat undistortedCropped = undistorted.submat((int) boundingBox.getMinY(), (int) boundingBox.getMaxY(),
					(int) boundingBox.getMinX(), (int) boundingBox.getMaxX());

			filename = String.format("calibrate-undist-cropped.png");
			file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, undistortedCropped);
		}

		isCalibrated = true;

		// Mat warpedBoardCorners = warpCorners(boardCorners);

		if (stepFindDelay.enabled()) {
			final Mat undistorted = warpPerspective(mat);

			// findColors(undistorted, warpedBoardCorners);

			final double squareHeight = boundingBox.getHeight() / (PATTERN_HEIGHT + 1);
			final double squareWidth = boundingBox.getWidth() / (PATTERN_WIDTH + 1);

			final int secondSquareCenterX = (int) (boundingBox.getMinX() + (squareWidth * 1.5));
			final int secondSquareCenterY = (int) (boundingBox.getMinY() + (squareHeight * .5));

			if (logger.isDebugEnabled()) logger.debug("pF getFrameDelayPixel x {} y {} p {}", secondSquareCenterX,
					secondSquareCenterY, undistorted.get(secondSquareCenterY, secondSquareCenterX));

		}

		return Optional.of(boundingBox);
	}

	private MatOfPoint2f sortPointsForWarpPerspective(final MatOfPoint2f boardRect, final Point[] corners) {
		final Point[] cornerArray = new Point[4];
		final Double[] cornerED = new Double[4];
		final Point[] boardRectArray = boardRect.toArray();
		for (int i = 0; i < 4; i++)
			cornerED[i] = -1.0;
		for (final Point cpt : corners) {
			for (int i = 0; i < 4; i++) {

				final double tempED = euclideanDistance(cpt, boardRectArray[i]);
				if (cornerED[i] == -1.0 || tempED < cornerED[i]) {
					cornerArray[i] = cpt;
					cornerED[i] = tempED;
				}
			}
		}

		final MatOfPoint2f corners2f = new MatOfPoint2f();
		corners2f.fromArray(cornerArray);
		return corners2f;
	}

	private Optional<Point[]> findCorners(MatOfPoint2f boardRect, Mat mat, MatOfPoint2f estimatedPatternRect) {

		final Point[] cornerArray = new Point[4];

		Mat mask;

		final Point[] estimatedPoints = estimatedPatternRect.toArray();

		// Establishes a search region
		final long region = mat.total() / 19200;

		final Mat denoisedMat = new Mat(mat.size(), CvType.CV_8UC1);
		Photo.fastNlMeansDenoising(mat, denoisedMat, 21f, 7, 21);

		Imgproc.GaussianBlur(denoisedMat, mat, new Size(0, 0), 10);
		Core.addWeighted(denoisedMat, 1.5, mat, -0.5, 0, mat);

		Mat tempMat = null;
		if (logger.isTraceEnabled()) {
			tempMat = new Mat(mat.size(), CvType.CV_8UC3);
			Imgproc.cvtColor(mat, tempMat, Imgproc.COLOR_GRAY2BGR);
		}

		int i = 0;
		for (final Point pt : estimatedPoints) {
			final MatOfPoint tempCorners = new MatOfPoint();

			mask = Mat.zeros(mat.size(), CvType.CV_8UC1);

			final Point leftpt = new Point(pt.x - region, pt.y - region);
			final Point rightpt = new Point(pt.x + region, pt.y + region);

			Core.rectangle(mask, leftpt, rightpt, new Scalar(255), -1);

			if (logger.isTraceEnabled()) {
				String filename = String.format("mask-%d.png", i);
				final File file = new File(filename);
				filename = file.toString();
				Highgui.imwrite(filename, mask);
			}

			Imgproc.goodFeaturesToTrack(mat, tempCorners, 2, .10, 0, mask, 3, true, .04);

			if (tempCorners.empty()) return Optional.empty();

			Point res = null;
			long dist = mat.total();

			for (final Point p : tempCorners.toArray()) {
				final long tempDist = (long) (Math.min(mat.width() - p.x, p.x) + Math.min(mat.height() - p.y, p.y));
				if (tempDist < dist) {
					dist = tempDist;
					res = p;
				}

				if (logger.isTraceEnabled()) {
					logger.trace("corner {} {}", p.x, p.y);
					Core.circle(tempMat, p, 1, new Scalar(0, 0, 255), -1);
				}
			}

			cornerArray[i] = res;

			i++;
		}

		if (logger.isTraceEnabled()) {

			String filename = String.format("corners.png");
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, tempMat);

		}
		return Optional.of(cornerArray);
	}

	/**
	 * Perspective pattern discovery
	 * 
	 * Works similar to arena calibration but does not try to identify the
	 * outline of the projection area We are only concerned with size, not
	 * alignment or angle
	 * 
	 */
	public Optional<Dimension2D> findPaperPattern(Mat mat, List<MatOfPoint2f> patternList) {

		MatOfPoint2f boardCorners = null;
		int index = 0;
		Optional<Dimension2D> result = Optional.empty();

		for (; index < patternList.size(); index++) {
			boardCorners = patternList.get(index);

			final RotatedRect rect = getPatternDimensions(boardCorners);

			// OpenCV gives us the checkerboard corners, not the outside
			// dimension
			// So this estimates where the outside corner would be, plus a fudge
			// factor for the edge of the paper
			// Printer margins are usually a quarter inch on each edge
			double rect_width = rect.size.width, rect_height = rect.size.height;
			double width = rect_width, height = rect_height;

			// Flip them if its sideways
			if (height > width) {
				width = rect_height;
				height = rect_width;
				rect_height = width;
				rect_width = height;
			}

			width = (width * ((double) (PATTERN_WIDTH + 1) / (double) (PATTERN_WIDTH - 1)) * PAPER_MARGIN_WIDTH);
			height = (height * ((double) (PATTERN_HEIGHT + 1) / (double) (PATTERN_HEIGHT - 1)) * PAPER_MARGIN_HEIGHT);

			final double PAPER_PATTERN_SIZE_THRESHOLD = .25;
			if (width > PAPER_PATTERN_SIZE_THRESHOLD * mat.cols()
					|| height > PAPER_PATTERN_SIZE_THRESHOLD * mat.rows()) {
				continue;
			}

			if (logger.isTraceEnabled()) {
				logger.trace("pattern width {} height {}", rect_width, rect_height);

				logger.trace("paper width {} height {}", width, height);

			}

			final Dimension2D newPaperDimensions = new Dimension2D(width, height);

			result = Optional.of(newPaperDimensions);

			break;
		}

		if (result.isPresent()) {
			logger.trace("Removing paper pattern from patternList (index {})", index);
			patternList.remove(index);
		}

		return result;

	}

	// What a stupid function, can't be the best way
	private void blankRotatedRect(Mat mat, final RotatedRect rect) {
		final Mat tempMat = Mat.zeros(mat.size(), CvType.CV_8UC1);

		final Point points[] = new Point[4];
		rect.points(points);
		for (int i = 0; i < 4; ++i) {
			Core.line(tempMat, points[i], points[(i + 1) % 4], new Scalar(255, 255, 255));
		}

		final Mat tempMask = Mat.zeros((mat.rows() + 2), (mat.cols() + 2), CvType.CV_8UC1);
		Imgproc.floodFill(tempMat, tempMask, rect.center, new Scalar(255, 255, 255), null, new Scalar(0, 0, 0),
				new Scalar(254, 254, 254), 4);

		if (logger.isTraceEnabled()) {
			String filename = String.format("poly.png");
			final File file = new File(filename);
			filename = file.toString();
			Highgui.imwrite(filename, tempMat);
		}

		mat.setTo(new Scalar(0, 0, 0), tempMat);
	}

	private RotatedRect getPatternDimensions(MatOfPoint2f boardCorners) {
		final MatOfPoint2f boardRect2f = calcBoardRectFromCorners(boardCorners);

		return Imgproc.minAreaRect(boardRect2f);

	}

	public Frame undistortFrame(Frame frame) {
		if (isCalibrated) {
			frame.setMat(warpPerspective(frame.getOriginalMat()));
		} else {
			logger.warn("undistortFrame called when isCalibrated is false");
		}

		return frame;
	}

	// Used in tests
	public BufferedImage undistortFrame(BufferedImage bimg) {
		if (!isCalibrated) {
			logger.warn("undistortFrame called when isCalibrated is false");
			return bimg;
		}

		return Camera.matToBufferedImage(warpPerspective(Camera.bufferedImageToMat(bimg)));
	}

	// MUST BE IN BGR pixel format
	public Mat undistortFrame(Mat mat) {
		if (!isCalibrated) {
			logger.warn("undistortFrame called when isCalibrated is false");
			return mat;
		}

		return warpPerspective(mat);
	}

	/*
	 * Make an estimate of a undistorted, unrotated rectangle
	 * Returns: Optional<>, MatOfPoint2f of rectangle, or empty if cannot be completed in bounds
	 * 
	 */
	private Optional<MatOfPoint2f> estimatePatternRect(Mat traceMat, MatOfPoint2f boardRect) {

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

		
		if (boundsRect.boundingRect().x < 0 || boundsRect.boundingRect().y < 0 ||
			boundsRect.boundingRect().y+boundsRect.boundingRect().height >= this.camera.getViewSize().getHeight() ||
			boundsRect.boundingRect().x+boundsRect.boundingRect().width >= this.camera.getViewSize().getWidth())
		{
			logger.debug("Pattern found but autocalibration failed--Cannot dedistort within camera bounds, make sure the projector area is a perfect rectangle and try again");
			return Optional.empty();
		}

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

		return Optional.of(rotatedPatternSizeRect);
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

			final double angle = Math.atan((topRight.y - topLeft.y) / (topRight.x - topLeft.x)) * 180 / Math.PI;
			final double angle2 = Math.atan((bottomRight.y - bottomLeft.y) / (bottomRight.x - bottomLeft.x)) * 180
					/ Math.PI;

			logger.trace("square size {} {} - angle {}", topWidth / (PATTERN_WIDTH - 1),
					leftHeight / (PATTERN_HEIGHT - 1), angle);
			logger.trace("square size {} {} - angle {}", bottomWidth / (PATTERN_WIDTH - 1),
					rightHeight / (PATTERN_HEIGHT - 1), angle2);
		}

		// Estimate the square widths, that is what we base the estimate of the
		// real corners on

		final double squareTopWidth = (1 + BORDER_FACTOR) * (topWidth / (PATTERN_WIDTH - 1));
		final double squareLeftHeight = (1 + BORDER_FACTOR) * (leftHeight / (PATTERN_HEIGHT - 1));
		final double squareBottomWidth = (1 + BORDER_FACTOR) * (bottomWidth / (PATTERN_WIDTH - 1));
		final double squareRightHeight = (1 + BORDER_FACTOR) * (rightHeight / (PATTERN_HEIGHT - 1));

		// The estimations
		final double[] newTopLeft = { topLeft.x - squareTopWidth, topLeft.y - squareLeftHeight };
		final double[] newBottomLeft = { bottomLeft.x - squareBottomWidth, bottomLeft.y + squareLeftHeight };
		final double[] newTopRight = { topRight.x + squareTopWidth, topRight.y - squareRightHeight };
		final double[] newBottomRight = { bottomRight.x + squareBottomWidth, bottomRight.y + squareRightHeight };

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

		if (logger.isTraceEnabled()) {
			logger.trace("initializeWarpPerspective src corners {} {} {} {}", sourceCorners.get(0, 0),
					sourceCorners.get(1, 0), sourceCorners.get(2, 0), sourceCorners.get(3, 0));
			logger.trace("initializeWarpPerspective dest corners {} {} {} {}", destCorners.get(0, 0),
					destCorners.get(1, 0), destCorners.get(2, 0), destCorners.get(3, 0));
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
			Mat debugFrame = null;
			if (frame.channels() == 3)
				debugFrame = frame.clone();
			else {
				debugFrame = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC3);
				Imgproc.cvtColor(frame, debugFrame, Imgproc.COLOR_GRAY2BGR);
			}

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
			final File file = new File(filename);
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

	public Optional<MatOfPoint2f> findChessboard(Mat mat) {

		final MatOfPoint2f imageCorners = new MatOfPoint2f();

		final boolean found = Calib3d.findChessboardCorners(mat, boardSize, imageCorners,
				Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_NORMALIZE_IMAGE);

		if (logger.isTraceEnabled()) logger.trace("found chessboard corners {}", found);

		if (found) {
			// optimization
			Imgproc.cornerSubPix(mat, imageCorners, new Size(1, 1), new Size(-1, -1), term);

			return Optional.of(imageCorners);
		}
		return Optional.empty();
	}

	// converts the chessboard corners into a quadrilateral
	private MatOfPoint2f calcBoardRectFromCorners(MatOfPoint2f corners) {
		final MatOfPoint2f result = new MatOfPoint2f();
		result.alloc(4);

		final Point topLeft = new Point(corners.get(0, 0)[0], corners.get(0, 0)[1]);
		final Point topRight = new Point(corners.get(PATTERN_WIDTH - 1, 0)[0], corners.get(PATTERN_WIDTH - 1, 0)[1]);
		final Point bottomRight = new Point(corners.get(PATTERN_WIDTH * PATTERN_HEIGHT - 1, 0)[0],
				corners.get(PATTERN_WIDTH * PATTERN_HEIGHT - 1, 0)[1]);
		final Point bottomLeft = new Point(corners.get(PATTERN_WIDTH * (PATTERN_HEIGHT - 1), 0)[0],
				corners.get(PATTERN_WIDTH * (PATTERN_HEIGHT - 1), 0)[1]);

		final Point[] unsorted = { topLeft, topRight, bottomLeft, bottomRight };
		final Point[] sorted = sortCorners(unsorted);

		result.fromArray(sorted);

		// result.put(0, 0, topLeft.x, topLeft.y, topRight.x, topRight.y,
		// bottomRight.x, bottomRight.y, bottomLeft.x,
		// bottomLeft.y);

		return result;
	}

	// Given 4 corners, use the mass center to arrange the corners into correct
	// order

	// 1st-------2nd
	// | |
	// | |
	// | |
	// 4th-------3rd
	private Point[] sortCorners(Point[] corners) {
		final Point[] result = new Point[4];

		final Point center = new Point(0, 0);
		for (final Point corner : corners) {
			center.x += corner.x;
			center.y += corner.y;
		}

		center.x *= (1.0 / corners.length);
		center.y *= (1.0 / corners.length);

		final List<Point> top = new ArrayList<>();
		final List<Point> bot = new ArrayList<>();

		for (Point corner : corners) {
			if (corner.y < center.y)
				top.add(corner);
			else
				bot.add(corner);
		}

		result[0] = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
		result[1] = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
		result[2] = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);
		result[3] = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);

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

	public java.awt.Point undistortCoords(int x, int y) {
		if (!warpInitialized) return new java.awt.Point(x, y);

		final MatOfPoint2f point = new MatOfPoint2f();
		point.alloc(1);
		point.put(0, 0, new double[] { x, y });

		Core.perspectiveTransform(point, point, perspMat);

		return new java.awt.Point((int) point.get(0, 0)[0], (int) point.get(0, 0)[1]);
	}
}