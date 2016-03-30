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

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.MockProjectorArenaController;
import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class TestTarget {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

	private TargetRegion tr0, trPlateRackPlate, trPepperPopper;
	private Configuration config;
	private TargetView pepperPopper;
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

		config = new Configuration(new String[0]);
		canvasManager = new MockCanvasManager(config);
		canvasManager.getCanvasGroup().getChildren().clear();

		MockProjectorArenaController arenaController = new MockProjectorArenaController();
		arenaController.init(config, canvasManager);

		targets = new ArrayList<Target>();
		pepperPopper = (TargetView) canvasManager.addTarget(
				new TargetView(TargetIO.loadTarget(new File("targets/Pepper_Popper.target")).get(), targets));
		targets.add(pepperPopper);
		targets.add(new TargetView(TargetIO.loadTarget(new File("targets/Reset.target")).get(), targets));
	}

	@Test
	public void testParseCommandNoTags() {
		TargetView.parseCommandTag(tr0, (commands, commandName, args) -> {
			assertEquals(0, commands.size());
		});
	}

	@Test
	public void testParseCommandTagDuelTree() {
		TargetView.parseCommandTag(trPlateRackPlate, (commands, commandName, args) -> {
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
		TargetView.parseCommandTag(trPepperPopper, (commands, commandName, args) -> {
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
		Optional<TargetRegion> r = TargetView.getTargetRegionByName(targets,
				(TargetRegion) pepperPopper.getRegions().get(0), "pepper_popper");

		assertTrue(r.isPresent());
		assertTrue(r.get().tagExists("name"));
		assertEquals("pepper_popper", r.get().getTag("name"));

		r = TargetView.getTargetRegionByName(targets, (TargetRegion) pepperPopper.getRegions().get(0), "not present");

		assertFalse(r.isPresent());
	}

	@Test
	public void testAnimateAndResetPepperPopper() {
		TargetRegion r = (TargetRegion) pepperPopper.getRegions().get(0);

		assertEquals(RegionType.IMAGE, r.getType());

		ImageRegion animated = (ImageRegion) r;
		assertTrue(animated.getAnimation().isPresent());
		assertTrue(animated.onFirstFrame());

		pepperPopper.animate(animated, new ArrayList<String>());

		animated.reset();

		assertTrue(animated.onFirstFrame());
	}

	@Test
	public void testAnimateAndResetFlagPepperPopper() {
		TargetRegion r = (TargetRegion) pepperPopper.getTargetGroup().getChildren().get(0);

		assertEquals(RegionType.IMAGE, r.getType());

		ImageRegion animated = (ImageRegion) r;
		assertTrue(animated.getAnimation().isPresent());
		assertTrue(animated.onFirstFrame());

		List<String> args = new ArrayList<String>();
		args.add("true");
		pepperPopper.animate(animated, args);

		assertTrue(animated.onFirstFrame());
	}

	@Test
	public void testLeftArrowKeyMoveTarget() {
		double oldX = pepperPopper.getPosition().getX();
		double oldY = pepperPopper.getPosition().getY();

		KeyEvent leftArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "left",
				"left", KeyCode.LEFT, false, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), leftArrowEvent);

		assertEquals(oldX - TargetView.MOVEMENT_DELTA, pepperPopper.getPosition().getX(), .001);
		assertEquals(oldY, pepperPopper.getPosition().getY(), .001);
	}

	@Test
	public void testRightArrowKeyMoveTarget() {
		double oldX = pepperPopper.getPosition().getX();
		double oldY = pepperPopper.getPosition().getY();

		KeyEvent rightArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "right",
				"right", KeyCode.RIGHT, false, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), rightArrowEvent);

		assertEquals(oldX + TargetView.MOVEMENT_DELTA, pepperPopper.getPosition().getX(), .001);
		assertEquals(oldY, pepperPopper.getPosition().getY(), .001);
	}

	@Test
	public void testUpArrowKeyMoveTarget() {
		double oldX = pepperPopper.getPosition().getX();
		double oldY = pepperPopper.getPosition().getY();

		KeyEvent upArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "up", "up",
				KeyCode.UP, false, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), upArrowEvent);

		assertEquals(oldX, pepperPopper.getPosition().getX(), .001);
		assertEquals(oldY - TargetView.MOVEMENT_DELTA, pepperPopper.getPosition().getY(), .001);
	}

	@Test
	public void testDownArrowKeyMoveTarget() {
		double oldX = pepperPopper.getPosition().getX();
		double oldY = pepperPopper.getPosition().getY();

		KeyEvent downArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "down",
				"down", KeyCode.DOWN, false, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), downArrowEvent);

		assertEquals(oldX, pepperPopper.getPosition().getX(), .001);
		assertEquals(oldY + TargetView.MOVEMENT_DELTA, pepperPopper.getPosition().getY(), .001);
	}

	@Test
	public void testLeftArrowKeyResizeTarget() {
		double oldWidth = pepperPopper.getDimension().getWidth();
		double oldHeight = pepperPopper.getDimension().getHeight();

		KeyEvent leftArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "left",
				"left", KeyCode.LEFT, true, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), leftArrowEvent);

		assertEquals(oldWidth + TargetView.SCALE_DELTA, pepperPopper.getDimension().getWidth(), .001);
		assertEquals(oldHeight, pepperPopper.getDimension().getHeight(), .001);
	}

	@Test
	public void testRightArrowKeyResizeTarget() {
		double oldWidth = pepperPopper.getDimension().getWidth();
		double oldHeight = pepperPopper.getDimension().getHeight();

		KeyEvent rightArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "right",
				"right", KeyCode.RIGHT, true, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), rightArrowEvent);

		assertEquals(oldWidth - TargetView.SCALE_DELTA, pepperPopper.getDimension().getWidth(), .001);
		assertEquals(oldHeight, pepperPopper.getDimension().getHeight(), .001);
	}

	@Test
	public void testUpArrowKeyResizeTarget() {
		double oldWidth = pepperPopper.getDimension().getWidth();
		double oldHeight = pepperPopper.getDimension().getHeight();

		KeyEvent upArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "up", "up",
				KeyCode.UP, true, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), upArrowEvent);

		assertEquals(oldWidth, pepperPopper.getDimension().getWidth(), .001);
		assertEquals(oldHeight + TargetView.SCALE_DELTA, pepperPopper.getDimension().getHeight(), .001);
	}

	@Test
	public void testDownArrowKeyResizeTarget() {
		double oldWidth = pepperPopper.getDimension().getWidth();
		double oldHeight = pepperPopper.getDimension().getHeight();

		KeyEvent downArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "down",
				"down", KeyCode.DOWN, true, false, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), downArrowEvent);

		assertEquals(oldWidth, pepperPopper.getDimension().getWidth(), .001);
		assertEquals(oldHeight - TargetView.SCALE_DELTA, pepperPopper.getDimension().getHeight(), .001);
	}

	@Test
	public void testUpArrowKeyProportionalResizeTarget() {
		double oldWidth = pepperPopper.getDimension().getWidth();
		double oldHeight = pepperPopper.getDimension().getHeight();

		KeyEvent upArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "up", "up",
				KeyCode.UP, true, true, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), upArrowEvent);

		assertEquals(oldWidth + TargetView.SCALE_DELTA, pepperPopper.getDimension().getWidth(), .001);
		assertEquals(oldHeight + TargetView.SCALE_DELTA, pepperPopper.getDimension().getHeight(), .001);
	}

	@Test
	public void testDownArrowKeyProportionalResizeTarget() {
		double oldWidth = pepperPopper.getDimension().getWidth();
		double oldHeight = pepperPopper.getDimension().getHeight();

		KeyEvent downArrowEvent = new KeyEvent(null, pepperPopper.getTargetGroup(), KeyEvent.KEY_PRESSED, "down",
				"down", KeyCode.DOWN, true, true, false, false);
		Event.fireEvent(pepperPopper.getTargetGroup(), downArrowEvent);

		assertEquals(oldWidth - TargetView.SCALE_DELTA, pepperPopper.getDimension().getWidth(), .001);
		assertEquals(oldHeight - TargetView.SCALE_DELTA, pepperPopper.getDimension().getHeight(), .001);
	}
}
