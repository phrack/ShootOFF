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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.Shot;

public class DeduplicationProcessor implements ShotProcessor {
	private final static Logger logger = LoggerFactory.getLogger(DeduplicationProcessor.class);

	private Optional<Shot> lastShot = Optional.empty();

	// About 30 pixels at 640x480
	private final static double DISTANCE_THRESHOLD_DIVISION_FACTOR = 8000.0;
	private double distanceThreshold;

	// frames
	public static final int DEDUPE_THRESHOLD_MINIMUM = 2;

	// ms
	private static final int timestampThreshold = 60;

	private final CameraManager cameraManager;

	public DeduplicationProcessor(final CameraManager cameraManager) {
		this.cameraManager = cameraManager;
		setDistanceThreshold();
	}

	private void setDistanceThreshold() {
		distanceThreshold = (cameraManager.getFeedWidth() * cameraManager.getFeedHeight())
				/ DISTANCE_THRESHOLD_DIVISION_FACTOR;
	}

	public Optional<Shot> getLastShot() {
		return lastShot;
	}

	public boolean processShot(Shot shot, boolean updateLastShot) {
		if (lastShot.isPresent()) {
			long timeDiff = shot.getTimestamp() - lastShot.get().getTimestamp();

			if (timeDiff > timestampThreshold
					&& (shot.getFrame() - lastShot.get().getFrame()) > DEDUPE_THRESHOLD_MINIMUM) {
				if (updateLastShot)
					lastShot = Optional.of(shot);
				return true;
			}

			timeDiff = Math.min(timeDiff, timestampThreshold);

			// The Size area for a dupe decreases from 1 * distanceThreshold to
			// .5 distanceThreshold
			// over the time period
			final double dynamicDistancePercentage = (int) ((1 - ((.5 * timeDiff) / timestampThreshold))
					* distanceThreshold);

			if (logger.isTraceEnabled()) {
				logger.trace("processShot {} {}", shot.getX(), shot.getY());
				logger.trace("processShot ts {} - {}", shot.getTimestamp(), lastShot.get().getTimestamp());

				logger.trace("processShot {} {} - {}", shot.getFrame(), lastShot.get().getFrame(),
						DEDUPE_THRESHOLD_MINIMUM);

				logger.trace("processShot distance {} - thresh {}", euclideanDistance(lastShot.get(), shot),
						dynamicDistancePercentage);
			}

			if (euclideanDistance(lastShot.get(), shot) <= dynamicDistancePercentage) {

				if (logger.isTraceEnabled())
					logger.trace("processShot DUPE {} {}", shot.getX(), shot.getY());
				if (updateLastShot)
					lastShot = Optional.of(shot);
				return false;
			}
		}

		if (updateLastShot)
			lastShot = Optional.of(shot);

		return true;
	}

	private double euclideanDistance(final Shot shot1, final Shot shot2) {
		return Math.sqrt(Math.pow(shot1.getX() - shot2.getX(), 2) + Math.pow(shot1.getY() - shot2.getY(), 2));
	}

	@Override
	public boolean processShot(final Shot shot) {
		return processShot(shot, true);
	}

	public boolean processShotLookahead(final Shot shot) {
		return processShot(shot, false);
	}

	@Override
	public void reset() {
		lastShot = Optional.empty();
	}

}
