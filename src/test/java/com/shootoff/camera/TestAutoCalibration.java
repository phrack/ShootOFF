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
	
		Configuration.disableErrorReporting();
		
		acm = new AutoCalibrationManager();
	}
	

	
	@Test
	public void testCalibrateProjection() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 5; i++)
		{
			
			if (!calibrated)
			{
				calibrationBounds = acm.processFrame(testFrame);
				if (calibrationBounds.isPresent())
					calibrated = true;
			}
			
			else if (calibrated)
			{
				calibrated = true;
				
				// Make sure this matches a saved image
				//BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 114);

		assertTrue((int)calibrationBounds.get().getMinY() == 41);

		assertTrue((int)calibrationBounds.get().getWidth() == 413);

		assertTrue((int)calibrationBounds.get().getHeight() == 307);

	}
	
	@Test
	public void testCalibrateProjection2() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 5; i++)
		{
			
			if (!calibrated)
			{
				calibrationBounds = acm.processFrame(testFrame);
				if (calibrationBounds.isPresent())
					calibrated = true;
			}
			
			else if (calibrated)
			{
				calibrated = true;
				
				// Make sure this matches a saved image
				//BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 114);

		assertTrue((int)calibrationBounds.get().getMinY() == 41);

		assertTrue((int)calibrationBounds.get().getWidth() == 413);

		assertTrue((int)calibrationBounds.get().getHeight() == 307);

	}
	
	@Test
	public void testCalibrateProjection3() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-3.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 5; i++)
		{
			
			if (!calibrated)
			{
				calibrationBounds = acm.processFrame(testFrame);
				if (calibrationBounds.isPresent())
					calibrated = true;
			}
			
			else if (calibrated)
			{
				calibrated = true;
				
				// Make sure this matches a saved image
				//BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 114);

		assertTrue((int)calibrationBounds.get().getMinY() == 41);

		assertTrue((int)calibrationBounds.get().getWidth() == 413);

		assertTrue((int)calibrationBounds.get().getHeight() == 307);

	}
	
	
	@Test
	public void testCalibrateTightPattern() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 5; i++)
		{
			
			if (!calibrated)
			{
				calibrationBounds = acm.processFrame(testFrame);
				if (calibrationBounds.isPresent())
					calibrated = true;
			}
			
			else if (calibrated)
			{
				calibrated = true;
				
				// Make sure this matches a saved image
				//BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());

	}


	@Test
	public void testCalibrateTightPatternTurned() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 5; i++)
		{
			
			if (!calibrated)
			{
				calibrationBounds = acm.processFrame(testFrame);
				if (calibrationBounds.isPresent())
					calibrated = true;
			}
			
			else if (calibrated)
			{
				calibrated = true;
				
				// Make sure this matches a saved image
				//BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());

	}
}

