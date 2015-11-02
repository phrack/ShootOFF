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

import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerBright extends ShotDetectionTestor {
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
		File videoFile = new  File(TestCameraManagerBright.class.getResource(videoPath).getFile());
		CameraManager cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses,
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
	// BRIGHT
	public void testPS3EyeHardwareDefaultsBrightRoom() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_bright_room.mp4", Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 176.5, 251.3, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(Color.RED, 236.5, 169.5, 0, 2));
		optionalShots.add(new Shot(Color.RED, 175, 191.5, 0, 2));
		optionalShots.add(new Shot(Color.RED, 229.5, 227.5, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}	
	
	@Test
	// BRIGHT
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOnSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_on.mp4", 
				Optional.empty());
		
		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 473.6, 126.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 349.2, 130.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 207.3, 113.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 183.1, 226.9, 0, 2));
		requiredShots.add(new Shot(Color.RED, 310.5, 228.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 468.7, 219.8, 0, 2));
		requiredShots.add(new Shot(Color.RED, 469.8, 268.5, 0, 2));
		requiredShots.add(new Shot(Color.RED, 339.9, 291.8, 0, 2));
		requiredShots.add(new Shot(Color.RED, 201.5, 297.7, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
	
	@Test
	// BRIGHT
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOnSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_on.mp4", Optional.empty());

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 464.1, 23.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 454.8, 102.9, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 317.5, 98.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 145.8, 88.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 220.5, 226.9, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 346.2, 227.6, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 485.9, 231.1, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 476.2, 312.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 337.7, 274.4, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 219.0, 298.0, 0, 2));
		
		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
}