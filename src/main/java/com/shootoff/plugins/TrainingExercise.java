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

import java.util.List;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;

public interface TrainingExercise {
	/**
	 * Any exercise specific initialization that needs to use exercise API
	 * methods should call them in this method instead of the constructor,
	 * otherwise the API may not be initialized yet.
	 */
	public void init();

	/**
	 * Called when a training exercise is first loaded to retrive information
	 * about the plugin that is displayable to the user.
	 * 
	 * @return a <tt>ExerciseMetadata</tt> object initialized with the data for
	 *         the loaded training exercise
	 */
	public ExerciseMetadata getInfo();

	/**
	 * Called whenever a shot is detected. If the shot hit a target, hitRegion
	 * will be set.
	 * 
	 * @param shot
	 *            the detect shot
	 * @param hit
	 *            empty if no target was hit, otherwise contains the specific
	 *            target and region in the target that was hit as well as shot
	 *            coordinates adjusted to be relative to the top left corner of
	 *            the region that was shot.
	 */
	public void shotListener(Shot shot, Optional<Hit> hit);

	/**
	 * Called when the reset button is hit or a reset target is shot. The
	 * training exercise should reset to its initial state here.
	 * 
	 * @param targets
	 *            a list of all of the targets currently added to webcam feeds
	 */
	public void reset(List<Target> targets);

	/**
	 * Called when a training exercise is being unloaded by the framework. This
	 * method must call super.destroy() otherwise an exception will occur when
	 * exercises are switched.
	 */
	public void destroy();
}
