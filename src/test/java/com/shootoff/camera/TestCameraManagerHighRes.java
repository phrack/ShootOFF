package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerHighRes extends ShotDetectionTestor {
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
	// Shots are missed because shot detection has not been modified to support
	// other resolutions
	public void test1280x720Green() {
		List<Shot> shots = findShots("/shotsearcher/highres-green.mp4", Optional.empty(), mockManager, config,
				sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 586.30, 395.44, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 532.16, 347.98, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 587.80, 396.73, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}
}