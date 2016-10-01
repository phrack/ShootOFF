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

package com.shootoff.camera;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

/**
 * This class encapsulates the operations to show a shot of a specific color and
 * size on a canvas.
 * 
 * @author phrack
 */
public class Shot {
	private static final Logger logger = LoggerFactory.getLogger(Shot.class);
	private final Color color;
	private double x;
	private double y;
	private final long timestamp;

	private final int frame;

	private Ellipse marker;
	private Optional<Shot> mirroredShot = Optional.empty();

	public Shot(Color color, double x, double y, long timestamp, int frame, int markerRadius) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.frame = frame;
		this.marker = new Ellipse(x, y, markerRadius, markerRadius);
		this.marker.setFill(color);
	}

	public Shot(Color color, double x, double y, long timestamp, int markerRadius) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.marker = new Ellipse(x, y, markerRadius, markerRadius);
		this.marker.setFill(color);
		this.frame = 0;
	}

	public Optional<Shot> getMirroredShot() {
		return mirroredShot;
	}
	
	public void setMirroredShot(Shot mirroredShot) {
		this.mirroredShot = Optional.of(mirroredShot);
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

	public int getFrame() {
		return frame;
	}

	public Ellipse getMarker() {
		return marker;
	}

	public void setTranslation(int displayWidth, int displayHeight, int feedWidth, int feedHeight) {
		final double scaleX = (double) displayWidth / (double) feedWidth;
		final double scaleY = (double) displayHeight / (double) feedHeight;

		final double scaledX = x * scaleX;
		final double scaledY = y * scaleY;

		if (logger.isTraceEnabled()) {
			logger.trace("setTranslation {} {} - {} {} to {} {}", scaleX, scaleY, x, y, scaledX, scaledY);
		}

		marker = new Ellipse(scaledX, scaledY, marker.radiusXProperty().get(), marker.radiusYProperty().get());
		marker.setFill(color);

		x = scaledX;
		y = scaledY;
	}
}
