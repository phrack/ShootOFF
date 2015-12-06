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
		
		acm = new AutoCalibrationManager(new MockCameraManager());
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
		
		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
		assertEquals(false, acm.getPerspMat() == null);
		
		double[][] expectedMatrix = { { 1.03, 0.02, -7.76 }, { -0.00, 1.04, -1.67 }, { 0.00, 0.00, 1.00 } };
		
		for (int i = 0; i < acm.getPerspMat().rows(); i++)
		{
			for (int j = 0; j < acm.getPerspMat().cols(); j++)
			{
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i,j)[0], .1);
			}
		}
		
		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
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

		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
		assertEquals(false, acm.getPerspMat() == null);
		
		double[][] expectedMatrix = { { 1.04, 0.03, -12.03 }, { -0.00, 1.04, -1.90 }, { 0.00, 0.00, 1.00 } };
		
		for (int i = 0; i < acm.getPerspMat().rows(); i++)
		{
			for (int j = 0; j < acm.getPerspMat().cols(); j++)
			{
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i,j)[0], .1);
			}
		}
		
		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
		
	}
	
	@Test
	public void testCalibrateProjectionCutoff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
					TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-cutoff.png"));

		Optional<Bounds> calibrationBounds = acm.processFrame(testFrame);
		
		assertEquals(false, calibrationBounds.isPresent());
		
	
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
		
		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
		assertEquals(false, acm.getPerspMat() == null);
		
		double[][] expectedMatrix = { { 1.00, 0.00, -0.08 }, { 0.00, 1.00, -0.12 }, { 0.00, 0.00, 1.00 } };
		
		for (int i = 0; i < acm.getPerspMat().rows(); i++)
		{
			for (int j = 0; j < acm.getPerspMat().cols(); j++)
			{
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i,j)[0], .1);
			}
		}
		
		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
		
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
		
		BufferedImage resultFrame = acm.undistortFrame(testFrame);
		
		assertEquals(false, acm.getPerspMat() == null);
		
		double[][] expectedMatrix = { { 0.88, -0.34, 86.78 }, { 0.24, 0.80, -61.16 }, { -0.00, -0.00, 1.00 } };
		
		for (int i = 0; i < acm.getPerspMat().rows(); i++)
		{
			for (int j = 0; j < acm.getPerspMat().cols(); j++)
			{
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i,j)[0], .1);
			}
		}
		
		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
	}
	
	/* http://stackoverflow.com/questions/11006394/is-there-a-simple-way-to-compare-bufferedimage-instances */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
	    // The images must be the same size.
	    if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
	        int width = imgA.getWidth();
	        int height = imgA.getHeight();

	        // Loop over every pixel.
	        for (int y = 0; y < height; y++) {
	            for (int x = 0; x < width; x++) {
	                // Compare the pixels for equality.
	                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
	                    return false;
	                }
	            }
	        }
	    } else {
	        return false;
	    }

	    return true;
	}
}

