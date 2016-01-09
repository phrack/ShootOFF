/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.plugins;

import com.shootoff.camera.Shot;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.targets.TargetRegion;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class NordicQuickShooting extends TrainingExerciseBase implements TrainingExercise, DelayedStartListener {
	private static final Logger logger = LoggerFactory.getLogger(NordicQuickShooting.class);

	private final static String SCORE_COL_NAME = "Score";
	private final static int SCORE_COL_WIDTH = 60;
	private final static String ROUND_COL_NAME = "Round";
	private final static int ROUND_COL_WIDTH = 80;
	private final static int START_DELAY = 10; // s
	private static final int CORE_POOL_SIZE = 4;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
	private ScheduledFuture<Void> endRound;
	private TrainingExerciseBase thisSuper;
	private int round = 1;
	private int shotCount = 0;
	private int runningScore = 0;
	private int showDelay = 7;
	private int roundTime = 3;
	private boolean repeatExercise = true;
	private boolean coloredRows = false;
	private boolean testing = false;

	public NordicQuickShooting() {}

	public NordicQuickShooting(List<Group> targets) {
		super(targets);
		thisSuper = super.getInstance();
		setInitialValues();
	}

	private void setInitialValues() {
		round = 1;
		shotCount = 0;
		runningScore = 0;
	}

	@Override
	public void init() {
		super.pauseShotDetection(true);
		super.getDelayedStartInterval(this);

		startExercise();
	}

	// For testing
	protected void init(int delay) {
		this.showDelay = delay;

		testing = true;

		startExercise();
	}

	private void startExercise() {
		super.addShotTimerColumn(SCORE_COL_NAME, SCORE_COL_WIDTH);
		super.addShotTimerColumn(ROUND_COL_NAME, ROUND_COL_WIDTH);

		if (!testing) {
			executorService.schedule(new SetupWait(), showDelay, TimeUnit.SECONDS);
		} else {
			new SetupWait().call();
		}
	}

	@Override
	public void updatedDelayedStartInterval(int min, int max) {

	}


	private class SetupWait implements Callable<Void> {
		@Override
		public Void call() {
			if (repeatExercise) {
				TrainingExerciseBase.playSound(new File("sounds/voice/shootoff-makeready.wav"));
				if (!testing) {
					executorService.schedule(new StartRound(), START_DELAY, TimeUnit.SECONDS);
				} else {
					new StartRound().call();
				}
			}

			return null;
		}
	}

	private class StartRound implements Callable<Void> {
		@Override
		public Void call() {

			if (repeatExercise) {
				if (coloredRows) {
					thisSuper.setShotTimerRowColor(Color.LIGHTGRAY);
				} else {
					thisSuper.setShotTimerRowColor(null);
				}

				coloredRows = !coloredRows;

				TrainingExerciseBase.playSound("sounds/beep.wav");
				thisSuper.pauseShotDetection(false);
				endRound = executorService.schedule(new EndRound(), roundTime, TimeUnit.SECONDS);
			}

			return null;
		}
	}

	private class EndRound implements Callable<Void> {
		@Override
		public Void call() {

				thisSuper.pauseShotDetection(true);
				TrainingExerciseBase.playSound("sounds/chime.wav");

				if (round < 4) {
					// Go to next round
					if (!testing) {
						executorService.schedule(new StartRound(), showDelay, TimeUnit.SECONDS);
					} else {
						new StartRound().call();
					}
				} else if (round > 4) {
					if (repeatExercise) {
						// Go to round 1 for next time
						round = 1;
						if (!testing) {
							executorService.schedule(new StartRound(), showDelay, TimeUnit.SECONDS);
						} else {
							new StartRound().call();
						}
					}
				} else {
					TextToSpeech.say("Event over... Your score is " + runningScore);
					thisSuper.pauseShotDetection(false);
					// At this point we end and the user has to hit reset to
					// start again
				}


			return null;
		}
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Nordic Quick Shot event", "1.0", "oluies",
				"This exercise implements the Nordic Quick Shot event ( Snabbpistol) described at: "
						+ "http://www.pistolskytteforbundet.se/om-pistolskytte/snabbskjutning.  "
						+ " In Nordic region and rules (Sweden, Finland, Norway, Denmark) "
						+ " there is a event type called Snabbpistol earlier \"duel\". "
						+ "One shoots at a target with 5 shots. "
						+ "The target is turned away and is shown after 10 seconds, shown 3, gone 7 and so on (5 times)."
				        + "You can use any scored target with this exercise, but use "
						+ "the ISSF target for the most authentic experience.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		shotCount++;

		int hitScore = 0;

		if (hitRegion.isPresent()) {
			TargetRegion r = hitRegion.get();

			if (r.tagExists("points")) {
				hitScore = Integer.parseInt(r.getTag("points"));
				runningScore += hitScore;
			}

			StringBuilder message = new StringBuilder();

			super.showTextOnFeed(message.toString() + "total score: " + runningScore);
		}

		String currentRound = String.format("R%d (%ds)", round, showDelay);
		super.setShotTimerColumnText(SCORE_COL_NAME, String.valueOf(hitScore));
		super.setShotTimerColumnText(ROUND_COL_NAME, currentRound);

		if (shotCount == 1 && !endRound.isDone()) {
			try {
				thisSuper.pauseShotDetection(true);
				endRound.cancel(true);
				new EndRound().call();
			} catch (Exception e) {
				logger.error("Error ending current  round", e);
			}
		}

	}

	@Override
	public void reset(List<Group> targets) {
		super.pauseShotDetection(true);

		repeatExercise = false;
		executorService.shutdownNow();

		setInitialValues();

		thisSuper.setShotTimerRowColor(null);
		super.showTextOnFeed("");

		repeatExercise = true;
		executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() {
		repeatExercise = false;
		executorService.shutdownNow();
		super.destroy();
	}
}