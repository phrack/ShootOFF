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

public class TestCameraManagerUserProvided extends ShotDetectionTestor {
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
	public void testC920CloseRed_Greatone123x() {
		List<Shot> shots = findShots("/shotsearcher/c920_close_red_laserlyte_greatone123x.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 325.0, 245.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 342.0, 247.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 333.0, 228.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 338.0, 229.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 328.0, 243.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 337.0, 233.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 346.0, 216.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 348.0, 230.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 346.0, 234.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 334.0, 235.0, 0, 2));
		requiredShots.add(new Shot(Color.RED, 338.0, 219.0, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

}