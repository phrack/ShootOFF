package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.geometry.Bounds;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;

import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginEngine;

public class TestAutoCalibration implements VideoFinishedListener {
	private AutoCalibrationManager acm;

	private Configuration config;
	private MockCanvasManager mockCanvasManager;
	private boolean[][] sectorStatuses;
	private MockCamera mockCamera = new MockCamera();
	
	@Rule public ErrorCollector collector = new ErrorCollector();

	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();

		acm = new AutoCalibrationManager(new MockCameraManager(), mockCamera, false);
		

		config = new Configuration(new String[0]);
		config.setDebugMode(false);
		mockCanvasManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[JavaShotDetector.SECTOR_ROWS][JavaShotDetector.SECTOR_COLUMNS];

		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < JavaShotDetector.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}

	Object processingLock = new Object();
	private MockCameraManager autoCalibrationVideo(String videoPath) {

		File videoFile = new File(TestAutoCalibration.class.getResource(videoPath).getFile());

		MockCameraManager cameraManager;
		cameraManager = new MockCameraManager(new MockCamera(videoFile), mockCanvasManager, sectorStatuses,
				Optional.empty(), this);

		mockCanvasManager.setCameraManager(cameraManager);
		
		
		ProjectorArenaPane pac = new ProjectorArenaPane(config, mockCanvasManager);

		cameraManager.setCalibrationManager(
				new CalibrationManager(new CalibrationConfigurator() {
					@Override
					public void toggleCalibrating() {}
					
					@Override
					public CalibrationOption getCalibratedFeedBehavior() {
						return CalibrationOption.ONLY_IN_BOUNDS;
					}
					
					@Override
					public void calibratedFeedBehaviorsChanged() {}
				}, cameraManager, pac, null, new ExerciseListener() {
					@Override
					public void setProjectorExercise(TrainingExercise exercise) {}
					
					@Override
					public void setExercise(TrainingExercise exercise) {}
					
					@Override
					public PluginEngine getPluginEngine() {
						return null;
					}
					
					@Override
					public Configuration getConfiguration() {
						return null;
					}
				}));
		cameraManager.enableAutoCalibration(false);

		cameraManager.setDetecting(false);
		cameraManager.setDetectionLockState(true);
		
		cameraManager.start();
		
		try {
			synchronized (processingLock) {
				processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return cameraManager;
	}

	@Test
	public void testCalibrateProjection() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection.png"));

		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));
		
		final Mat mat = acm.prepTestFrame(testFrame);

		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);

		assertTrue(calibrationBounds.isPresent());

		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(32, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(422, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(316, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
	    //File outputfile = new File("calibrate-projection-result.png");
	    //ImageIO.write(resultFrame, "png", outputfile);

		
		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
	}

	@Test
	public void testCalibrateProjection2() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		final Mat mat = acm.prepTestFrame(testFrame);

		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);


		assertTrue(calibrationBounds.isPresent());

		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(34, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(420, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(316, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
	    //File outputfile = new File("calibrate-projection-2-result.png");
	    //ImageIO.write(resultFrame, "png", outputfile);


		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));

	}

	@Test
	public void testCalibrateProjectionCutoff() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-cutoff.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		final Mat mat = acm.prepTestFrame(testFrame);

		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);

		assertEquals(false, calibrationBounds.isPresent());

	}

	@Test
	public void testCalibrateTightPatternUpsidedown() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO.read(TestAutoCalibration.class
				.getResourceAsStream("/autocalibration/tight-calibration-pattern-upsidedown.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		final Mat mat = acm.prepTestFrame(testFrame);

		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);

		assertTrue(calibrationBounds.isPresent());


	}

	@Test
	public void testCalibrateTightPatternCutOff() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-cutoff.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		final Mat mat = acm.prepTestFrame(testFrame);

		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);

		assertEquals(false, calibrationBounds.isPresent());

	}

	@Test
	public void testCalibrateTightPattern() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		final Mat mat = acm.prepTestFrame(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);

		assertTrue(calibrationBounds.isPresent());

		assertEquals(45, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(25, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(570, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(431, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);

	    //File outputfile = new File("tight-calibration-pattern-result.png");
	    //ImageIO.write(resultFrame, "png", outputfile);


		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));

	}

	@Test
	public void testCalibrateTightPatternTurned() throws IOException {
		BufferedImage testFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		final Mat mat = acm.prepTestFrame(testFrame);

		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), mat);

		assertTrue(calibrationBounds.isPresent());

		assertEquals(137, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(66, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(402, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(280, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
	    //File outputfile = new File("tight-calibration-pattern-turned-result.png");
	    //ImageIO.write(resultFrame, "png", outputfile);
		

		BufferedImage compareFrame = ImageIO.read(TestAutoCalibration.class
				.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
	}

	@Test
	public void testCalibrateHighRes() throws IOException {
		CameraManager result = autoCalibrationVideo("/autocalibration/highres-autocalibration-1280x720.mp4");
		assertEquals(true, result.cameraAutoCalibrated);
	}
	
	@Test
	public void testCalibrateWithPaperPattern() throws IOException {
		MockCameraManager result = autoCalibrationVideo("/autocalibration/calibrate-projection-paper-ifly53e.mp4");
		assertEquals(true, result.cameraAutoCalibrated);
		
		assertEquals(76.84, result.getACM().getPaperDimensions().get().getWidth(), 1);
		assertEquals(56.00, result.getACM().getPaperDimensions().get().getHeight(), 1);

	
	}
	
	
	@Test
	public void testCalibrateWithPaperPattern2() throws IOException {
		MockCameraManager result = autoCalibrationVideo("/autocalibration/calibrate-projection-paper-ifly53e-2.mp4");
		assertEquals(true, result.cameraAutoCalibrated);
		
		assertEquals(76.80, result.getACM().getPaperDimensions().get().getWidth(), 1);
		assertEquals(58.15, result.getACM().getPaperDimensions().get().getHeight(), 1);
		
	}
	
	
	@Test
	public void testCalibrateProjectionPaper() throws IOException {
		acm.reset();
		
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-paper.png"));
		mockCamera.setViewSize(new Dimension(testFrame.getWidth(), testFrame.getHeight()));

		acm.processFrame(Camera.bufferedImageToMat(testFrame), 2000);

		Optional<Bounds> calibrationBounds = Optional.of(acm.getBoundsResult());

		assertTrue(calibrationBounds.isPresent());

		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(37, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(418, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(316, calibrationBounds.get().getHeight(), 1.0);

	}
	
	
	
	
	
	
	/*
	 * http://stackoverflow.com/questions/11006394/is-there-a-simple-way-to-
	 * compare -bufferedimage-instances
	 */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
		// The images must be the same size.
		if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
			int width = imgA.getWidth();
			int height = imgA.getHeight();

			// Loop over every pixel.
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Compare the pixels for equality.
					if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		return true;
	}

	@Override
	public void videoFinished() {
		synchronized (processingLock) {
			processingLock.notifyAll();
		}
	}

}
