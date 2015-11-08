package com.shootoff.plugins;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.targets.TargetRegion;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class TestBouncingTargets {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();
	
	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private BouncingTargets bt;
	private TargetRegion shootRegion;
	private TargetRegion dontShootRegion;
	
	@Before
	public void seUp() throws ConfigurationException, IOException {
		new JFXPanel(); // Initialize the JFX toolkit
		
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		
		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);
		
		bt = new BouncingTargets();
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);

		CamerasSupervisor cs = new CamerasSupervisor(config);

	    ProjectorArenaController pac = new ProjectorArenaController();
		pac.init(config, new MockCanvasManager(config));
			
		bt.init(config, cs, null, pac);
		bt.init(6, 5, 0);
		
		shootRegion = (TargetRegion)bt.getShootTargets().get(0).getTarget().getTargetGroup().getChildren().get(0);
		dontShootRegion = (TargetRegion)bt.getDontShootTargets().get(0).getTarget().getTargetGroup().getChildren().get(0);
	}
	
	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);
	}
	
	@Test
	public void testShootFourThenDontShoot() throws UnsupportedEncodingException {
		assertEquals(6, bt.getShootTargets().size());
		assertEquals(5, bt.getDontShootTargets().size());

		assertEquals("Score: 0\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
		
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(shootRegion));
		assertEquals("Score: 1\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
		
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(shootRegion));
		assertEquals("Score: 2\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
		
		
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(shootRegion));
		assertEquals("Score: 3\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
		
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(shootRegion));
		assertEquals("Score: 4\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
	
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(dontShootRegion));
		assertEquals("sounds/beep.wav\nYour score was 4\nScore: 0\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
	}
	
	@Test
	public void testShootThenReset() throws UnsupportedEncodingException {
		assertEquals(6, bt.getShootTargets().size());
		assertEquals(5, bt.getDontShootTargets().size());

		assertEquals("Score: 0\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
		
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(shootRegion));
		assertEquals("Score: 1\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
		
		bt.reset(new ArrayList<Group>());
		bt.shotListener(new Shot(Color.GREEN, 0, 0, 0, 2), Optional.of(shootRegion));
		assertEquals("Score: 0\nScore: 1\n", stringOut.toString("UTF-8").replace("\r\n", "\n"));
		stringOut.reset();
	}
}
