package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

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
		mockManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];
		
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}
	
	private List<Shot> findShots(String videoPath) {
		Object processingLock = new Object();
		File videoFile = new  File(getClass().getResource(videoPath).getFile());
		CameraManager cameraManager = new CameraManager(videoFile, processingLock, mockManager, config);
		
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
		List<Shot> shots = findShots("/shotsearcher/no_interference_two_shots.mp4");
		
		assertEquals(2, shots.size());
		
		assertEquals(627, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(430, shots.get(1).getX(), 1);
		assertEquals(130, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
	}

}
