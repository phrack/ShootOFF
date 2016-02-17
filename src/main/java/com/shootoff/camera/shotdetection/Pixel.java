package com.shootoff.camera.shotdetection;

import java.awt.Color;
import java.awt.Point;

public class Pixel extends Point {

	private static final long serialVersionUID = 1L;

	private int currentLum;
	private int lumAverage;
	private int color;
	private int colorAverage;

	public int getColorAverage() {
		return colorAverage;
	}

	public void setColorAverage(int colorAverage) {
		this.colorAverage = colorAverage;
	}

	private int connectedness = 0;

	public Pixel(int x, int y, int color, int currentLum, int lumAverage, int colorAverage) {
		super(x, y);
		this.color = color;
		this.currentLum = currentLum;
		this.lumAverage = lumAverage;
		this.colorAverage = colorAverage;
	}

	public Pixel(int x, int y) {
		super(x, y);
	}

	public int getConnectedness() {
		return connectedness;
	}

	public void setConnectedness(int connectedness) {
		this.connectedness = connectedness;
	}

	public int getCurrentLum() {
		return currentLum;
	}

	public void setCurrentLum(int currentLum) {
		this.currentLum = currentLum;
	}

	public int getLumAverage() {
		return lumAverage;
	}

	public void setLumAverage(int lumAverage) {
		this.lumAverage = lumAverage;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

}
