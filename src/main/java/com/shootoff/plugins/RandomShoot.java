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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;

import com.shootoff.camera.Shot;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

public class RandomShoot extends TrainingExerciseBase implements TrainingExercise {
	private final List<String> subtargets = new ArrayList<String>();
	private final Stack<Integer> currentSubtargets = new Stack<Integer>();
	private Random rng = new Random();

	public RandomShoot() {}

	public RandomShoot(List<Target> targets) {
		super(targets);
		if (fetchSubtargets(targets)) startRound();
	}

	/**
	 * This is used to make this plugin deterministic for testing.
	 * 
	 * @param rng
	 *            an rng with a known seed
	 */
	protected RandomShoot(List<Target> targets, Random rng) {
		super(targets);
		this.rng = rng;
		if (fetchSubtargets(targets)) startRound();
	}

	@Override
	public void init() {}

	private void startRound() {
		pickSubtargets();
		saySubtargets();
	}

	/**
	 * Returns the list of all known subtargets. This method exists to make this
	 * exercise easier to test.
	 * 
	 * @return a list of all known subtargets
	 */
	protected List<String> getSubtargets() {
		return subtargets;
	}

	/**
	 * Returns the current subtarget stack. This method exists to make this
	 * exercise easier to test.
	 * 
	 * @return current subtarget stack
	 */
	protected Stack<Integer> getCurrentSubtargets() {
		return currentSubtargets;
	}

	/**
	 * Finds the first target with subtargets and gets its regions. If there is
	 * no target with substargets, this method uses TTS to tell the user
	 * 
	 * @param targets
	 *            a list of all targets known to this exercise
	 * @return <tt>true</tt> if we found subtargets, <tt>false</tt> otherwise
	 */
	private boolean fetchSubtargets(List<Target> targets) {
		subtargets.clear();

		boolean foundTarget = false;
		for (Target target : targets) {
			for (TargetRegion region : target.getRegions()) {
				if (region.getAllTags().containsKey("subtarget")) {
					subtargets.add(region.getTag("subtarget"));
					foundTarget = true;
				}
			}

			if (foundTarget) break;
		}

		if (foundTarget && subtargets.size() > 0) {
			return true;
		} else {
			TrainingExerciseBase.playSound(new File("sounds/voice/shootoff-subtargets-warning.wav"));
			return false;
		}
	}

	private void pickSubtargets() {
		currentSubtargets.clear();

		int count = rng.nextInt((subtargets.size() - 1) + 1) + 1;
		for (int i : rng.ints(count, 0, subtargets.size()).toArray()) {
			currentSubtargets.push(Integer.valueOf(i));
		}
	}

	private void saySubtargets() {
		List<File> soundFiles = new ArrayList<File>();
		soundFiles.add(new File("sounds/voice/shootoff-shoot.wav"));

		Stack<Integer> temp = new Stack<Integer>();
		temp.addAll(currentSubtargets);
		Collections.reverse(temp);
		Iterator<Integer> it = temp.iterator();

		while (it.hasNext()) {
			Integer index = it.next();

			if (!it.hasNext() && currentSubtargets.size() > 1)
				soundFiles.add(new File("sounds/voice/shootoff-and.wav"));

			File targetNameSound = new File(String.format("sounds/voice/shootoff-%s.wav", subtargets.get(index)));

			if (targetNameSound.exists()) {
				soundFiles.add(targetNameSound);
			} else {
				// We don't have a voice actor sounds file for a target
				// subregion, fall back
				// to TTS
				saySubtargetsTTS();
				return;
			}
		}

		super.playSounds(soundFiles);
	}

	private void saySubtargetsTTS() {
		StringBuilder sentence = new StringBuilder("shoot subtarget ");

		sentence.append(subtargets.get(currentSubtargets.get(currentSubtargets.size() - 1)));

		for (int i = currentSubtargets.size() - 2; i >= 0; i--) {
			sentence.append(" then ");
			sentence.append(subtargets.get(currentSubtargets.get(i)));
		}

		TextToSpeech.say(sentence.toString());
	}

	private void sayCurrentSubtarget() {
		List<File> soundFiles = new ArrayList<File>();
		soundFiles.add(new File("sounds/voice/shootoff-shoot.wav"));

		File targetNameSound = new File(
				String.format("sounds/voice/shootoff-%s.wav", subtargets.get(currentSubtargets.peek())));

		if (targetNameSound.exists()) {
			soundFiles.add(targetNameSound);
		} else {
			sayCurrentSubtargetTTS();
			return;
		}

		super.playSounds(soundFiles);
	}

	private void sayCurrentSubtargetTTS() {
		String sentence = "shoot " + subtargets.get(currentSubtargets.peek());
		TextToSpeech.say(sentence);
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Random Shoot", "1.0", "phrack",
				"This exercise works with targets that have subtarget tags "
						+ "assigned to some regions. Subtargets are selected at random "
						+ "and the shooter is asked to shoot those subtargets in order. "
						+ "If a subtarget is shot out of order or the shooter misses, the "
						+ "name of the subtarget that should have been shot is repeated.");
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		if (currentSubtargets.isEmpty()) return;

		if (hit.isPresent()) {
			String subtargetValue = hit.get().getHitRegion().getTag("subtarget");
			if (subtargetValue != null && subtargetValue.equals(subtargets.get(currentSubtargets.peek()))) {
				currentSubtargets.pop();
			} else {
				sayCurrentSubtarget();
			}

			if (currentSubtargets.isEmpty()) startRound();
		} else {
			sayCurrentSubtarget();
		}
	}

	@Override
	public void reset(List<Target> targets) {
		if (fetchSubtargets(targets)) startRound();
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}