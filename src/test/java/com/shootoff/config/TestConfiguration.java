package com.shootoff.config;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class TestConfiguration {
	Configuration defaultConfig;
	
	@Before
	public void setUp() throws ConfigurationException, IOException {
		String[] emptyArgs = new String[0];
		defaultConfig = new Configuration(emptyArgs);
	}
		
	@Test
	public void testConfirmDefaults() {
		assertEquals(100, defaultConfig.getDetectionRate());
		assertEquals(230, defaultConfig.getLaserIntensity());
		assertEquals(2, defaultConfig.getMarkerRadius());
		assertEquals(false, defaultConfig.ignoreLaserColor());
		assertEquals("None", defaultConfig.getIgnoreLaserColorName());
		assertEquals(false, defaultConfig.useVirtualMagazine());
		assertEquals(7, defaultConfig.getVirtualMagazineCapacity());
		assertEquals(false, defaultConfig.useMalfunctions());
		assertTrue(defaultConfig.getMalfunctionsProbability() == 10.0);
		assertEquals(false, defaultConfig.inDebugMode());
	}

	@Test(expected=ConfigurationException.class)
	public void testDetectionRateBelowRange() throws ConfigurationException {
		defaultConfig.setDetectionRate(0);
		defaultConfig.validateConfiguration();		
	}

	@Test
	public void testDetectionRateInRange() {
		try {
			defaultConfig.setDetectionRate(1);
			defaultConfig.validateConfiguration();
			defaultConfig.setDetectionRate(50);
			defaultConfig.validateConfiguration();
			defaultConfig.setDetectionRate(1000);
			defaultConfig.validateConfiguration();
		} catch (ConfigurationException e) {
			fail("Detection rate values are in range but got ConfigurationException");
		}
	}
	
	@Test(expected=ConfigurationException.class)
	public void testLaserIntesityBelowRange() throws ConfigurationException {
		defaultConfig.setLaserIntensity(0);
		defaultConfig.validateConfiguration();		
	}

	@Test(expected=ConfigurationException.class)
	public void testLaserIntesityAboveRange() throws ConfigurationException {
		defaultConfig.setLaserIntensity(256);
		defaultConfig.validateConfiguration();		
	}
	
	@Test
	public void testLaserIntensityInRange() {
		try {
			defaultConfig.setLaserIntensity(1);
			defaultConfig.validateConfiguration();
			defaultConfig.setLaserIntensity(100);
			defaultConfig.validateConfiguration();
			defaultConfig.setLaserIntensity(255);
			defaultConfig.validateConfiguration();
		} catch (ConfigurationException e) {
			fail("Laser intensity values are in range but got ConfigurationException");
		}
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
			defaultConfig.setIgnoreLaserColorName("green");
			defaultConfig.validateConfiguration();
		} catch (ConfigurationException e) {
			fail("Ignore laser color values are correct but got ConfigurationException");
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
				getClass().getResourceAsStream("/test.properties"),
				"test.properties");

		assertEquals(40, config.getDetectionRate());
		assertEquals(120, config.getLaserIntensity());
		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertTrue(config.getMalfunctionsProbability() == (float)43.15);
		assertEquals(false, config.inDebugMode());
	}
	
	@Test
	public void testReadConfigFileCmdLineOverride() throws IOException, ConfigurationException {
		Configuration config = new Configuration(
				getClass().getResourceAsStream("/test.properties"),
				"test.properties",
				new String[] {"-i", "20"});

		assertEquals(40, config.getDetectionRate());
		assertEquals(20, config.getLaserIntensity());
		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertTrue(config.getMalfunctionsProbability() == (float)43.15);
		assertEquals(false, config.inDebugMode());
	}
	
	@Test
	public void testReadCmdLineShort() throws IOException, ConfigurationException {
		Configuration config = new Configuration(new String[]{
				"-d", "-r", "40", "-i", "120", "-m", "4", "-c", "green",
				"-u", "25", "-f", "43.15" 
			});
		
		assertEquals(40, config.getDetectionRate());
		assertEquals(120, config.getLaserIntensity());
		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertTrue(config.getMalfunctionsProbability() == (float)43.15);
		assertEquals(true, config.inDebugMode());
	}
	
	@Test
	public void testReadCmdLineLong() throws IOException, ConfigurationException {
		Configuration config = new Configuration(new String[]{
				"--debug", "--detection-rate", "40", 
				"--laser-intensity", "120", "--marker-radius", "4", 
				"--ignore-laser-color", "green",
				"--use-virtual-magazine", "25", 
				"--use-malfunctions", "43.15" 
			});				
		
		assertEquals(40, config.getDetectionRate());
		assertEquals(120, config.getLaserIntensity());
		assertEquals(4, config.getMarkerRadius());
		assertEquals(true, config.ignoreLaserColor());
		assertEquals("green", config.getIgnoreLaserColorName());
		assertEquals(true, config.useVirtualMagazine());
		assertEquals(25, config.getVirtualMagazineCapacity());
		assertEquals(true, config.useMalfunctions());
		assertTrue(config.getMalfunctionsProbability() == (float)43.15);
		assertEquals(true, config.inDebugMode());
	}
}
