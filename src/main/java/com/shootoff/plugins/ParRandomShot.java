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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.shootoff.camera.Shot;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

/**
 * Merge of ParForScore and RandomShoot.
 * 
 * @author Edward Kort
 *
 */
public class ParRandomShot extends ParForScore {
	private final List<String> subtargets = new ArrayList<String>();
	private Random rng = new Random();
	private boolean foundTarget;
	private int currentSubtarget;

	public ParRandomShot() {

	}

	public ParRandomShot(List<Target> targets) {
		super(targets);
		fetchSubtargets(targets);
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("PAR Drill with a random Subtarget", "1.0", "Edward Kort",
				"This exercise works with targets that have subtarget tags "
						+ "assigned to some regions. When the exercise is started you "
						+ "are asked to enter a range for randomly delayed starts, and "
						+ "for the interval (PAR time) in which those scores will be "
						+ "counted. You are then given 10 seconds to position yourself. "
						+ "After a random wait (within the entered range), a randomly "
						+ "selected subtarget is called out, telling you to draw the "
						+ "pistol from its holster and fire at your target; a chime "
						+ "signals the end of the Par time, to finally re-holster. The "
						+ "score for each shot, performed during the PAR time and hitting "
						+ "the subtarget, is points assigned to that subtarget (or 1 if "
						+ "there is no assignment). This process is repeated as long as " + "this exercise is on.");
	}

	@Override
	protected void initService() {
		if (foundTarget) {
			super.initService();
		}
	}

	@Override
	protected void doRound() throws Exception {
		pickSubtarget();
		saySubtarget();
		pauseShotDetection(false);
		startRoundTimer();
		Thread.sleep((long) (parTime * 1000.));
		TrainingExerciseBase.playSound("sounds/chime.wav");
		pauseShotDetection(true);
		countScore = false;
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		setLength();

		if (!foundTarget || !hit.isPresent() || !countScore) return;

		String subtarget = subtargets.get(currentSubtarget);
		String hitTarget = getSubtarget(Optional.of(hit.get().getHitRegion()));
		if (subtarget.equals(hitTarget)) {
			String points = getPoints(Optional.of(hit.get().getHitRegion()));
			setPoints(shot.getColor(), points);
		}
	}

	protected void resetValues() {
		super.resetValues();
		List<Target> targets = super.getCurrentTargets();
		fetchSubtargets(targets);
	}

	private String getPoints(Optional<TargetRegion> hitRegion) {
		String points = "1";

		if (hitRegion.isPresent()) {
			TargetRegion region = hitRegion.get();
			if (region.getAllTags().containsKey("points")) {
				points = region.getTag("points");
			}
		}

		return points;
	}

	private String getSubtarget(Optional<TargetRegion> hitRegion) {
		String subtargetName = null;

		if (hitRegion.isPresent()) {
			TargetRegion region = hitRegion.get();
			if (region.getAllTags().containsKey("subtarget")) {
				subtargetName = region.getTag("subtarget");
			}
		}

		return subtargetName;
	}

	/**
	 * @see RandomShoot.fetchSubtargets
	 * @param targets
	 * @return
	 */
	private void fetchSubtargets(List<Target> targets) {
		subtargets.clear();

		foundTarget = false;
		for (Target target : targets) {
			for (TargetRegion region : target.getRegions()) {
				if (region.getAllTags().containsKey("subtarget")) {
					subtargets.add(region.getTag("subtarget"));
					foundTarget = true;
				}
			}

			if (foundTarget) break;
		}

		if (!foundTarget) {
			playSound(new File("sounds/voice/shootoff-subtargets-warning.wav"));
		}
	}

	private void pickSubtarget() {
		if (foundTarget) {
			currentSubtarget = rng.nextInt(subtargets.size());
		}
	}

	private void saySubtarget() {
		if (foundTarget) {
			String subValue = subtargets.get(currentSubtarget);
			File targetNameSound = new File(String.format("sounds/voice/shootoff-%s.wav", subValue));

			if (targetNameSound.exists()) {
				playSound(targetNameSound);
			} else {
				// We don't have a voice actor sounds file for a target
				// subregion, fall back
				// to TTS
				TextToSpeech.say(subValue);
			}
		}
	}
}
