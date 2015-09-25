package com.shootoff.camera;

import java.awt.Color;
import java.awt.Point;



public class Pixel extends Point {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3121372738372679655L;

	
	private int currentLum;
	private int lumAverage;
	private Color color;
	private double colorAverage;
	
	public Pixel(int x, int y, Color color, int currentLum, int lumAverage, double colorAverage) {
		super(x,y);
		this.color = color;
		this.currentLum = currentLum;
		this.lumAverage = lumAverage;
		this.colorAverage = colorAverage;
	}
	public Pixel(int x, int y) {
		super(x,y);
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

	public double redColorDistance()
	{
		return colorDistance(Color.RED);
	}
	
	public double greenColorDistance()
	{
		return colorDistance(Color.GREEN);
	}
	
	public double colorDistance(Color c2)
	{
	    return Math.sqrt(22*Math.pow(color.getRed()-c2.getRed(),2) + 43*Math.pow(color.getGreen()-c2.getGreen(),2) + 35*Math.pow(color.getBlue()-c2.getBlue(),2));
	} 
	
	public static double colorDistance(Color color, Color c2)
	{
	    return Math.sqrt(22*Math.pow(color.getRed()-c2.getRed(),2) + 43*Math.pow(color.getGreen()-c2.getGreen(),2) + 35*Math.pow(color.getBlue()-c2.getBlue(),2));
	}
	public double getColorAverage() {
		return colorAverage;
	}
	public void setColorAverage(double colorAverage) {
		this.colorAverage = colorAverage;
	} 
}
