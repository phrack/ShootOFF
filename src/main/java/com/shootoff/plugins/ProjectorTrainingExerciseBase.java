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

package com.shootoff.plugins;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.courses.Course;
import com.shootoff.courses.io.CourseIO;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.targets.Target;

import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * The API for training exercises that only work on the projector arena.
 * Training exercises that are intended to be used with the projector arena only
 * (i.e. those that require a projector) should extend this class. If the
 * exercise is not intended to only work with a projector, extend
 * {@link TrainingExerciseBase} instead.
 * 
 * @author phrack
 */
public class ProjectorTrainingExerciseBase extends TrainingExerciseBase {
	private CamerasSupervisor camerasSupervisor;
	private ProjectorArenaPane arenaPane;
	private final List<Target> targets = new ArrayList<>();
	private final Label exerciseLabel = new Label();

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public ProjectorTrainingExerciseBase() {}

	public ProjectorTrainingExerciseBase(List<Target> targets) {
		super(targets);
	}

	public void init(CamerasSupervisor camerasSupervisor, TrainingExerciseView exerciseView,
			ProjectorArenaPane arenaPane) {
		super.init(camerasSupervisor, exerciseView);
		this.camerasSupervisor = camerasSupervisor;
		this.arenaPane = arenaPane;
		exerciseLabel.setTextFill(Color.WHITE);
		Platform.runLater(() -> arenaPane.getCanvasManager().getCanvasGroup().getChildren().add(exerciseLabel));
	}

	// For unit tests
	public void init(Configuration config, CamerasSupervisor camerasSupervisor, VBox buttonsContainer,
			TableView<ShotEntry> shotEntryTable, ProjectorArenaPane arenaPane) {
		super.init(config, camerasSupervisor, buttonsContainer, shotEntryTable);
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.arenaPane = arenaPane;
	}

	@Override
	public void reset() {
		camerasSupervisor.reset();
		if (config.getExercise().isPresent())
			config.getExercise().get().reset(arenaPane.getCanvasManager().getTargets());
	}

	/**
	 * Add a target to the projector arena at specific coordinates.
	 * 
	 * @param target
	 *            the file to load the target from
	 * @param x
	 *            the top left x coordinate of the target
	 * @param y
	 *            the top left y coordinate of the target
	 * 
	 * @return the group that was loaded from the target file
	 * 
	 * @since 2.1
	 */
	public Optional<Target> addTarget(File target, final double x, final double y) {
		if ('@' != target.toString().charAt(0) && !target.isAbsolute())
			target = new File(System.getProperty("shootoff.home") + File.separator + target.getPath());

		final Optional<Target> newTarget = arenaPane.getCanvasManager().addTarget(target, false);

		if (newTarget.isPresent()) {
			final Target t = newTarget.get();

			t.setPosition(x, y);

			if (isPerspectiveInitialized()) {
				arenaPane.resizeTargetToDefaultPerspective(t);
			}

			targets.add(t);
		}

		return newTarget;
	}

	public void removeTarget(Target target) {
		arenaPane.getCanvasManager().removeTarget(target);
		targets.remove(target);
	}

	/**
	 * Get the width of the arena in pixels.
	 * 
	 * @return the arena's width in pixels
	 * 
	 * @since 2.1
	 */
	public double getArenaWidth() {
		return arenaPane.getWidth();
	}

	/**
	 * Get the height of the arena in pixels.
	 * 
	 * @return the arena's height in pixels
	 * 
	 * @since 2.1
	 */
	public double getArenaHeight() {
		return arenaPane.getHeight();
	}

	/**
	 * Get the coordinates of the origin for the display the arena is currently
	 * located on. This is useful if you need to know what display the arena is
	 * on.
	 * 
	 * @return the origin coordinates for the JavaFX screen the arena is located
	 *         on, relative to other displays.
	 * 
	 * @since 3.8
	 */
	public Point2D getArenaScreenOrigin() {
		return arenaPane.getArenaScreenOrigin();
	}

	/**
	 * This function corrects coordinates in the arena area for the DPI of the
	 * arena screen and relative to the origin.  This is for creating mouse
	 * events and other javafx functions that need true coordinates rather 
	 * than scaled ones.
	 * 
	 * @param point
	 * 			The coordinates to be corrected
	 * 
	 * @return Translated coordinates
	 * 
	 * @since 3.8
	 */
	public Point2D translateToTrueArenaCoords(Point2D point)
	{
		final double dpiScaleFactor = ShootOFFController.getDpiScaleFactorForScreen();

		final Point2D origin = arenaPane.getArenaScreenOrigin();

		return new Point2D(origin.getX() + (point.getX() * dpiScaleFactor),
				origin.getY() + (point.getY() * dpiScaleFactor));
	}

	@Override
	public void showTextOnFeed(String message) {
		super.showTextOnFeed(message);
		Platform.runLater(() -> exerciseLabel.setText(message));
	}

	/**
	 * Show a message on all webcam feeds, but optionally do not show the
	 * message on the arena itself.
	 * 
	 * @param message
	 *            the message to show
	 * @param showOnArena
	 *            <tt>false</tt> if the message should not show on the arena
	 */
	public void showTextOnFeed(String message, boolean showOnArena) {
		if (showOnArena) {
			showTextOnFeed(message);
		} else {
			super.showTextOnFeed(message);
		}
	}

