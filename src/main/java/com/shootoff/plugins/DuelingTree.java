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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.Group;
import javafx.scene.Node;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;
import com.shootoff.util.NamedThreadFactory;

public class DuelingTree extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private final static String HIT_COL_NAME = "Hit By";
	private final static int HIT_COL_WIDTH = 60;

	private static final int NEW_ROUND_DELAY = 5; // s
	private static final int CORE_POOL_SIZE = 2;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE,
			new NamedThreadFactory("DuelingTreeExercise"));
	private TrainingExerciseBase thisSuper;

	private boolean continueExercise = true;
	private boolean isResetting = false;
	private int leftScore = 0;
	private int rightScore = 0;
	private List<TargetRegion> paddlesOnLeft = new ArrayList<TargetRegion>();
	private List<TargetRegion> paddlesOnRight = new ArrayList<TargetRegion>();

	public DuelingTree() {}

	public DuelingTree(List<Group> targets) {
		super(targets);
		this.thisSuper = super.getInstance();
		findTargets(targets);
	}

	@Override
	public void init() {
		// We need to make sure we start with a clean slate because the position
		// of the plates matter
		super.reset();

		super.addShotTimerColumn(HIT_COL_NAME, HIT_COL_WIDTH);
		super.showTextOnFeed("left score: 0\nright score: 0");
	}

	private boolean findTargets(List<Group> targets) {
		boolean foundTarget = false;

		// Find the first target with directional subtargets and gets its
		// regions
		for (Group target : targets) {
			if (foundTarget) break;

			for (Node node : target.getChildren()) {
				TargetRegion region = (TargetRegion) node;

				if (region.tagExists("subtarget")) {
					if (region.getTag("subtarget").startsWith("left_paddle")) {
						paddlesOnLeft.add(region);
						foundTarget = true;
					} else if (region.getTag("subtarget").startsWith("right_paddle")) {
						paddlesOnRight.add(region);
						foundTarget = true;
					}
				}
			}
		}

		if (!foundTarget) {
			TrainingExerciseBase.playSound(new File("sounds/voice/shootoff-duelingtree-warning.wav"));
			continueExercise = false;
		}

		return foundTarget;
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Dueling Tree", "1.0", "phrack",
				"This exercise works with the dueling tree target. Challenge "
						+ "a friend, assign a side (left or right) to each participant, "
						+ "and try to shoot the plates from your side to your friend's "
						+ "side. A round ends when all plates are on one person's side.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (!continueExercise) return;

		if (hitRegion.isPresent()) {
			TargetRegion r = hitRegion.get();

			if (r.tagExists("subtarget") && (r.getTag("subtarget").startsWith("left_paddle")
					|| r.getTag("subtarget").startsWith("right_paddle"))) {

				String hitBy = "";

				if (paddlesOnLeft.contains(r)) {
					paddlesOnLeft.remove(r);
					paddlesOnRight.add(r);
					hitBy = "left";
				} else if (paddlesOnRight.contains(r)) {
					paddlesOnLeft.add(r);
					paddlesOnRight.remove(r);
					hitBy = "right";
				}

				super.setShotTimerColumnText(HIT_COL_NAME, hitBy);

				if (paddlesOnLeft.size() == 6) {
					rightScore++;
					roundOver();
				}

				if (paddlesOnRight.size() == 6) {
					leftScore++;
					roundOver();
				}
			}
		}
	}

	private void roundOver() {
		if (continueExercise) {
			thisSuper.showTextOnFeed(String.format("left score: %d%nright score: %d", leftScore, rightScore));
			super.pauseShotDetection(true);
			executorService.schedule(new NewRound(), NEW_ROUND_DELAY, TimeUnit.SECONDS);
		}
	}

	private class NewRound implements Runnable {
		@Override
		public void run() {
			isResetting = true;
			thisSuper.reset();
			isResetting = false;
			thisSuper.pauseShotDetection(false);
		}
	}

	@Override
	public void reset(List<Group> targets) {
		if (!isResetting) {
			leftScore = 0;
			rightScore = 0;
			super.showTextOnFeed(String.format("left score: 0%nright score: 0"));
		}

		paddlesOnLeft.clear();
		paddlesOnRight.clear();

		findTargets(targets);
	}

	@Override
	public void destroy() {
		continueExercise = false;
		executorService.shutdownNow();
		super.destroy();
	}
}
