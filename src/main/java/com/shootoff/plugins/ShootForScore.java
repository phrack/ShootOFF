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

import javafx.scene.paint.Color;

import com.shootoff.camera.Shot;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

public class ShootForScore extends TrainingExerciseBase implements TrainingExercise {
	private final static String POINTS_COL_NAME = "Score";
	private final static int POINTS_COL_WIDTH = 60;

	private int redScore = 0;
	private int greenScore = 0;

	public ShootForScore() {}

	public ShootForScore(List<Target> targets) {
		super(targets);
	}

	@Override
	public void init() {
		super.addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
	}

	/**
	 * Returns the score for the red player. This method exists to make this
	 * exercise easier to test.
	 * 
	 * @return red's score
	 */
	protected int getRedScore() {
		return redScore;
	}

	/**
	 * Returns the score for the green player. This method exists to make this
	 * exercise easier to test.
	 * 
	 * @return green's score
	 */
	protected int getGreenScore() {
		return greenScore;
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Shoot for Score", "1.0", "phrack",
				"This exercise works with targets that have score tags "
						+ "assigned to regions. Any time a target region is hit, "
						+ "the number of points assigned to that region are added " + "to your total score.");
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		if (!hit.isPresent()) return;

		TargetRegion r = hit.get().getHitRegion();
		if (r.tagExists("points")) {
			super.setShotTimerColumnText(POINTS_COL_NAME, r.getTag("points"));

			if (shot.getColor().equals(Color.RED)) {
				redScore += Integer.parseInt(r.getTag("points"));
			} else if (shot.getColor().equals(Color.GREEN)) {
				greenScore += Integer.parseInt(r.getTag("points"));
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

		super.showTextOnFeed(message);
	}

	@Override
	public void reset(List<Target> targets) {
		redScore = 0;
		greenScore = 0;
		super.showTextOnFeed("score: 0");
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
