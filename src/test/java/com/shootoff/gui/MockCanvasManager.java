package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.Shot;
import com.shootoff.camera.processors.ShotProcessor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.targets.Target;

import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;

public class MockCanvasManager extends CanvasManager {
	private final Logger logger = LoggerFactory.getLogger(MockCanvasManager.class);
	private final List<Shot> shots = new ArrayList<Shot>();
	private final Configuration config;
	private final String cameraName;
	private final boolean useShotProcessors;

	public MockCanvasManager(Configuration config) {
		super(new Group(), new ShootOFFController(), String.format("%d", System.nanoTime()),
				FXCollections.observableArrayList());
		new JFXPanel(); // Initialize the JFX toolkit
		this.config = config;
		this.cameraName = "Default";
		this.useShotProcessors = false;
	}

	public MockCanvasManager(Configuration config, boolean useShotProcessors) {
		super(new Group(), new ShootOFFController(), String.format("%d", System.nanoTime()),
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
	public void addShot(Shot shot, boolean mirroredShot) {
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

	@Override
	public Target addTarget(Target newTarget) {
		super.getCanvasGroup().getChildren().add(((TargetView) newTarget).getTargetGroup());
		super.getTargets().add(newTarget);

		return newTarget;
	}
}
