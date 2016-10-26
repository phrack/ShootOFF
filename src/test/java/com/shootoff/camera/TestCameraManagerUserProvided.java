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

public class TestCameraManagerUserProvided extends ShotDetectionTestor {

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
	public void testC920CloseRed_Greatone123x() {
		List<Shot> shots = findShots("/shotsearcher/c920_close_red_laserlyte_greatone123x.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 325.0, 245.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 342.0, 247.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 333.0, 228.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 338.0, 229.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 328.0, 243.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 337.0, 233.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 346.0, 216.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 348.0, 230.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 346.0, 234.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 334.0, 235.0, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}
	
	

	@Test
	public void testC615CloseRed_edwardkort() {
		List<Shot> shots = findShots("/shotsearcher/c615_close_red_edwardkort.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 340.0, 73.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 429.6, 230.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 287.6, 403.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 171.0, 224.7, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 375.7, 228.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 289.1, 144.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 200.4, 209.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 334.3, 227.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 295.6, 352.7, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}
	
}