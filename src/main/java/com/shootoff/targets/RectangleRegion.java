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

import java.util.HashMap;
import java.util.Map;

import javafx.scene.shape.Rectangle;

public class RectangleRegion extends Rectangle implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();

	public RectangleRegion(final double x, final double y, final double width, final double height) {
		super(x, y, width, height);
	}

	@Override
	public void changeWidth(final double widthDelta) {
		this.setWidth(this.getWidth() + widthDelta);
	}

	@Override
	public void changeHeight(final double heightDelta) {
		this.setHeight(this.getHeight() + heightDelta);
	}

	@Override
	public RegionType getType() {
		return RegionType.RECTANGLE;
	}

	@Override
	public boolean tagExists(final String name) {
		return tags.containsKey(name);
	}

	@Override
	public String getTag(final String name) {
		return tags.get(name);
	}

	@Override
	public Map<String, String> getAllTags() {
		return tags;
	}

	@Override
	public void setTags(final Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}
}
