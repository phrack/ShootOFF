package com.shootoff.gui;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.DisplayShot;
import com.shootoff.camera.MockCamera;
import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotColor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.gui.targets.TargetView;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.shape.Shape;

public class TestCanvasManager {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private CanvasManager cm;
	private Target ipscTarget;
	private ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();

	private Configuration config;

	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));

		nu.pattern.OpenCV.loadShared();

		config = new Configuration(new String[0]);
		CamerasSupervisor cs = new CamerasSupervisor(config);
		cm = new CanvasManager(new Group(), new ShootOFFController(), "test", shotEntries);
		CameraManager cameraManager = cs.addCameraManager(new MockCamera(), null, cm).get();
		cs.setDetectingAll(false);
		cm.setCameraManager(cameraManager);

		ipscTarget = cm.addTarget(new File("targets/IPSC.target")).get();
		ipscTarget.setPosition(0, 0);
	}

	@Test
	public void testCheckHitMiss() {
		Optional<Hit> h = cm.checkHit(new DisplayShot(ShotColor.RED, 0, 0, 0, 2), Optional.empty(), false);

		assertFalse(h.isPresent());
	}

	@Test
	public void testCheckHitHit() {
		Optional<Hit> h = cm.checkHit(new DisplayShot(ShotColor.RED, 150, 150, 0, 2), Optional.empty(), false);

		assertTrue(h.isPresent());
		assertTrue(ipscTarget.getRegions().contains(h.get().getHitRegion()));
		assertEquals(ipscTarget, h.get().getTarget());
	}

	@Test
	public void testAddShotMissHitMiss() {
		Optional<Hit> h = cm.checkHit(new DisplayShot(ShotColor.RED, 0, 0, 0, 2), Optional.empty(), false);

		assertFalse(h.isPresent());

		h = cm.checkHit(new DisplayShot(ShotColor.RED, 150, 150, 0, 2), Optional.empty(), false);

		assertTrue(h.isPresent());
		assertTrue(ipscTarget.getRegions().contains(h.get().getHitRegion()));
		assertEquals(ipscTarget, h.get().getTarget());

		h = cm.checkHit(new DisplayShot(ShotColor.GREEN, 0, 0, 0, 2), Optional.empty(), false);

		assertFalse(h.isPresent());
	}

	// TODO: Add infrared?

	@Test
	public void testWebCodeRed() {
		assertEquals("#FF0000", CanvasManager.colorToWebCode(Shot.colorMap.get(ShotColor.RED)));
	}

	@Test
	public void testWebCodeGreen() {
		assertEquals("#008000", CanvasManager.colorToWebCode(Shot.colorMap.get(ShotColor.GREEN)));
	}

	@Test
	public void testRemoveTarget() {
		cm.removeTarget(ipscTarget);

		assertEquals(0, cm.getTargets().size());
	}

	@Test
	public void testGetTargetGroups() {
		assertEquals(1, cm.getTargets().size());
		assertTrue(cm.getTargets().contains(ipscTarget));
	}

	@Test
	public void testTargetSelection() {
		Shape firstShape = (Shape) ipscTarget.getRegions().get(0);

		assertEquals(null, firstShape.getStroke());

		cm.toggleTargetSelection(Optional.of((TargetView) ipscTarget));

		assertEquals(TargetRegion.SELECTED_STROKE_COLOR, firstShape.getStroke());

		cm.toggleTargetSelection(Optional.empty());

		assertEquals(TargetRegion.UNSELECTED_STROKE_COLOR, firstShape.getStroke());
	}

	@Test
	public void testAddShot() {
		assertEquals(0, cm.getShots().size());

		DisplayShot shot = new DisplayShot(new Shot(ShotColor.RED, 0, 0, 0), 2);
		cm.addShot(shot, false);

		assertEquals(1, cm.getShots().size());
	}

	@Test
	public void testDisplayResolutionTranslationLarger() {
		config.setDisplayResolution(800, 600);
		cm.getCameraManager().setFeedResolution(640, 480);

		assertEquals(0, cm.getShots().size());

		cm.getCameraManager().injectShot(ShotColor.RED, 640, 480, true);

		// This sleep is to give the shot notification thread a chance
		// to do its job
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		assertEquals(1, cm.getShots().size());

		assertEquals(800, cm.getShots().get(0).getX(), 1.0);
		assertEquals(600, cm.getShots().get(0).getY(), 1.0);
	}

	@Test
	public void testDisplayResolutionTranslationSmaller() {
		config.setDisplayResolution(320, 240);
		cm.getCameraManager().setFeedResolution(640, 480);

		assertEquals(0, cm.getShots().size());

		cm.getCameraManager().injectShot(ShotColor.RED, 640, 480, true);

		// This sleep is to give the shot notification thread a chance
		// to do its job
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		assertEquals(1, cm.getShots().size());

		assertEquals(320, cm.getShots().get(0).getX(), 1.0);
		assertEquals(240, cm.getShots().get(0).getY(), 1.0);
	}

	@Test
	public void testClickToShoot() {
		config.setDisplayResolution(320, 240);

		assertEquals(0, cm.getShots().size());

		cm.getCameraManager().injectShot(ShotColor.RED, 320, 240, false);

		// This sleep is to give the shot notification thread a chance
		// to do its job
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		assertEquals(1, cm.getShots().size());

		assertEquals(320, cm.getShots().get(0).getX(), 1.0);
		assertEquals(240, cm.getShots().get(0).getY(), 1.0);
	}

	@Test
	public void testPOIAdjust() {
		config.setDisplayResolution(640, 480);
		config.updatePOIAdjustment(-10.0, -10.0);
		config.updatePOIAdjustment(-10.0, -10.0);
		config.updatePOIAdjustment(-10.0, -10.0);
		config.updatePOIAdjustment(-10.0, -10.0);
		config.updatePOIAdjustment(-10.0, -10.0);

		assertEquals(0, cm.getShots().size());

		cm.getCameraManager().injectShot(ShotColor.RED, 160, 160, false);

		// This sleep is to give the shot notification thread a chance
		// to do its job
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		assertEquals(1, cm.getShots().size());

		assertEquals(170, cm.getShots().get(0).getX(), 1.0);
		assertEquals(170, cm.getShots().get(0).getY(), 1.0);
		
		config.updatePOIAdjustment(-10.0, -10.0);
		
		cm.getCameraManager().reset();
		
		cm.getCameraManager().injectShot(ShotColor.RED, 160, 160, false);

		// This sleep is to give the shot notification thread a chance
		// to do its job
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		assertEquals(1, cm.getShots().size());

		assertEquals(160, cm.getShots().get(0).getX(), 1.0);
		assertEquals(160, cm.getShots().get(0).getY(), 1.0);
		
	}

}
