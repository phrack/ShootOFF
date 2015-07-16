package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManager {
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
	
	private List<Shot> findShots(String videoPath) {
		Object processingLock = new Object();
		File videoFile = new  File(getClass().getResource(videoPath).getFile());
		CameraManager cameraManager;
		cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses);
		
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
	public void testNoInterferenceTwoShots() {
		List<Shot> shots = findShots("/shotsearcher/no_interference_two_shots.mp4");
		
		assertEquals(2, shots.size());
		
		assertEquals(627, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(430, shots.get(1).getX(), 1);
		assertEquals(130, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
	}
	
	@Test
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOff() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_default_contrast_whitebalance_off.mp4");
		
		assertEquals(10, shots.size());

		assertEquals(249, shots.get(0).getX(), 1);
		assertEquals(191, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(385.5, shots.get(1).getX(), 1);
		assertEquals(182.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
		
		assertEquals(520, shots.get(2).getX(), 1);
		assertEquals(170.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());
		
		assertEquals(531.5, shots.get(3).getX(), 1);
		assertEquals(258.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
		
		assertEquals(383, shots.get(4).getX(), 1);
		assertEquals(263, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
		
		assertEquals(251, shots.get(5).getX(), 1);
		assertEquals(276.5, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());
		
		assertEquals(250, shots.get(6).getX(), 1);
		assertEquals(392.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
		
		assertEquals(392, shots.get(7).getX(), 1);
		assertEquals(382.5, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());
		
		assertEquals(532, shots.get(8).getX(), 1);
		assertEquals(334.5, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());
		
		assertEquals(529.5, shots.get(9).getX(), 1);
		assertEquals(356, shots.get(9).getY(), 1);
		assertEquals(Color.RED, shots.get(9).getColor());
	}
	
	@Test
	public void testMSHD3000MinBrightnessDefaultContrastWhiteBalanceOn() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_default_contrast_whitebalance_on.mp4");
		
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
	public void testMSHD3000MinBrightnessMinContrastWhiteBalanceOff() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/mshd3000_min_brightness_min_contrast_whitebalance_off.mp4");
		
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
	public void testMSHD3000ardwareDefaultsAmbientLightNatureScene() {
		List<Shot> shots = findShots("/shotsearcher/mshd3000_hardware_defaults_ambient_light_nature_scene.mp4");

		assertEquals(0, shots.size());
	}
	
	@Test
	public void testPS3EyeHardwareDefaultsBrightRoom() {
		// Turn off the top sectors because they are all just noise.
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			sectorStatuses[0][x] = false;
		}
		
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_bright_room.mp4");
		
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
	public void testPS3EyeHardwareDefaultsDarkRoom() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_projector_dark_room.mp4");
		
		assertEquals(9, shots.size());
		
		assertEquals(119, shots.get(0).getX(), 1);
		assertEquals(142.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(279.5, shots.get(1).getX(), 1);
		assertEquals(123.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());
		
		assertEquals(438, shots.get(2).getX(), 1);
		assertEquals(145.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());
		
		assertEquals(443.5, shots.get(3).getX(), 1);
		assertEquals(230, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
		
		assertEquals(302, shots.get(4).getX(), 1);
		assertEquals(239, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());
		
		assertEquals(218.5, shots.get(5).getX(), 1);
		assertEquals(244.5, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());
		
		assertEquals(122, shots.get(6).getX(), 1);
		assertEquals(244, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
		
		assertEquals(288, shots.get(7).getX(), 1);
		assertEquals(375, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());
		
		assertEquals(436.5, shots.get(8).getX(), 1);
		assertEquals(377, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());
	}
	
	@Test
	public void testPS3EyeHardwareDefaultsAmbientLightNatureScene() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_ambient_light_nature_scene.mp4");
		
		assertEquals(0, shots.size());
	}
	
	@Test
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOnSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_on.mp4");
		
		// This misses the bottom two shots on the water
		
		assertEquals(7, shots.size());
		
		assertEquals(473.5, shots.get(0).getX(), 1);
		assertEquals(127.0, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(345.0, shots.get(1).getX(), 1);
		assertEquals(130.0, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(202.0, shots.get(2).getX(), 1);
		assertEquals(114.0, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(182.0, shots.get(3).getX(), 1);
		assertEquals(228.0, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(310.0, shots.get(4).getX(), 1);
		assertEquals(228.5, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		assertEquals(468.0, shots.get(5).getX(), 1);
		assertEquals(220.0, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(467.0, shots.get(6).getX(), 1);
		assertEquals(268.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());
	}
	
	@Test
	public void testPS3EyeHardwareDefaultsRedLaserRoomLightOffSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_red_laser_lights_off.mp4");
		
		// Misses all three shots on the sky at the top and both water shots on the bottom
		// these are basically non-starters with a fully bright projection like this.
			
		assertEquals(4, shots.size());
		
		assertEquals(161.0, shots.get(0).getX(), 1);
		assertEquals(220.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(367.0, shots.get(1).getX(), 1);
		assertEquals(223.0, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		assertEquals(504.0, shots.get(2).getX(), 1);
		assertEquals(218.0, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(440.5, shots.get(3).getX(), 1);
		assertEquals(250.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());
	}
	
	@Test
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOnSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_on.mp4");
		
		// This gets every shot but the far left shot in the middle row it's likely too red
		
		assertEquals(12, shots.size());
		
		// All of these colors are misidentified, probably because the frames are heated up
		// to much for the current color detection algorithm 
		assertEquals(464.0, shots.get(0).getX(), 1);
		assertEquals(24.0, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());

		assertEquals(454.0, shots.get(1).getX(), 1);
		assertEquals(102.0, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());

		// Dupe of shot above
		assertEquals(453.5, shots.get(2).getX(), 1);
		assertEquals(103.5, shots.get(2).getY(), 1);
		assertEquals(Color.RED, shots.get(2).getColor());

		assertEquals(314.0, shots.get(3).getX(), 1);
		assertEquals(99.5, shots.get(3).getY(), 1);
		assertEquals(Color.RED, shots.get(3).getColor());

		assertEquals(146.0, shots.get(4).getX(), 1);
		assertEquals(88.0, shots.get(4).getY(), 1);
		assertEquals(Color.RED, shots.get(4).getColor());

		// Dupe of shot above
		assertEquals(144.0, shots.get(5).getX(), 1);
		assertEquals(87.0, shots.get(5).getY(), 1);
		assertEquals(Color.RED, shots.get(5).getColor());

		assertEquals(346.0, shots.get(6).getX(), 1);
		assertEquals(225.5, shots.get(6).getY(), 1);
		assertEquals(Color.RED, shots.get(6).getColor());

		assertEquals(486.0, shots.get(7).getX(), 1);
		assertEquals(232.0, shots.get(7).getY(), 1);
		assertEquals(Color.RED, shots.get(7).getColor());

		// Dupe of shot above
		assertEquals(485.0, shots.get(8).getX(), 1);
		assertEquals(231.5, shots.get(8).getY(), 1);
		assertEquals(Color.RED, shots.get(8).getColor());

		assertEquals(476.5, shots.get(9).getX(), 1);
		assertEquals(312.5, shots.get(9).getY(), 1);
		assertEquals(Color.RED, shots.get(9).getColor());

		assertEquals(337.5, shots.get(10).getX(), 1);
		assertEquals(273.0, shots.get(10).getY(), 1);
		assertEquals(Color.RED, shots.get(10).getColor());

		assertEquals(215.0, shots.get(11).getX(), 1);
		assertEquals(301.0, shots.get(11).getY(), 1);
		assertEquals(Color.RED, shots.get(11).getColor());
	}
	
	@Test
	public void testPS3EyeHardwareDefaultsGreenLaserRoomLightOffSafari() {
		List<Shot> shots = findShots("/shotsearcher/ps3eye_hardware_defaults_safari_green_laser_lights_off.mp4");
		
		// Misses all three shots on the sky at the top and the middle water shot on the bottom
		// these are basically non-starters with a fully bright projection like this.
			
		assertEquals(6, shots.size());
		
		assertEquals(472.5, shots.get(0).getX(), 1);
		assertEquals(64.0, shots.get(0).getY(), 1);
		assertEquals(Color.GREEN, shots.get(0).getColor());

		assertEquals(200.0, shots.get(1).getX(), 1);
		assertEquals(234.0, shots.get(1).getY(), 1);
		assertEquals(Color.GREEN, shots.get(1).getColor());

		assertEquals(376.0, shots.get(2).getX(), 1);
		assertEquals(228.0, shots.get(2).getY(), 1);
		assertEquals(Color.GREEN, shots.get(2).getColor());

		assertEquals(498.0, shots.get(3).getX(), 1);
		assertEquals(224.5, shots.get(3).getY(), 1);
		assertEquals(Color.GREEN, shots.get(3).getColor());

		assertEquals(479.0, shots.get(4).getX(), 1);
		assertEquals(281.0, shots.get(4).getY(), 1);
		assertEquals(Color.GREEN, shots.get(4).getColor());

		assertEquals(207.0, shots.get(5).getX(), 1);
		assertEquals(281.0, shots.get(5).getY(), 1);
		assertEquals(Color.GREEN, shots.get(5).getColor());
	}
}