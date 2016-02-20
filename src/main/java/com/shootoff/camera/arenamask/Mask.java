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

import org.opencv.core.CvType;
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
	
	private int avgMaskLum = 0;
	public int getAvgMaskLum()
	{
		return avgMaskLum;
	}
	
	public Mat getLumMask(Size targetSize)
	{
		// Indicates it is already initialized
		if (mask != null && mask.rows() > 0)
			return mask;
		
		mask = new Mat((int)targetSize.height, (int)targetSize.width, CvType.CV_8UC1);
		
		Mat src = Camera.bufferedImageToMat(bImage);	
		Imgproc.resize(src, src, targetSize);
		
		bImage = Camera.matToBufferedImage(mask);
		
		Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2HSV);
		
		
		for (int y = 0; y < src.rows(); y++)
		{
			for (int x = 0; x < src.cols(); x++)
			{
				byte[] px = {0, 0, 0};
				src.get(y, x, px);
				int pxS = px[1] & 0xFF;
				int pxV = px[2] & 0xFF;
				
				int pxLum = ((255-pxS)*pxV);
				
				avgMaskLum += pxLum;
				
				byte[] dstLum = { (byte) pxLum };
				mask.put(y, x, dstLum);
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
