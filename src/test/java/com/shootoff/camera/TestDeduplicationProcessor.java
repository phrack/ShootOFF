package com.shootoff.camera;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;

import javafx.scene.paint.Color;

public class TestDeduplicationProcessor {
	private Configuration config;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
	}
	
	@Test 
	public void testReset() {
		DeduplicationProcessor deduplicationProcessor = null;
		
		for (ShotProcessor s : config.getShotProcessors()) {
			if (s instanceof DeduplicationProcessor) deduplicationProcessor = (DeduplicationProcessor)s;
		}
		
		assertFalse(deduplicationProcessor.getLastShot().isPresent());
		
		Shot shot = new Shot(Color.GREEN, 0, 0, 0, 0);
		
		deduplicationProcessor.processShot(shot);
		
		assertTrue(deduplicationProcessor.getLastShot().isPresent());
		
		deduplicationProcessor.reset();
		
		assertFalse(deduplicationProcessor.getLastShot().isPresent());
	}
}
