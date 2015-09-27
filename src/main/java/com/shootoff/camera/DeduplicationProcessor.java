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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeduplicationProcessor implements ShotProcessor {
	private Optional<Shot> lastShot = Optional.empty();
	
	private static double DISTANCE_THRESHOLD = 0.10;
	private static double DISTANCE_THRESHOLD_X = 640 * DISTANCE_THRESHOLD;
	private static double DISTANCE_THRESHOLD_Y = 480 * DISTANCE_THRESHOLD;
	
	private static int frameThreshold = 10;
	
	public static int getThreshold() {
		return frameThreshold;
	}

	public static void setThreshold(int ft) {
		frameThreshold = ft;
	}

	private final Logger logger = LoggerFactory.getLogger(DeduplicationProcessor.class);


	public DeduplicationProcessor() {

	}
	
	protected Optional<Shot> getLastShot() {
		return lastShot;
	}
	

	public boolean processShot(Shot shot, boolean updateLastShot) {
		if (lastShot.isPresent()) {
			logger.trace("processShot {} {} - {}", shot.getFrame(), lastShot.get().getFrame(), frameThreshold);
			
			// FIX ME MAYBE? Color detection is disabled
			//shot.getColor().equals(lastShot.get().getColor()) && 
			
			// If two shots have the same color, appear to have happened fast than Jerry Miculek can shoot
			// and are very close to each other, ignore the new shot
			
			if (	
					shot.getFrame() - lastShot.get().getFrame() <= frameThreshold &&
					Math.abs(lastShot.get().getX() - shot.getX()) <= DISTANCE_THRESHOLD_X &&
					Math.abs(lastShot.get().getY() - shot.getY()) <= DISTANCE_THRESHOLD_Y) {
				return false;
			}
			
		}

		if (updateLastShot)
			lastShot = Optional.of(shot);
		
		return true;
	}
	
	@Override
	public boolean processShot(Shot shot) {
		return processShot(shot, true);
	}
	
	public boolean processShotLookahead(Shot shot) {
		return processShot(shot, false);
	}

	@Override
	public void reset() {
		lastShot = Optional.empty();
	}
}
