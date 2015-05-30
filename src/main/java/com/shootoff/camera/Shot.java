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

package com.shootoff.camera;

import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class Shot {
	private final Color color;
	private final double x;
	private final double y;
	private final long timestamp;
	private final Ellipse marker;
	
	public Shot (Color color, double x, double y, long timestamp, int markerRadius) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.marker = new Ellipse(x, y, markerRadius, markerRadius);
		this.marker.setFill(color);
	}
	
	public Color getColor() {
		return color;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public Ellipse getMarker() {
		return marker;
	}
}
