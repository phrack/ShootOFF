/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;

import com.shootoff.camera.Shot;
import com.shootoff.gui.DelayedStartListener;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.util.NamedThreadFactory;

public class TimedHolsterDrill extends TrainingExerciseBase implements TrainingExercise, DelayedStartListener {
	private final static String LENGTH_COL_NAME = "Length";
	private final static int LENGTH_COL_WIDTH = 60;
	private final static int START_DELAY = 10; // s
	private final static int RESUME_DELAY = 5; // s
	private static final int CORE_POOL_SIZE = 2;
	private static final String PAUSE = "Pause";
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE,
			new NamedThreadFactory("TimedHolsterDrillExercise"));
	private int delayMin = 4;
	private int delayMax = 8;
	private boolean repeatExercise = true;
	private long beepTime = 0;
	private boolean coloredRows = false;
	private Button pauseResumeButton;

	public TimedHolsterDrill() {}

	public TimedHolsterDrill(List<Target> targets) {
		super(targets);
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
	public void init() {
		initUI();
		initService();
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hi) {
		if (repeatExercise) {
			setLength();
		}
	}

	protected void setLength() {
		float drawShotLength = (float) (System.currentTimeMillis() - beepTime) / (float) 1000; // s
		setShotTimerColumnText(LENGTH_COL_NAME, String.format("%.2f", drawShotLength));
	}

	@Override
	public void reset(List<Target> targets) {
		repeatExercise = false;
		pauseShotDetection(true);
		executorService.shutdownNow();
		pauseResumeButton.setText(PAUSE);
		resetValues();
		repeatExercise = true;
		executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE,
				new NamedThreadFactory("TimedHolsterDrillExercise"));
		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() {
		repeatExercise = false;
		executorService.shutdownNow();
		super.destroy();
	}

	protected class SetupWait implements Callable<Void> {
		@Override
		public Void call() {
			pauseShotDetection(true);
			playSound(new File("sounds/voice/shootoff-makeready.wav"));
			int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;

			if (repeatExercise) executorService.schedule(new Round(), randomDelay, TimeUnit.SECONDS);

			return null;
		}
	}

	protected class Round implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			if (repeatExercise) {
				int randomDelay = setupRound();
				doRound();
				executorService.schedule(new Round(), randomDelay, TimeUnit.SECONDS);
			}

			return null;
		}
	}

	protected void initUI() {
		this.pauseResumeButton = addShootOFFButton(PAUSE, (event) -> {
			Button pauseResumeButton = (Button) event.getSource();
			if (PAUSE.equals(pauseResumeButton.getText())) {
				pauseResumeButton.setText("Resume");
				repeatExercise = false;
				pauseShotDetection(true);
			} else {
				pauseResumeButton.setText(PAUSE);
				repeatExercise = true;
				executorService.schedule(new SetupWait(), RESUME_DELAY, TimeUnit.SECONDS);
			}
		});
		addShotTimerColumn(LENGTH_COL_NAME, LENGTH_COL_WIDTH);
	}

	protected void initService() {
		pauseShotDetection(true);
		resetValues();

		executorService.schedule(new SetupWait(), START_DELAY, TimeUnit.SECONDS);
	}

	protected int setupRound() {
		if (coloredRows) {
			setShotTimerRowColor(Color.LIGHTGRAY);
		} else {
			setShotTimerRowColor(null);
		}

		coloredRows = !coloredRows;

		int randomDelay = new Random().nextInt((delayMax - delayMin) + 1) + delayMin;
		return randomDelay;
	}

	protected void doRound() throws Exception {
		playSound("sounds/beep.wav");
		pauseShotDetection(false);
		startRoundTimer();
	}

	protected void startRoundTimer() {
		beepTime = System.currentTimeMillis();
	}

	@Override
	public void updatedDelayedStartInterval(int min, int max) {
		delayMin = min;
		delayMax = max;
	}

	protected void resetValues() {
		getDelayedStartInterval(this);
	}
}
