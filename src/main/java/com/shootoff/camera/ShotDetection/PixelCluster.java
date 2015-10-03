package com.shootoff.camera.ShotDetection;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;


public class PixelCluster extends java.util.ArrayList<Pixel> {

	private final Logger logger = LoggerFactory.getLogger(PixelCluster.class);
	
	
	private static final long serialVersionUID = 1L;

	
	public double centerPixelX;
	public double centerPixelY;
	
	// We ignore fully connected pixels because they are not on the edges
	private final static int MAXIMUM_CONNECTEDNESS = 8;
	
	// We collect all the pixels AROUND the detected shot, not any in the shot itself
	// Usually the pixels in the shot are max brightness which are biased green
	// So we look around the shot instead
	public double getColorDifference(BufferedImage frame, double[][] colorDiffMovingAverage)
	{
		ArrayList<Pixel> visited = new ArrayList<Pixel>();
		
		double diff = 0;
		double lumDiff = 0;
		
		for (Pixel pixel : this)
		{
			if (pixel.getConnectedness()<MAXIMUM_CONNECTEDNESS)
			{
				for(int h=-1;h<=1;h++)
					for(int w=-1;w<=1;w++) 
					{
						if (h==0 && w==0)
							continue;
						
						int rx = pixel.x+w; 
						int ry = pixel.y+h; 
						
						if (rx<0 || ry<0 || rx>=frame.getWidth() || ry>=frame.getHeight())
							continue;
						
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
	

	public Optional<javafx.scene.paint.Color> getColorJavafx(BufferedImage frame, double[][] colorDiffMovingAverage)
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

}
