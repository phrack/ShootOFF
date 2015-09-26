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
		
		double result = 0;
		
		int redder = 0;
		int greener = 0;
		
		int redder_withcma = 0;
		int greener_withcma = 0;
		int i = 0;
		for (Pixel pixel : this)
		{
			
			double rcd = pixel.redColorDistance();
			double gcd = pixel.greenColorDistance();
			double cddiff = rcd-gcd;
			
			double pixel_ca = pixel.getColorAverage();
			
			double weighted_cd_withcma = (cddiff*2-pixel_ca)/3;
			
			lumDiff = lumDiff + pixel_ca;
			
			
			logger.trace("{} {} - {}", i, cddiff, pixel_ca);
			diff = diff + (rcd - gcd);
			
			if (Math.abs(rcd-gcd)>10)
			{
				if (rcd<gcd)
					redder++;
				else
					greener++;
			}
				
			if (Math.abs(weighted_cd_withcma)>10)
			{
				if (weighted_cd_withcma<0)
					redder_withcma++;
				else
					greener_withcma++;
			}
			
			i++;
			
		}
		
		lumDiff = lumDiff / this.size();
		diff = diff / this.size();
		
		result = (diff*2 - lumDiff)/3;
		
		logger.warn("getColorDifference {} -  {} {} - {} {} - {} - {} {} - {} {}", this.size(), centerPixel.x, centerPixel.y, diff, lumDiff, result, redder, greener, redder_withcma, greener_withcma);
		
		
		return result;
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
