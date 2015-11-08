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
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
public class TestAutoCalibration {
	private AutoCalibrationManager acm;
	
	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();
	
		//Configuration.disableErrorReporting();
		
		acm = new AutoCalibrationManager();
	}
	
	@Test
	public void testCalibrateProjection() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();

		calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 113);

		assertTrue((int)calibrationBounds.get().getMinY() == 36);

		assertTrue((int)calibrationBounds.get().getWidth() == 419);

		assertTrue((int)calibrationBounds.get().getHeight() == 313);

	}
	
	@Test
	public void testCalibrateProjection2() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		

		calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 113);

		assertTrue((int)calibrationBounds.get().getMinY() == 37);

		assertTrue((int)calibrationBounds.get().getWidth() == 418);

		assertTrue((int)calibrationBounds.get().getHeight() == 312);

	}
	
	@Test
	public void testCalibrateProjectionCutoff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-cutoff.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		

		calibrationBounds = acm.processFrame(testFrame);
		
		assertFalse(calibrationBounds.isPresent());
	}
	
	
	@Test
	public void testCalibrateTightPatternUpsidedown() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-upsidedown.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		calibrationBounds = acm.processFrame(testFrame);
		
		assertEquals(false, calibrationBounds.isPresent());

	}

	
	@Test
	public void testCalibrateTightPatternCutOff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-cutoff.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		calibrationBounds = acm.processFrame(testFrame);
		
		assertEquals(false, calibrationBounds.isPresent());

	}
	
	@Test
	public void testCalibrateTightPattern() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 45);

		assertTrue((int)calibrationBounds.get().getMinY() == 25);

		assertTrue((int)calibrationBounds.get().getWidth() == 569);

		assertTrue((int)calibrationBounds.get().getHeight() == 431);
	}


	@Test
	public void testCalibrateTightPatternTurned() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned.png"));


		Optional<Bounds> calibrationBounds = Optional.empty();
		
		calibrationBounds = acm.processFrame(testFrame);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 116);

		assertTrue((int)calibrationBounds.get().getMinY() == 88);

		assertTrue((int)calibrationBounds.get().getWidth() == 422);

		assertTrue((int)calibrationBounds.get().getHeight() == 296);

	}
}

