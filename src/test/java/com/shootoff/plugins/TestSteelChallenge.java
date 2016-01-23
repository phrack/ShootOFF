package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.targets.TargetRegion;

import javafx.scene.Node;
import javafx.scene.paint.Color;

public class TestSteelChallenge {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private SteelChallenge steelChallengeExercise;
	private Course course;
	private TargetRegion nonStopRegion;
	private TargetRegion stopRegion;

	@Before
	public void setUp() throws UnsupportedEncodingException, ConfigurationException {
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		System.setProperty("shootoff.home", System.getProperty("user.dir"));

		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);

		steelChallengeExercise = new SteelChallenge();
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		CamerasSupervisor cs = new CamerasSupervisor(config);

		ProjectorArenaController pac = new ProjectorArenaController();
		pac.init(config, new MockCanvasManager(config));

		Optional<Course> course = CourseIO.loadCourse(pac,
				new File("courses/steel_challenge/steel_challenge_accelerator.course".replaceAll("/", File.separator)));
		steelChallengeExercise.init(config, cs, null, null, pac);
		steelChallengeExercise.init(course.get());
		this.course = course.get();

		for (Target t : course.get().getTargets()) {
			for (Node n : t.getTargetGroup().getChildren()) {
				TargetRegion r = (TargetRegion) n;

				if (r.tagExists("subtarget") && r.getTag("subtarget").equalsIgnoreCase("stop_target")) {
					stopRegion = r;
				} else if (r.getAllTags().size() > 0 && nonStopRegion == null) {
					nonStopRegion = r;
				}
			}
		}
	}

	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);
	}

	@Test
	public void testMissAllTargets() throws UnsupportedEncodingException {
		assertEquals(String.format("Are you ready?%nStandby!%nsounds/beep.wav%n").replace(File.separatorChar, '/'),
				stringOut.toString("UTF-8"));
		stringOut.reset();

		steelChallengeExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(stopRegion));

		assertEquals(String
				.format("Your time was 0.00 seconds. You missed %d targets!%nAre you ready?%nStandby!%nsounds/beep.wav%n",
						course.getTargets().size() - 1)
				.replace(File.separatorChar, '/'), stringOut.toString("UTF-8"));
		stringOut.reset();
	}

	@Test
	public void testMissAllThenThreeTargets() throws UnsupportedEncodingException {
		assertEquals(String.format("Are you ready?%nStandby!%nsounds/beep.wav%n").replace(File.separatorChar, '/'),
				stringOut.toString("UTF-8"));
		stringOut.reset();

		steelChallengeExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(stopRegion));

		assertEquals(String
				.format("Your time was 0.00 seconds. You missed %d targets!%nAre you ready?%nStandby!%nsounds/beep.wav%n",
						course.getTargets().size() - 1)
				.replace(File.separatorChar, '/'), stringOut.toString("UTF-8"));
		stringOut.reset();

		steelChallengeExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(nonStopRegion));
		steelChallengeExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 0), Optional.of(stopRegion));

		assertEquals(String
				.format("Your time was 0.00 seconds. You missed %d targets!%nAre you ready?%nStandby!%nsounds/beep.wav%n",
						course.getTargets().size() - 2)
				.replace(File.separatorChar, '/'), stringOut.toString("UTF-8"));
		stringOut.reset();
	}

}
