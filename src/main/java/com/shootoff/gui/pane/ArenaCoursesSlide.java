/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

package com.shootoff.gui.pane;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.courses.Course;
import com.shootoff.courses.io.CourseIO;
import com.shootoff.gui.TargetView;
import com.shootoff.targets.Target;

import javafx.geometry.Dimension2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArenaCoursesSlide extends Slide implements ItemSelectionListener<File> {
	private static final Logger logger = LoggerFactory.getLogger(ArenaCoursesSlide.class);
	
	private final Map<String, ItemSelectionPane<File>> categoryMap = new HashMap<>();
	
	private final VBox coursePanes = new VBox();
	private final ProjectorArenaPane arenaPane;
	private final Stage shootOffStage;
	
	private boolean choseCourse = false;
	
	public ArenaCoursesSlide(Pane parentControls, Pane parentBody, ProjectorArenaPane arenaPane,
			Stage shootOffStage) {
		super(parentControls, parentBody);
		
		this.arenaPane = arenaPane;
		this.shootOffStage = shootOffStage;
		
		addSlideControlButton("Save Course", (event) -> {
			saveCourse();
		});
		
		addSlideControlButton("Clear Course", (event) -> {
			arenaPane.getCanvasManager().clearTargets();
		});
		
		addBodyNode(buildCoursePanes());
	}
	
	private void saveCourse() {
		final File coursesDir = new File(System.getProperty("shootoff.courses"));

		if (!coursesDir.exists()) {
			if (!coursesDir.mkdirs()) {
				logger.error("Courses folder does not exist and cannot be created: {}", coursesDir.getAbsolutePath());
			}
		}

		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Course");
		fileChooser.setInitialDirectory(coursesDir);
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Course File (*.course)", "*.course"));

		File courseFile = fileChooser.showSaveDialog(shootOffStage);

		if (courseFile != null) {
			String path = courseFile.getPath();
			if (!path.endsWith(".course")) path += ".course";

			courseFile = new File(path);

			CourseIO.saveCourse(arenaPane, courseFile);
			
			// Add the course to the list in the appropriate category
			ItemSelectionPane<File> itemPane;
			
			if (categoryMap.containsKey(courseFile.getParent())) {
				itemPane = categoryMap.get(courseFile.getParent());
				addCourseButton(itemPane, courseFile);
			} else {
				itemPane = buildCategoryPane(courseFile.getParentFile());
				coursePanes.getChildren().add(
						new TitledPane(courseFile.getParentFile().getName().replaceAll("_", "") + " Courses", itemPane));
				categoryMap.put(courseFile.getParent(), itemPane);
			}
		}
	}
	
	private static final FileFilter FOLDER_FILTER = new FileFilter() {
		@Override
		public boolean accept(File path) {
			return path.isDirectory();
		}
	};
	
	private Pane buildCoursePanes() {
		final File coursesDirectory = new File(System.getProperty("shootoff.courses"));
		
		final ItemSelectionPane<File> uncategorizedPane = buildCategoryPane(coursesDirectory);
		coursePanes.getChildren().add(
				new TitledPane("Uncategorized Courses", uncategorizedPane));
		categoryMap.put(coursesDirectory.getPath(), uncategorizedPane);
		
		final File[] courseFolders = coursesDirectory.listFiles(FOLDER_FILTER);
		
		if (courseFolders != null) {
			for (File courseFolder : courseFolders) {
				coursePanes.getChildren().add(
						new TitledPane(courseFolder.getName().replaceAll("_", "") + " Courses", buildCategoryPane(courseFolder)));
			}
		} else {
			logger.error("{} does not appear to be a valid course directory", coursesDirectory.getPath());			
		}
	
		return coursePanes;
	}
	
	private static final FilenameFilter COURSE_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File directory, String fileName) {
			return fileName.endsWith(".course");
		}
	};
	
	private ItemSelectionPane<File> buildCategoryPane(File path) {
		final ItemSelectionPane<File> itemPane = new ItemSelectionPane<>(false, this);
		categoryMap.put(path.getPath(), itemPane);

		final File[] courseFiles = path.listFiles(COURSE_FILTER);
		
		if (courseFiles != null) {
			for (File f : courseFiles) {
				addCourseButton(itemPane, f);
			}
		} else {
			logger.error("{} doesn't appear to be a valid course-containing directory", path.getPath());
		}

		return itemPane;
	}
	
	private void addCourseButton(ItemSelectionPane<File> itemPane, File courseFile) {
		final ImageView courseThumbnail = getCourseThumbnail(courseFile);
		
		if (courseThumbnail == null) {
			itemPane.addButton(courseFile, courseFile.getName().replace(".course", "").replaceAll("_", " "));
		} else {
			itemPane.addButton(courseFile, courseFile.getName().replace(".course", "").replaceAll("_", " "),
					Optional.of(courseThumbnail), Optional.empty());
		}
	}
	
	private ImageView getCourseThumbnail(File courseFile) {
		final Optional<Course> course = CourseIO.loadCourse(arenaPane, courseFile);

		if (course.isPresent()) {
			final Group courseGroup = new Group();
			
			final Course c = course.get();
			
			if (c.getBackground().isPresent()) {
				final Dimension2D courseDimensions;
				
				if (c.getResolution().isPresent()) {
					courseDimensions = c.getResolution().get();
				} else {
					courseDimensions = new Dimension2D(arenaPane.getWidth(), arenaPane.getWidth());
				}
				
				final ImageView backgroundImageView = new ImageView(c.getBackground().get());
				backgroundImageView.setFitWidth(courseDimensions.getWidth());
				backgroundImageView.setFitHeight(courseDimensions.getHeight());
				backgroundImageView.setSmooth(true);
				
				courseGroup.getChildren().add(backgroundImageView);
			}
			
			for (Target t : c.getTargets()) {
				courseGroup.getChildren().add(((TargetView) t).getTargetGroup());
			}
			
			final Image courseThumbnail = courseGroup.snapshot(new SnapshotParameters(), null);
			final ImageView courseImageView = new ImageView(courseThumbnail);
			courseImageView.setFitWidth(60);
			courseImageView.setFitHeight(60);
			courseImageView.setPreserveRatio(true);
			courseImageView.setSmooth(true);
			
			return courseImageView;
		}
		
		return null;
	}

	@Override
	public void onItemClicked(File courseFile) {
		final Optional<Course> course = CourseIO.loadCourse(arenaPane, courseFile);

		if (course.isPresent()) {
			arenaPane.setCourse(course.get());
		}
		
		choseCourse = true;
		
		hide();
	}
	
	public boolean choseCourse() {
		return choseCourse;
	}
}
