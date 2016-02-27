package com.shootoff;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

public class TestJWS {
	@Test
	public void testResourcesMetadata() {
		Main main = new Main();

		String metadataXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<resources version=\"1.1\" fileSize=\"400319\" />";

		Optional<Main.ResourcesInfo> ri = main.deserializeMetadataXML(metadataXML);

		assertTrue(ri.isPresent());

		assertEquals("1.1", ri.get().getVersion());
		assertEquals(400319, ri.get().getFileSize());
		assertEquals(metadataXML, ri.get().getXML());
	}
}
