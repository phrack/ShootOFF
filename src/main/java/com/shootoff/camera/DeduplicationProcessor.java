/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.camera;

import java.util.Optional;

public class DeduplicationProcessor implements ShotProcessor {
	private Optional<Shot> lastShot = Optional.empty();
	private final double DISTANCE_THRESHOLD_X;
	private final double DISTANCE_THRESHOLD_Y;
	
	public DeduplicationProcessor() {
		final double DISTANCE_THRESHOLD = 0.10;
		DISTANCE_THRESHOLD_X = 640 * DISTANCE_THRESHOLD;
		DISTANCE_THRESHOLD_Y = 480 * DISTANCE_THRESHOLD;
	}
	
	@Override
	public boolean processShot(Shot shot) {
		if (lastShot.isPresent()) {
			final int TIME_THRESHOLD = 155; // This is Miculek constant because it's based on how fast Jerry Miculek
											// can pull the trigger. It's a safe bet ShootOFF users aren't faster :).
			
			// If two shots have the same color, appear to have happened fast than Jerry Miculek can shoot
			// and are very close to each other, ignore the new shot
			if (shot.getColor().equals(lastShot.get().getColor()) && 
					shot.getTimestamp() - lastShot.get().getTimestamp() <= TIME_THRESHOLD &&
					Math.abs(lastShot.get().getX() - shot.getX()) <= DISTANCE_THRESHOLD_X &&
					Math.abs(lastShot.get().getY() - shot.getY()) <= DISTANCE_THRESHOLD_Y) {
				return false;
			}
		}
		
		lastShot = Optional.of(shot);
		
		return true;
	}
}
