package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Main;
import com.shootoff.camera.ShotColor;
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
		requiredShots.add(new Shot(ShotColor.RED, 397.0, 241.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 289.1, 144.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 200.4, 209.5, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 334.3, 227.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 295.6, 352.7, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 397.5, 242.0, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}
	
	
	@Test
	public void testC910Red_z() {
		List<Shot> shots = findShots("/shotsearcher/c910_red_z.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 394.0, 244.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 315.0, 237.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 430.0, 187.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 485.0, 276.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 397.0, 193.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 329.0, 208.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 387.0, 273.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 468.0, 243.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 399.0, 192.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 332.0, 232.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 279.0, 251.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 266.0, 217.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 298.0, 190.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 219.0, 205.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 254.0, 292.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 345.0, 299.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 373.0, 186.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 473.0, 198.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 503.0, 168.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 513.0, 240.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 510.0, 239.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 414.0, 171.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 319.0, 193.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 245.0, 161.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 148.0, 218.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 202.0, 300.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 191.0, 293.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 292.0, 324.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 375.0, 324.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 432.0, 306.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 394.0, 208.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 447.0, 215.0, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}
	
}