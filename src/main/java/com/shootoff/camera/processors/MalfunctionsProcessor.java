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

package com.shootoff.camera.processors;

import java.io.File;
import java.util.Random;

import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.plugins.TrainingExerciseBase;

public class MalfunctionsProcessor implements ShotProcessor {
	private static boolean useTTS = true;

	private static final Random rand = new Random();
	private final float prob;

	public MalfunctionsProcessor(final Configuration config) {
		prob = config.getMalfunctionsProbability() / 100;
	}

	public static void setUseTTS(final boolean useTTS) {
		MalfunctionsProcessor.useTTS = useTTS;
	}

	@Override
	public boolean processShot(Shot shot) {
		if (rand.nextFloat() < prob) {
			if (useTTS)
				TrainingExerciseBase.playSound(new File("sounds/voice/shootoff-malfunction.wav"));
			return false;
		}

		return true;
	}

	@Override
	public void reset() {
	}
}
