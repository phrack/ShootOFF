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

package com.shootoff.courses;

import java.util.List;
import java.util.Optional;

import javafx.geometry.Dimension2D;

import com.shootoff.gui.LocatedImage;
import com.shootoff.targets.Target;

public class Course {
	private final Optional<LocatedImage> background;
	private final List<Target> targets;
	private final Optional<Dimension2D> resolution;

	public Course(final List<Target> targets) {
		background = Optional.empty();
		this.targets = targets;
		resolution = Optional.empty();
	}

	public Course(final LocatedImage background, final List<Target> targets) {
		this.background = Optional.of(background);
		this.targets = targets;
		resolution = Optional.empty();
	}

	public Course(final Optional<LocatedImage> background, final List<Target> targets, final Dimension2D resolution) {
		this.background = background;
		this.targets = targets;
		this.resolution = Optional.of(resolution);
	}

	public Optional<LocatedImage> getBackground() {
		return background;
	}

	public List<Target> getTargets() {
		return targets;
	}

	/**
	 * The dimensions of the arena when the course was saved.
	 * 
	 * @return Optional.empty for courses saved prior to 3.7
	 */
	public Optional<Dimension2D> getResolution() {
		if (resolution.isPresent()) {
			return resolution;
		} else {
			return Optional.empty();
		}
	}
}
