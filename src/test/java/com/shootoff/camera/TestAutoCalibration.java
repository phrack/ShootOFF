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
	public void testDark() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/dark.png"));

		//List<Point2dImpl> corners = acm.findChessboardBufferedImage(testFrame);
		
		//assertTrue(corners != null);
		
		
		//Optional<Bounds> bounds = acm.calcBounds(corners);
			
		acm.setFrame(testFrame);
		
		acm.test();
		
	}

}
