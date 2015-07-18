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
		assertEquals(133, shots.get(1).getY(), 1);
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
		assertEquals(125, shots.get(1).getY(), 1);
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
		assertEquals(246, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(122.0, shots.get(6).getX(), 1);
		assertEquals(244.0, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());

		assertEquals(285.0, shots.get(7).getX(), 1);
		assertEquals(375.0, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());

		assertEquals(436.5, shots.get(8).getX(), 1);
		assertEquals(379.0, shots.get(8).getY(), 1);
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
		
		// Misses very middle shot and both water shots
				
		assertEquals(6, shots.size());
		
		assertEquals(467.5, shots.get(0).getX(), 1);
		assertEquals(120.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(335.5, shots.get(1).getX(), 1);
		assertEquals(124.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(199.5, shots.get(2).getX(), 1);
		assertEquals(109.0, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(160.5, shots.get(3).getX(), 1);
		assertEquals(220.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(501.5, shots.get(4).getX(), 1);
		assertEquals(218.0, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		assertEquals(439.0, shots.get(5).getX(), 1);
		assertEquals(255.0, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());
	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOffSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_off.mp4", Optional.empty());
		
		// Misses middle shots on middle and bottom row
		
		assertEquals(10, shots.size());
		
		assertEquals(473.0, shots.get(0).getX(), 1);
		assertEquals(63.0, shots.get(0).getY(), 1);
		assertEquals(Color.GREEN, shots.get(0).getColor());

		// Dupe of shot above
		assertEquals(472.0, shots.get(1).getX(), 1);
		assertEquals(61.0, shots.get(1).getY(), 1);
		assertEquals(Color.GREEN, shots.get(1).getColor());

		// Dupe of shot above
		assertEquals(471.0, shots.get(2).getX(), 1);
		assertEquals(59.0, shots.get(2).getY(), 1);
		assertEquals(Color.GREEN, shots.get(2).getColor());

		assertEquals(473.5, shots.get(3).getX(), 1);
		assertEquals(99.5, shots.get(3).getY(), 1);
		assertEquals(Color.GREEN, shots.get(3).getColor());

		assertEquals(337.5, shots.get(4).getX(), 1);
		assertEquals(97.0, shots.get(4).getY(), 1);
		assertEquals(Color.GREEN, shots.get(4).getColor());

		assertEquals(205.5, shots.get(5).getX(), 1);
		assertEquals(99.0, shots.get(5).getY(), 1);
		assertEquals(Color.GREEN, shots.get(5).getColor());

		assertEquals(199.5, shots.get(6).getX(), 1);
		assertEquals(234.0, shots.get(6).getY(), 1);
		assertEquals(Color.GREEN, shots.get(6).getColor());

		assertEquals(496.0, shots.get(7).getX(), 1);
		assertEquals(224.0, shots.get(7).getY(), 1);
		assertEquals(Color.GREEN, shots.get(7).getColor());

		assertEquals(479.0, shots.get(8).getX(), 1);
		assertEquals(281.0, shots.get(8).getY(), 1);
		assertEquals(Color.GREEN, shots.get(8).getColor());

		assertEquals(207.5, shots.get(9).getX(), 1);
		assertEquals(280.5, shots.get(9).getY(), 1);
		assertEquals(Color.GREEN, shots.get(9).getColor());

	}
	
	@Test
	// DARK
	public void testPS3EyeHardwareDefaultsBrightRoomLimitedBounds() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		Bounds projectionBounds = new BoundingBox(109, 104, 379, 297);
		
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_bright_room.mp4", Optional.of(projectionBounds));
		
		assertEquals(4, shots.size());
		
		assertEquals(236.5, shots.get(0).getX(), 1);
		assertEquals(169.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(174.5, shots.get(1).getX(), 1);
		assertEquals(192.0, shots.get(1).getY(), 1);
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
		
		assertEquals(8, shots.size());
		
		assertEquals(473.0, shots.get(0).getX(), 1);
		assertEquals(127.0, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(349.5, shots.get(1).getX(), 1);
		assertEquals(130.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(207.5, shots.get(2).getX(), 1);
		assertEquals(113.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(182.5, shots.get(3).getX(), 1);
		assertEquals(228.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(310.5, shots.get(4).getX(), 1);
		assertEquals(228.5, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		assertEquals(468.0, shots.get(5).getX(), 1);
		assertEquals(220.0, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(469.5, shots.get(6).getX(), 1);
		assertEquals(268.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());

		assertEquals(201.5, shots.get(7).getX(), 1);
		assertEquals(297.5, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());
	}
}