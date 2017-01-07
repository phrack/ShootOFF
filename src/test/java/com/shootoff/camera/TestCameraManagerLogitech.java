package com.shootoff.camera;

import static org.junit.Assert.*;

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

public class TestCameraManagerLogitech extends ShotDetectionTestor {
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
	public void testLogitechIndoorGreen() {
		// Missing 2 shots
		List<Shot> shots = findShots("/shotsearcher/logitech-indoor-green.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.GREEN, 517.1, 255.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 222.9, 259.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 509.7, 184.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 515.5, 50.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 388.3, 85.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 216.9, 71.4, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.GREEN, 386.0, 258, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}

	@Test
	public void testLogitechOutdoorGreen2() {
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-green-2.mp4", Optional.empty(), mockManager,
				config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.GREEN, 415.6, 50.7, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 276.5, 70.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 116.8, 72.8, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 269.2, 207.2, 0, 2));

		requiredShots.add(new Shot(ShotColor.GREEN, 418.9, 316.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 269.0, 309.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 96.8, 300.8, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.GREEN, 113.4, 214.6, 0, 2));
		optionalShots.add(new Shot(ShotColor.GREEN, 409.8, 214.7, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	public void testLogitechSafariGreen() {
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-green.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.GREEN, 488.8, 237.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 239.7, 255.7, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 259.3, 141.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 386.8, 185.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 522.6, 181.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 232.0, 79.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 512.4, 66.0, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.GREEN, 366.8, 253.3, 0, 2));
		optionalShots.add(new Shot(ShotColor.GREEN, 390.4, 84.6, 0, 2));
		optionalShots.add(new Shot(ShotColor.GREEN, 370.6, 256.3, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}

	@Test
	public void testLogitechOutdoorRed() {
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-red.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 293.4, 79.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 131.5, 72.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 131.7, 174.8, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 295.6, 153.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 446.3, 172.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 418.8, 279.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 289.7, 296.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 119.1, 287.5, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 432.6, 74.4, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 432.6, 74.4, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}

	@Test
	public void testLogitechSafariRed() {
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-red.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 440.5, 90.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 290.7, 91.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 140.4, 94.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 290.2, 191.7, 0, 2));

		requiredShots.add(new Shot(ShotColor.RED, 437.5, 299.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 137.4, 293.9, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 129.3, 193.0, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 288.4, 299.2, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 448.7, 200.7, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}

	@Test
	public void testLogitechBouncingTargetsNoBG() {

		List<Shot> shots = findShots("/shotsearcher/logitech-nobg-bouncingtargets-noshots.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		assertEquals(0, shots.size());
	}

	@Test
	public void testLogitechBouncingTargetsOutdoor() {

		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-bouncingtargets-noshots.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		assertEquals(0, shots.size());
	}

}