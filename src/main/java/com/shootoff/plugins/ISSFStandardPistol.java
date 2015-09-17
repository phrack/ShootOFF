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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.scene.Group;
import javafx.scene.paint.Color;

import com.shootoff.camera.Shot;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.targets.TargetRegion;

public class ISSFStandardPistol extends TrainingExerciseBase implements TrainingExercise, DelayedStartListener {
	private final static String SCORE_COL_NAME = "Score";
	private final static int SCORE_COL_WIDTH = 60;
	private final static String ROUND_COL_NAME = "Round";
	private final static int ROUND_COL_WIDTH = 60;
	private final static int START_DELAY = 10; // s
	private static final int CORE_POOL_SIZE = 4;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
	private ScheduledFuture<Void> endRound;
	private TrainingExerciseBase thisSuper;
	private static int[] ROUND_TIMES = {150, 20, 10};
	private int roundTimeIndex = 0;
	private int round = 1;
	private int shotCount = 0;
	private int runningScore = 0;
	private Map<Integer, Integer> sessionScores = new HashMap<Integer, Integer>();
	private int delayMin = 4;
	private int delayMax = 8;
	private boolean repeatExercise = true;
	private boolean coloredRows = false;
	
	public ISSFStandardPistol() {}
	
	public ISSFStandardPistol(List<Group> targets) {
		super(targets);
		thisSuper = super.getInstance();
		setInitialValues();
	}
	
	private void setInitialValues() {
		roundTimeIndex = 0;
		round = 1;
		shotCount = 0;
		runningScore = 0;
		
		for (int time : ROUND_TIMES) {
			sessionScores.put(time, 0);
		}
	}
	
	@Override
	public void init() {
		super.pauseShotDetection(true);
		super.addShotTimerColumn(SCORE_COL_NAME, SCORE_COL_WIDTH);
		super.addShotTimerColumn(ROUND_COL_NAME, ROUND_COL_WIDTH);
		super.getDelayedStartInterval(this);
		
		thisSuper.pauseShotDetection(true);
		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);	
	}
	
	@Override
	public void updatedDelayedStartInterval(int min, int max) {
		delayMin = min;
		delayMax = max;
	}

	private class SetupWait implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			if (repeatExercise) {
				TextToSpeech.say("Shooter... make ready");
				int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;
            	executorService.schedule(new StartRound(), randomDelay, TimeUnit.SECONDS);
			}
			
			return null;
		}
	}
	
	private class StartRound implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			shotCount = 0;
			
			if (repeatExercise) {
				if (coloredRows) {
					thisSuper.setShotTimerRowColor(Color.LIGHTGRAY);
				} else {
					thisSuper.setShotTimerRowColor(null);
				}
				
				coloredRows = !coloredRows;
				
				TrainingExerciseBase.playSound("sounds/beep.wav");
				thisSuper.pauseShotDetection(false);
				endRound = executorService.schedule(new EndRound(), ROUND_TIMES[roundTimeIndex], TimeUnit.SECONDS);
			}
			
			return null;
		}
	}
	
	private class EndRound implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			if (repeatExercise) {
				thisSuper.pauseShotDetection(true);
				TextToSpeech.say("Round over");
				
				int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;
				
				if (round < 4) {
					// Go to next round
					round++;
	            	executorService.schedule(new StartRound(), randomDelay, TimeUnit.SECONDS);
				} else if (roundTimeIndex < ROUND_TIMES.length - 1) {
					// Go to round 1 for next time
					round = 1;
					roundTimeIndex++;
	            	executorService.schedule(new StartRound(), randomDelay, TimeUnit.SECONDS);			
				} else {
					TextToSpeech.say("Event over... Your score is " + runningScore);
					thisSuper.pauseShotDetection(false);
					// At this point we end and the user has to hit reset to start again
				}
			}
	            
			return null;
		}	
	}
	
	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("ISSF 25M Standard Pistol", "1.0", "phrack",
				 "This exercise implements the ISSF event describe at: "
						 + "http://www.pistol.org.au/events/disciplines/issf. You "
						 + "can use any scored target with this exercise, but use "
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
				sessionScores.put(ROUND_TIMES[roundTimeIndex], 
						sessionScores.get(ROUND_TIMES[roundTimeIndex]) + hitScore);
				runningScore += hitScore;
			}
		
			StringBuilder message = new StringBuilder();
			
			for (Integer time : ROUND_TIMES) {
				message.append(String.format("%ss score: %d%n", time, sessionScores.get(time)));
			}
			
			super.showTextOnFeed(message.toString() + "total score: " + runningScore);
		}
		
		String currentRound = String.format("R%d (%ds)", round, ROUND_TIMES[roundTimeIndex]);
		super.setShotTimerColumnText(SCORE_COL_NAME, String.valueOf(hitScore));
		super.setShotTimerColumnText(ROUND_COL_NAME, currentRound);
		
		if (shotCount == 5) {
			try {
				thisSuper.pauseShotDetection(true);
				endRound.cancel(true);
				new EndRound().call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void reset(List<Group> targets) {
        repeatExercise = false;
        executorService.shutdownNow();
       
        setInitialValues();
        
		roundTimeIndex = 0;
		round = 1;
		shotCount = 0;
		runningScore = 0;
		
		for (int time : ROUND_TIMES) {
			sessionScores.put(time, 0);
		}
        
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