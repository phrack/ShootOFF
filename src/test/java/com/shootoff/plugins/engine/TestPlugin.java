package com.shootoff.plugins.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.shootoff.gui.JavaFXThreadingRule;

public class TestPlugin {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private Path pluginDir;

	@Before
	public void setUp() {
		pluginDir = Paths.get(System.getProperty("user.dir") + File.separator + "src" + File.separator + "test"
				+ File.separator + "exercises");
	}

	@Test
	public void testStandardPlugin() throws ParserConfigurationException, SAXException, IOException {
		Plugin p = new Plugin(pluginDir.resolve(Paths.get("ShotScore.jar")));
		assertEquals("com.shootoff.plugins.ShotScore", p.getExercise().getClass().getName());
		assertEquals("Shot Score", p.getExercise().getInfo().getName());
		assertEquals(PluginType.STANDARD, p.getType());
	}

	@Test
	public void testProjectorPlugin() throws ParserConfigurationException, SAXException, IOException {
		Plugin p = new Plugin(pluginDir.resolve(Paths.get("SteelContest.jar")));
		assertEquals("com.shootoff.plugins.SteelContest", p.getExercise().getClass().getName());
		assertEquals("Steel Contest", p.getExercise().getInfo().getName());
		assertEquals(PluginType.PROJECTOR_ONLY, p.getType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownPlugin() throws ParserConfigurationException, SAXException, IOException {
		@SuppressWarnings("unused")
		Plugin p = new Plugin(pluginDir.resolve(Paths.get("Unknown.jar")));
	}
}
