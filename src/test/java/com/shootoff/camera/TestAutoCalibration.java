package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.geometry.Bounds;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;

import com.shootoff.camera.AutoCalibration.AutoCalibrationManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
public class TestAutoCalibration {
	private Configuration config;
	private AutoCalibrationManager acm;
	
	@Before
	public void setUp() throws ConfigurationException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		
		config = new Configuration(new String[0]);
		
		acm = new AutoCalibrationManager();
	}
	
	@Test
	public void testCalibratePaper() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-4-rotated.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 7; i++)
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
				
				BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
				
				File outputfile = new File(String.format("undistortFrame-%s.png",i));
				try {
					ImageIO.write(newFrame, "png", outputfile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 40);

		assertTrue((int)calibrationBounds.get().getMinY() == 189);

		assertTrue((int)calibrationBounds.get().getWidth() == 298);

		assertTrue((int)calibrationBounds.get().getHeight() == 207);

	}

	
	@Test
	public void testCalibrateProjection() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection.png"));

		Optional<Bounds> calibrationBounds = Optional.empty();
		
		boolean calibrated = false;
		
		for (int i = 0; i <= 7; i++)
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
				
				BufferedImage newFrame = acm.undistortFrame(testFrame, i);
				
				
				File outputfile = new File(String.format("undistortFrame-%s.png",i));
				try {
					ImageIO.write(newFrame, "png", outputfile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		assertEquals(true, calibrated);
		
		assertTrue(calibrationBounds.isPresent());
		
		assertTrue((int)calibrationBounds.get().getMinX() == 46);

		assertTrue((int)calibrationBounds.get().getMinY() == 194);

		assertTrue((int)calibrationBounds.get().getWidth() == 288);

		assertTrue((int)calibrationBounds.get().getHeight() == 197);

	}
	
}
