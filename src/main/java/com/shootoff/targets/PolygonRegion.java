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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;

public class PolygonRegion extends Polygon implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();

	public PolygonRegion(double... points) {
		super(points);
	}

	@Override
	public void changeWidth(final double widthDelta) {
		final ObservableList<Double> points = this.getPoints();
		final List<Double> pointsX = new ArrayList<Double>();

		for (int i = 0; i < points.size(); i += 2) {
			pointsX.add(points.get(i));
		}

		final double width = Collections.max(pointsX);
		final double scaleFactor = (width + widthDelta) / width;

		this.setScaleX(this.getScaleX() * scaleFactor);
	}

	@Override
	public void changeHeight(final double heightDelta) {
		final ObservableList<Double> points = this.getPoints();
		final List<Double> pointsY = new ArrayList<Double>();

		for (int i = 1; i < points.size(); i += 2) {
			pointsY.add(points.get(i));
		}

		final double height = Collections.max(pointsY);
		final double scaleFactor = (height + heightDelta) / height;

		this.setScaleY(this.getScaleY() * scaleFactor);
	}

	@Override
	public RegionType getType() {
		return RegionType.POLYGON;
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
