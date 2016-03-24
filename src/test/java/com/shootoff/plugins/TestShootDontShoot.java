package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.Hit;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.MockProjectorArenaController;
import com.shootoff.targets.TargetRegion;

import ch.qos.logback.classic.Logger;
import javafx.scene.Group;

public class TestShootDontShoot {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private ShootDontShoot sds;
	private List<Target> shootTargets;
	private List<Target> dontShootTargets;

	@Before
	public void setUp() throws UnsupportedEncodingException, ConfigurationException {
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		Random rng = new Random(15); // Changing this seed will cause tests to
										// fail

		shootTargets = new ArrayList<Target>();
		dontShootTargets = new ArrayList<Target>();
		sds = new ShootDontShoot(new ArrayList<Group>(), rng, shootTargets, dontShootTargets);
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		CamerasSupervisor cs = new CamerasSupervisor(config);

		MockProjectorArenaController pac = new MockProjectorArenaController();
		pac.init(config, new MockCanvasManager(config));

		sds.init(config, cs, null, null, pac);
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
	public void testAddRemoveTargets() {
		sds.addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target");

		assertEquals(4, shootTargets.size());

		sds.removeTarget(shootTargets, (TargetRegion) shootTargets.get(0).getTargetGroup().getChildren().get(0));

		assertEquals(3, shootTargets.size());
	}

	@Test
	public void testMissGoodOneBad() throws UnsupportedEncodingException {
		sds.addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target");

		assertEquals(4, shootTargets.size());

		sds.addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target");

		assertEquals(5, dontShootTargets.size());

		// Shoot dont shoot target
		Hit dontShootHit = new Hit(dontShootTargets.get(0),
				(TargetRegion) dontShootTargets.get(0).getTargetGroup().getChildren().get(0), 0, 0);

		sds.shotListener(null, Optional.of(dontShootHit));

		assertEquals("Bad shoot!", stringOut.toString("UTF-8").replace(String.format("%n"), ""));
		stringOut.reset();

		sds.callNewRound();

		String roundEndText = String.format("You missed %d targets.%nmissed targets: %d%nbad hits: %d%n",
				shootTargets.size(), shootTargets.size(), 1);

		assertEquals(roundEndText, stringOut.toString("UTF-8"));
		stringOut.reset();
	}
}
