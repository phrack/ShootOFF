package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.IOException;

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
	public void testCalibrate() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-4-rotated.png"));

		for (int i = 0; i < 6; i++)
			acm.processFrame(testFrame);

		// calibrate-4 should equal calibrate-undist-5
	}

}
