package com.shootoff.gui;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class TestTarget {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();
	
	private TargetRegion tr0, trPlateRackPlate, trPepperPopper;
	private Target pepperPopper;
	private List<Target> targets;
	private CanvasManager canvasManager;
	
	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		
		tr0 = new EllipseRegion(0, 0, 10, 10);
		
		trPlateRackPlate = new EllipseRegion(0, 0, 10, 10);
		Map<String, String> tags1 = new HashMap<String, String>();
		tags1.put("command", "animate;reverse;play_sound(sounds/steel_sound_1.wav)");
		trPlateRackPlate.setTags(tags1);
		
		trPepperPopper = new EllipseRegion(0, 0, 10, 10);
		Map<String, String> tags2 = new HashMap<String, String>();
		tags2.put("command", "animate(pepper_popper);play_sound(sounds/steel_sound_1.wav,pepper_popper)");
		trPepperPopper.setTags(tags2);
	
		Configuration config = new Configuration(new String[0]);
		canvasManager = new CanvasManager(new Group(), config, new CamerasSupervisor(config), "test", FXCollections.observableArrayList());
		
		ProjectorArenaController arenaController = new ProjectorArenaController();
		arenaController.init(config, canvasManager);
		
		targets = new ArrayList<Target>();
		pepperPopper = canvasManager.addTarget(new File("targets/pepper_popper.target")).get();
		targets.add(pepperPopper);
		targets.add(new Target(TargetIO.loadTarget(new File("targets/reset.target")).get(), targets));
		
	}

	@Test
	public void testParseCommandNoTags() {
		Target.parseCommandTag(tr0, (commands, commandName, args) -> {
				assertEquals(0, commands.size());
			});
	}
	
	@Test
	public void testParseCommandTagDuelTree() {
		Target.parseCommandTag(trPlateRackPlate, (commands, commandName, args) -> {
				assertEquals(3, commands.size());
				
				switch (commandName) {
				case "animate":
					assertEquals(0, args.size());
					break;
					
				case "reverse":
					assertEquals(0, args.size());
					break;
					
				case "play_sound":
					assertEquals(1, args.size());
					assertEquals("sounds/steel_sound_1.wav", args.get(0));
					break;

				default:
					fail("Unexpected command tag: " + commandName);
					break;
				}
			});
	}
	
	@Test
	public void testParseCommandTagPepperPopper() {
		Target.parseCommandTag(trPepperPopper, (commands, commandName, args) -> {
				assertEquals(2, commands.size());
				
				switch (commandName) {
				case "animate":
					assertEquals(1, args.size());
					assertEquals("pepper_popper", args.get(0));
					break;
					
				case "play_sound":
					assertEquals(2, args.size());
					assertEquals("sounds/steel_sound_1.wav", args.get(0));
					assertEquals("pepper_popper", args.get(1));
					break;

				default:
					fail("Unexpected command tag: " + commandName);
					break;
				}
			});
	}
	
	@Test
	public void testGetTargetRegionByName() {
		Optional<TargetRegion> r = Target.getTargetRegionByName(targets, 
				(TargetRegion)pepperPopper.getTargetGroup().getChildren().get(0), "pepper_popper");
	
		assertTrue(r.isPresent());
		assertTrue(r.get().tagExists("name"));
		assertEquals("pepper_popper", r.get().getTag("name"));
		
		r = Target.getTargetRegionByName(targets, (TargetRegion)pepperPopper.getTargetGroup().getChildren().get(0), 
				"not present");

		assertFalse(r.isPresent());
	}
	
	@Test
	public void testAnimateAndResetPepperPopper() {
		TargetRegion r = (TargetRegion)pepperPopper.getTargetGroup().getChildren().get(0);
		
		assertEquals(RegionType.IMAGE, r.getType());
		
		ImageRegion animated = (ImageRegion)r;
		assertTrue(animated.getAnimation().isPresent());
		assertTrue(animated.onFirstFrame());
		
		pepperPopper.animate(animated, new ArrayList<String>());

		animated.reset();
		
		assertTrue(animated.onFirstFrame());
	}
	
	@Test
	public void testAnimateAndResetFlagPepperPopper() {
		TargetRegion r = (TargetRegion)pepperPopper.getTargetGroup().getChildren().get(0);
		
		assertEquals(RegionType.IMAGE, r.getType());
		
		ImageRegion animated = (ImageRegion)r;
		assertTrue(animated.getAnimation().isPresent());
		assertTrue(animated.onFirstFrame());
		
		List<String> args = new ArrayList<String>();
		args.add("true");
		pepperPopper.animate(animated, args);
		
		assertTrue(animated.onFirstFrame());
	}
}
