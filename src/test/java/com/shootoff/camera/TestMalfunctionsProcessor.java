package com.shootoff.camera;

import static org.junit.Assert.*;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;

public class TestMalfunctionsProcessor {
	private Configuration config;
	private Shot shot;

	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setMalfunctions(true);

		shot = new Shot(Color.GREEN, 0, 0, 0, 0);

		MalfunctionsProcessor.setUseTTS(false);
	}

	private int simulateShots(MalfunctionsProcessor malfunctionsProcessor) {
		// Simulate shooting many times
		int malfunctionCount = 0;
		for (int i = 0; i < 100; i++) {
			if (!malfunctionsProcessor.processShot(shot)) malfunctionCount++;
		}

		return malfunctionCount;
	}

	@Test
	public void testManyMalfunctions() {
		config.setMalfunctionsProbability((float) 90.0);

		MalfunctionsProcessor malfunctionsProcessor = null;

		for (ShotProcessor s : config.getShotProcessors()) {
			if (s instanceof MalfunctionsProcessor) malfunctionsProcessor = (MalfunctionsProcessor) s;
		}

		assertTrue(simulateShots(malfunctionsProcessor) >= 80);
	}

	@Test
	public void testFewMalfunctions() {
		config.setMalfunctionsProbability((float) 10.0);

		MalfunctionsProcessor malfunctionsProcessor = null;

		for (ShotProcessor s : config.getShotProcessors()) {
			if (s instanceof MalfunctionsProcessor) malfunctionsProcessor = (MalfunctionsProcessor) s;
		}

		assertTrue(simulateShots(malfunctionsProcessor) <= 20);
	}
}
