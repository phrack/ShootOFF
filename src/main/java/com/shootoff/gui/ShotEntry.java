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

package com.shootoff.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;

import javafx.scene.paint.Color;

public class ShotEntry {
	private final DisplayShot shot;
	private final String timestamp;
	private final String color;
	private final Optional<Color> rowColor;
	private final SplitData split;
	private final Map<String, String> exerciseData = new HashMap<>();

	public ShotEntry(DisplayShot shot, Optional<Shot> lastShot, Optional<Color> rowColor, boolean hadMalfunction,
			boolean hadReload) {
		this.shot = shot;

		if (ShotColor.RED.equals(shot.getColor())) {
			color = "red";
		} else if (ShotColor.GREEN.equals(shot.getColor())) {
			color = "green";
		} else {
			color = "infrared";
		}

		this.rowColor = rowColor;

		final float timestampS = ((float) shot.getTimestamp()) / 1000f;
		timestamp = String.format("%.2f", timestampS);

		String split;
		if (lastShot.isPresent()) {
			split = String.format("%.2f", timestampS - ((float) lastShot.get().getTimestamp() / 1000f));
		} else {
			split = "-";
		}

		this.split = new SplitData(split, rowColor, hadMalfunction, hadReload);
	}

	public static class SplitData {
		private final String split;
		private final Optional<Color> rowColor;
		private final boolean hadMalfunction;
		private final boolean hadReload;

		public SplitData(String split, Optional<Color> rowColor, boolean hadMalfunction, boolean hadReload) {
			this.split = split;
			this.rowColor = rowColor;
			this.hadMalfunction = hadMalfunction;
			this.hadReload = hadReload;
		}

		public String getSplit() {
			return split;
		}

		public Optional<Color> getRowColor() {
			return rowColor;
		}

		public boolean hadMalfunction() {
			return hadMalfunction;
		}

		public boolean hadReload() {
			return hadReload;
		}
	}

	public String getColor() {
		return color;
	}

	public Optional<Color> getRowColor() {
		return rowColor;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public SplitData getSplit() {
		return split;
	}

	public DisplayShot getShot() {
		return shot;
	}

	public void setExerciseValue(String name, String value) {
		exerciseData.put(name, value);
	}

	public String getExerciseValue(String name) {
		if (exerciseData.containsKey(name))
			return exerciseData.get(name);
		else
			return "";
	}

	public void clearExerciseData() {
		exerciseData.clear();
	}
}
