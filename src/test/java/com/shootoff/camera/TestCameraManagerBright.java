package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerBright extends ShotDetectionTestor {
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;

	@Rule public ErrorCollector collector = new ErrorCollector();

	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setDebugMode(false);
		mockManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[JavaShotDetector.SECTOR_ROWS][JavaShotDetector.SECTOR_COLUMNS];

		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < JavaShotDetector.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}

	}

	@Test
	// BRIGHT
	public void testPS3EyeHardwareDefaultsBrightRoom() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_bright_room.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

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
				Optional.empty(), mockManager, config, sectorStatuses);

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
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_on.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

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

	@Test
	// BRIGHT
	public void testGreen45inch() {
		List<Shot> shots = findShots("/shotsearcher/45in-green.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 334.0, 164.9, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 334.1, 166.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 332.4, 165.5, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 325.0, 161.5, 0, 2));

		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
}