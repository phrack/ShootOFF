package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.courses.Course;
import com.shootoff.courses.io.CourseIO;
import com.shootoff.gui.Hit;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.MockProjectorArenaController;
import com.shootoff.targets.TargetRegion;

import javafx.scene.Node;
import javafx.scene.paint.Color;

public class TestSteelChallenge {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private SteelChallenge noTargetsSC;
	private SteelChallenge targetsSC;
	private Course course;
	private Hit nonStopRegionHit;
	private Hit stopRegionHit;

	@Before
	public void setUp() throws UnsupportedEncodingException, ConfigurationException {
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		System.setProperty("shootoff.home", System.getProperty("user.dir"));

		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);

		// Init without targets

		noTargetsSC = new SteelChallenge();
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		CamerasSupervisor cs = new CamerasSupervisor(config);

		MockProjectorArenaController pacNoTargets = new MockProjectorArenaController();
		pacNoTargets.init(config, new MockCanvasManager(config));

		noTargetsSC.init(config, cs, null, null, pacNoTargets);

		// Init with target

		targetsSC = new SteelChallenge();

		MockProjectorArenaController pac = new MockProjectorArenaController();
		pac.init(config, new MockCanvasManager(config));

		Optional<Course> course = CourseIO.loadCourse(pac,
				new File("courses/steel_challenge/accelerator.course".replace("/", File.separator)));
		targetsSC.init(config, cs, null, null, pac);
		targetsSC.init(course.get());
		this.course = course.get();

		for (Target t : course.get().getTargets()) {
			for (Node n : t.getTargetGroup().getChildren()) {
				TargetRegion r = (TargetRegion) n;

				if (r.tagExists("subtarget") && r.getTag("subtarget").equalsIgnoreCase("stop_target")) {
					stopRegionHit = new Hit(t, r, 0, 0);
				} else if (r.getAllTags().size() > 0 && nonStopRegionHit == null) {
					nonStopRegionHit = new Hit(t, r, 0, 0);
				}
			}
		}
	}

	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);

		noTargetsSC.destroy();
		targetsSC.destroy();
	}

	@Test
	public void testNoTargets() throws UnsupportedEncodingException {
		stringOut.reset();

		noTargetsSC.init(new Course(new ArrayList<Target>()));

		assertEquals(String
				.format("sounds/voice/shootoff-lay-out-own-course.wav%nsounds/voice/shootoff-add-stop-target.wav%n")
				.replace('/', File.separatorChar), stringOut.toString("UTF-8"));
		stringOut.reset();
	}

	@Test
	public void testMissAllTargets() throws UnsupportedEncodingException {
		assertEquals(
				String.format("sounds/voice/shootoff-are-you-ready.wav%n"
						+ "sounds/voice/shootoff-standby.wav%nsounds/beep.wav%n").replace('/', File.separatorChar),
				stringOut.toString("UTF-8"));
		stringOut.reset();

		targetsSC.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(stopRegionHit));

		assertEquals(String
				.format("Your time was 0.00 seconds. You missed %d targets!%nsounds/voice/shootoff-are-you-ready.wav%n"
						+ "sounds/voice/shootoff-standby.wav%nsounds/beep.wav%n", course.getTargets().size() - 1)
				.replace('/', File.separatorChar), stringOut.toString("UTF-8"));
		stringOut.reset();
	}

	@Test
	public void testMissAllThenThreeTargets() throws UnsupportedEncodingException {
		assertEquals(
				String.format("sounds/voice/shootoff-are-you-ready.wav%n"
						+ "sounds/voice/shootoff-standby.wav%nsounds/beep.wav%n").replace('/', File.separatorChar),
				stringOut.toString("UTF-8"));
		stringOut.reset();

		targetsSC.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(stopRegionHit));

		assertEquals(String
				.format("Your time was 0.00 seconds. You missed %d targets!%nsounds/voice/shootoff-are-you-ready.wav%n"
						+ "sounds/voice/shootoff-standby.wav%nsounds/beep.wav%n", course.getTargets().size() - 1)
				.replace('/', File.separatorChar), stringOut.toString("UTF-8"));
		stringOut.reset();

		targetsSC.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(nonStopRegionHit));
		targetsSC.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(stopRegionHit));

		assertEquals(String
				.format("Your time was 0.00 seconds. You missed %d targets!%nsounds/voice/shootoff-are-you-ready.wav%n"
						+ "sounds/voice/shootoff-standby.wav%nsounds/beep.wav%n", course.getTargets().size() - 2)
				.replace('/', File.separatorChar), stringOut.toString("UTF-8"));
		stringOut.reset();
	}

}
