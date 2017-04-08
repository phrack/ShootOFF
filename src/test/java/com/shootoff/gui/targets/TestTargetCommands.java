package com.shootoff.gui.targets;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.MockCameraManager;
import com.shootoff.camera.Shot;
import com.shootoff.camera.shot.ArenaShot;
import com.shootoff.camera.shot.BoundsShot;
import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.controller.MockProjectorArenaController;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.plugins.TextToSpeech;
import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;
import com.shootoff.targets.io.TargetIO.TargetComponents;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

public class TestTargetCommands {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private Configuration config;
	private TargetView poiTarget;
	private List<Target> targets;
	private CanvasManager canvasManager;
	private CameraManager cameraManager;
	private Bounds bounds;

	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));

		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		
		config = new Configuration(new String[0]);
		canvasManager = new MockCanvasManager(config);
		cameraManager = new MockCameraManager();
		
		bounds = new BoundingBox(100, 100, 540, 260);
		
		cameraManager.setProjectionBounds(bounds);
		canvasManager.setCameraManager(cameraManager);
		
		ProjectorArenaPane projectorArenaPane = new MockProjectorArenaController(config, canvasManager);
		
		canvasManager.setProjectorArena(projectorArenaPane, bounds);
		canvasManager.getCanvasGroup().getChildren().clear();

		targets = new ArrayList<Target>();
		TargetComponents poiComponents = TargetIO.loadTarget(new File("targets/POI_Offset_Adjustment.target")).get();
		poiTarget = (TargetView) canvasManager.addTarget(
				new TargetView(poiComponents.getTargetGroup(), poiComponents.getTargetTags(), targets));
		targets.add(poiTarget);
		
		canvasManager.addTarget(new File("targets/POI_Offset_Adjustment.target"));
		
		canvasManager.getTargets().get(0).setDimensions(640, 360);
		canvasManager.getTargets().get(0).setPosition(0, 0);
	}

	
	@Test
	public void testPOIAdjust() {
		Optional<TargetRegion> r = TargetView.getTargetRegionByName(targets,
				(TargetRegion) poiTarget.getRegions().get(0), "center");

		assertTrue(r.isPresent());
		assertTrue(r.get().tagExists("name"));
		assertEquals("center", r.get().getTag("name"));
		

		assertFalse(config.isAdjustingPOI());		
		assertFalse(config.getPOIAdjustmentX().isPresent());
		assertFalse(config.getPOIAdjustmentY().isPresent());
		
		BoundsShot bShot = new BoundsShot(ShotColor.GREEN, 271, 125, 0);
		bShot.adjustBounds(100, 100);
		
		ArenaShot shot = new ArenaShot(new DisplayShot(bShot, 0));
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);


		shot = new ArenaShot(new DisplayShot(new Shot(ShotColor.GREEN, 381, 235, 0), 0));
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		shot = new ArenaShot(new DisplayShot(new Shot(ShotColor.GREEN, 381, 235, 0), 0));
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);


		shot = new ArenaShot(new DisplayShot(new Shot(ShotColor.GREEN, 381, 235, 0), 0));
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		shot = new ArenaShot(new DisplayShot(new Shot(ShotColor.GREEN, 381, 235, 0), 0));
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		assertTrue(config.isAdjustingPOI());				
		assertTrue(config.getPOIAdjustmentX().isPresent());
		assertTrue(config.getPOIAdjustmentY().isPresent());
		
		assertEquals(-7.0, config.getPOIAdjustmentX().get(), 1.0);
		assertEquals(-7.0, config.getPOIAdjustmentY().get(), 1.0);

		Shot nshot = new Shot(ShotColor.GREEN, 50, 50, 0, 0);
		nshot.adjustPOI(config.getPOIAdjustmentX().get(), config.getPOIAdjustmentY().get());
		assertEquals(43, nshot.getX(), 1);
		assertEquals(43, nshot.getY(), 1);
		
		
		shot = new ArenaShot(new DisplayShot(new Shot(ShotColor.GREEN, 381, 235, 0), 0));
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		assertFalse(config.isAdjustingPOI());		

	}
}