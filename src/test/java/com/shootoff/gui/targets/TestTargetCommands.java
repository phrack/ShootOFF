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
import com.shootoff.camera.ShotColor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.controller.MockProjectorArenaController;
import com.shootoff.gui.pane.ProjectorArenaPane;
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
		
		System.out.println(canvasManager.getTargets().get(0).getDimension());

		assertFalse(config.isAdjustingPOI());		
		assertFalse(config.getPOIAdjustmentX().isPresent());
		assertFalse(config.getPOIAdjustmentY().isPresent());
		
		Shot shot = new Shot(ShotColor.GREEN, 371, 225, 0, 0);
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		shot = new Shot(ShotColor.GREEN, 381, 235, 0, 0);
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		shot = new Shot(ShotColor.GREEN, 381, 235, 0, 0);
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);


		shot = new Shot(ShotColor.GREEN, 381, 235, 0, 0);
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		shot = new Shot(ShotColor.GREEN, 381, 235, 0, 0);
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		assertTrue(config.isAdjustingPOI());				
		assertTrue(config.getPOIAdjustmentX().isPresent());
		assertTrue(config.getPOIAdjustmentY().isPresent());
		
		assertEquals(config.getPOIAdjustmentX().get(), -7.0, 1.0);
		assertEquals(config.getPOIAdjustmentY().get(), -7.0, 1.0);

		shot = new Shot(ShotColor.GREEN, 50, 50, 0, 0);
		shot.adjustCoords(config.getPOIAdjustmentX().get(), config.getPOIAdjustmentY().get());
		assertEquals(shot.getX(), 43, 1);
		assertEquals(shot.getY(), 43, 1);
		
		
		shot = new Shot(ShotColor.GREEN, 381, 235, 0, 0);
		canvasManager.scaleShotToArenaBounds(shot);
		canvasManager.addArenaShot(shot, Optional.empty(), false);

		assertFalse(config.isAdjustingPOI());		

	}
}