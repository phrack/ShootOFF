package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

public class TestDuelingTree {
	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private List<Group> targets;
	private List<TargetRegion> leftPaddles;
	private List<TargetRegion> rightPaddles;
	private DuelingTree dt;
	
	@Before
	public void setUp() throws ConfigurationException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
		new JFXPanel(); // Initialize the JFX toolkit
		
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		
		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);
		
		targets = new ArrayList<Group>();
		Group duelTree = TargetIO.loadTarget(new File("targets" + File.separator + 
				"duel_tree.target")).get();
		targets.add(duelTree);
		
		leftPaddles = new ArrayList<TargetRegion>();
		rightPaddles = new ArrayList<TargetRegion>();
		
		for (Node node : duelTree.getChildren()) {
			TargetRegion region = (TargetRegion)node;
			
			if (region.tagExists("subtarget") && region.getTag("subtarget").startsWith("left_paddle")) {
				leftPaddles.add(region);
			} else if (region.tagExists("subtarget") && region.getTag("subtarget").startsWith("right_paddle")) {
				rightPaddles.add(region);
			}
		}
		
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		dt = new DuelingTree(targets);
		dt.init(config, new CamerasSupervisor(config), null, null);
		
		config.setExercise(dt);
		
		// Set the wait to zero
		Field delayConstant = dt.getClass().getDeclaredField("NEW_ROUND_DELAY");
		delayConstant.setAccessible(true);
	    Field modifiersField = Field.class.getDeclaredField("modifiers");
	    modifiersField.setAccessible(true);
	    modifiersField.setInt(delayConstant, delayConstant.getModifiers() & ~Modifier.FINAL);
		delayConstant.setInt(dt, 0);
	}
	
	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);
	}

	@Test
	public void testNoTarget() throws IOException, ConfigurationException {
		List<Group> targets = new ArrayList<Group>();
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);
		
		DuelingTree dt = new DuelingTree(targets);
		dt.init(config, new CamerasSupervisor(config), null, null);
		
		assertEquals(String.format("sounds/voice/shootoff-duelingtree-warning.wav%n"), stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();
		
		dt.reset(targets);
		
		assertEquals(String.format("left score: 0%n")
				      + String.format("right score: 0%n")
				      + String.format("sounds/voice/shootoff-duelingtree-warning.wav%n"), stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();
	}
	
	@Test
	public void testOneRoundsLeftWins() throws UnsupportedEncodingException {
		for (TargetRegion leftPaddle : leftPaddles) {
			dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(leftPaddle));
		}
		
		assertEquals(String.format("left score: 1%n")
			      + String.format("right score: 0%n"), stringOut.toString("UTF-8"));
		stringOut.reset();
	
		dt.destroy();
		assertEquals("", stringOut.toString("UTF-8"));
		stringOut.reset();
	}
	
	@Test
	public void testTwoSeparateRoundsEachSideWinsOnce() throws UnsupportedEncodingException {
		// Let right shoot two paddles then have left come in for the win
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddles.get(0)));
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddles.get(1)));
		
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddles.get(0)));
		dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddles.get(1)));
		
		for (TargetRegion leftPaddle : leftPaddles) {
			dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(leftPaddle));
		}		
		
		assertEquals(String.format("left score: 1%n")
			      + String.format("right score: 0%n"), stringOut.toString("UTF-8"));
		stringOut.reset();
		
		dt.reset(targets);

		assertEquals(String.format("left score: 0%n")
			      + String.format("right score: 0%n"), stringOut.toString("UTF-8"));
		stringOut.reset();
		
		// Right pulls out the win with no competition
		for (TargetRegion rightPaddle : rightPaddles) {
			dt.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(rightPaddle));
		}		
		
		assertEquals(String.format("left score: 0%n")
			      + String.format("right score: 1%n"), stringOut.toString("UTF-8"));
		stringOut.reset();
		
		dt.destroy();
		assertEquals("", stringOut.toString("UTF-8"));
		stringOut.reset();
	}
}
