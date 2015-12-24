package com.shootoff.camera.shotdetection;

import java.awt.Color;
import java.awt.Point;

public class Pixel extends Point {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int currentLum;
	private int lumAverage;
	private Color color;
	private double colorAverage;

	private int connectedness = 0;

	public Pixel(int x, int y, Color color, int currentLum, int lumAverage,
			double colorAverage) {
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

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public double redColorDistance() {
		return colorDistance(new Color(0xFF0000));
	}

	public double greenColorDistance() {
		return colorDistance(new Color(0x00FF00));
	}

	public double redColorDistance(Color c2) {
		return colorDistance(c2, new Color(0xFF0000));
	}

	public double greenColorDistance(Color c2) {
		return colorDistance(c2, new Color(0x00FF00));
	}

	public static double colorDistance(Color color, Color c2) {
		return Math.sqrt(22 * Math.pow(color.getRed() - c2.getRed(), 2) + 43
				* Math.pow(color.getGreen() - c2.getGreen(), 2) + 35
				* Math.pow(color.getBlue() - c2.getBlue(), 2));
	}

	public double colorDistance(Color c2) {
		return colorDistance(this.color, c2);
	}

	public double getColorAverage() {
		return colorAverage;
	}

	public void setColorAverage(double colorAverage) {
		this.colorAverage = colorAverage;
	}

	public static int calcLums(int rgb) {

		// #------------------------------#
		// # For sRGB (and NTSC Rec. 709) #
		// #------------------------------#
		// Y = 0.2126 Red + 0.7152 Green + 0.0722 Blue
		// http://www.odelama.com/data-analysis/How-to-Compute-RGB-Image-Standard-Deviation-from-Channels-Statistics/

		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;

		/*
		 * return (r + r + r + b + g + g + g + g) >> 3;
		 */
		return (int) ((float) r * .2126 + (float) g * .7152 + (float) b * .0722);
	}

	public int calcLums() {
		return calcLums(color.getRGB());
	}

}
