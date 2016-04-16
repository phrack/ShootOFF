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

package com.shootoff.camera.shotdetection;

import java.awt.Point;

public class Pixel extends Point {
	private static final long serialVersionUID = 1L;

	private int currentLum;
	private int lumAverage;
	private int color;
	private int colorAverage;

	private int connectedness;

	protected Pixel(final int x, final int y, final int color, final int currentLum, final int lumAverage,
			final int colorAverage) {
		super(x, y);
		this.color = color;
		this.currentLum = currentLum;
		this.lumAverage = lumAverage;
		this.colorAverage = colorAverage;
	}

	public Pixel(final int x, final int y) {
		super(x, y);
	}

	public int getColorAverage() {
		return colorAverage;
	}

	public void setColorAverage(final int colorAverage) {
		this.colorAverage = colorAverage;
	}

	public int getConnectedness() {
		return connectedness;
	}

	public void setConnectedness(final int connectedness) {
		this.connectedness = connectedness;
	}

	public int getCurrentLum() {
		return currentLum;
	}

	public void setCurrentLum(final int currentLum) {
		this.currentLum = currentLum;
	}

	public int getLumAverage() {
		return lumAverage;
	}

	public void setLumAverage(final int lumAverage) {
		this.lumAverage = lumAverage;
	}

	public int getColor() {
		return color;
	}

	public void setColor(final int color) {
		this.color = color;
	}

	// We explicitly don't want to make use of extra data in this class
	// when checking for object equality
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
