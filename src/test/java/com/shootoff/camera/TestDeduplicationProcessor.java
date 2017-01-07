package com.shootoff.camera;

import static org.junit.Assert.*;

import org.junit.Test;

import com.shootoff.camera.ShotColor;
import com.shootoff.camera.processors.DeduplicationProcessor;
import com.shootoff.config.ConfigurationException;

public class TestDeduplicationProcessor {

	@Test
	public void testReset() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();

		DeduplicationProcessor deduplicationProcessor = new DeduplicationProcessor(new MockCameraManager());

		assertFalse(deduplicationProcessor.getLastShot().isPresent());

		Shot shot = new Shot(ShotColor.GREEN, 0, 0, 0, 0);

		deduplicationProcessor.processShot(shot);

		assertTrue(deduplicationProcessor.getLastShot().isPresent());

		deduplicationProcessor.reset();

		assertFalse(deduplicationProcessor.getLastShot().isPresent());
	}
}
