package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.config.Configuration;

import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class MockCanvasManager extends CanvasManager {
	private final Logger logger = LoggerFactory.getLogger(MockCanvasManager.class);
	private final List<Shot> shots = new ArrayList<Shot>();
	private final Configuration config;
	private final boolean useShotProcessors;
	private long startTime = 0;
	
	public MockCanvasManager(Configuration config) {
		super(new Group(), config, new CamerasSupervisor(config), FXCollections.observableArrayList());
		new JFXPanel(); // Initialize the JFX toolkit
		this.config = config;
		this.useShotProcessors = false;
	}
	
	public MockCanvasManager(Configuration config, boolean useShotProcessors) {
		super(new Group(), config, new CamerasSupervisor(config), FXCollections.observableArrayList());
		new JFXPanel(); // Initialize the JFX toolkit
		this.config = config;
		this.useShotProcessors = useShotProcessors;
	}

	@Override
	public void addShot(Color color, double x, double y) {
		if (startTime == 0) startTime = System.currentTimeMillis();
		Shot shot = new Shot(color, x, y, 
				System.currentTimeMillis() - startTime, config.getMarkerRadius());
		
		if (useShotProcessors) {
			for (ShotProcessor p : config.getShotProcessors()) {
				if (!p.processShot(shot)) {
					logger.info("Processing Shot: Shot Rejected By {}", p.getClass().getName());
					return;
				}
			}
		}
		
		shots.add(shot);
	}
	
	public List<Shot> getShots() {
		return shots;
	}
}
