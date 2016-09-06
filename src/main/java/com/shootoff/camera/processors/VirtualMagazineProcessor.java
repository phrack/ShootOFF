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

import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.plugins.TrainingExerciseBase;

public class VirtualMagazineProcessor implements ShotProcessor {
	private final Configuration config;
	private boolean useTTS = true;
	private int roundCount = 0;

	public VirtualMagazineProcessor(Configuration config) {
		this.config = config;
		roundCount = config.getVirtualMagazineCapacity();
	}

	public void setUseTTS(boolean useTTS) {
		this.useTTS = useTTS;
	}

	public int getRountCount() {
		return roundCount;
	}

	@Override
	public boolean processShot(Shot shot) {
		if (roundCount == 0) {
			roundCount = config.getVirtualMagazineCapacity();
			if (useTTS)
				TrainingExerciseBase.playSound(new File("sounds/voice/shootoff-reload.wav"));
			return false;
		}

		roundCount--;
		return true;
	}

	@Override
	public void reset() {
		roundCount = config.getVirtualMagazineCapacity();
	}
}
