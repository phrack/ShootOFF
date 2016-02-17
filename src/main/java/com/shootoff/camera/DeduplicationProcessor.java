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

	// About 12.5 pixels at 640x480
	private final static double DISTANCE_THRESHOLD_DIVISION_FACTOR = 24576.0;
	private double distanceThreshold;

	public static final double DEDUPE_THRESHOLD_DIVISION_FACTOR = 6.0;
	public static final int DEDUPE_THRESHOLD_MINIMUM = 4;

	private int frameThreshold;

	private CameraManager cameraManager;

	public int getThreshold() {
		return frameThreshold;
	}

	public void setThreshold(int ft) {
		frameThreshold = ft;
	}

	private final static Logger logger = LoggerFactory.getLogger(DeduplicationProcessor.class);

	public DeduplicationProcessor(CameraManager cameraManager) {
		this.cameraManager = cameraManager;

		frameThreshold = DEDUPE_THRESHOLD_MINIMUM;

		setDistanceThreshold();
	}

	private void setDistanceThreshold() {
		distanceThreshold = (cameraManager.getFeedWidth() * cameraManager.getFeedHeight())
				/ DISTANCE_THRESHOLD_DIVISION_FACTOR;
	}

	protected Optional<Shot> getLastShot() {
		return lastShot;
	}

	public boolean processShot(Shot shot, boolean updateLastShot) {
		if (lastShot.isPresent()) {

			if (logger.isTraceEnabled()) {
				logger.trace("processShot {} {}", shot.getX(), shot.getY());

				logger.trace("processShot {} - {}", shot.getFrame() - lastShot.get().getFrame(), frameThreshold);

				logger.trace("processShot distance {}", euclideanDistance(lastShot.get(), shot));
			}

			// If two shots have the same color, appear to have happened fast
			// than Jerry Miculek can shoot
			// and are very close to each other, ignore the new shot

			if (shot.getFrame() - lastShot.get().getFrame() <= frameThreshold
					&& euclideanDistance(lastShot.get(), shot) <= distanceThreshold) {

				logger.trace("processShot DUPE {} {}", shot.getX(), shot.getY());

				return false;
			}

		}

		if (updateLastShot) lastShot = Optional.of(shot);

		return true;
	}

	public double euclideanDistance(Shot shot1, Shot shot2) {
		return Math.sqrt(Math.pow(shot1.getX() - shot2.getX(), 2) + Math.pow(shot1.getY() - shot2.getY(), 2));
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

	public void setThresholdUsingFPS(double webcamFPS) {
		int newThreshold = (int) (webcamFPS / DEDUPE_THRESHOLD_DIVISION_FACTOR);

		newThreshold = Math.max(newThreshold, DEDUPE_THRESHOLD_MINIMUM);

		logger.trace("setThresholdUsingFPS {} {}", webcamFPS, newThreshold);

		setThreshold(newThreshold);

	}
}
