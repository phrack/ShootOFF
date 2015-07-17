package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerDark {
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
		CameraManager cameraManager;
		cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses, 
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
	// DARK
	public void testNoInterferenceTwoShots() {
		List<Shot> shots = findShots("/shotsearcher/no_interference_two_shots.mp4", Optional.empty());
		
		assertEquals(2, shots.size());
		
		assertEquals(627, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(430, shots.get(1).getX(), 1);
		assertEquals(130, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsDarkRoom() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_projector_dark_room.mp4", Optional.empty());
				
		assertEquals(9, shots.size());
		
		assertEquals(119.0, shots.get(0).getX(), 1);
		assertEquals(142.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(279.5, shots.get(1).getX(), 1);
		assertEquals(123.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(438.0, shots.get(2).getX(), 1);
		assertEquals(145.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(443.5, shots.get(3).getX(), 1);
		assertEquals(230.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(298.0, shots.get(4).getX(), 1);
		assertEquals(239.0, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		assertEquals(214.0, shots.get(5).getX(), 1);
		assertEquals(244.5, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(122.0, shots.get(6).getX(), 1);
		assertEquals(244.0, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());

		assertEquals(285.0, shots.get(7).getX(), 1);
		assertEquals(375.0, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());

		assertEquals(436.5, shots.get(8).getX(), 1);
		assertEquals(377.0, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());
	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsAmbientLightNatureScene() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_ambient_light_nature_scene.mp4", Optional.empty());
		
		assertEquals(0, shots.size());
	}	
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOffSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_off.mp4", Optional.empty());
		
		// Misses left two sky shots at the top and both water shots on the bottom
		// these are basically non-starters with a fully bright projection like this.
		
		assertEquals(5, shots.size());
		
		assertEquals(467.5, shots.get(0).getX(), 1);
		assertEquals(120.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(160.5, shots.get(1).getX(), 1);
		assertEquals(220.0, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(371.0, shots.get(2).getX(), 1);
		assertEquals(221.0, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(501.5, shots.get(3).getX(), 1);
		assertEquals(218.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(439.0, shots.get(4).getX(), 1);
		assertEquals(255.0, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOffSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_off.mp4", Optional.empty());
		
		// Gets misses middle shots on middle and bottom row
		
		assertEquals(9, shots.size());
		
		assertEquals(473.0, shots.get(0).getX(), 1);
		assertEquals(63.0, shots.get(0).getY(), 1);
		assertEquals(Color.GREEN, shots.get(0).getColor());

		// Dupe of the shot above
		assertEquals(470.5, shots.get(1).getX(), 1);
		assertEquals(59.0, shots.get(1).getY(), 1);
		assertEquals(Color.GREEN, shots.get(1).getColor());

		assertEquals(473.5, shots.get(2).getX(), 1);
		assertEquals(99.5, shots.get(2).getY(), 1);
		assertEquals(Color.GREEN, shots.get(2).getColor());

		assertEquals(337.5, shots.get(3).getX(), 1);
		assertEquals(97.0, shots.get(3).getY(), 1);
		assertEquals(Color.GREEN, shots.get(3).getColor());

		assertEquals(205.5, shots.get(4).getX(), 1);
		assertEquals(99.0, shots.get(4).getY(), 1);
		assertEquals(Color.GREEN, shots.get(4).getColor());

		assertEquals(199.5, shots.get(5).getX(), 1);
		assertEquals(233.5, shots.get(5).getY(), 1);
		assertEquals(Color.GREEN, shots.get(5).getColor());

		assertEquals(498.0, shots.get(6).getX(), 1);
		assertEquals(224.0, shots.get(6).getY(), 1);
		assertEquals(Color.GREEN, shots.get(6).getColor());

		assertEquals(479.0, shots.get(7).getX(), 1);
		assertEquals(281.0, shots.get(7).getY(), 1);
		assertEquals(Color.GREEN, shots.get(7).getColor());

		assertEquals(207.0, shots.get(8).getX(), 1);
		assertEquals(279.5, shots.get(8).getY(), 1);
		assertEquals(Color.GREEN, shots.get(8).getColor());

	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsBrightRoomLimitedBounds() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		// This misses the middle two shots
		Bounds projectionBounds = new BoundingBox(109, 104, 379, 297);
		
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_bright_room.mp4", Optional.of(projectionBounds));
		
		assertEquals(4, shots.size());
		
		assertEquals(236.5, shots.get(0).getX(), 1);
		assertEquals(169.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(175, shots.get(1).getX(), 1);
		assertEquals(191, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
		
		assertEquals(229.5, shots.get(2).getX(), 1);
		assertEquals(227.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());
		
		assertEquals(176.5, shots.get(3).getX(), 1);
		assertEquals(251.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOnSafariLimitedBounds() {
		Bounds projectionBounds = new BoundingBox(131, 77, 390, 265);
		
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_on.mp4", 
				Optional.of(projectionBounds));
		
		// This misses the bottom two shots on the water
		
		assertEquals(7, shots.size());
		
		assertEquals(604.0, shots.get(0).getX(), 1);
		assertEquals(204.0, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(481.5, shots.get(1).getX(), 1);
		assertEquals(207.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(338.5, shots.get(2).getX(), 1);
		assertEquals(190.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(315.0, shots.get(3).getX(), 1);
		assertEquals(305.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(441.5, shots.get(4).getX(), 1);
		assertEquals(307.0, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		assertEquals(599.0, shots.get(5).getX(), 1);
		assertEquals(297.0, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(600.5, shots.get(6).getX(), 1);
		assertEquals(345.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
	}
}