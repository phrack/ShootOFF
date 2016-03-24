package com.shootoff.gui.controller;

import static org.junit.Assert.*;

import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import com.shootoff.gui.controller.PluginManagerController.PluginMetadata;

public class TestPluginManagerController {
	@Test
	public void testGetPluginMetadataXMLValidXML() {
		PluginManagerController pmc = new PluginManagerController();

		String validXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<plugins>\n"
				+ "  <plugin name=\"Pistol Isometrics\" version=\"1.0\" minShootOFFVersion=\"3.7\" maxShootOFFVersion=\"4.0\"\n"
				+ "    creator=\"phrack\" download=\"https://github.com/phrack/ShootOFF-Pistol-Isometrics/releases/download/v1.0-FINAL/ShootOFF-Pistol-Isometrics.jar\"\n"
				+ "    description=\"Walk through a series of shoot and hold exercises to strenghten muscles activated when accurately shooting a firearm.\" />'n"
				+ "  <plugin name=\"Something Cool\" version=\"9.0\" minShootOFFVersion=\"4.0\" maxShootOFFVersion=\"5.0\"\n"
				+ "    creator=\"someone\" download=\"https://example.com/plugin.jar\"\n"
				+ "    description=\"This does something neat.\" />'n" + "</plugins>";

		Set<PluginMetadata> pluginMetadata = pmc.parsePluginMetadata(validXML);

		assertEquals(2, pluginMetadata.size());

		for (PluginMetadata pm : pluginMetadata) {
			if ("phrack".equals(pm.getCreator())) {
				assertEquals("Pistol Isometrics", pm.getName());
				assertEquals("1.0", pm.getVersion());
				assertEquals("3.7", pm.getMinShootOFFVersion());
				assertEquals("4.0", pm.getMaxShootOFFVersion());
				assertEquals("phrack", pm.getCreator());
				assertEquals(
						"https://github.com/phrack/ShootOFF-Pistol-Isometrics/releases/download/v1.0-FINAL/ShootOFF-Pistol-Isometrics.jar",
						pm.getDownload());
				assertEquals(
						"Walk through a series of shoot and hold exercises to strenghten muscles activated when accurately shooting a firearm.",
						pm.getDescription());
			} else {
				assertEquals("Something Cool", pm.getName());
				assertEquals("9.0", pm.getVersion());
				assertEquals("4.0", pm.getMinShootOFFVersion());
				assertEquals("5.0", pm.getMaxShootOFFVersion());
				assertEquals("someone", pm.getCreator());
				assertEquals("https://example.com/plugin.jar", pm.getDownload());
				assertEquals("This does something neat.", pm.getDescription());
			}
		}
	}

	@Test
	public void testGetPluginMetadataXMLEmptyXML() {
		PluginManagerController pmc = new PluginManagerController();

		Set<PluginMetadata> pluginMetadata = pmc.parsePluginMetadata("");

		assertEquals(0, pluginMetadata.size());
	}

	@Test
	public void testIsPluginCompatibleTrue() {
		PluginManagerController pmc = new PluginManagerController();

		assertTrue(pmc.isPluginCompatible(Optional.of("3.0"), "2.9", "3.1"));
		assertTrue(pmc.isPluginCompatible(Optional.of("2.9"), "2.9", "3.1"));
		assertTrue(pmc.isPluginCompatible(Optional.of("2.10"), "2.9", "3.1"));
		assertTrue(pmc.isPluginCompatible(Optional.of("2.89"), "2.9", "3.1"));
		assertTrue(pmc.isPluginCompatible(Optional.of("3.1"), "2.9", "3.1"));

		assertTrue(pmc.isPluginCompatible(Optional.of("3"), "2.9", "3.1"));
	}

	@Test
	public void testIsPluginCompatibleFalse() {
		PluginManagerController pmc = new PluginManagerController();

		assertFalse(pmc.isPluginCompatible(Optional.empty(), "2.9", "3.1"));
		assertFalse(pmc.isPluginCompatible(Optional.of("4.0"), "2.9", "3.1"));
		assertFalse(pmc.isPluginCompatible(Optional.of("3.2"), "2.9", "3.1"));
		assertFalse(pmc.isPluginCompatible(Optional.of("2.8"), "2.9", "3.1"));
	}
}
