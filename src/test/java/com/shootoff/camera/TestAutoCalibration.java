package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import javafx.geometry.Bounds;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.config.ConfigurationException;
public class TestAutoCalibration {
	private AutoCalibrationManager acm;
	
	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();
		
		acm = new AutoCalibrationManager();
	}
	
	@Test
	public void testCalibrateProjection() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(36, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(419, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(313, calibrationBounds.get().getHeight(), 1.0);

	}
	
	@Test
	public void testCalibrateProjection2() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2.png"));
		
		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());

		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(37, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(418, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(312, calibrationBounds.get().getHeight(), 1.0);

	}
	
	@Test
	public void testCalibrateProjectionCutoff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-cutoff.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertFalse(calibrationBounds.isPresent());
	}
	
	
	@Test
	public void testCalibrateTightPatternUpsidedown() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-upsidedown.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertEquals(false, calibrationBounds.isPresent());

	}

	
	@Test
	public void testCalibrateTightPatternCutOff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-cutoff.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertEquals(false, calibrationBounds.isPresent());

	}
	
	@Test
	public void testCalibrateTightPattern() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertEquals(45, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(25, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(569, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(431, calibrationBounds.get().getHeight(), 1.0);
	}


	@Test
	public void testCalibrateTightPatternTurned() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertEquals(116, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(88, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(422, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(296, calibrationBounds.get().getHeight(), 1.0);
	}
}

