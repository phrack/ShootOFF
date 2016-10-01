package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.TargetView;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

import ch.qos.logback.classic.Logger;

public class TestDuelingTree {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();
	
	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private List<Target> targets;
	private List<Hit> leftPaddlesHits;
	private List<Hit> rightPaddlesHits;
	private DuelingTree dt;

	@Before
	public void setUp() throws ConfigurationException, NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException, IOException {
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		System.setProperty("shootoff.home", System.getProperty("user.dir"));

		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);

		targets = new ArrayList<Target>();
		TargetComponents tc = TargetIO.loadTarget(new File("targets" + File.separator + "Duel_Tree.target")).get();
		TargetView duelTreeTarget = new TargetView(tc.getTargetGroup(), tc.getTargetTags(), new ArrayList<Target>());
		targets.add(duelTreeTarget);

		leftPaddlesHits = new ArrayList<Hit>();
		rightPaddlesHits = new ArrayList<Hit>();

		for (Node node : tc.getTargetGroup().getChildren()) {
			TargetRegion region = (TargetRegion) node;

			if (region.tagExists("subtarget") && region.getTag("subtarget").startsWith("left_paddle")) {
				leftPaddlesHits.add(new Hit(duelTreeTarget, region, 0, 0));
			} else if (region.tagExists("subtarget") && region.getTag("subtarget").startsWith("right_paddle")) {
				rightPaddlesHits.add(new Hit(duelTreeTarget, region, 0, 0));
			}
		}

		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		dt = new DuelingTree(targets);
		CanvasManager arenaCanvas = new CanvasManager(new Group(), null, "arena", null);
		arenaCanvas.addTarget(duelTreeTarget);
		dt.init(config, new CamerasSupervisor(config), null, null, new ProjectorArenaPane(config, arenaCanvas));

		config.setExercise(dt);

		// Set the wait to zero
		Field delayConstant = dt.getClass().getDeclaredField("NEW_ROUND_DELAY");
		delayConstant.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(delayConstant, delayConstant.getModifiers() & ~Modifier.FINAL);
		delayConstant.setInt(dt, 0);
	}

	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);

		Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		rootLogger.detachAndStopAllAppenders();
	}

	@Test
	public void testNoTarget() throws IOException, ConfigurationException {
		List<Target> targets = new ArrayList<Target>();
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		DuelingTree dt = new DuelingTree(targets);
		dt.init(config, new CamerasSupervisor(config), null, null, null);

		assertEquals(String.format("sounds/voice/shootoff-duelingtree-warning.wav%n"),
				stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();

		dt.reset(targets);

		assertEquals(
				String.format("left score: 0%n") + String.format("right score: 0%n")
						+ String.format("sounds/voice/shootoff-duelingtree-warning.wav%n"),
				stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();
	}

	@Test
	public void testOneRoundsLeftWins() throws UnsupportedEncodingException {
		for (Hit leftPaddleHit : leftPaddlesHits) {
			dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(leftPaddleHit));
		}

		assertEquals(String.format("sounds/beep.wav%n") + 
				String.format("left score: 1%n") + 
				String.format("right score: 0%n"), stringOut.toString("UTF-8"));
		stringOut.reset();

		dt.destroy();
		assertEquals("", stringOut.toString("UTF-8"));
		stringOut.reset();
	}

	@Test
	public void testTwoSeparateRoundsEachSideWinsOnce() throws UnsupportedEncodingException {
		// Let right shoot two paddles then have left come in for the win
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddlesHits.get(0)));
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddlesHits.get(1)));

		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddlesHits.get(0)));
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddlesHits.get(1)));

		for (Hit leftPaddleHit : leftPaddlesHits) {
			dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(leftPaddleHit));
		}

		assertEquals(String.format("sounds/beep.wav%n") + 
				String.format("left score: 1%n") + 
				String.format("right score: 0%n"), stringOut.toString("UTF-8"));
		stringOut.reset();

		dt.reset(targets);

		assertEquals(String.format("left score: 0%n") + String.format("right score: 0%n"), stringOut.toString("UTF-8"));
		stringOut.reset();

		// Right pulls out the win with no competition
		for (Hit rightPaddleHit : rightPaddlesHits) {
			dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddleHit));
		}

		assertEquals(String.format("sounds/beep.wav%n") + 
				String.format("left score: 0%n") + 
				String.format("right score: 1%n"), stringOut.toString("UTF-8"));
		stringOut.reset();

		dt.destroy();
		assertEquals("", stringOut.toString("UTF-8"));
		stringOut.reset();
	}
}
