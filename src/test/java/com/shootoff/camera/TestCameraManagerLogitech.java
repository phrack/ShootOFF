package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerLogitech extends ShotDetectionTestor {
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;
	
    @Rule
    public ErrorCollector collector = new ErrorCollector();
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setDebugMode(false);
		mockManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[ShotDetectionManager.SECTOR_ROWS][ShotDetectionManager.SECTOR_COLUMNS];
		
		for (int x = 0; x < ShotDetectionManager.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotDetectionManager.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}
	
	private List<Shot> findShots(String videoPath, Optional<Bounds> projectionBounds) {
		Object processingLock = new Object();
		File videoFile = new  File(TestCameraManagerLogitech.class.getResource(videoPath).getFile());
		
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
	public void testLogitechIndoorGreen() {
		// Missing 3 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-indoor-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 517.1, 255.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 222.9, 259.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 509.7, 184.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 515.5, 50.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 388.3, 85.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 216.9, 71.4, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	
	@Test
	public void testLogitechOutdoorGreen2() {
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-green-2.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 415.6, 50.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 276.5, 70.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 116.8, 72.8, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 113.4, 214.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 269.2, 207.2, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 409.8, 214.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 418.9, 316.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 269.0, 309.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 96.8, 300.8, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	@Test
	public void testLogitechSafariGreen() {
		// Missing 1 shot
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 488.8, 237.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 366.8, 253.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 239.7, 255.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 259.3, 141.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 386.8, 185.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 522.6, 181.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 512.4, 66.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 232.0, 79.1, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	@Test
	public void testLogitechOutdoorRed() {
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-red.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 432.6, 74.4, 0, 2));
		requiredShots.add(new Shot(Color.RED, 293.4, 79.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 131.5, 72.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 131.7, 174.8, 0, 2));
		requiredShots.add(new Shot(Color.RED, 295.6, 153.4, 0, 2));
		requiredShots.add(new Shot(Color.RED, 446.3, 172.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 418.8, 279.6, 0, 2));
		requiredShots.add(new Shot(Color.RED, 289.7, 296.1, 0, 2));
		requiredShots.add(new Shot(Color.RED, 119.1, 287.5, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), false);
	}
	
	
	@Test
	public void testLogitechSafariRed() {
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-red.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 440.5, 90.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 290.7, 91.6, 0, 2));
		requiredShots.add(new Shot(Color.RED, 140.4, 94.4, 0, 2));
		requiredShots.add(new Shot(Color.RED, 129.3, 193.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 290.2, 191.7, 0, 2));
		requiredShots.add(new Shot(Color.RED, 448.7, 200.7, 0, 2));
		requiredShots.add(new Shot(Color.RED, 437.5, 299.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 288.4, 299.2, 0, 2));
		requiredShots.add(new Shot(Color.RED, 137.4, 293.9, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	@Test
	public void testLogitechBouncingTargetsNoBG() {
		config.setIgnoreLaserColor(true);
		config.setIgnoreLaserColorName("green");
		
		List<Shot> shots = findShots("/shotsearcher/logitech-nobg-bouncingtargets-noshots.mp4", Optional.empty());
		
		assertEquals(0, shots.size());
	}
	
	@Test
	public void testLogitechBouncingTargetsOutdoor() {
		config.setIgnoreLaserColor(true);
		config.setIgnoreLaserColorName("green");
		
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-bouncingtargets-noshots.mp4", Optional.empty());
	
		assertEquals(0, shots.size());
	}
}