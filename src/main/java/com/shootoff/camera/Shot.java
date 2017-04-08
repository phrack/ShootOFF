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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.shot.ShotColor;

import javafx.scene.paint.Color;

/**
 * This class encapsulates a shot of a specific color, time, and frame count
 * 
 * It can be paired with a mirrored shot.
 * 
 * The shot location can be modified for POI offset, which must happen before
 * other operations.
 * 
 * @author phrack, cbdmaul
 */
public class Shot {
	static public final Map<ShotColor, Color> colorMap = new HashMap<ShotColor, Color>();
	static {
		colorMap.put(ShotColor.RED, Color.RED);
		colorMap.put(ShotColor.GREEN, Color.GREEN);
		colorMap.put(ShotColor.INFRARED, Color.ORANGE);
	}

	protected final ShotColor color;

	private double x;
	private double y;
	
	protected final long timestamp;
	protected final int frame;
	
	public Shot(Shot shot)
	{
		this.color = shot.color;
		this.x = shot.getX();
		this.y = shot.getY();
		this.timestamp = shot.timestamp;
		this.frame = shot.frame;
	}

	public Shot(ShotColor color, double x, double y, long timestamp, int frame) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.frame = frame;

	}

	public Shot(ShotColor color, double x, double y, long timestamp) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		frame = 0;
	}
	public ShotColor getColor() {
		return color;
	}

	public Color getPaintColor() {
		return colorMap.get(color);
	}
	
	public double getOrigX() {
		return x;
	}

	public double getOrigY() {
		return y;
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

	
	public void adjustPOI(double adjX, double adjY)
	{
		x = x + adjX;
		y = y + adjY;
	}
	
	

	private Optional<Shot> mirroredShot = Optional.empty();

	
	public Optional<Shot> getMirroredShot() {
		return mirroredShot;
	}

	public void setMirroredShot(Shot mirroredShot) {
		this.mirroredShot = Optional.of(mirroredShot);
	}

}

