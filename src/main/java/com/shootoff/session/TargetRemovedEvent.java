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

public class TargetRemovedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final int targetIndex;

	public TargetRemovedEvent(String cameraName, long timestamp, int targetIndex) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}

	public int getTargetIndex() {
		return targetIndex;
	}

	@Override
	public EventType getType() {
		return EventType.TARGET_REMOVED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "target removed";
	}
}
