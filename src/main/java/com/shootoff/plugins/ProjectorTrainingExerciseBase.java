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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.gui.controller.ShootOFFController;

public class ProjectorTrainingExerciseBase extends TrainingExerciseBase {
	private Configuration config;
	private CamerasSupervisor camerasSupervisor;
	private ProjectorArenaController arenaController;
	private final List<Target> targets = new ArrayList<Target>();
	private final Label exerciseLabel = new Label();

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public ProjectorTrainingExerciseBase() {}

	public ProjectorTrainingExerciseBase(List<Group> targets) {
		super(targets);
	}

	public void init(Configuration config, CamerasSupervisor camerasSupervisor, ShootOFFController controller,
			ProjectorArenaController arenaController) {
		super.init(config, camerasSupervisor, controller);
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.arenaController = arenaController;
		exerciseLabel.setTextFill(Color.WHITE);
		Platform.runLater(() -> arenaController.getCanvasManager().getCanvasGroup().getChildren().add(exerciseLabel));
	}

	// For unit tests
	public void init(Configuration config, CamerasSupervisor camerasSupervisor, GridPane buttonsPane,
			TableView<ShotEntry> shotEntryTable, ProjectorArenaController arenaController) {
		super.init(config, camerasSupervisor, buttonsPane, shotEntryTable);
		this.config = config;
		this.camerasSupervisor = camerasSupervisor;
		this.arenaController = arenaController;
	}

	@Override
	public void reset() {
		camerasSupervisor.reset();
		if (config.getExercise().isPresent())
			config.getExercise().get().reset(arenaController.getCanvasManager().getTargetGroups());
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
	 */
	public Optional<Target> addTarget(File target, final double x, final double y) {
		if (!target.isAbsolute())
			target = new File(System.getProperty("shootoff.home") + File.separator + target.getPath());

		final Optional<Target> newTarget = arenaController.getCanvasManager().addTarget(target);

		if (newTarget.isPresent()) {
			newTarget.get().setPosition(x, y);
			targets.add(newTarget.get());

		}

		return newTarget;
	}

	public void removeTarget(Target target) {
		arenaController.getCanvasManager().removeTarget(target);
		targets.remove(target);
	}

	public double getArenaWidth() {
		return arenaController.getWidth();
	}

	public double getArenaHeight() {
		return arenaController.getHeight();
	}

	public Point2D getArenaScreenOrigin() {
		return arenaController.getArenaScreenOrigin();
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
	 * Returns the current instance of this class. This metehod exists so that
	 * we can call methods in this class when in an internal class (e.g. to
	 * implement Callable) that doesn't have access to super.
	 * 
	 * @return the current instance of this class
	 */
	public ProjectorTrainingExerciseBase getInstance() {
		return this;
	}

	/**
	 * Set the projector arena's background image
	 * 
	 * @param background
	 *            a file on the filesystem or a resource to set as the projector
	 *            arena's background.
	 */
	public void setArenaBackground(LocatedImage background) {
		arenaController.setBackground(background);
	}

	@Override
	public void destroy() {
		for (Target target : targets)
			arenaController.getCanvasManager().removeTarget(target);

		Platform.runLater(() -> {
			if (arenaController != null)
				arenaController.getCanvasManager().getCanvasGroup().getChildren().remove(exerciseLabel);
		});

		targets.clear();

		super.destroy();
	}
}
