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

package com.shootoff.headless.protocol;

import com.shootoff.camera.shot.ShotColor;

public class NewShotMessage extends Message {
	private final ShotColor color;
	private final double x;
	private final double y;
	private final long timestamp;
	private final double arenaWidth;
	private final double arenaHeight;

	public NewShotMessage(ShotColor color, double x, double y, long timestamp, double arenaWidth, double arenaHeight) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.arenaWidth = arenaWidth;
		this.arenaHeight = arenaHeight;
	}

	public ShotColor getColor() {
		return color;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public double getArenaWidth() {
		return arenaWidth;
	}

	public double getArenaHeight() {
		return arenaHeight;
	}
}
