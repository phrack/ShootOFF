package com.shootoff.camera.perspective;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import com.shootoff.camera.MockCamera;
import com.shootoff.camera.MockCameraManager;
import com.shootoff.camera.TestAutoCalibration;
import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.ConfigurationException;

import javafx.geometry.BoundingBox;
import javafx.geometry.Dimension2D;

public class TestPerspectiveManager {
	private AutoCalibrationManager acm;

	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();

		acm = new AutoCalibrationManager(new MockCameraManager(), new MockCamera(), false);
	}

	@Test
	public void testOne() throws ConfigurationException {
		assertTrue(PerspectiveManager.isCameraSupported("C270", new Dimension2D(1280, 720)));
		
		PerspectiveManager pm = new PerspectiveManager("C270", new Dimension2D(1280, 720), new BoundingBox(0, 0, 736, 544));

		pm.setCameraFeedSize(1280, 720);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);

		pm.calculateUnknown();

		assertEquals(1753.0, pm.getProjectionWidth(), 1);
		assertEquals(1299.0, pm.getProjectionHeight(), 1);

		Optional<Dimension2D> dims = pm.calculateObjectSize(300, 200, 3406);
		assertTrue(dims.isPresent());
		assertEquals(175.3, dims.get().getWidth(), 1);
		assertEquals(118.2, dims.get().getHeight(), 1);

		dims = pm.calculateObjectSize(300, 200, 3406*2);
		assertTrue(dims.isPresent());
		assertEquals(87.7, dims.get().getWidth(), 1);
		assertEquals(59.1, dims.get().getHeight(), 1);

		dims = pm.calculateObjectSize(300, 200, 3406 / 2);
		assertTrue(dims.isPresent());
		assertEquals(350.7, dims.get().getWidth(), 1);
		assertEquals(236.5, dims.get().getHeight(), 1);

		pm.setShooterDistance(3406 * 2);
		dims = pm.calculateObjectSize(300, 200, 3406);
		assertTrue(dims.isPresent());
		assertEquals(350.7, dims.get().getWidth(), 1);
		assertEquals(236.5, dims.get().getHeight(), 1);

	}

	@Test
	public void testTwo() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager(new BoundingBox(0, 0, 422, 316));

		pm.setCameraParameters(4, 3.125, 2.32);
		pm.setCameraFeedSize(640, 480);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);

		pm.calculateUnknown();

		assertEquals(1753.0, pm.getProjectionWidth(), 1);
		assertEquals(1299.0, pm.getProjectionHeight(), 1);

		Optional<Dimension2D> dims = pm.calculateObjectSize(300, 200, 3406);

		assertTrue(dims.isPresent());
		assertEquals(175.3, dims.get().getWidth(), 1);
		assertEquals(118.2, dims.get().getHeight(), 1);
	}

	@Test
	public void testThree() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager(new BoundingBox(0, 0, 422, 316));

		pm.setProjectionSize(1753, 1299);
		pm.setCameraFeedSize(640, 480);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);

		pm.calculateUnknown();

		assertEquals(4, pm.getFocalLength(), 1);
		assertEquals(3.122, pm.getSensorWidth(), .01);
		assertEquals(2.317, pm.getSensorHeight(), .01);

		Optional<Dimension2D> dims = pm.calculateObjectSize(300, 200, 3406);

		assertTrue(dims.isPresent());
		assertEquals(175.3, dims.get().getWidth(), 1);
		assertEquals(118.2, dims.get().getHeight(), 1);
	}

	@Test
	public void testPaperPixelsCalcParams() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager(new BoundingBox(0, 0, 422, 316), new Dimension2D(640, 480),
				new Dimension2D(67, 53), new Dimension2D(1024, 768));
		
		pm.setCameraDistance(3498);

		pm.setShooterDistance(3498);

		pm.calculateUnknown();

		assertEquals(4, pm.getFocalLength(), 1);
		assertEquals(3.047, pm.getSensorWidth(), .01);
		assertEquals(2.235, pm.getSensorHeight(), .01);

		Optional<Dimension2D> dims = pm.calculateObjectSize(279, 216, pm.getCameraDistance());

		assertTrue(dims.isPresent());
		assertEquals(162.6, dims.get().getWidth(), 1);
		assertEquals(128.9, dims.get().getHeight(), 1);
	}

	@Test
	public void testPaperPattern() throws IOException {
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/perspective/c270_pattern_new.png"));

		Mat matTemp = Camera.bufferedImageToMat(testFrame);
		final Mat mat = new Mat();
		Imgproc.cvtColor(matTemp, mat, Imgproc.COLOR_BGR2GRAY);
		
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		final List<MatOfPoint2f> patternList = new ArrayList<MatOfPoint2f>();
		patternList.add(boardCorners.get());
		
		Optional<Dimension2D> paperDimensions = acm.findPaperPattern(mat,
				patternList);
		
		assertTrue(paperDimensions.isPresent());

		PerspectiveManager pm = new PerspectiveManager("C270", new BoundingBox(329, 35, 701, 545),
				new Dimension2D(1280, 720), paperDimensions.get(), new Dimension2D(1024, 768));

		pm.calculateUnknown();
		
		assertEquals(3504, pm.getCameraDistance());

		pm.setShooterDistance(pm.getCameraDistance());

		Optional<Dimension2D> dims = pm.calculateObjectSize(279, 216, pm.getCameraDistance());

		assertTrue(dims.isPresent());
		assertEquals(169.00, dims.get().getWidth(), 1);
		assertEquals(123.00, dims.get().getHeight(), 1);

		pm.setCameraDistance(-1);

		pm.calculateUnknown();

		assertEquals(3504, pm.getCameraDistance());
		
		pm = new PerspectiveManager(new BoundingBox(329, 35, 701, 545),
				new Dimension2D(1280, 720), paperDimensions.get(), new Dimension2D(1024, 768));
		
		pm.setCameraDistance(3504);
		
		pm.calculateUnknown();
		
		assertEquals(3.58, pm.getSensorWidth(), .05);
		assertEquals(2.02, pm.getSensorHeight(), .05);
	}

	
	@Test
	public void testPaperPatternSmall() throws IOException {
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/perspective/c270_pattern_new_small.png"));

		Mat matTemp = Camera.bufferedImageToMat(testFrame);
		final Mat mat = new Mat();
		Imgproc.cvtColor(matTemp, mat, Imgproc.COLOR_BGR2GRAY);
		
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		final List<MatOfPoint2f> patternList = new ArrayList<MatOfPoint2f>();
		patternList.add(boardCorners.get());
		
		Optional<Dimension2D> paperDimensions = acm.findPaperPattern(mat,
				patternList);
		
		PerspectiveManager pm = new PerspectiveManager("C270", new BoundingBox(0, 0, 698, 544),
				new Dimension2D(1280, 720), paperDimensions.get(), new Dimension2D(1024, 768));

		pm.setCameraDistance(6767);
		
		assertEquals(6767, pm.getCameraDistance());

		pm.setShooterDistance(pm.getCameraDistance());
		
		Optional<Dimension2D> dims = pm.calculateObjectSize(279, 216, pm.getCameraDistance());

		assertTrue(dims.isPresent());
		assertEquals(86.2, dims.get().getWidth(), 1);
		assertEquals(62.24, dims.get().getHeight(), 1);

		pm.setCameraDistance(-1);

		pm.calculateUnknown();

		assertEquals(6927, pm.getCameraDistance());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDesiredDistanceCannotBeZero() {
		PerspectiveManager pm = new PerspectiveManager("C270", new Dimension2D(1280, 720), new BoundingBox(0, 0, 736, 544));

		pm.setCameraFeedSize(1280, 720);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);

		pm.calculateUnknown();
		
		pm.calculateObjectSize(10, 10, 0);
	}
}
