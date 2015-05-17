package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;

import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class MockCanvasManager extends CanvasManager {
	private final List<Shot> shots = new ArrayList<Shot>();
	private final Configuration config;
	
	public MockCanvasManager(Configuration config) {
		super(new Group(), config, new CamerasSupervisor(config), FXCollections.observableArrayList());
		this.config = config;
	}

	@Override
	public void addShot(Color color, double x, double y) {
		shots.add(new Shot(color, x, y, 0, config.getMarkerRadius()));
	}
	
	public List<Shot> getShots() {
		return shots;
	}
}
