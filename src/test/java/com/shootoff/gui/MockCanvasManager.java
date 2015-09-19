package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
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
	private final String cameraName;
	private final boolean useShotProcessors;
	private long startTime = 0;
	
	public MockCanvasManager(Configuration config) {
		super(new Group(), config, new CamerasSupervisor(config), String.format("%d", System.nanoTime()), 
				FXCollections.observableArrayList());
		new JFXPanel(); // Initialize the JFX toolkit
		this.config = config;
		this.cameraName = "Default";
		this.useShotProcessors = false;
	}
	
	public MockCanvasManager(Configuration config, boolean useShotProcessors) {
		super(new Group(), config, new CamerasSupervisor(config), String.format("%d", System.nanoTime()), 
				FXCollections.observableArrayList());
		new JFXPanel(); // Initialize the JFX toolkit
		this.config = config;
		this.cameraName = "Default";
		this.useShotProcessors = useShotProcessors;
	}

	public String getCameraName() {
		return cameraName;
	}
	
	@Override
	public void addShot(Color color, double x, double y) {
		if (startTime == 0) startTime = System.currentTimeMillis();
		Shot shot = new Shot(color, x, y, 
				CameraManager.getFrameCount(), config.getMarkerRadius());
		
		if (useShotProcessors) {
			for (ShotProcessor p : config.getShotProcessors()) {
				if (!p.processShot(shot)) {
					logger.info("Processing Shot: Shot Rejected By {}", p.getClass().getName());
					return;
				}
			}
		}
		
		logger.info("Processing Shot: Shot Validated {} {}", shot.getX(), shot.getY());
		shots.add(shot);
	}
	
	public List<Shot> getShots() {
		return shots;
	}
}
