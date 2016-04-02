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

package com.shootoff.targets;

/**
 * This class encapsulates the information for a shot that hit a target.
 * 
 * @author phrack
 */
public class Hit {
	private final Target target;
	private final TargetRegion hitRegion;
	private final int impactX, impactY;

	/**
	 * Create a new Hit with coordinates for the shot adjusted to the hit region
	 * 
	 * @param target
	 *            the target that owns the <tt>TargetRegion</tt> that was hit
	 * @param hitRegion
	 *            the <tt>TargetRegion</tt> that was shot
	 * @param impactX
	 *            the x coordinate of the shot adjusted to be relative to the
	 *            hit region (i.e. the origin for the impact is the top left
	 *            corner of the region instead of the top left corner of the
	 *            canvas)
	 * @param impactY
	 *            the y coordinate of the shot adjusted to be relative to the
	 *            hit region
	 */
	public Hit(final Target target, final TargetRegion hitRegion, final int impactX, final int impactY) {
		this.target = target;
		this.hitRegion = hitRegion;
		this.impactX = impactX;
		this.impactY = impactY;
	}

	public Target getTarget() {
		return target;
	}

	public TargetRegion getHitRegion() {
		return hitRegion;
	}

	public int getImpactX() {
		return impactX;
	}

	public int getImpactY() {
		return impactY;
	}
}