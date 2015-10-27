package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import javafx.geometry.Bounds;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;

import com.shootoff.camera.AutoCalibration.AutoCalibrationManager;
//import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
public class TestAutoCalibration {
	//private Configuration config;
	private AutoCalibrationManager acm;
	
	@Before
	public void setUp() throws ConfigurationException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		
		//config = new Configuration(new String[0]);
		
		acm = new AutoCalibrationManager();
	}
	
	@Test
	public void testCalibratePaper() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-4-rotated.png"));

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
		
		assertTrue((int)calibrationBounds.get().getMinX() == 36);

		assertTrue((int)calibrationBounds.get().getMinY() == 184);

		assertTrue((int)calibrationBounds.get().getWidth() == 307);

		assertTrue((int)calibrationBounds.get().getHeight() == 216);

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
	public void testCalibratePattern() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/pattern_with_line.png"));

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

