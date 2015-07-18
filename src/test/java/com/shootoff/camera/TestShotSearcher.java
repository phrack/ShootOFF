package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
	private boolean[][] sectorStatuses;

	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setDebugMode(true);
		mockManager = new MockCanvasManager(config);
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];

		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}

	private List<Shot> findShots(String imagePath) throws IOException, InterruptedException {
		BufferedImage testFrame = ImageIO.read(
				getClass().getResourceAsStream(imagePath));

		BufferedImage grayScale = new BufferedImage(testFrame.getWidth(),
				testFrame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

		grayScale.createGraphics().drawImage(testFrame, 0, 0, null);
		
		Thread searcher = new Thread(
				new ShotSearcher(config, mockManager, sectorStatuses,
						testFrame, grayScale, Optional.empty()));
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
	public void testNoInterenceOneShotSectorOff() throws IOException, InterruptedException {
		// Turn off the sector the shot is in
		sectorStatuses[1][0] = false;
 		List<Shot> shots = findShots("/shotsearcher/no_interference_one_shot.png");

		assertEquals(0, shots.size());
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
		
		assertEquals(5, shots.size());

		assertEquals(174.0, shots.get(0).getX(), 1);
		assertEquals(108.0, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(2, shots.get(1).getX(), 1);
		assertEquals(214, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(163.5, shots.get(2).getX(), 1);
		assertEquals(179, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(53.0, shots.get(3).getX(), 1);
		assertEquals(240.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		// Real shot
		assertEquals(411.0, shots.get(4).getX(), 1);
		assertEquals(331.5, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
	}
}
