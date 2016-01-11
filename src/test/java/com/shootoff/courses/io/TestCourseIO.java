package com.shootoff.courses.io;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.courses.Course;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.targets.io.TargetIO;

public class TestCourseIO {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private ProjectorArenaController arenaController;
	private String backgroundURL;
	private boolean backgroundIsResource;
	private String targetName;
	private double targetX;
	private double targetY;
	private double targetWidth;
	private double targetHeight;

	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		System.setProperty("shootoff.sessions", System.getProperty("shootoff.home") + File.separator + "sessions");

		Configuration config = new Configuration(new String[0]);
		arenaController = new ProjectorArenaController();
		arenaController.init(config, new MockCanvasManager(config));
		backgroundURL = "/arena/backgrounds/indoor_range.gif";
		backgroundIsResource = true;
		targetName = "targets/reset.target";
		targetX = 10;
		targetY = 100;
		targetWidth = 10;
		targetHeight = 1;

		InputStream is = TestCourseIO.class.getResourceAsStream(backgroundURL);
		LocatedImage img = new LocatedImage(is, backgroundURL);
		arenaController.setBackground(img);

		File targetFile = new File(targetName);
		Target target = new Target(targetFile, TargetIO.loadTarget(targetFile).get(), config,
				new MockCanvasManager(config), false, 0);
		target.setPosition(targetX, targetY);
		target.setDimensions(targetWidth, targetHeight);

		arenaController.getCanvasManager().addTarget(target);
	}

	private void checkCourse(Optional<Course> course) {
		assertTrue(course.isPresent());

		Optional<LocatedImage> background = course.get().getBackground();

		assertTrue(background.isPresent());
		assertEquals(backgroundURL, background.get().getURL());
		assertEquals(backgroundIsResource, background.get().isResource());

		List<Target> targets = course.get().getTargets();

		assertEquals(1, targets.size());

		final int FIRST_TARGET_INDEX = 0;
		assertEquals(targetX, targets.get(FIRST_TARGET_INDEX).getPosition().getX(), 1);
		assertEquals(targetY, targets.get(FIRST_TARGET_INDEX).getPosition().getY(), 1);
		assertEquals(targetWidth, targets.get(FIRST_TARGET_INDEX).getDimension().getWidth(), 1);
		assertEquals(targetHeight, targets.get(FIRST_TARGET_INDEX).getDimension().getHeight(), 1);
	}

	@Test
	public void testXMLSerialization() {
		File tempXMLCourse = new File("temp_course.course");
		CourseIO.saveCourse(arenaController, tempXMLCourse);

		Optional<Course> course = CourseIO.loadCourse(arenaController, tempXMLCourse);
		checkCourse(course);

		if (!tempXMLCourse.delete()) System.err.println("Failed to delete " + tempXMLCourse.getPath());
	}
	
	@Test
	public void testCourseDoesntExist() {
		File XMLCourse = new File("does_not_exist.course");
		Optional<Course> course = CourseIO.loadCourse(arenaController, XMLCourse);
		
		assertEquals(Optional.empty(), course);
	}
	
	@Test
	public void testUnknownCourseExtension() {
		File XMLCourse = new File("does_not_exist.watisthis");
		Optional<Course> course = CourseIO.loadCourse(arenaController, XMLCourse);
		
		assertEquals(Optional.empty(), course);
	}
}