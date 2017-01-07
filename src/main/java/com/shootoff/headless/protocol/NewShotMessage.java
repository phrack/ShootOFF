package com.shootoff.headless.protocol;

import com.shootoff.camera.ShotColor;

public class NewShotMessage extends Message {
	private final ShotColor color;
	private final double x;
	private final double y;
	private final long timestamp;

	public NewShotMessage(ShotColor color, double x, double y, long timestamp) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
	}

	public ShotColor getColor() {
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
}
