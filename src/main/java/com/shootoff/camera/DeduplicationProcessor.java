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
