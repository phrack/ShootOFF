/* Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.targets;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.shape.Ellipse;

public class EllipseRegion extends Ellipse implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();
	
	public EllipseRegion(double centerX, double centerY, 
			double radiusX, double radiusY) {
		
		super(centerX, centerY, radiusX, radiusY);
	}

	@Override
	public void changeWidth(double widthDelta) {
		this.setRadiusX(this.getRadiusX() + widthDelta);
	}

	@Override
	public void changeHeight(double heightDelta) {
		this.setRadiusY(this.getRadiusY() + heightDelta);
	}
	
	@Override
	public RegionType getType() {
		return RegionType.ELLIPSE;
	}
	
	@Override
	public boolean tagExists(String name) {
		return tags.containsKey(name);
	}
	
	@Override
	public String getTag(String name) {
		return tags.get(name);
	}
	
	@Override
	public Map<String, String> getAllTags() {
		return tags;
	}
	
	@Override
	public void setTags(Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}
}