	/**
	 * Show a message on all webcam feeds and the arena, but customize the
	 * location, font, and colors used to display the message.
	 * 
	 * @param message
	 *            the message to show
	 * @param x
	 *            the x coordinate of the top left of the message
	 * @param y
	 *            the y coordinate of the top left of the message
	 * @param backgroundColor
	 *            the background color for the message
	 * @param textColor
	 *            the color of the letters in the message
	 * @param font
	 *            the font to use to display the message
	 * 
	 * @since 3.7
	 */
	public void showTextOnFeed(String message, int x, int y, Color backgroundColor, Color textColor, Font font) {
		showTextOnFeed(message);
		Platform.runLater(() -> {
			exerciseLabel.setLayoutX(x);
			exerciseLabel.setLayoutY(y);
			exerciseLabel.setBackground(
					new Background(new BackgroundFill(backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
			exerciseLabel.setTextFill(textColor);
			exerciseLabel.setFont(font);
		});
	}

	/**
	 * Returns the current instance of this class. This method exists so that we
	 * can call methods in this class when in an internal class (e.g. to
	 * implement Callable) that doesn't have access to super.
	 * 
	 * @return the current instance of this class
	 */
	@Override
	public ProjectorTrainingExerciseBase getInstance() {
		return this;
	}

	/**
	 * Set the projector arena's background image.
	 * 
	 * @param background
	 *            a file on the filesystem or a resource to set as the projector
	 *            arena's background.
	 * 
	 * @since 3.7
	 */
	public void setArenaBackground(LocatedImage background) {
		arenaPane.setArenaBackground(background);
	}

	/**
	 * Set the projector arena's background image to one of the default
	 * backgrounds using its resource path.
	 * 
	 * @param defaultResourcePath
	 *            the resource path to the default ShootOFF background to set.
	 * 
	 * @since 3.10
	 */
	public void setArenaBackground(String defaultResourcePath) {
		final InputStream is = ProjectorTrainingExerciseBase.class.getResourceAsStream(defaultResourcePath);
		final LocatedImage img = new LocatedImage(is, defaultResourcePath);
		setArenaBackground(img);
	}
	
	/**
	 * Remove all targets on the arena and replace them with the course
	 * specified in the file <code>courseFile</code>.
	 * 
	 * @param courseFile
	 *            a file specifying the course to use
	 * @return a list of the targets loaded from <code>courseFile</code>
	 * 
	 * @since 3.8
	 */
	public List<Target> setCourse(File courseFile) {
		final Optional<Course> newCourse = CourseIO.loadCourse(arenaPane, courseFile);
		arenaPane.setCourse(newCourse.get());
		return newCourse.get().getTargets();
	}

	/**
	 * Determines whether or not ShootOFF has sufficient information to set a
	 * target's distance to a real world distance.
	 * 
	 * @return <code>true</code> if ShootOFF can set a target to a real world
	 *         distance
	 * 
	 * @since 3.8
	 */
	public boolean isPerspectiveInitialized() {
		return arenaPane.getPerspectiveManager().isPresent()
				&& arenaPane.getPerspectiveManager().get().isInitialized();
	}

	/**
	 * Changes the size of a target to real world dimensions at a particular
	 * distance (e.g. to simulate a target that is 36"x24" at 10 yards). To do
	 * this, you must already know the size of the target and ShootOFF must be
	 * initialized with the distance and camera specification information
	 * required to measure distances in the real world. Look at the USPSA target
	 * for an example of setting a default real world width, height, and
	 * distance for a target.
	 * 
	 * Distance and camera specification information is typically determined
	 * during the auto-calibration process. However, a user will have to
	 * manually enter at least the camera distance by manually setting a
	 * target's distance if they did not auto-calibrate or if they
	 * auto-calibrated without the perspective calibration pattern present.
	 * 
	 * @param target
	 *            the target to resize
	 * @param currentRealWidth
	 *            the current width of the target as it appears on the
	 *            projection in mm
	 * @param currentRealHeight
	 *            the current height of the target as it appears on the
	 *            projection in mm
	 * @param currentRealDistance
	 *            the distance the target is currently sized to appear at (e.g.
	 *            the target appears to be currentRealWidth x currentRealHeight
	 *            because it is currentRealDistance away in mm)
	 * @param desiredDistance
	 *            the new distance the target should appear at in mm (i.e.
	 *            resize the target so it appears to be desiredDistance away
	 *            based on its current size and distance)
	 * @return <code>true</code> if ShootOFF has the data to resize a target and
	 *         successfully calculated new target dimensions
	 */
	public boolean setTargetDistance(Target target, int currentRealWidth, int currentRealHeight,
			int desiredDistance) {
		if (!isPerspectiveInitialized()) return false;

		final Optional<Dimension2D> targetDimensions = arenaPane.getPerspectiveManager().get()
				.calculateObjectSize(currentRealWidth, currentRealHeight, desiredDistance);

		if (targetDimensions.isPresent()) {
			final Dimension2D d = targetDimensions.get();
			target.setDimensions(d.getWidth(), d.getHeight());

			return true;
		}

		return false;
	}

	@Override
	public void destroy() {
		for (final Target target : targets)
			arenaPane.getCanvasManager().removeTarget(target);

		targets.clear();

		Platform.runLater(() -> {
			if (arenaPane != null)
				arenaPane.getCanvasManager().getCanvasGroup().getChildren().remove(exerciseLabel);
		});

		super.destroy();
	}
}
