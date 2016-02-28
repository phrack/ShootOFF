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

public class FMath {
	public final static int FULL_SCALE = 1000000000;
	public final static int HALF_SCALE = FULL_SCALE / 2;

	// Scales from 0-255 (opencv range) to HALF of the full scale
	public final static int TWOFIFTYFIVE_TO_HALF = (HALF_SCALE / 255);

	public final static int FULL_TO_TWOFIFTYFIVE = (FULL_SCALE / 255);
}
