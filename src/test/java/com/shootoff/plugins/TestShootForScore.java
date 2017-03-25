package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotColor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.targets.TargetView;
import com.shootoff.targets.Hit;
import com.shootoff.targets.MockHit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

public class TestShootForScore {
	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private List<Target> targets;
	private Hit tenRegionHit;
	private Hit fiveRegionHit;
	private ShootForScore sfs;

	@Before
	public void setUp() throws ConfigurationException, UnsupportedEncodingException {
		new JFXPanel(); // Initialize the JFX toolkit

		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		originalOut = System.out;
		System.setOut(stringOutStream);

		targets = new ArrayList<Target>();
		TargetComponents tc = TargetIO.loadTarget(new File("targets" + File.separator + "SimpleBullseye_score.target"))
				.get();
		TargetView bullseyeScoreTarget = new TargetView(tc.getTargetGroup(), tc.getTargetTags(), new ArrayList<Target>());
		targets.add(bullseyeScoreTarget);

		for (Node node : tc.getTargetGroup().getChildren()) {
			TargetRegion region = (TargetRegion) node;

			if (region.tagExists("points") && region.getTag("points").equals("10")) {
				tenRegionHit = new MockHit(bullseyeScoreTarget, region, 0, 0);
			} else if (region.tagExists("points") && region.getTag("points").equals("5")) {
				fiveRegionHit = new MockHit(bullseyeScoreTarget, region, 0, 0);
			}
		}

		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		sfs = new ShootForScore();
		sfs.init(config, new CamerasSupervisor(config), null, null);
	}

	@After
	public void tearDown() {
		System.setOut(originalOut);
	}

	@Test
	public void testReset() throws UnsupportedEncodingException {
		sfs.reset(targets);
		assertEquals("score: 0\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
	}

	@Test
	public void testJustRed() throws UnsupportedEncodingException {
		// Miss
		sfs.shotListener(new Shot(ShotColor.RED, 0, 0, 0, 2), Optional.empty());
		assertEquals("", stringOut.toString("UTF-8"));
		stringOut.reset();

		// Hit ten
		sfs.shotListener(new Shot(ShotColor.RED, 0, 0, 0, 2), Optional.of(tenRegionHit));
		assertEquals("red score: 10\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		// Hit five
		sfs.shotListener(new Shot(ShotColor.RED, 0, 0, 0, 2), Optional.of(fiveRegionHit));
		assertEquals("red score: 15\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		assertEquals(15, sfs.getRedScore());
		assertEquals(0, sfs.getGreenScore());

		sfs.reset(targets);
		assertEquals("score: 0\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		assertEquals(0, sfs.getRedScore());
		assertEquals(0, sfs.getGreenScore());
	}

	@Test
	public void testJustGreen() throws UnsupportedEncodingException {
		// Miss
		sfs.shotListener(new Shot(ShotColor.GREEN, 0, 0, 0, 2), Optional.empty());
		assertEquals("", stringOut.toString("UTF-8"));
		stringOut.reset();

		// Hit ten
		sfs.shotListener(new Shot(ShotColor.GREEN, 0, 0, 0, 2), Optional.of(tenRegionHit));
		assertEquals("green score: 10\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		// Hit five
		sfs.shotListener(new Shot(ShotColor.GREEN, 0, 0, 0, 2), Optional.of(fiveRegionHit));
		assertEquals("green score: 15\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		assertEquals(0, sfs.getRedScore());
		assertEquals(15, sfs.getGreenScore());

		sfs.reset(targets);
		assertEquals("score: 0\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		assertEquals(0, sfs.getRedScore());
		assertEquals(0, sfs.getGreenScore());
	}

	@Test
	public void testRedAndGreen() throws UnsupportedEncodingException {
		// Red hit ten
		sfs.shotListener(new Shot(ShotColor.RED, 0, 0, 0, 2), Optional.of(tenRegionHit));
		assertEquals("red score: 10\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		// Green hit five
		sfs.shotListener(new Shot(ShotColor.GREEN, 0, 0, 0, 2), Optional.of(fiveRegionHit));
		assertEquals("red score: 10\ngreen score: 5\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();

		assertEquals(10, sfs.getRedScore());
		assertEquals(5, sfs.getGreenScore());
	}
}
