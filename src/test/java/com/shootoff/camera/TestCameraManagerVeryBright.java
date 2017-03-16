package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.ShotColor;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerVeryBright extends ShotDetectionTestor {
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
	// VERY BRIGHT
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOff() {

		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_default_contrast_whitebalance_off.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 251.3, 275.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 392.9, 383.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 249.5, 191, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 385.5, 182.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 520, 170.5, 0, 2));

		requiredShots.add(new Shot(ShotColor.RED, 250, 392.5, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 382.9, 263.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 531.5, 335, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 542.5, 390.8, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 549, 382.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 531.4, 258.9, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 530.0, 356, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	// VERY BRIGHT
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOn() {

		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_default_contrast_whitebalance_on.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 378.5, 168.5, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 251.5, 183, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 521.5, 163.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 530, 251.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 380.5, 264, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 233, 270, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 249.5, 379, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 383.5, 375.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 539, 381, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	// VERY BRIGHT
	public void testMSHD3000MinBrightnessMinContrastWhiteBalanceOff() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}

		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_min_contrast_whitebalance_off.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 377.1, 274.7, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 226.5, 180.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 251, 377.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 537, 383.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 272, 278.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 375.5, 200.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 403, 363, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 505, 167.5, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 486.5, 268, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 496.0, 268, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	// VERY BRIGHT
	public void testMSHD3000HardwareDefaultsAmbientLightNatureScene() {
		List<Shot> shots = findShots("/shotsearcher/mshd3000_hardware_defaults_ambient_light_nature_scene.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 113.8, 11.0, 0, 2));

		super.checkShots(collector, shots, new ArrayList<Shot>(), optionalShots, false);
	}
}
