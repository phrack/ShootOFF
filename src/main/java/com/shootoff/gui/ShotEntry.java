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

package com.shootoff.gui;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

import com.shootoff.camera.Shot;

public class ShotEntry {
	private final Shot shot;
	private final String color;
	private final String timestamp;
	private final Map<String, String> exerciseData = new HashMap<String, String>();
	
	public ShotEntry(Shot shot) {
		this.shot = shot;
		
		if (shot.getColor().equals(Color.RED)) {
			color = "red";
		} else {
			color = "green";
		}
		
		timestamp = String.format("%.2f", ((float)shot.getTimestamp()) / (float)1000);
	}
	
	public String getColor() {
		return color;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public Shot getShot() {
		return shot;
	}
	
	public void setExerciseValue(String name, String value) {
		exerciseData.put(name, value);
	}
	
	public String getExerciseValue(String name) {
		if (exerciseData.containsKey(name)) return exerciseData.get(name);
		else return "";
	}
	
	public void clearExerciseData() {
		exerciseData.clear();
	}
}
