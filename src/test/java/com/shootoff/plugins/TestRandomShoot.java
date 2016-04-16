/*
 * Copyright (C) 2016 phrack. All rights reserved.
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

import javafx.scene.Group;
import javafx.scene.paint.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.Shot;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.TargetView;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

public class TestRandomShoot {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private Random rng;

	@Before
	public void setUp() throws UnsupportedEncodingException {
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);
		rng = new Random(15); // Changing this seed will cause tests to fail
	}

	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);
	}

	@Test
	public void testNoTarget() throws IOException {
		List<Target> targets = new ArrayList<Target>();

		RandomShoot rs = new RandomShoot(targets, rng);

		assertEquals(String.format("sounds/voice/shootoff-subtargets-warning.wav%n"),
				stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();

		rs.reset(targets);

		assertEquals(String.format("sounds/voice/shootoff-subtargets-warning.wav%n"),
				stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
	}

	@Test
	public void testFiveSmallTarget() throws IOException {
		List<Target> targets = new ArrayList<Target>();
		Group bullseyeFiveGroup = TargetIO
				.loadTarget(new File("targets" + File.separator + "SimpleBullseye_five_small.target")).get();
		TargetView bullseyeFiveTarget = new TargetView(bullseyeFiveGroup, new ArrayList<Target>());
		targets.add(bullseyeFiveTarget);

		RandomShoot rs = new RandomShoot(targets, rng);

		// Make sure initial state makes sense

		assertEquals(5, rs.getSubtargets().size());

		assertTrue(rs.getSubtargets().contains("1"));
		assertTrue(rs.getSubtargets().contains("2"));
		assertTrue(rs.getSubtargets().contains("3"));
		assertTrue(rs.getSubtargets().contains("4"));
		assertTrue(rs.getSubtargets().contains("5"));

		String firstSubtarget = rs.getSubtargets().get(rs.getCurrentSubtargets().peek());

		assertEquals("sounds/voice/shootoff-shoot.wav",
				stringOut.toString("UTF-8").replace(File.separatorChar, '/').split(String.format("%n"))[0]);
		stringOut.reset();

		// Simulate missing a shot

		rs.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.empty());

		assertEquals(String.format("sounds/voice/shootoff-shoot.wav%nsounds/voice/shootoff-%s.wav%n", firstSubtarget),
				stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();

		// Simulate a hit

		TargetRegion expectedRegion = null;

		for (TargetRegion r : targets.get(0).getRegions()) {
			expectedRegion = r;

			if (expectedRegion.getTag("subtarget").equals(firstSubtarget)) break;
		}

		int oldSize = rs.getCurrentSubtargets().size();
		Hit expectedHit = new Hit(bullseyeFiveTarget, expectedRegion, 0, 0);

		rs.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(expectedHit));

		if (oldSize > 1) {
			assertEquals(oldSize - 1, rs.getCurrentSubtargets().size());
		} else {
			String nextSubtarget = rs.getSubtargets().get(rs.getCurrentSubtargets().peek());
			assertTrue(stringOut.toString("UTF-8").startsWith("shoot subtarget " + nextSubtarget));
			stringOut.reset();
		}
	}

	@Test
	public void testNoSoundFilesForSubtargetNames() throws IOException {
		List<Target> targets = new ArrayList<Target>();
		TargetView missingSoundTarget = new TargetView(TargetIO
				.loadTarget(new File(TestRandomShoot.class.getResource("/test_missing_sound_files.target").getFile()))
				.get(), targets);
		targets.add(missingSoundTarget);

		RandomShoot rs = new RandomShoot(targets, rng);

		// Make sure initial state makes sense

		assertEquals(5, rs.getSubtargets().size());

		String firstSubtarget = rs.getSubtargets().get(rs.getCurrentSubtargets().peek());

		assertEquals("shoot subtarget undefined_region_name_5 then undefined_region_name_3",
				stringOut.toString("UTF-8").replace(String.format("%n"), ""));
		stringOut.reset();

		// Simulate missing a shot

		rs.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.empty());

		assertEquals(String.format("shoot %s%n", firstSubtarget), stringOut.toString("UTF-8"));
		stringOut.reset();
	}
}
