/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.targets;

import javafx.scene.shape.Rectangle;

public class RectangleRegion extends Rectangle implements TargetRegion {
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
}
