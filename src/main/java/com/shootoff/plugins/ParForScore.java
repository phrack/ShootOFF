package com.shootoff.plugins;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.shootoff.camera.Shot;
import com.shootoff.gui.ParListener;
import com.shootoff.targets.TargetRegion;

import javafx.scene.Group;
import javafx.scene.paint.Color;

/**
 * Merge of TimedHolsterDrill and ShootForScore, with the addition of a PAR
 * interval during which scores are counted.
 * 
 * @author Edward Kort
 *
 */
public class ParForScore extends TimedHolsterDrill implements ParListener {
	protected double parTime = 2.0;

	private final static String POINTS_COL_NAME = "Score";
	private final static int POINTS_COL_WIDTH = 60;

	private int redScore = 0;
	private int greenScore = 0;

	private boolean countScore = false;

	public ParForScore() {}

	public ParForScore(List<Group> targets) {
		super(targets);
		this.thisSuper = super.getInstance();
	}

	@Override
	public void init() {
		addShotTimerColumn(LENGTH_COL_NAME, LENGTH_COL_WIDTH);
		addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
		pauseShotDetection(true);
		getParInterval(this);

		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("PAR Drill with Score", "1.0", "Edward Kort",
				"This exercise does not require a target, but one may be used "
						+ "to give the shooter something to shoot at. If a target with "
						+ "score areas is used, the scores are displayed and tracked. "
						+ "When the exercise is started you are asked to enter a range "
						+ "for randomly delayed starts, and for the interval (PAR time) "
						+ "in which those scores will be counted. You are then given 10 "
						+ "seconds to position yourself. After a random wait (within "
						+ "the entered range) a beep tells you to draw the pistol from "
						+ "its holster and fire at your target; a chime signals the end "
						+ "of the Par time, to finally re-holster. This process is "
						+ "repeated as long as this exercise is on.");
	}

	/*
	 * This class is copied from the parent class, TimedHolsterDrill. Otherwise
	 * the parent Round is called, rather than the one included in this class. A
	 * factory pattern for Round creation would alleviate this issue.
	 */
	protected class SetupWait implements Callable<Void> {
		@Override
		public Void call() {
			TrainingExerciseBase.playSound(new File("sounds/voice/shootoff-makeready.wav"));
			int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;

			if (repeatExercise) {
				executorService.schedule(new Round(), randomDelay, TimeUnit.SECONDS);
			}
			return null;
		}
	}

	/*
	 * Java does not support subclassing inner classes. Thus, much of this code
	 * is copied from the parent class, TimedHolsterDrill.
	 */
	protected class Round implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			if (repeatExercise) {
				if (coloredRows) {
					thisSuper.setShotTimerRowColor(Color.LIGHTGRAY);
				} else {
					thisSuper.setShotTimerRowColor(null);
				}

				coloredRows = !coloredRows;

				TrainingExerciseBase.playSound("sounds/beep.wav");
				thisSuper.pauseShotDetection(false);
				countScore = true;
				beepTime = System.currentTimeMillis();

				Thread.sleep((long) (parTime * 1000.));
				TrainingExerciseBase.playSound("sounds/chime.wav");
				thisSuper.pauseShotDetection(true);
				countScore = false;

				int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;
				executorService.schedule(new Round(), randomDelay, TimeUnit.SECONDS);
			}

			return null;
		}
	}

	/*
	 * This method merges shotListener for TimedHolsterDrill and ShootForScore.
	 */
	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		super.shotListener(shot, hitRegion);

		if (!hitRegion.isPresent() || !countScore)
			return;

		if (hitRegion.get().tagExists("points")) {
			setShotTimerColumnText(POINTS_COL_NAME, hitRegion.get().getTag("points"));

			if (shot.getColor().equals(Color.RED)) {
				redScore += Integer.parseInt(hitRegion.get().getTag("points"));
			} else if (shot.getColor().equals(Color.GREEN)) {
				greenScore += Integer.parseInt(hitRegion.get().getTag("points"));
			}
		}

		String message = "score: 0";

		if (redScore > 0 && greenScore > 0) {
			message = String.format("red score: %d%ngreen score: %d", redScore, greenScore);
		}

		if (redScore > 0 && greenScore > 0) {
			message = String.format("red score: %d%ngreen score: %d", redScore, greenScore);
		} else if (redScore > 0) {
			message = String.format("red score: %d", redScore);
		} else if (greenScore > 0) {
			message = String.format("green score: %d", greenScore);
		}

		showTextOnFeed(message);
	}

	@Override
	public void reset(List<Group> targets) {
		repeatExercise = false;
		executorService.shutdownNow();
		redScore = 0;
		greenScore = 0;
		showTextOnFeed("score: 0");
		getParInterval(this);
		repeatExercise = true;
		executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);
	}

	/*
	 * Copied from parent, TimedHolsterDrill, in order to invoke the SetupWait
	 * class in this child.
	 */
	@Override
	public void resumeExercise() {
		repeatExercise = true;
		executorService.schedule(new SetupWait(), RESUME_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void updatedParInterval(double parTime) {
		this.parTime = parTime;
	}
}
