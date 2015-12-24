package com.shootoff.courses.io;

import java.io.File;
import java.util.Optional;

import com.shootoff.courses.Course;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.ProjectorArenaController;

public class CourseIO {
	public static void saveCourse(ProjectorArenaController arenaController,
			File courseFile) {
		CourseVisitor visitor;

		if (courseFile.getName().endsWith("course")) {
			visitor = new XMLCourseWriter(courseFile);
		} else {
			System.err.println("Unknown course file type.");
			return;
		}

		if (arenaController.getBackground().isPresent()) {
			LocatedImage background = arenaController.getBackground().get();
			visitor.visitBackground(background.getURL(),
					background.isResource());
		}

		for (Target t : arenaController.getCanvasManager().getTargets()) {
			File relativeTargetFile = new File(t
					.getTargetFile()
					.getAbsolutePath()
					.replace(
							System.getProperty("shootoff.home")
									+ File.separator, ""));
			visitor.visitTarget(relativeTargetFile, t.getPosition().getX(), t
					.getPosition().getY(), t.getDimension().getWidth(), t
					.getDimension().getHeight());
		}

		visitor.visitEnd();
	}

	public static Optional<Course> loadCourse(
			ProjectorArenaController arenaController, File courseFile) {
		if (!courseFile.getName().endsWith("course")) {
			System.err.println("Unknown course file type.");
			return Optional.empty();
		}

		return new XMLCourseReader(arenaController, courseFile).load();
	}
}
