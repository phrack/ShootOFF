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

import java.io.File;

public class TargetAddedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final String targetName;

	public TargetAddedEvent(String cameraName, long timestamp, String targetName) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetName = targetName;
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}

	public String getTargetName() {
		return targetName;
	}

	@Override
	public EventType getType() {
		return EventType.TARGET_ADDED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		final String target = targetName.substring(targetName.lastIndexOf(File.separator) + 1, targetName.lastIndexOf('.'));

		return String.format("target added (%s)", target);
	}
}
