package com.shootoff.camera;

import java.awt.Color;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.ShotSearcher.PixelColor;


public class PixelCluster extends java.util.ArrayList<Pixel> {

	private final Logger logger = LoggerFactory.getLogger(PixelCluster.class);
	
	
	private static final long serialVersionUID = 8652050835557402069L;

	
	private Pixel centerPixel;


	public Pixel getCenterPixel() {
		return centerPixel;
	}


	public void setCenterPixel(Pixel centerPixel) {
		this.centerPixel = centerPixel;
	}
	
	public double getColorDifference()
	{
		double diff = 0;
		
		double lumDiff = 0;
		
		for (Pixel pixel : this)
		{
			
			lumDiff = lumDiff + pixel.getColorAverage();
			
			diff = diff + (pixel.redColorDistance() - pixel.greenColorDistance());
		}
		
		lumDiff = lumDiff / this.size();
		diff = diff / this.size();
		
		logger.warn("getColorDifference {} {} - {} {} - {}", centerPixel.x, centerPixel.y, diff, lumDiff, diff - lumDiff);
		
		
		return (diff - lumDiff);
	}


	public String getPredictedColor() {
		double colorDist = getColorDifference();
		if (Math.abs(colorDist) < 5) {
			return "None";
			//return Optional.empty();
		} else if (colorDist < 0) {
			return "Red";
			//return Optional.of(PixelColor.RED);
		} else {
			return "Green";
			//return Optional.of(PixelColor.GREEN);
		}
	}
	
	public Optional<javafx.scene.paint.Color> getPredictedColorJavafx()
	{
		double colorDist = getColorDifference();
		if (Math.abs(colorDist) < 5) {
			return Optional.empty();
		} else if (colorDist < 0) {
			return Optional.of(javafx.scene.paint.Color.RED);
		} else {
			return Optional.of(javafx.scene.paint.Color.GREEN);
		}
	}
}
