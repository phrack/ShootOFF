/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

import java.util.Optional;

import com.shootoff.camera.Shot;

import javafx.scene.paint.Color;

public class ShotEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final Shot shot;
	private final Optional<Integer> targetIndex;
	private final Optional<Integer> hitRegionIndex;
	
	public ShotEvent(String cameraName, long timestamp, Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex) {
		this.cameraName = cameraName;
		this.timestamp = timestamp; 
		this.shot = shot;
		this.targetIndex = targetIndex;
		this.hitRegionIndex = hitRegionIndex;
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}
	
	public Shot getShot() {
		return shot;
	}
	
	public Optional<Integer> getTargetIndex() {
		return targetIndex;
	}
	
	public Optional<Integer> getHitRegionIndex() {
		return hitRegionIndex;
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
		
		if (shot.getColor().equals(Color.RED)) {
			colorName = "red";
		} else {
			colorName = "green";
		}
		
		return String.format("%s shot (%.2f, %.2f)", colorName, shot.getX(), shot.getY());
	}
}