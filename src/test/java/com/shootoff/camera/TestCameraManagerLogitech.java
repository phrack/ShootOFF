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

import com.shootoff.camera.ShotDetection.ShotDetectionManager;
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
		config.setDetectionRate(0);
		config.setDebugMode(true);
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
		// Missing 4 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-indoor-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 223.3, 258.9, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 509.8, 184.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 515.9, 50.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 388.8, 86.2, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 216.7, 71.2, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	
	@Test
	public void testLogitechOutdoorGreen2() {
		// Missing 4 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-green-2.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 415.7, 50.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 276.8, 69.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 269.2, 207.2, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 268.0, 309.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 97.0, 301.0, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	@Test
	public void testLogitechSafariGreen() {
		// Missing 3 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 241.7, 253.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 259.5, 141.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 387.5, 185.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 522.8, 181.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 390.3, 84.8, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 232.3, 78.8, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	@Test
	public void testLogitechOutdoorRed() {
		// Missing 7 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-red.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 446.2, 172.6, 0, 2));
		requiredShots.add(new Shot(Color.RED, 119.3, 287.3, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), false);
	}
	
	
	@Test
	public void testLogitechSafariRed() {
		// Missing 6 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-red.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 290.2, 191.8, 0, 2));
		requiredShots.add(new Shot(Color.RED, 437.8, 299.3, 0, 2));
		requiredShots.add(new Shot(Color.RED, 288.6, 299.4, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	
	@Test
	public void testLogitechBouncingTargetsNoBG() {
		List<Shot> shots = findShots("/shotsearcher/logitech-nobg-bouncingtargets-noshots.mp4", Optional.empty());
		
		assertEquals(0, shots.size());
	}
	
	@Test
	public void testLogitechBouncingTargetsOutdoor() {
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-bouncingtargets-noshots.mp4", Optional.empty());
		
		// These are noise but we can't get rid of them without really messing up other tests.
		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(Color.GREEN, 233.2, 192.2, 0, 2));
		optionalShots.add(new Shot(Color.GREEN, 233.3, 278.4, 0, 2));

		super.checkShots(collector, shots, new ArrayList<Shot>(), optionalShots, false);
	}
}