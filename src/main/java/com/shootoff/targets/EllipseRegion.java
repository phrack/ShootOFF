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

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class EllipseRegion extends Ellipse implements TargetRegion {
	private final Map<String, String> tags = new HashMap<>();

	public EllipseRegion(final double centerX, final double centerY, final double radiusX, final double radiusY) {
		super(centerX, centerY, radiusX, radiusY);
	}

	@Override
	public void changeWidth(final double widthDelta) {
		setRadiusX(getRadiusX() + widthDelta);
	}

	@Override
	public void changeHeight(final double heightDelta) {
		setRadiusY(getRadiusY() + heightDelta);
	}

	@Override
	public RegionType getType() {
		return RegionType.ELLIPSE;
	}

	@Override
	public void setFill(Color fill) {
		if (Platform.isFxApplicationThread()) {
			super.setFill(fill);
		} else {
			Platform.runLater(() -> super.setFill(fill));
		}
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
