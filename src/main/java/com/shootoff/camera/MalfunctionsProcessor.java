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

package com.shootoff.camera;

import java.util.Random;

import com.shootoff.config.Configuration;
import com.shootoff.plugins.TextToSpeech;

public class MalfunctionsProcessor implements ShotProcessor {
	private final Random rand;
	private final float prob;
	private boolean useTTS = true;

	public MalfunctionsProcessor(Configuration config) {
		this.rand = new Random();
		this.prob = config.getMalfunctionsProbability() / 100;
	}
	
	public void setUseTTS(boolean useTTS) {
		this.useTTS = useTTS; 
	}

	@Override
	public boolean processShot(Shot shot) {
		if (rand.nextFloat() < prob) {
			if (useTTS) TextToSpeech.say("malfunction");
			return false;
		}
		
		return true;
	}

	@Override
	public void reset() {}
}
