package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TableView;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.ProjectorArenaController;
import com.shootoff.gui.ShotEntry;

public class ProjectorTrainingProtocolBase extends TrainingProtocolBase {
	private ProjectorArenaController arenaController;
	private final List<Group> targets = new ArrayList<Group>();
	
	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public ProjectorTrainingProtocolBase() {}
	
	public ProjectorTrainingProtocolBase(List<Group> targets) {
		super(targets);
	}
	
	public void init(Configuration config, CamerasSupervisor camerasSupervisor, 
			TableView<ShotEntry> shotTimerTable, ProjectorArenaController arenaController) {
		super.init(config, camerasSupervisor, shotTimerTable);
		this.arenaController = arenaController;
	}
	
	/**
	 * Add a target to the projector arena at specific coordinates.
	 * 
	 * @param target 	the file to load the target from
	 * @param x			the top left x coordinate of the target
	 * @param y			the top left y coordinate of the target
	 * 
	 * @return	the group that was loaded from the target file
	 */
	public Optional<Group> addTarget(File target, double x, double y) {
		Optional<Group> newTarget = arenaController.getCanvasManager().addTarget(target);
		
		if (newTarget.isPresent()) {
			for (Node node : newTarget.get().getChildren()) {
				node.setLayoutX(x);
				node.setLayoutY(y);
			}
			
			targets.add(newTarget.get());
		}
		
		return newTarget;
	}
	
	public void removeTarget(Group target) {
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
	 * Returns the current instance of this class. This metehod exists so that we can
	 * call methods in this class when in an internal class (e.g. to implement Callable)
	 * that doesn't have access to super.
	 * 
	 * @return the current instance of this class
	 */
	public ProjectorTrainingProtocolBase getInstance() {
		return this;
	}
	
	@Override
	public void destroy() {
		super.destroy();
		
		for (Group target : targets) arenaController.getCanvasManager().removeTarget(target);
		
		targets.clear();
	}
}
