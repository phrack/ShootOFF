/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.camera;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Shot {
	private final Color color;
	private final int x;
	private final int y;
	private final int timestamp;
	private final int markerRadius;
	
	public Shot (Color color, int x, int y, int timestamp, int markerRadius) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.markerRadius = markerRadius;
	}
	
	public void drawShot(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillOval(x, y, markerRadius, markerRadius);
	}

	public Color getColor() {
		return color;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
}
