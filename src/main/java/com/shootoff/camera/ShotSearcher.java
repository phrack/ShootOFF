/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.util.Optional;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

import javafx.scene.paint.Color;

public class ShotSearcher implements Runnable {
	private final Configuration config;
	private final CanvasManager canvasManager;
	private final BufferedImage currentFrame;
	private final BufferedImage threshholded;
	
	public ShotSearcher(Configuration config, CanvasManager canvasManager, 
			BufferedImage currentFrame, BufferedImage grayScale) {
		this.config = config;
		this.canvasManager = canvasManager;
		this.currentFrame = currentFrame;
		this.threshholded = threshold(grayScale);
	}
	
	@Override
	public void run() {
		// Split the image into 3 columns and 3 rows, and search
		// each independently
		int sub_width = threshholded.getWidth() / 3;
		int sub_height = threshholded.getHeight() / 3;
		
		for (int x_start = 0; x_start <= threshholded.getWidth() - sub_width; 
				x_start += sub_width) {
			for (int y_start = 0; y_start <= threshholded.getHeight() - sub_height; 
					y_start += sub_height) {
				
				findShot(x_start, x_start + sub_width, y_start, y_start + sub_height);
			}
		}
	}
	
	private BufferedImage threshold(BufferedImage grayScale) {
		BufferedImage threshholdedImg = new BufferedImage(grayScale.getWidth(),
				grayScale.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		for (int x = 0; x < grayScale.getWidth(); x++) {
			for (int y = 0; y < grayScale.getHeight(); y++) {
				int pixel = grayScale.getRGB(x, y) & 0xFF;
				
				if (pixel > config.getLaserIntensity()) {
					threshholdedImg.setRGB(x, y, mixColor(255, 255, 255));
				} else {
					threshholdedImg.setRGB(x, y, mixColor(0, 0, 0));
				}
			}
		}
		
		return threshholdedImg;
	}
	
	private void findShot(int x_start, int x_end, int y_start, int y_end) {
		for (int x = x_start; x < x_end; x++) {
			for (int y = y_start; y < y_end; y++) {
				int pixel = threshholded.getRGB(x, y) & 0xFF;
				
				if (pixel == 255) {
					Optional<Color> areaColor = detectColor(x, y);
					if (areaColor.isPresent()) {
						if (config.ignoreLaserColor() && config.getIgnoreLaserColor().isPresent() &&
								areaColor.get().equals(config.getIgnoreLaserColor().get()))
									continue; 
						
						canvasManager.addShot(areaColor.get(), x, y);
						return;
					}
				}
			}
		}
	}
	
	private Optional<Color> detectColor(int x, int y) {
		int rgb = currentFrame.getRGB(x, y);
		double r = getRed(rgb);
		double g = getGreen(rgb);
		double b = getBlue(rgb);
		
		final int colorDetectionRadius = 5;
		int pixelsSeen = 1;
		
		// Average colorDetectionRadius pixels left
		for (int x_offset = x; x_offset > 0 && x - x_offset < colorDetectionRadius; 
				x_offset--) {
			
			rgb = currentFrame.getRGB(x_offset, y);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
		
		// Average colorDetectionRadius pixels right
		for (int x_offset = x; 
				x_offset < currentFrame.getWidth() && x_offset - x < colorDetectionRadius; 
				x_offset++) {
			
			rgb = currentFrame.getRGB(x_offset, y);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
			
		// Average colorDetectionRadius pixels up
		for (int y_offset = y; 
				y_offset < currentFrame.getHeight() && y_offset - y < colorDetectionRadius; 
				y_offset++) {
			
			rgb = currentFrame.getRGB(x, y_offset);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
			
		// Average colorDetectionRadius pixels down
		for (int y_offset = y; 
				y_offset > 0 && y - y_offset < colorDetectionRadius; 
				y_offset--) {
			
			rgb = currentFrame.getRGB(x, y_offset);
			r += getRed(rgb);
			g += getGreen(rgb);
			b += getBlue(rgb);
			pixelsSeen++;
		}
		
		r /= (double)pixelsSeen;
		g /= (double)pixelsSeen;
		b /= (double)pixelsSeen;
		
        // We only detect a color if the largest component is at least
        // 2% bigger than the other components. This is based on the
        // heuristic that noise tends to have color values that are very
        // similar
		final double PDIFF_THRESHOLD = 1.05;
		
        if (g == 0 || b == 0) 
            return Optional.empty();

        if ((r / g) > PDIFF_THRESHOLD && (r / b) > PDIFF_THRESHOLD)
            return Optional.of(Color.RED);

        if (r == 0 || b == 0)
        	return Optional.empty();

        if ((g / r) > PDIFF_THRESHOLD && (g / b) > PDIFF_THRESHOLD)
            return Optional.of(Color.GREEN);

		
		return Optional.empty();
	}
	
	private int mixColor(int red, int green, int blue) {
		return red << 16 | green << 8 | blue;
	}
	
	private int getRed(int rgb) {
		return (rgb & 0x00ff0000) >> 16;
	}
	
	private int getGreen(int rgb) {
		return (rgb & 0x0000ff00) >> 8;
	}
	
	private int getBlue(int rgb) {
		return (rgb & 0x000000ff) >> 0;
	}		
}