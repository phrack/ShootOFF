package com.shootoff.camera;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerDark extends ShotDetectionTestor {
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
	// DARK
	public void testNoInterferenceTwoShots() {
		List<DisplayShot> shots = findShots("/shotsearcher/no_interference_two_shots.mp4", Optional.empty(), mockManager,
				config, sectorStatuses);

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 431.7, 132.4, 0, 2));

		// Bad trigger pull gives this shot a long tail
		// Different algorithms will have different ideas of where this shot is
		optionalShots.add(new Shot(ShotColor.RED, 633.0, 159.0, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 626.0, 170.0, 0, 2));

		super.checkShots(collector, shots, new ArrayList<Shot>(), optionalShots, false);
	}

	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsDarkRoom() {
		List<DisplayShot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_projector_dark_room.mp4", Optional.empty(),
				mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 118.8, 143.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 279.6, 123.6, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 302.5, 238.8, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 218.0, 244.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 122.0, 243.7, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 288.2, 375.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 436.6, 377.5, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 438, 145, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 443.5, 230, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsAmbientLightNatureScene() {
		List<DisplayShot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_ambient_light_nature_scene.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		assertEquals(0, shots.size());
	}

	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOffSafari() {

		// Turn off the bottom sectors because there was light flashed in the
		// room.
		for (int x = 0; x < JavaShotDetector.SECTOR_ROWS; x++) {
			sectorStatuses[2][x] = false;
		}

		List<DisplayShot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_off.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 467.2, 120.3, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 334.4, 125.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 199.7, 108.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 164.5, 220.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 503.6, 218.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 323.2, 311.0, 0, 2));
		requiredShots.add(new Shot(ShotColor.RED, 372.0, 222.3, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 194.4, 314.9, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 441.6, 250.2, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, true);
	}

	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOffSafari() {
		List<DisplayShot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_off.mp4",
				Optional.empty(), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.GREEN, 472.8, 62.9, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 472.9, 100.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 337.8, 97.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 376.8, 226.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 203.9, 99.4, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 200.6, 233.1, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 494.7, 224.2, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 479.2, 281.8, 0, 2));
		requiredShots.add(new Shot(ShotColor.GREEN, 207.2, 281.3, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 331.68, 284.1, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsBrightRoomLimitedBounds() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < JavaShotDetector.SECTOR_ROWS; x++) {
			sectorStatuses[0][x] = false;
		}

		Bounds projectionBounds = new BoundingBox(109, 104, 379, 297);

		List<DisplayShot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_bright_room.mp4",
				Optional.of(projectionBounds), mockManager, config, sectorStatuses);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(ShotColor.RED, 176.5, 251.3, 0, 2));

		List<Shot> optionalShots = new ArrayList<Shot>();
		optionalShots.add(new Shot(ShotColor.RED, 236.5, 169.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 175, 191.5, 0, 2));
		optionalShots.add(new Shot(ShotColor.RED, 229.5, 227.5, 0, 2));

		super.checkShots(collector, shots, requiredShots, optionalShots, false);
	}

	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOnSafariLimitedBounds() {
		Bounds projectionBounds = new BoundingBox(131, 77, 390, 265);

		List<DisplayShot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_on.mp4",
				Optional.of(projectionBounds), mockManager, config, sectorStatuses);

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
}