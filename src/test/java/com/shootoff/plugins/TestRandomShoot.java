/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

public class TestRandomShoot {
	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private Random rng;
	
	@Before
	public void setUp() throws UnsupportedEncodingException {
		new JFXPanel(); // Initialize the JFX toolkit
		
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);
		rng = new Random(15);
	}
	
	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);
	}

	@Test
	public void testNoTarget() throws IOException {
		List<Group> targets = new ArrayList<Group>();
		
		RandomShoot rs = new RandomShoot(targets, rng);

		assertEquals("sounds/voice/shootoff-subtargets-warning.wav\n", stringOut.toString("UTF-8").replace("\r\n", "\n").replace('/', File.separatorChar));
		stringOut.reset();
		
		rs.reset(targets);
		
		assertEquals("sounds/voice/shootoff-subtargets-warning.wav\n", stringOut.toString("UTF-8").replace("\r\n", "\n").replace('/', File.separatorChar));
	}

	@Test
	public void testFiveSmallTarget() throws IOException {
		List<Group> targets = new ArrayList<Group>();
		targets.add(TargetIO.loadTarget(new File("targets" + File.separator + 
				"SimpleBullseye_five_small.target")).get());
		
		RandomShoot rs = new RandomShoot(targets, rng);
		
		// Make sure initial state makes sense
		
		assertEquals(5, rs.getSubtargets().size());
		
		assertTrue(rs.getSubtargets().contains("1"));
		assertTrue(rs.getSubtargets().contains("2"));
		assertTrue(rs.getSubtargets().contains("3"));
		assertTrue(rs.getSubtargets().contains("4"));
		assertTrue(rs.getSubtargets().contains("5"));
		
		String firstSubtarget = rs.getSubtargets().get(rs.getCurrentSubtargets().peek());
		
		assertEquals("sounds/voice/shootoff-shoot.wav", stringOut.toString("UTF-8").split("\n")[0]);
		stringOut.reset();
		
		// Simulate missing a shot
		
		rs.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.empty());
		
		assertEquals(String.format("sounds/voice/shootoff-shoot.wav%nsounds/voice/shootoff-%s.wav%n", firstSubtarget),
				stringOut.toString("UTF-8").replace('/', File.separatorChar));
		stringOut.reset();
		
		// Simulate a hit
		
		TargetRegion expectedRegion = null;
		
		for (Node node : targets.get(0).getChildren()) {
			expectedRegion = (TargetRegion)node;
			
			if (expectedRegion.getTag("subtarget").equals(firstSubtarget)) break;
		}
		
		int oldSize = rs.getCurrentSubtargets().size();
		
		rs.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(expectedRegion));
		
		if (oldSize > 1) {
			assertEquals(oldSize - 1, rs.getCurrentSubtargets().size());
		} else {
			String nextSubtarget = rs.getSubtargets().get(rs.getCurrentSubtargets().peek());
			assertTrue(stringOut.toString("UTF-8").startsWith("shoot subtarget " + nextSubtarget));
			stringOut.reset();
		}
	}	
}
