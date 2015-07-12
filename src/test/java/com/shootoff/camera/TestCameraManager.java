package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManager {
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setDetectionRate(0);
		config.setDebugMode(true);
		mockManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];
		
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}
	
	private List<Shot> findShots(String videoPath, Optional<boolean[][]> overrideSectorStatuses) {
		Object processingLock = new Object();
		File videoFile = new  File(getClass().getResource(videoPath).getFile());
		CameraManager cameraManager;
		if (overrideSectorStatuses.isPresent()) {
			cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, overrideSectorStatuses.get());
		} else {
			cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses);
		}
		
		try {
			synchronized (processingLock) {
				while (!cameraManager.getProcessedVideo())
					processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return mockManager.getShots();
	}
	
	@Test
	public void testNoInterferenceTwoShots() {
		List<Shot> shots = findShots("/shotsearcher/no_interference_two_shots.mp4", Optional.empty());
		//List<Shot> shots = findShots("/shotsearcher/ps3eye_constrast_default_brightness_default_whitebalance_on.mp4", Optional.empty());
		
		assertEquals(2, shots.size());
		
		assertEquals(627, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(430, shots.get(1).getX(), 1);
		assertEquals(130, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
	}
	
	@Test
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOff() {
		// Turn off the top sectors because they are all just noise.
		boolean[][] overrideShotSectors = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				if (y == 0) {
					overrideShotSectors[y][x] = false;
				} else {
					overrideShotSectors[y][x] = true;
				}
			}
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_brightness_30_constrast_5_whitebalance_off.mp4", 
				Optional.of(overrideShotSectors));
		
		
		// Currently missing shot in top left and two shots on bottom right
		assertEquals(8, shots.size());

		assertEquals(385.5, shots.get(0).getX(), 1);
		assertEquals(182.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(520, shots.get(1).getX(), 1);
		assertEquals(170.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
		
		assertEquals(531.5, shots.get(2).getX(), 1);
		assertEquals(258.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());
		
		assertEquals(383, shots.get(3).getX(), 1);
		assertEquals(263, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
		
		assertEquals(251, shots.get(4).getX(), 1);
		assertEquals(276.5, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
		
		assertEquals(250, shots.get(5).getX(), 1);
		assertEquals(392.5, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());
		
		assertEquals(392, shots.get(6).getX(), 1);
		assertEquals(382.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
		
		assertEquals(532, shots.get(7).getX(), 1);
		assertEquals(334.5, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());
	}
}
