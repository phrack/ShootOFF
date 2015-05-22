/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
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
}
