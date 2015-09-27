package com.shootoff.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.ShotSearcher.PixelColor;


public class PixelCluster extends java.util.ArrayList<Pixel> {

	private final Logger logger = LoggerFactory.getLogger(PixelCluster.class);
	
	
	private static final long serialVersionUID = 8652050835557402069L;

	
	public double centerPixelX;
	public double centerPixelY;
	

	public double getColorDifference(BufferedImage frame, BufferedImage prevFrame)
	{
		ArrayList<Pixel> visited = new ArrayList<Pixel>();
		
		double diff = 0;
		double lumDiff = 0;
		for (Pixel pixel : this)
		{
			if (pixel.getConnectedness()<8)
			{
				for(int h=-1;h<=1;h++)
					for(int w=-1;w<=1;w++) 
					{
						int rx = pixel.x+w; 
						int ry = pixel.y+h; 
						Pixel nearPoint = new Pixel(rx,ry);
						if (!this.contains(nearPoint) && !visited.contains(nearPoint))
						{
							
							java.awt.Color npColor = new java.awt.Color(frame.getRGB(rx, ry));
							java.awt.Color prevnpColor = new java.awt.Color(prevFrame.getRGB(rx, ry));

							double rcd = pixel.redColorDistance(npColor);
							double gcd = pixel.greenColorDistance(npColor);
							diff += (rcd-gcd);
							
							
							double prevrcd = pixel.redColorDistance(prevnpColor);
							double prevgcd = pixel.greenColorDistance(prevnpColor);
							lumDiff += (prevrcd-prevgcd);
							
							visited.add(nearPoint);
							
							logger.trace("Visiting pixel {} {} - {} - {}", rx, ry, (rcd-gcd), (prevrcd-prevgcd));
						}
					}
			}
		}
		
		logger.trace("Done visiting_alt - {}", diff-lumDiff);
		
		return diff-lumDiff;
	}
	
	public double getColorDifference(BufferedImage frame, double[][] colorDiffMovingAverage)
	{
		ArrayList<Pixel> visited = new ArrayList<Pixel>();
		
		double diff = 0;
		double lumDiff = 0;
		for (Pixel pixel : this)
		{
			if (pixel.getConnectedness()<8)
			{
				for(int h=-1;h<=1;h++)
					for(int w=-1;w<=1;w++) 
					{
						int rx = pixel.x+w; 
						int ry = pixel.y+h; 
						Pixel nearPoint = new Pixel(rx,ry);
						if (!this.contains(nearPoint) && !visited.contains(nearPoint))
						{
							
							java.awt.Color npColor = new java.awt.Color(frame.getRGB(rx, ry));

							double rcd = pixel.redColorDistance(npColor);
							double gcd = pixel.greenColorDistance(npColor);
							diff += (rcd-gcd);
							lumDiff += colorDiffMovingAverage[rx][ry];
							
							visited.add(nearPoint);
							
							logger.trace("Visiting pixel {} {} - {} - {}", rx, ry, (rcd-gcd), colorDiffMovingAverage[rx][ry]);
						}
					}
			}
		}
		
		logger.trace("Done visiting - {}", diff-lumDiff);
		
		return diff-lumDiff;
	}
	
	public double getColorDifference()
	{
		double diff = 0;
		
		double lumDiff = 0;
		
		double scaledDiff = 0;
		
		double result = 0;
		
		int redder = 0;
		int greener = 0;
		
		int redder_withcma = 0;
		int greener_withcma = 0;
		int i = 0;
		
		double avglum = 0;
		double avgmalum = 0;
		double avgcdness = 0;
		

		
		for (Pixel pixel : this)
		{
			
			i++;
			if (pixel.getCurrentLum() > 240)
				continue;
			
			avglum += (double)pixel.getCurrentLum();
			avgcdness += (double)pixel.getConnectedness();
			avgmalum += (double)pixel.getLumAverage();
			
			
			double rcd = pixel.redColorDistance();
			double gcd = pixel.greenColorDistance();
			
			double cddiff = (rcd-gcd);
			
			double pixel_ca = pixel.getColorAverage();
			

			lumDiff = lumDiff + pixel_ca;
			diff = diff + cddiff;
			
			scaledDiff = scaledDiff + ((diff - lumDiff)/(double)pixel.getCurrentLum());
			
			logger.trace("{} - avglum {} - avgmalum {} - {} {} - {} - {}", i, (double)pixel.getCurrentLum(), (double)pixel.getLumAverage(), cddiff, pixel_ca, (cddiff-pixel_ca), scaledDiff);

			
			if (Math.abs(rcd-gcd)>10)
			{
				if (rcd<gcd)
					redder++;
				else
					greener++;
			}

		}
		
		
		
		logger.trace("getColorDifference1 {} - {} {} - {}", avglum, diff, lumDiff, (diff-lumDiff));

		avgmalum = avgmalum / this.size();
		avglum = avglum / this.size();
		avgcdness = avgcdness / this.size();

		
		//result = (diff - lumDiff);
		result = scaledDiff;
		
		logger.trace("getColorDifference {} -  {} {} | {} {} | {} {} - {}", this.size(), avglum, avgcdness, centerPixelX, centerPixelY, diff, lumDiff, result);
		
		
		return result;
	}


	public String getPredictedColor() {
		double colorDist = getColorDifference();
		if (Math.abs(colorDist) < 2) {
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
		if (Math.abs(colorDist) < 2) {
			return Optional.empty();
		} else if (colorDist < 0) {
			return Optional.of(javafx.scene.paint.Color.RED);
		} else {
			return Optional.of(javafx.scene.paint.Color.GREEN);
		}
	}
	
	public Optional<javafx.scene.paint.Color> getPredictedColorJavafxNew(BufferedImage frame, double[][] colorDiffMovingAverage)
	{
		double colorDist = getColorDifference(frame, colorDiffMovingAverage);
		
		logger.trace("getcolorjavafx {} - {}", colorDist, (colorDist<0));
		
		if (Math.abs(colorDist) < 2) {
			return Optional.empty();
		} else if (colorDist < 0) {
			return Optional.of(javafx.scene.paint.Color.RED);
		} else {
			return Optional.of(javafx.scene.paint.Color.GREEN);
		}
	}
	
	public Optional<javafx.scene.paint.Color> getPredictedColorJavafxNew(BufferedImage frame, BufferedImage prevFrame)
	{
		double colorDist = getColorDifference(frame, prevFrame);
		if (Math.abs(colorDist) < 2) {
			return Optional.empty();
		} else if (colorDist < 0) {
			return Optional.of(javafx.scene.paint.Color.RED);
		} else {
			return Optional.of(javafx.scene.paint.Color.GREEN);
		}
	}
}
