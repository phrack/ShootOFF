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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;

public class ShotEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final DisplayShot shot;
	private final boolean isMalfunction;
	private final boolean isReload;
	private final Optional<Integer> targetIndex;
	private final Optional<Integer> hitRegionIndex;
	private final Optional<String> videoString;
	private final Map<String, File> videos = new HashMap<>();

	public ShotEvent(String cameraName, long timestamp, DisplayShot shot, boolean isMalfunction, boolean isReload,
			Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex, Optional<String> videoString) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.shot = shot;
		this.isMalfunction = isMalfunction;
		this.isReload = isReload;
		this.targetIndex = targetIndex;
		this.hitRegionIndex = hitRegionIndex;
		this.videoString = videoString;

		if (videoString.isPresent()) {
			final String[] videoSet = videoString.get().split(",");

			for (final String video : videoSet) {
				final String[] v = video.split(":");
				videos.put(v[0], new File("sessions" + File.separator + v[1]));
			}
		}
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}

	public DisplayShot getShot() {
		return shot;
	}

	public boolean isMalfunction() {
		return isMalfunction;
	}

	public boolean isReload() {
		return isReload;
	}

	public Optional<Integer> getTargetIndex() {
		return targetIndex;
	}

	public Optional<Integer> getHitRegionIndex() {
		return hitRegionIndex;
	}

	public Optional<String> getVideoString() {
		return videoString;
	}

	public Map<String, File> getVideos() {
		return videos;
	}

	@Override
	public EventType getType() {
		return EventType.SHOT;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		String colorName;

		if (shot.getColor().equals(ShotColor.RED)) {
			colorName = "red";
		} else if (shot.getColor().equals(ShotColor.INFRARED)) {
			colorName = "infrared";
		} else {
			colorName = "green";
		}

		return String.format("%s shot (%.2f, %.2f)", colorName, shot.getX(), shot.getY());
	}
}