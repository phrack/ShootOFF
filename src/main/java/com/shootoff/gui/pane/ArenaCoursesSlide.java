package com.shootoff.gui.pane;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.courses.Course;
import com.shootoff.courses.io.CourseIO;
import com.shootoff.gui.controller.ProjectorArenaController;

import javafx.event.ActionEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArenaCoursesSlide extends Slide {
	private static final Logger logger = LoggerFactory.getLogger(ArenaCoursesSlide.class);
	
	private final ProjectorArenaController arenaController;
	private final Stage shootOffStage;
	
	public ArenaCoursesSlide(Pane parentControls, Pane parentBody, ProjectorArenaController arenaController,
			Stage shootOffStage) {
		super(parentControls, parentBody);
		
		this.arenaController = arenaController;
		this.shootOffStage = shootOffStage;
	}
	
	public void saveCourseMenuItemClicked(ActionEvent event) {
		File coursesDir = new File(System.getProperty("shootoff.courses"));

		if (!coursesDir.exists()) {
			if (!coursesDir.mkdirs()) {
				logger.error("Courses folder does not exist and cannot be created: {}", coursesDir.getAbsolutePath());
			}
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Course");
		fileChooser.setInitialDirectory(coursesDir);
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Course File (*.course)", "*.course"));

		File courseFile = fileChooser.showSaveDialog(shootOffStage);

		if (courseFile != null) {
			String path = courseFile.getPath();
			if (!path.endsWith(".course")) path += ".course";

			courseFile = new File(path);

			CourseIO.saveCourse(arenaController, courseFile);
		}
	}

	public void loadCourseMenuItemClicked(ActionEvent event) {
		File coursesDir = new File(System.getProperty("shootoff.courses"));

		if (!coursesDir.exists()) {
			if (!coursesDir.mkdirs()) {
				logger.error("Courses folder does not exist and cannot be created: {}", coursesDir.getAbsolutePath());
			}
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Course");
		fileChooser.setInitialDirectory(coursesDir);
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Course File (*.course)", "*.course"));

		File courseFile = fileChooser.showOpenDialog(shootOffStage);

		if (courseFile != null) {
			Optional<Course> course = CourseIO.loadCourse(arenaController, courseFile);

			if (course.isPresent()) {
				arenaController.setCourse(course.get());
			}
		}
	}
}
