package com.shootoff.camera.shotdetection;

import java.awt.Point;

public class Pixel extends Point {
	private static final long serialVersionUID = 1L;

	private int currentLum;
	private int lumAverage;
	private int color;
	private int colorAverage;

	private int connectedness;

	public Pixel(int x, int y, int color, int currentLum, int lumAverage, int colorAverage) {
		super(x, y);
		this.color = color;
		this.currentLum = currentLum;
		this.lumAverage = lumAverage;
		this.colorAverage = colorAverage;
	}

	public Pixel(final int x, final int y) {
		super(x, y);
	}

	public int getColorAverage() {
		return colorAverage;
	}

	public void setColorAverage(final int colorAverage) {
		this.colorAverage = colorAverage;
	}

	public int getConnectedness() {
		return connectedness;
	}

	public void setConnectedness(final int connectedness) {
		this.connectedness = connectedness;
	}

	public int getCurrentLum() {
		return currentLum;
	}

	public void setCurrentLum(final int currentLum) {
		this.currentLum = currentLum;
	}

	public int getLumAverage() {
		return lumAverage;
	}

	public void setLumAverage(final int lumAverage) {
		this.lumAverage = lumAverage;
	}

	public int getColor() {
		return color;
	}

	public void setColor(final int color) {
		this.color = color;
	}
}
