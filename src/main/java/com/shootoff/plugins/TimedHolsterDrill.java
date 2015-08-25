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

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.Group;

import com.shootoff.camera.Shot;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.targets.TargetRegion;

public class TimedHolsterDrill extends TrainingExerciseBase implements TrainingExercise, DelayedStartListener {
	private final static String LENGTH_COL_NAME = "Length";
	private final static int LENGTH_COL_WIDTH = 60;
	private final static int START_DELAY = 10; // s
	private static final int CORE_POOL_SIZE = 2;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
	private TrainingExerciseBase thisSuper;
	private int delayMin = 4;
	private int delayMax = 8;
	private boolean repeatExercise = true;
	private long beepTime = 0;
	
	public TimedHolsterDrill() {}
	
	public TimedHolsterDrill(List<Group> targets) {
		super(targets);
		this.thisSuper = super.getInstance();
	}
	
	@Override
	public void init() {
		super.addShotTimerColumn(LENGTH_COL_NAME, LENGTH_COL_WIDTH);
		super.pauseShotDetection(true);
		super.getDelayedStartInterval(this);
		
		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);	
	}
	
	private class SetupWait implements Callable<Void> {
		@Override
		public Void call() {
			TextToSpeech.say("shooter... make ready");
			int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;
			
			if (repeatExercise)
				executorService.schedule(new Round(), randomDelay, TimeUnit.SECONDS);
		
			return null;
		}
	}
	
	private class Round implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			if (repeatExercise) {
	            TrainingExerciseBase.playSound("sounds/beep.wav");
	            thisSuper.pauseShotDetection(false);
	            beepTime = System.currentTimeMillis();
	            
	            int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;
	            executorService.schedule(new Round(), randomDelay, TimeUnit.SECONDS);
			}
			
			return null;
		}
	}
	
	@Override
	public void updatedDelayedStartInterval(int min, int max) {
		delayMin = min;
		delayMax = max;
	}
	
	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Timed Holster Drill", "1.0", "phrack",
			    "This exercise does not require a target, but one may be used "
			    		+ "to give the shooter something to shoot at. When the exercise "
			    		+ "is started you are asked to enter a range for randomly "
			    		+ "delayed starts. You are then given 10 seconds to position "
			    		+ "yourself. After a random wait (within the entered range) a "
			    		+ "beep tells you to draw their pistol from it's holster, "
			    		+ "fire at your target, and finally re-holster. This process is "
			    		+ "repeated as long as this exercise is on.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		float drawShotLength = (float)(System.currentTimeMillis() - beepTime) / (float)1000; // s
		super.setShotTimerColumnText(LENGTH_COL_NAME, String.format("%.2f", drawShotLength));
	}

	@Override
	public void reset(List<Group> targets) {
		repeatExercise = false;
		executorService.shutdownNow();		
		super.getDelayedStartInterval(this);
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
