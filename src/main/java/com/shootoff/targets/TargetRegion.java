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

package com.shootoff.targets;

import java.util.Map;

import javafx.scene.paint.Color;

public interface TargetRegion {
	public static final Color UNSELECTED_STROKE_COLOR = Color.BLACK;
	public static final Color SELECTED_STROKE_COLOR = Color.GOLD;

	public void changeWidth(double widthDelta);

	public void changeHeight(double heightDelta);

	public RegionType getType();

	public void setFill(Color fill);
	
	public boolean tagExists(String name);	

	public String getTag(String name);

	public Map<String, String> getAllTags();

	public void setTags(Map<String, String> newTags);
}
