package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.ConfigurationException;

import javafx.util.Pair;

public class TestPerspectiveManager {
	
	private AutoCalibrationManager acm;

	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();

		acm = new AutoCalibrationManager(new MockCameraManager(), false);
	}

	@Test
	public void testOne() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setCameraParams(PerspectiveManager.C270_FOCAL_LENGTH, PerspectiveManager.C270_SENSOR_WIDTH, PerspectiveManager.C270_SENSOR_HEIGHT);
		pm.setCameraFeedSize(1280, 720);
		pm.setPatternSize(736, 544);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);
		
		pm.calculateUnknown();
		
		assertEquals(1753.0,pm.getProjectionWidth(), 1);
		assertEquals(1299.0,pm.getProjectionHeight(), 1);
		
		Pair<Double, Double> pair = pm.calculateObjectSize(300, 200, 3406, 3406);
		
		assertEquals(175.3, pair.getKey(), 1);
		assertEquals(118.2, pair.getValue(), 1);

		pair = pm.calculateObjectSize(300, 200, 3406, 3406*2);
		
		assertEquals(87.7, pair.getKey(), 1);
		assertEquals(59.1, pair.getValue(), 1);
		
		
		pair = pm.calculateObjectSize(300, 200, 3406*2, 3406);
		
		assertEquals(350.7, pair.getKey(), 1);
		assertEquals(236.5, pair.getValue(), 1);
		
		pm.setShooterDistance(3406*2);
		pair = pm.calculateObjectSize(300, 200, 3406, 3406);
		assertEquals(87.7, pair.getKey(), 1);
		assertEquals(59.1, pair.getValue(), 1);
		
	
	}
	
	@Test
	public void testTwo() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setCameraParams(4, 3.125, 2.32);
		pm.setCameraFeedSize(640, 480);
		pm.setPatternSize(422, 316);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);
		
		pm.calculateUnknown();
		
		assertEquals(1753.0,pm.getProjectionWidth(), 1);
		assertEquals(1299.0,pm.getProjectionHeight(), 1);
		
		Pair<Double, Double> pair = pm.calculateObjectSize(300, 200, 3406, 3406);
		
		assertEquals(175.3, pair.getKey(), 1);
		assertEquals(118.2, pair.getValue(), 1);
	}
	
	
	@Test
	public void testThree() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setProjectionSize(1753, 1299);
		pm.setCameraFeedSize(640, 480);
		pm.setPatternSize(422, 316);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);
		
		pm.calculateUnknown();
		
		assertEquals(1,pm.getFocalLength(), 1);
		assertEquals(.781, pm.getSensorWidth(), .1);
		assertEquals(.579, pm.getSensorHeight(), .1);
		
		Pair<Double, Double> pair = pm.calculateObjectSize(300, 200, 3406, 3406);
		
		assertEquals(175.3, pair.getKey(), 1);
		assertEquals(118.2, pair.getValue(), 1);
	}
	
	
	
	@Test
	public void testFour() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setCameraFeedSize(640, 480);
		pm.setPatternSize(422, 316);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);
		
		pm.setProjectionSizeFromLetterPaperPixels(67, 53);
		
		pm.calculateUnknown();
		
		assertEquals(1,pm.getFocalLength(), 1);
		assertEquals(.781, pm.getSensorWidth(), .1);
		assertEquals(.579, pm.getSensorHeight(), .1);
		
		Pair<Double, Double> pair = pm.calculateObjectSize(279, 216, pm.getCameraDistance(), pm.getCameraDistance());
		
		assertEquals(162.6, pair.getKey(), 1);
		assertEquals(128.9, pair.getValue(), 1);
	}
	
	
	@Test
	public void testPaperPattern() throws IOException {
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/perspective/c270_pattern_new.png"));

		Optional<Pair<Integer,Integer>> paperDimensions = acm.findPaperPattern(Camera.bufferedImageToMat(testFrame));

		assertTrue(paperDimensions.isPresent());
		
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setCameraParams(PerspectiveManager.C270_FOCAL_LENGTH, PerspectiveManager.C270_SENSOR_WIDTH, PerspectiveManager.C270_SENSOR_HEIGHT);
		
		pm.setCameraFeedSize(1280, 720);
		pm.setPatternSize(698, 544);

		pm.setProjectorResolution(1024, 768);
		
		pm.setProjectionSizeFromLetterPaperPixels(paperDimensions.get().getKey(), paperDimensions.get().getValue());
		
		pm.calculateUnknown();
		
		assertEquals(3498, pm.getCameraDistance());
		
		pm.setShooterDistance(pm.getCameraDistance());
		
		Pair<Double, Double> pair = pm.calculateObjectSize(279, 216, pm.getCameraDistance(), pm.getCameraDistance());
		
		assertEquals(170.25, pair.getKey(), 1);
		assertEquals(124.3, pair.getValue(), 1);
	}

}
