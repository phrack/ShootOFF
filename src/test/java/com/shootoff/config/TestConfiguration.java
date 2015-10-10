package com.shootoff.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

public class TestConfiguration {
	Configuration defaultConfig;
	
	@Before
	public void setUp() throws ConfigurationException, IOException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		String[] emptyArgs = new String[0];
		defaultConfig = new Configuration(emptyArgs);
	}
		
	@Test
	public void testConfirmDefaults() {
		assertEquals(0, defaultConfig.getWebcams().size());
		assertEquals(4, defaultConfig.getMarkerRadius());
		assertEquals(false, defaultConfig.ignoreLaserColor());
		assertEquals("None", defaultConfig.getIgnoreLaserColorName());
		assertEquals(false, defaultConfig.useRedLaserSound());
		assertEquals(System.getProperty("user.dir") + File.separator + "sounds/walther_ppq.wav", defaultConfig.getRedLaserSound().getPath());
		assertEquals(false, defaultConfig.useGreenLaserSound());
		assertEquals(System.getProperty("user.dir") + File.separator + "sounds/walther_ppq.wav", defaultConfig.getGreenLaserSound().getPath());
		assertEquals(false, defaultConfig.useVirtualMagazine());
		assertEquals(7, defaultConfig.getVirtualMagazineCapacity());
		assertEquals(false, defaultConfig.useMalfunctions());
		assertTrue(defaultConfig.getMalfunctionsProbability() == 10.0);
		assertEquals(false, defaultConfig.inDebugMode());
	}
	
	@Test(expected=ConfigurationException.class)
	public void testMarkerRadiusBelowRange() throws ConfigurationException {
		defaultConfig.setMarkerRadius(0);
		defaultConfig.validateConfiguration();		
	}

	@Test(expected=ConfigurationException.class)
	public void testMarkerRadiusAboveRange() throws ConfigurationException {
		defaultConfig.setMarkerRadius(21);
		defaultConfig.validateConfiguration();		
	}
	
	@Test
	public void testMarkerRadiusInRange() {
		try {
			defaultConfig.setMarkerRadius(1);
			defaultConfig.validateConfiguration();
			defaultConfig.setMarkerRadius(10);
			defaultConfig.validateConfiguration();
			defaultConfig.setMarkerRadius(20);
			defaultConfig.validateConfiguration();
		} catch (ConfigurationException e) {
			fail("Marker radius values are in range but got ConfigurationException");
		}
	}
	
	@Test(expected=ConfigurationException.class)
	public void testIgnoreLaserColorInvalid() throws ConfigurationException {
		defaultConfig.setIgnoreLaserColor(true);
		defaultConfig.setIgnoreLaserColorName("purple");
		defaultConfig.validateConfiguration();		
	}

	@Test
	public void testIgnoreLaserColorValid() {
		try {
			defaultConfig.setIgnoreLaserColor(true);
			
			defaultConfig.setIgnoreLaserColorName("red");
			defaultConfig.validateConfiguration();
			assertEquals(Color.RED, defaultConfig.getIgnoreLaserColor().get());
			defaultConfig.setIgnoreLaserColorName("green");
			defaultConfig.validateConfiguration();
			assertEquals(Color.GREEN, defaultConfig.getIgnoreLaserColor().get());
		} catch (ConfigurationException e) {
			fail("Ignore laser color values are correct but got ConfigurationException");
		}
	}
	
	@Test(expected=ConfigurationException.class)
	public void testRedLaserSoundInvalid() throws ConfigurationException {
		defaultConfig.setUseRedLaserSound(true);
		defaultConfig.setRedLaserSound(new File("sounds/some_crazy_sound.wav"));
		defaultConfig.validateConfiguration();		
	}

	@Test
	public void testRedLaserSoundValid() {
		try {
			defaultConfig.setUseRedLaserSound(true);
			
			defaultConfig.setRedLaserSound(new File("sounds/walther_ppq.wav"));
			defaultConfig.validateConfiguration();	
		} catch (ConfigurationException e) {
			fail("Red laser sound values are correct but got ConfigurationException");
		}
	}
	
	@Test(expected=ConfigurationException.class)
	public void testGreenLaserSoundInvalid() throws ConfigurationException {
		defaultConfig.setUseGreenLaserSound(true);
		defaultConfig.setGreenLaserSound(new File("sounds/some_crazy_sound.wav"));
		defaultConfig.validateConfiguration();		
	}

	@Test
	public void testGreenLaserSoundValid() {
		try {
			defaultConfig.setUseGreenLaserSound(true);
			
			defaultConfig.setGreenLaserSound(new File("sounds/walther_ppq.wav"));
			defaultConfig.validateConfiguration();	
		} catch (ConfigurationException e) {
			fail("Red laser sound values are correct but got ConfigurationException");
		}
	}
	
	@Test(expected=ConfigurationException.class)
	public void testVirtualMagazineCapacityBelowRange() throws ConfigurationException {
		defaultConfig.setVirtualMagazineCapacity(0);
		defaultConfig.validateConfiguration();		
	}

	@Test(expected=ConfigurationException.class)
	public void testVirtualMagazineCapacityAboveRange() throws ConfigurationException {
		defaultConfig.setVirtualMagazineCapacity(46);
		defaultConfig.validateConfiguration();		
	}
	
	@Test
	public void testVirtualMagazineCapacityInRange() {
		try {
			defaultConfig.setVirtualMagazineCapacity(1);
			defaultConfig.validateConfiguration();
			defaultConfig.setVirtualMagazineCapacity(10);
			defaultConfig.validateConfiguration();
			defaultConfig.setVirtualMagazineCapacity(45);
			defaultConfig.validateConfiguration();
		} catch (ConfigurationException e) {
			fail("Virtual magazine values are in range but got ConfigurationException");
		}
	}
	
	@Test(expected=ConfigurationException.class)
	public void testMalfunctionsProbabilityBelowRange() throws ConfigurationException {
		defaultConfig.setMalfunctionsProbability((float)0.09);
		defaultConfig.validateConfiguration();		
	}

	@Test(expected=ConfigurationException.class)
	public void testMalfunctionsProbabilityAboveRange() throws ConfigurationException {
		defaultConfig.setMalfunctionsProbability((float)99.91);
		defaultConfig.validateConfiguration();		
	}
	
	@Test
	public void testMalfunctionsProbabilityInRange() {
		try {
			defaultConfig.setMalfunctionsProbability((float)0.1);
			defaultConfig.validateConfiguration();
			defaultConfig.setMalfunctionsProbability((float)50.5);
			defaultConfig.validateConfiguration();
			defaultConfig.setMalfunctionsProbability((float)99.9);
			defaultConfig.validateConfiguration();
		} catch (ConfigurationException e) {
			fail("Malfunction probability values are in range but got ConfigurationException");
		}
	}
	
	@Test
	public void testReadConfigFile() throws IOException, ConfigurationException {
		Configuration config = new Configuration(
				TestConfiguration.class.getResourceAsStream("/test.properties"),
				"test.properties");

		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useRedLaserSound());
		assertEquals(System.getProperty("user.dir") + File.separator + "sounds/steel_sound_1.wav", config.getRedLaserSound().getPath());
		assertEquals(false, config.useGreenLaserSound());
		assertEquals(System.getProperty("user.dir") + File.separator + "sounds/beep.wav", config.getGreenLaserSound().getPath());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertEquals(43.15f, config.getMalfunctionsProbability(), 0.5);
		assertEquals(false, config.inDebugMode());
	}
	
	@Test
	public void testReadConfigFileCmdLineOverride() throws IOException, ConfigurationException {
		Configuration config = new Configuration(
				TestConfiguration.class.getResourceAsStream("/test.properties"),
				"test.properties",
				new String[] {"-m", "6"});
		
		assertEquals(6, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertEquals(43.15f, config.getMalfunctionsProbability(), 0.5);
		assertEquals(false, config.inDebugMode());
	}
	
	@Test
	public void testReadCmdLineShort() throws IOException, ConfigurationException {
		Configuration config = new Configuration(new String[]{
				"-d", "-m", "4", "-c", "green",
				"-u", "25", "-f", "43.15" 
			});
		
		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertEquals(43.15f, config.getMalfunctionsProbability(), 0.5);
		assertEquals(true, config.inDebugMode());
	}
	
	@Test
	public void testReadCmdLineLong() throws IOException, ConfigurationException {
		Configuration config = new Configuration(new String[]{
				"--debug", "--marker-radius", "4", 
				"--ignore-laser-color", "green",
				"--use-virtual-magazine", "25", 
				"--use-malfunctions", "43.15" 
			});				

		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertEquals(43.15f, config.getMalfunctionsProbability(), 0.5);
		assertEquals(true, config.inDebugMode());
	}
	
	@Test
	public void testWriteConfigFile() throws IOException, ConfigurationException {
		File props = new File("test_write.properties");
		if (!props.createNewFile()) {
			System.err.println("Can't create test config file: " + props.getPath());
		}
		
		Configuration writtenConfig = new Configuration(props.getPath(), new String[]{
				"--marker-radius", "4", 
				"--ignore-laser-color", "green",
				"--use-virtual-magazine", "25", 
				"--use-malfunctions", "43.15" 
			});
		
		writtenConfig.writeConfigurationFile();
		
		Configuration readConfig = new Configuration(props.getPath());
		
		assertEquals(writtenConfig.getMarkerRadius(), readConfig.getMarkerRadius());
		assertEquals(Color.GREEN, writtenConfig.getIgnoreLaserColor().get());
		assertEquals(writtenConfig.getIgnoreLaserColorName(), readConfig.getIgnoreLaserColorName());
		assertEquals(writtenConfig.useVirtualMagazine(), readConfig.useVirtualMagazine());
		assertEquals(25, writtenConfig.getVirtualMagazineCapacity());
		assertEquals(writtenConfig.getVirtualMagazineCapacity(), readConfig.getVirtualMagazineCapacity());
		assertEquals(writtenConfig.useMalfunctions(), readConfig.useMalfunctions());
		assertEquals(43.15f, writtenConfig.getMalfunctionsProbability(), 0.5f);
		
		if (!props.delete()) {
			System.err.println("Can't delete test config file: " + props.getPath());
		}
	}
}
