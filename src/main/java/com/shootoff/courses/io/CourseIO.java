/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.courses.io;

import java.io.File;
import java.util.Optional;

import com.shootoff.courses.Course;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.ProjectorArenaController;

public class CourseIO {
	public static void saveCourse(ProjectorArenaController arenaController, File courseFile) {
		CourseVisitor visitor;

		if (courseFile.getName().endsWith("course")) {
			visitor = new XMLCourseWriter(courseFile);
		} else {
			System.err.println("Unknown course file type.");
			return;
		}

		if (arenaController.getBackground().isPresent()) {
			LocatedImage background = arenaController.getBackground().get();
			visitor.visitBackground(background.getURL(), background.isResource());
		}

		for (Target t : arenaController.getCanvasManager().getTargets()) {
			File relativeTargetFile = new File(t.getTargetFile().getAbsolutePath()
					.replace(System.getProperty("shootoff.home") + File.separator, ""));
			visitor.visitTarget(relativeTargetFile, t.getPosition().getX(), t.getPosition().getY(),
					t.getDimension().getWidth(), t.getDimension().getHeight());
		}
		
		visitor.visitResolution(arenaController.getWidth(), arenaController.getHeight());

		visitor.visitEnd();
	}

	public static Optional<Course> loadCourse(ProjectorArenaController arenaController, File courseFile) {
		if (!courseFile.getName().endsWith("course")) {
			System.err.println("Unknown course file type.");
			return Optional.empty();
		}

		return new XMLCourseReader(arenaController, courseFile).load();
	}
}
