package com.shootoff.gui;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class TestCanvasManager {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();
	
	private CanvasManager cm;
	private Target ipscTarget;
	private ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
	
	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		
		Configuration config = new Configuration(new String[0]);
		cm = new CanvasManager(new Group(), config, new CamerasSupervisor(config), "test", shotEntries);
	
		ipscTarget = cm.addTarget(new File("targets/IPSC.target")).get();
		ipscTarget.setPosition(0, 0);
	}
	
	@Test
	public void testCheckHitMiss() {
		Optional<CanvasManager.Hit> h = cm.checkHit(new Shot(Color.RED, 0, 0, 0, 2));
		
		assertFalse(h.isPresent());
	}
	
	@Test
	public void testCheckHitHit() {
		Optional<CanvasManager.Hit> h = cm.checkHit(new Shot(Color.RED, 150, 150, 0, 2));
		
		assertTrue(h.isPresent());	
		assertTrue(ipscTarget.getTargetGroup().getChildren().contains(h.get().getHitRegion()));
		assertEquals(ipscTarget, h.get().getTarget());
	}
	
	@Test
	public void testAddShotMissHitMiss() {
		Optional<CanvasManager.Hit> h = cm.checkHit(new Shot(Color.RED, 0, 0, 0, 2));
		
		assertFalse(h.isPresent());
		
		h = cm.checkHit(new Shot(Color.RED, 150, 150, 0, 2));
		
		assertTrue(h.isPresent());	
		assertTrue(ipscTarget.getTargetGroup().getChildren().contains(h.get().getHitRegion()));
		assertEquals(ipscTarget, h.get().getTarget());
		
		h = cm.checkHit(new Shot(Color.GREEN, 0, 0, 0, 2));
		
		assertFalse(h.isPresent());
	}
}
