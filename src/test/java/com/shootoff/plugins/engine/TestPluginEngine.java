package com.shootoff.plugins.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.plugins.TrainingExercise;

public class TestPluginEngine {
	private String pluginsPath;
	private PluginEngine pe;

	@Before
	public void setUp() throws IOException {
		pluginsPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator
				+ "exercises";
		System.setProperty("shootoff.plugins", pluginsPath);

		pe = new PluginEngine(new PluginListener() {
			@Override
			public void registerExercise(TrainingExercise exercise) {}

			@Override
			public void registerProjectorExercise(TrainingExercise exercise) {}

			@Override
			public void unregisterExercise(TrainingExercise exercise) {}
		});
	}

	@Test
	public void testExistingPlugins() {
		assertEquals(2, pe.getPlugins().size());
	}
}
