package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerHighRes extends ShotDetectionTestor {
	private final Logger logger = LoggerFactory.getLogger(TestCameraManagerHighRes.class);
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;

	@Rule public ErrorCollector collector = new ErrorCollector();

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

	@Test
	// BRIGHT
	public void test1280x720() {
		List<Shot> shots = findShots("/shotsearcher/testvid800x600.avi", Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.RED, 176.5, 251.3, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(Color.RED, 236.5, 169.5, 0, 2));
		// optionalShots.add(new Shot(Color.RED, 175, 191.5, 0, 2));
		// optionalShots.add(new Shot(Color.RED, 229.5, 227.5, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}
}