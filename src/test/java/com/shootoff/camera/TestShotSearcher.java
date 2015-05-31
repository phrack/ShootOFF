package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javafx.scene.paint.Color;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestShotSearcher {
	private Configuration config;
	private MockCanvasManager mockManager;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		mockManager = new MockCanvasManager(config);
	}
	
	private List<Shot> findShots(String imagePath) throws IOException, InterruptedException {
		BufferedImage testFrame = ImageIO.read(
				getClass().getResourceAsStream(imagePath));
				
		BufferedImage grayScale = new BufferedImage(testFrame.getWidth(),
				testFrame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		grayScale.createGraphics().drawImage(testFrame, 0, 0, null);
		
		BufferedImage threshed = CameraManager.threshold(config, grayScale);
		
		Thread searcher = new Thread(
				new ShotSearcher(config, mockManager, testFrame, CameraManager.getFrameCount(threshed)));
		searcher.start();
		searcher.join();
		
		return mockManager.getShots();
	}
	
	@Test
	public void testNoInterenceNoShot() throws IOException, InterruptedException {		
		List<Shot> shots = findShots("/shotsearcher/no_interference_no_shot.png");;	
		
		assertEquals(0, shots.size());
	}

	@Test
	public void testNoInterenceOneShotIgnoreRed() throws IOException, InterruptedException {
		config.setIgnoreLaserColor(true);
		config.setIgnoreLaserColorName("red");
		List<Shot> shots = findShots("/shotsearcher/no_interference_one_shot.png");	
		
		assertEquals(0, shots.size());
	}
	
	@Test
	public void testNoInterenceOneShotIgnoreGreen() throws IOException, InterruptedException {
		config.setIgnoreLaserColor(true);
		config.setIgnoreLaserColorName("green");
		List<Shot> shots = findShots("/shotsearcher/no_interference_one_shot.png");	
		
		assertEquals(1, shots.size());
		
		assertEquals(53, shots.get(0).getX(), 1);
		assertEquals(175, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
	}
	
	@Test
	public void testNoInterenceOneShot() throws IOException, InterruptedException {
		List<Shot> shots = findShots("/shotsearcher/no_interference_one_shot.png");	
		
		assertEquals(1, shots.size());
		
		assertEquals(53, shots.get(0).getX(), 1);
		assertEquals(175, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
	}
	
	@Test
	public void testNoInterenceTwoShots() throws IOException, InterruptedException {
		List<Shot> shots = findShots("/shotsearcher/no_interference_two_shots.png");	

		assertEquals(2, shots.size());

		assertEquals(42, shots.get(0).getX(), 1);
		assertEquals(33, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(53, shots.get(1).getX(), 1);
		assertEquals(175, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
	}
	
	@Test
	public void testInterenceOneShot() throws IOException, InterruptedException {
		List<Shot> shots = findShots("/shotsearcher/interference_one_shot.png");	
		
		assertEquals(1, shots.size());
				
		// Real shot
		assertEquals(411, shots.get(0).getX(), 1);
		assertEquals(331, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
	}
	
	@Test
	public void testInterenceOneShotIgnoreGreen() throws IOException, InterruptedException {
		config.setIgnoreLaserColor(true);
		config.setIgnoreLaserColorName("green");
		List<Shot> shots = findShots("/shotsearcher/interference_one_shot.png");	
		
		assertEquals(1, shots.size());
		
		// Real shot
		assertEquals(411, shots.get(0).getX(), 1);
		assertEquals(331, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
	}
}
