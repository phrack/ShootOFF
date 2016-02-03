package com.shootoff.camera;

import static org.junit.Assert.*;

import org.junit.Test;



import com.shootoff.config.ConfigurationException;

import javafx.scene.paint.Color;

public class TestDeduplicationProcessor {

	@Test 
	public void testReset() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();
		
		DeduplicationProcessor deduplicationProcessor = new DeduplicationProcessor(new MockCameraManager());
				
		assertFalse(deduplicationProcessor.getLastShot().isPresent());
		
		Shot shot = new Shot(Color.GREEN, 0, 0, 0, 0);
		
		deduplicationProcessor.processShot(shot);
		
		assertTrue(deduplicationProcessor.getLastShot().isPresent());
		
		deduplicationProcessor.reset();
		
		assertFalse(deduplicationProcessor.getLastShot().isPresent());
	}
}
