package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.math.geometry.point.Point2dImpl;

import com.shootoff.camera.AutoCalibration.AutoCalibrationManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
public class TestAutoCalibration {
	private Configuration config;
	private AutoCalibrationManager acm;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		
		acm = new AutoCalibrationManager();
	}
	
	@Test
	public void testDark() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/dark.png"));

		List<Point2dImpl> corners = acm.findChessboardBufferedImage(testFrame);
		
		assertTrue(corners != null);
		
		
		Optional<Bounds> bounds = acm.calcBounds(corners);
				
		
	}

}
