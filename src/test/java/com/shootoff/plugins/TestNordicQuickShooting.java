package com.shootoff.plugins;

import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.JavaFXThreadingRule;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class TestNordicQuickShooting {
	@Rule public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();
	
	private PrintStream originalOut;
	private ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	private PrintStream stringOutStream;
	private List<Group> targetGroups;
	private NordicQuickShooting nqsExercise;
	private TargetRegion scoredRegion;
	private int regionScore;
	
	@Before
	public void setUp() throws UnsupportedEncodingException, ConfigurationException {
		new JFXPanel(); // Initialize the JFX toolkit
		
		stringOutStream = new PrintStream(stringOut, false, "UTF-8");
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		
		TextToSpeech.silence(true);
		TrainingExerciseBase.silence(true);
		originalOut = System.out;
		System.setOut(stringOutStream);
		
		Configuration config = new Configuration(new String[0]);
		config.setDebugMode(true);
		
		CamerasSupervisor cs = new CamerasSupervisor(config);
		
		List<Target> targets = new ArrayList<Target>();
		targetGroups = new ArrayList<Group>();
		Target issfTarget = new Target(TargetIO.loadTarget(new File("targets/ISSF.target")).get(), targets);
		targets.add(issfTarget);
		targetGroups.add(issfTarget.getTargetGroup());
		scoredRegion = (TargetRegion)issfTarget.getTargetGroup().getChildren().get(0);
		regionScore = Integer.parseInt(scoredRegion.getTag("points"));
		
		nqsExercise = new NordicQuickShooting(targetGroups);
		TableView<ShotEntry> shotTimerTable = new TableView<ShotEntry>();
		ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
		shotEntries.add(new ShotEntry(new Shot(Color.RED, 0, 0, 0, 2), Optional.empty(), Optional.empty(), false, false));
		shotTimerTable.setItems(shotEntries);
		nqsExercise.init(config, cs, null, shotTimerTable);
		nqsExercise.init(0);
	}
	
	@After
	public void tearDown() {
		TextToSpeech.silence(false);
		TrainingExerciseBase.silence(false);
		System.setOut(originalOut);
	}
	
	private String getScoreString(int roundOne, int roundTwo, int roundThree, boolean roundOver, boolean gameOver) {
		String scoreString = String.format("150s score: %d%n20s score: %d%n10s score: %d%ntotal score: %d%n",
				roundOne, roundTwo, roundThree, roundOne + roundTwo + roundThree);
		
		if (gameOver) {
			return scoreString + String.format("sounds/voice/shootoff-roundover.wav%nEvent over... Your score is 60%n").replace('/', File.separatorChar);
		}
		
		if (roundOver) {
			return scoreString + String.format("sounds/voice/shootoff-roundover.wav%nsounds/beep.wav%n").replace('/', File.separatorChar);
		}
		
		return scoreString;
	}
	
	@Test
	public void testFullRound() throws UnsupportedEncodingException {
		assertEquals(String.format("sounds/voice/shootoff-makeready.wav%nsounds/beep.wav%n"), stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();


		// 10s round 1-4
		for (int i = 0; i < 4; i++) {
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			// (regionScore * 5 * i) = last round's score
			assertEquals(getScoreString(regionScore * 20, regionScore * 20, (regionScore * 5 * i) + regionScore, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString(regionScore * 20, regionScore * 20, (regionScore * 5 * i) + regionScore * 2, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString(regionScore * 20, regionScore * 20, (regionScore * 5 * i) + regionScore * 3, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString(regionScore * 20, regionScore * 20, (regionScore * 5 * i) + regionScore * 4, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			boolean gameOver = i == 3;
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString(regionScore * 20, regionScore * 20, (regionScore * 5 * i) + regionScore * 5, true, gameOver), stringOut.toString("UTF-8"));
			stringOut.reset();
		}
	}
	
	@Test
	public void testFull150sThenReset() throws UnsupportedEncodingException {
		assertEquals(String.format("sounds/voice/shootoff-makeready.wav%nsounds/beep.wav%n"), stringOut.toString("UTF-8").replace(File.separatorChar, '/'));
		stringOut.reset();

		// 150s round 1-4
		for (int i = 0; i < 4; i++) {
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			// (regionScore * 5 * i) = last round's score
			assertEquals(getScoreString((regionScore * 5 * i) + regionScore, 0, 0, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString((regionScore * 5 * i) + regionScore * 2, 0, 0, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString((regionScore * 5 * i) + regionScore * 3, 0, 0, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString((regionScore * 5 * i) + regionScore * 4, 0, 0, false, false), stringOut.toString("UTF-8"));
			stringOut.reset();
			
			nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
			assertEquals(getScoreString((regionScore * 5 * i) + regionScore * 5, 0, 0, true, false), stringOut.toString("UTF-8"));
			stringOut.reset();
		}
		
		nqsExercise.reset(targetGroups);
		
		nqsExercise.shotListener(new Shot(Color.RED, 0, 0, 0, 2), Optional.of(scoredRegion));
		// (regionScore * 5 * i) = last round's score
		assertEquals(String.format("%n") + getScoreString(regionScore, 0, 0, false, false), stringOut.toString("UTF-8"));
		stringOut.reset();
	}
}
