package com.shootoff.camera;

import java.awt.image.BufferedImage;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import com.shootoff.camera.cameratypes.Camera;

public class Frame {
	protected Mat mat;
	final protected long timestamp;

	public Frame(Mat mat, long timestamp) {
		this.mat = mat;
		this.timestamp = timestamp;
	}

	public Frame(BufferedImage bimg, long timestamp) {
		mat = Camera.bufferedImageToMat(bimg);
		this.timestamp = timestamp;
	}

	public void setMat(Mat mat) {
		this.mat = mat;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Mat getOriginalMat() {
		return mat;
	}

	public Mat getCloneMat() {
		return mat.clone();
	}

	public BufferedImage getOriginalBufferedImage() {
		return Camera.matToBufferedImage(mat);
	}

	public Size size() {
		return mat.size();
	}
}
