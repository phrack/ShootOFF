package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCamerManagerVeryBright {
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setDetectionRate(0);
		config.setDebugMode(true);
		mockManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];
		
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}
	
	private List<Shot> findShots(String videoPath, Optional<Bounds> projectionBounds) {
		Object processingLock = new Object();
		File videoFile = new  File(getClass().getResource(videoPath).getFile());
		CameraManager cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses,
				projectionBounds);
		
		try {
			synchronized (processingLock) {
				while (!cameraManager.isVideoProcessed())
					processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return mockManager.getShots();
	}
	
	@Test
	// VERY BRIGHT
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOff() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_default_contrast_whitebalance_off.mp4", 
				Optional.empty());
		
		assertEquals(10, shots.size());
		
		assertEquals(249.5, shots.get(0).getX(), 1);
		assertEquals(191.0, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(385.5, shots.get(1).getX(), 1);
		assertEquals(182.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(520.0, shots.get(2).getX(), 1);
		assertEquals(170.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(531.5, shots.get(3).getX(), 1);
		assertEquals(258.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(383.0, shots.get(4).getX(), 1);
		assertEquals(263.5, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		assertEquals(251.0, shots.get(5).getX(), 1);
		assertEquals(276.0, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(250.0, shots.get(6).getX(), 1);
		assertEquals(392.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());

		assertEquals(392.0, shots.get(7).getX(), 1);
		assertEquals(382.5, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());

		assertEquals(531.5, shots.get(8).getX(), 1);
		assertEquals(335.0, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());

		assertEquals(529.5, shots.get(9).getX(), 1);
		assertEquals(356.0, shots.get(9).getY(), 1);
		assertEquals(Color.RED, shots.get(9).getColor());
	}
	
	@Test
	// VERY BRIGHT
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOn() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_default_contrast_whitebalance_on.mp4",
				Optional.empty());
		
		assertEquals(9, shots.size());

		assertEquals(251.5, shots.get(0).getX(), 1);
		assertEquals(183, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(378.5, shots.get(1).getX(), 1);
		assertEquals(168.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
		
		assertEquals(521.5, shots.get(2).getX(), 1);
		assertEquals(163.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());
		
		assertEquals(530, shots.get(3).getX(), 1);
		assertEquals(251.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
		
		assertEquals(380.5, shots.get(4).getX(), 1);
		assertEquals(264, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
		
		assertEquals(233, shots.get(5).getX(), 1);
		assertEquals(270, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());
		
		assertEquals(249.5, shots.get(6).getX(), 1);
		assertEquals(379, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
		
		assertEquals(381.5, shots.get(7).getX(), 1);
		assertEquals(375.5, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());
		
		assertEquals(539, shots.get(8).getX(), 1);
		assertEquals(381, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());
	}

	@Test
	// VERY BRIGHT
	public void testMSHD3000MinBrightnessMinContrastWhiteBalanceOff() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_min_contrast_whitebalance_off.mp4",
				Optional.empty());
		
		// Currently missing first shot in top left and last two shots on
		// bottom right getting rejected due to size heuristic
		assertEquals(9, shots.size());

		assertEquals(377, shots.get(0).getX(), 1);
		assertEquals(274.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(226.5, shots.get(1).getX(), 1);
		assertEquals(180.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
		
		assertEquals(251, shots.get(2).getX(), 1);
		assertEquals(377.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());
		
		assertEquals(537, shots.get(3).getX(), 1);
		assertEquals(383.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
		
		assertEquals(505, shots.get(4).getX(), 1);
		assertEquals(167.5, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
		
		assertEquals(496.5, shots.get(5).getX(), 1);
		assertEquals(268, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());
		
		assertEquals(272, shots.get(6).getX(), 1);
		assertEquals(278.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
		
		assertEquals(375.5, shots.get(7).getX(), 1);
		assertEquals(200.5, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());
		
		assertEquals(403, shots.get(8).getX(), 1);
		assertEquals(363, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());
	}

	@Test
	// VERY BRIGHT
	public void testMSHD3000HardwareDefaultsAmbientLightNatureScene() {
		List<Shot> shots = findShots("/shotsearcher/mshd3000_hardware_defaults_ambient_light_nature_scene.mp4", 
				Optional.empty());

		assertEquals(0, shots.size());
	}
}
