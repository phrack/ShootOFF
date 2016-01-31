package com.shootoff.gui;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.MockCamera;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.targets.TargetRegion;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.paint.Color;
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
		
		config = new Configuration(new String[0]);
		config.setDebugMode(true);
		CamerasSupervisor cs = new CamerasSupervisor(config);
		cm = new CanvasManager(new Group(), config, cs, "test", shotEntries);
		CameraManager cameraManager = cs.addCameraManager(new MockCamera(), cm);
		cs.setDetectingAll(false);
		cm.setCameraManager(cameraManager);
	
		ipscTarget = cm.addTarget(new File("targets/IPSC.target")).get();
		ipscTarget.setPosition(0, 0);
	}
	
	@Test
	public void testCheckHitMiss() {
		Optional<CanvasManager.Hit> h = cm.checkHit(new Shot(Color.RED, 0, 0, 0, 2), Optional.empty());
		
		assertFalse(h.isPresent());
	}
	
	@Test
	public void testCheckHitHit() {
		Optional<CanvasManager.Hit> h = cm.checkHit(new Shot(Color.RED, 150, 150, 0, 2), Optional.empty());
		
		assertTrue(h.isPresent());	
		assertTrue(ipscTarget.getTargetGroup().getChildren().contains(h.get().getHitRegion()));
		assertEquals(ipscTarget, h.get().getTarget());
	}
	
	@Test
	public void testAddShotMissHitMiss() {
		Optional<CanvasManager.Hit> h = cm.checkHit(new Shot(Color.RED, 0, 0, 0, 2), Optional.empty());
		
		assertFalse(h.isPresent());
		
		h = cm.checkHit(new Shot(Color.RED, 150, 150, 0, 2), Optional.empty());
		
		assertTrue(h.isPresent());	
		assertTrue(ipscTarget.getTargetGroup().getChildren().contains(h.get().getHitRegion()));
		assertEquals(ipscTarget, h.get().getTarget());
		
		h = cm.checkHit(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.empty());
		
		assertFalse(h.isPresent());
	}
	
	@Test
	public void testWebCodeRed() {
		assertEquals("#FF0000", CanvasManager.colorToWebCode(Color.RED));
	}
	
	@Test
	public void testWebCodeGreen() {
		assertEquals("#008000", CanvasManager.colorToWebCode(Color.GREEN));
	}
	
	@Test
	public void testRemoveTarget() {
		cm.removeTarget(ipscTarget);
		
		assertEquals(0, cm.getTargets().size());
	}
	
	@Test
	public void testGetTargetGroups() {
		assertEquals(1, cm.getTargetGroups().size());
		assertTrue(cm.getTargetGroups().contains(ipscTarget.getTargetGroup()));
	}
	
	@Test
	public void testTargetSelection() {
		Shape firstShape = (Shape)ipscTarget.getTargetGroup().getChildren().get(0);
		
		assertEquals(null, firstShape.getStroke());
		
		cm.toggleTargetSelection(Optional.of(ipscTarget.getTargetGroup()));
		
		assertEquals(TargetRegion.SELECTED_STROKE_COLOR, firstShape.getStroke());
		
		cm.toggleTargetSelection(Optional.empty());
		
		assertEquals(TargetRegion.UNSELECTED_STROKE_COLOR, firstShape.getStroke());
	}
	
	@Test
	public void testAddShot() {
		assertEquals(0, cm.getShots().size());
		
		cm.addShot(Color.RED, 0, 0);
		
		assertEquals(1, cm.getShots().size());
	}
	
	@Test
	public void testDisplayResolutionTranslationLarger() {
		config.setDisplayResolution(800, 600);
		
		assertEquals(0, cm.getShots().size());
		
		cm.addShot(Color.RED, 640, 480);
		
		assertEquals(1, cm.getShots().size());
		
		assertEquals(800, cm.getShots().get(0).getX(), 1.0);
		assertEquals(600, cm.getShots().get(0).getY(), 1.0);
	}
	
	@Test
	public void testDisplayResolutionTranslationSmaller() {
		config.setDisplayResolution(320, 240);
		
		assertEquals(0, cm.getShots().size());
		
		cm.addShot(Color.RED, 640, 480);
		
		assertEquals(1, cm.getShots().size());
		
		assertEquals(320, cm.getShots().get(0).getX(), 1.0);
		assertEquals(240, cm.getShots().get(0).getY(), 1.0);
	}
	
	@Test
	public void testClickToShoot() {
		config.setDisplayResolution(320, 240);
		
		assertEquals(0, cm.getShots().size());
		
		cm.addShot(Color.RED, 320, 240, true);
		
		assertEquals(1, cm.getShots().size());
		
		assertEquals(320, cm.getShots().get(0).getX(), 1.0);
		assertEquals(240, cm.getShots().get(0).getY(), 1.0);
	}
}
