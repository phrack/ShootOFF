package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.Shot.ShotColor;
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
		// Minimize logging attempts because Travis-CI will kill us
		// due to verbose output. To re-enable log outputs you also
		// need to comment out the code in ShotDetectionTestor.setUpBaseClass()
		// that disables all loggers.
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
		requiredShots.add(new Shot(ShotColor.RED, 176.5, 251.3, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 236.5, 169.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 175, 191.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 229.5, 227.5, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}

	@Test
	// BRIGHT
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOnSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_on.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 473.6, 126.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 349.2, 130.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 207.3, 113.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 183.1, 226.9, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 310.5, 228.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 468.7, 219.8, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 469.8, 268.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 339.9, 291.8, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 201.5, 297.7, 0, 2));

		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}

	@Test
	// BRIGHT
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOnSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_on.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.GREEN, 464.1, 23.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 454.8, 102.9, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 317.5, 98.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 145.8, 88.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 220.5, 226.9, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 346.2, 227.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 485.9, 231.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 476.2, 312.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 337.7, 274.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 219.0, 298.0, 0, 2));

		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}

	@Test
	// BRIGHT
	public void testGreen45inch() {
		List<Shot> shots = findShots("/shotsearcher/45in-green.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.GREEN, 334.0, 164.9, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 334.1, 166.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 332.4, 165.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 325.0, 161.5, 0, 2));

		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}
}