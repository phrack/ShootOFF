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

package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.Group;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.Target;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.gui.controller.ShootOFFController;

public class ProjectorTrainingExerciseBase extends TrainingExerciseBase {
	private Configuration config;
	private CamerasSupervisor camerasSupervisor;
	private ProjectorArenaController arenaController;
	private final List<Target> targets = new ArrayList<Target>();

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
	public Optional<Target> addTarget(File target, double x, double y) {
		if (!target.isAbsolute())
			target = new File(System.getProperty("shootoff.home") + File.separator + target.getPath());

		Optional<Target> newTarget = arenaController.getCanvasManager().addTarget(target);

        if (newTarget.isPresent()) {
            newTarget.get().setPosition(x, y);
            targets.add(newTarget.get());
           
            if (newTarget.isPresent()) {
                newTarget.get().setTargetEventListener(arenaController);
            }
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

	@Override
	public void destroy() {
		super.destroy();

		for (Target target : targets)
			arenaController.getCanvasManager().removeTarget(target);

		targets.clear();
	}
}
