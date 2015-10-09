package com.shootoff.camera;

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

public class TestCameraManagerLifecam extends ShotDetectionTestor {
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;
	
    @Rule
    public ErrorCollector collector = new ErrorCollector();
	
	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		
		config = new Configuration(new String[0]);
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
		List<Shot> shots = findShots("/shotsearcher/lifecam-indoor-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 432.7, 309.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 295.5, 320.2, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 75.0, 339.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 141.2, 208.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 295.3, 234.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 471.8, 226.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 443.4, 100.9, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 257.8, 109.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 83.3, 79.3, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), false);
	}
	
	
	@Test
	public void testLifecamOutdoorGreen() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-outdoor-green.mp4", Optional.empty());

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 449.1, 324.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 276.6, 325.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 97.6, 333.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 143.9, 197.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 304.3, 225.7, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 441.3, 226.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 441.2, 109.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 294.3, 121.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 112.8, 111.4, 0, 2));

		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	
	@Test
	public void testLifecamSafariGreen() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-safari-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 413.0, 265.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 266.1, 298.2, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 87.8, 312.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 108.4, 192.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 257.9, 213.8, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 428.2, 220.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 433.2, 91.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 310.4, 113.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 117.2, 107.4, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), false);
	}
	
	@Test
	public void testLifecamMotion() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-motion-in-room.mp4", Optional.empty());
		
		// This is noise but we can't get rid of it without really messing up other tests.
		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(Color.GREEN, 308.4, 394.6, 0, 2));

		super.checkShots(collector, shots, new ArrayList<Shot>(), optionalShots, false);
	}
	
	@Test
	public void testLifecamDuelTree() {
		List<Shot> shots = findShots("/shotsearcher/lifecam-indoor-tree-green.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 350.4, 275.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 332.6, 308.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 316.6, 284.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 266.8, 252.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 324.9, 223.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 268.0, 181.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 330.1, 152.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 325.4, 162.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 261.7, 119.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 328.7, 155.5, 0, 2));
		
		List<Shot> optionalShots = new ArrayList<Shot>();
		// This is noise on the table in the middle
		optionalShots.add(new Shot(Color.GREEN, 268.3, 264.05, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}
}