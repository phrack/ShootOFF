/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.targets;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.shape.Rectangle;

public class RectangleRegion extends Rectangle implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();
	
	public RectangleRegion(double x, double y, double width, double height) {
		super(x, y, width, height);
	}

	@Override
	public void changeWidth(double widthDelta) {
		this.setWidth(this.getWidth() + widthDelta);
	}

	@Override
	public void changeHeight(double heightDelta) {
		this.setHeight(this.getHeight() + heightDelta);
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
	public void replaceAllTags(Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}
}
