package com.shootoff.camera;

import static org.junit.Assert.*;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
public class TestVirtualMagazineProcessor {
	private Configuration config;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setUseVirtualMagazine(true);
	}
	
	@Test
	public void testThreeShotCapacity() {
		config.setVirtualMagazineCapacity(3);
		
		VirtualMagazineProcessor magazineProcessor = 
				(VirtualMagazineProcessor)config.getShotProcessors().toArray()[0];
		
		Shot shot = new Shot(Color.GREEN, 0, 0, 0, 0);
		
		// Simulate shooting through two magazine
		
		assertTrue(magazineProcessor.processShot(shot));
		assertTrue(magazineProcessor.processShot(shot));
		assertTrue(magazineProcessor.processShot(shot));
		assertFalse(magazineProcessor.processShot(shot));
		
		assertTrue(magazineProcessor.processShot(shot));
		assertTrue(magazineProcessor.processShot(shot));
		assertTrue(magazineProcessor.processShot(shot));
		assertFalse(magazineProcessor.processShot(shot));
	}
}
