/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.shootoff.camera.arenamask;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.shootoff.camera.Camera;

public class Mask {
	public BufferedImage bImage;
	public Mat mask;
	public final long timestamp;
	
	public Mask(BufferedImage bImage, long timestamp)
	{
		this.bImage = bImage;
		this.timestamp = timestamp;
		this.mask = new Mat();
	}
	
	public Mat getMask(Size targetSize)
	{
		// Indicates it is already initialized
		if (mask.rows() > 0)
			return mask;
		
		Mat src = Camera.bufferedImageToMat(bImage);	
		Imgproc.resize(src, mask, targetSize);
		
		bImage = Camera.matToBufferedImage(mask);
		
		int dilation_size = 5;
		Mat kern = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new  Size(2*dilation_size + 1, 2*dilation_size+1));
		Imgproc.dilate(mask, mask, kern);

		Imgproc.blur(mask, mask, new Size(9,9));
		
		
		//Imgproc.GaussianBlur(mask, mask, new Size(11,11), 8.0);	
		
		return mask;
	}
	
	private double avgMaskLum = 0;
	public double getAvgMaskLum()
	{
		return avgMaskLum;
	}
	
	public Mat getSplitMask(Size targetSize)
	{
		// Indicates it is already initialized
		if (mask.rows() > 0)
			return mask;
		
		Mat src = Camera.bufferedImageToMat(bImage);	
		Imgproc.resize(src, mask, targetSize);
		
		bImage = Camera.matToBufferedImage(mask);
		
		Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2HSV);
		
		
		List<Mat> maskchannels = new ArrayList<Mat>();
		Core.split(mask, maskchannels );
		
		mask = maskchannels.get(2);
		
		
		for (int y = 0; y < mask.rows(); y++)
		{
			for (int x = 0; x < mask.cols(); x++)
			{
				avgMaskLum += mask.get(y, x)[0];
			}
		}
		
		avgMaskLum /= mask.rows()*mask.cols();
		
		int dilation_size = 5;
		Mat kern = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new  Size(2*dilation_size + 1, 2*dilation_size+1));
		Imgproc.dilate(mask, mask, kern);

		Imgproc.blur(mask, mask, new Size(9,9));
		
		
		//Imgproc.GaussianBlur(mask, mask, new Size(11,11), 8.0);	
		
		return mask;
	}
}
