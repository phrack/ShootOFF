package com.shootoff.camera;

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
	private java.lang.Float[] cielabColor;
	
	private int connectedness = 0;
	
	public Pixel(int x, int y, Color color, int currentLum, int lumAverage, double colorAverage) {
		super(x,y);
		this.color = color;
		this.currentLum = currentLum;
		this.lumAverage = lumAverage;
		this.colorAverage = colorAverage;
	}
	
	public Pixel(int x, int y, Color color, int currentLum, int lumAverage, double colorAverage, java.lang.Float[] cielabColor) {
		super(x,y);
		this.color = color;
		this.currentLum = currentLum;
		this.lumAverage = lumAverage;
		this.colorAverage = colorAverage;
		this.cielabColor = cielabColor;
	}
	
	
	public Pixel(int x, int y) {
		super(x,y);
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

	public double redColorDistance()
	{
		return colorDistance(new Color(0xFF0000));
	}
	
	public double greenColorDistance()
	{
		return colorDistance(new Color(0x00FF00));
	}
	
	/*public double redColorDeltaDistance()
	{
		return ShotSearchingBrightnessPixelTransformer.deltaE_red(cielabColor);
	}
	
	public double greenColorDeltaDistance()
	{
		return ShotSearchingBrightnessPixelTransformer.deltaE_green(cielabColor);
	}*/
	

	public double redColorDistance(Color c2)
	{
		return colorDistance(c2, new Color(0xFF0000));
	}
	
	public double greenColorDistance(Color c2)
	{
		return colorDistance(c2, new Color(0x00FF00));
	}
	
	// Green turned down from "43" to "20".
	// There is just too much of a bias for green in these algorithms at high brightness
	// This isn't surprising, these are linear approximations of "real" color distances (CIELAB DeltaE)
	// It is not a perfect fit across the spectrum, so we have to correct for its problems	
	public static double colorDistance(Color color, Color c2)
	{
	    return Math.sqrt(22*Math.pow(color.getRed()-c2.getRed(),2) + 43*Math.pow(color.getGreen()-c2.getGreen(),2) + 35*Math.pow(color.getBlue()-c2.getBlue(),2));
	}

	
	public double colorDistance(Color c2)
	{
	    return colorDistance(this.color, c2);
	} 

	public double getColorAverage() {
		return colorAverage;
	}
	public void setColorAverage(double colorAverage) {
		this.colorAverage = colorAverage;
	}
	
	

	
}
