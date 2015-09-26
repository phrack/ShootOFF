package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerLifecam {
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
		
		CameraManager.setFrameCount(0);
	}
	
	private List<Shot> findShots(String videoPath, Optional<Bounds> projectionBounds) {
		Object processingLock = new Object();
		File videoFile = new  File(TestCameraManagerLifecam.class.getResource(videoPath).getFile());
		
		CameraManager cameraManager;
		cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses, 
				projectionBounds);
		
		try {
			synchronized (processingLock) {
				while (!cameraManager.isVideoProcessed())
					processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return mockManager.getShots();
	}
	
	@Test
	public void testLifecamIndoorGreen() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-indoor-green.avi", Optional.empty());
		
		assertEquals(9, shots.size());
		

	}
	
	
	@Test
	public void testLifecamOutdoorGreen() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-outdoor-green.avi", Optional.empty());
		
		assertEquals(9, shots.size());
		

	}
	
	
	@Test
	public void testLifecamSafariGreen() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-safari-green.avi", Optional.empty());
		
		assertEquals(9, shots.size());
		

	}
	
	@Test
	public void testLifecamMotion() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-motion-in-room.avi", Optional.empty());
		
		assertEquals(0, shots.size());
		

	}
	
	@Test
	public void testLifecamDuelTree() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-indoor-tree-green.avi", Optional.empty());
		
		assertEquals(10, shots.size());
		

	}
	
}