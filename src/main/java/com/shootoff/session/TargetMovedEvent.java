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

package com.shootoff.session;

public class TargetMovedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final int targetIndex;
	private final int newX;
	private final int newY;

	public TargetMovedEvent(String cameraName, long timestamp, int targetIndex, int newX, int newY) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
		this.newX = newX;
		this.newY = newY;
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}

	public int getTargetIndex() {
		return targetIndex;
	}

	public int getNewX() {
		return newX;
	}

	public int getNewY() {
		return newY;
	}

	@Override
	public EventType getType() {
		return EventType.TARGET_MOVED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return String.format("target moved (%d, %d)", newX, newY);
	}
}
