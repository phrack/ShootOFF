package com.shootoff.camera;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.processors.ShotProcessor;
import com.shootoff.camera.processors.VirtualMagazineProcessor;
import com.shootoff.camera.shot.Shot;
import com.shootoff.camera.shot.ShotColor;
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

		VirtualMagazineProcessor magazineProcessor = null;

		for (ShotProcessor s : config.getShotProcessors()) {
			if (s instanceof VirtualMagazineProcessor) magazineProcessor = (VirtualMagazineProcessor) s;
		}

		magazineProcessor.setUseTTS(false);

		Shot shot = new Shot(ShotColor.GREEN, 0, 0, 0, 0);

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

	@Test
	public void testReset() {
		final int expectedCapacity = 4;

		config.setVirtualMagazineCapacity(expectedCapacity);

		VirtualMagazineProcessor magazineProcessor = null;

		for (ShotProcessor s : config.getShotProcessors()) {
			if (s instanceof VirtualMagazineProcessor) magazineProcessor = (VirtualMagazineProcessor) s;
		}

		magazineProcessor.setUseTTS(false);

		Shot shot = new Shot(ShotColor.GREEN, 0, 0, 0, 0);

		magazineProcessor.processShot(shot);
		magazineProcessor.processShot(shot);

		assertEquals(2, magazineProcessor.getRountCount());

		magazineProcessor.reset();

		assertEquals(expectedCapacity, magazineProcessor.getRountCount());
	}
}
