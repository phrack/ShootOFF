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

import java.util.List;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.gui.ParListener;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

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

	protected boolean countScore = false;

	public ParForScore() {}

	public ParForScore(List<Target> targets) {
		super(targets);
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

	@Override
	protected void initUI() {
		super.initUI();
		addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
	}

	@Override
	protected int setupRound() {
		countScore = true;
		int delay = super.setupRound();

		return delay;
	}

	@Override
	protected void doRound() throws Exception {
		super.doRound();
		Thread.sleep((long) (parTime * 1000.));
		TrainingExerciseBase.playSound("sounds/chime.wav");
		pauseShotDetection(true);
		countScore = false;
	}

	/*
	 * This method merges shotListener for TimedHolsterDrill and ShootForScore.
	 */
	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		super.shotListener(shot, hit);

		if (!hit.isPresent() || !countScore) return;

		TargetRegion r = hit.get().getHitRegion();
		if (r.tagExists("points")) {
			setPoints(shot.getColor(), r.getTag("points"));
		}
	}

	protected void setPoints(Color shotColor, String points) {
		setShotTimerColumnText(POINTS_COL_NAME, points);

		if (shotColor.equals(Color.RED)) {
			redScore += Integer.parseInt(points);
		} else if (shotColor.equals(Color.GREEN)) {
			greenScore += Integer.parseInt(points);
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
	protected void resetValues() {
		redScore = 0;
		greenScore = 0;
		showTextOnFeed("score: 0");
		getParInterval(this);
	}

	@Override
	public void updatedParInterval(double parTime) {
		this.parTime = parTime;
	}
}
