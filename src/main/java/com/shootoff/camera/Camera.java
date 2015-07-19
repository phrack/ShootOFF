package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.github.sarxos.webcam.Webcam;

public class Camera {
	private Webcam webcam;
	
	private Camera(Webcam webcam) {
		this.webcam = webcam;
	}
	
	public static Camera getDefault() {
		return new Camera(Webcam.getDefault());
	}
	
	public static List<Camera> getWebcams() {
		List<Camera> webcams = new ArrayList<Camera>();
		
		for (Webcam w : Webcam.getWebcams()) {
			webcams.add(new Camera(w));
		}
		
		return webcams;
	}
	
	public BufferedImage getImage() {
		return webcam.getImage();
	}
	
	public boolean open() {
		return webcam.open();
	}
	
	public boolean close() {
		return webcam.close();
	}
	
	public String getName() {
		return webcam.getName();
	}
	
	public boolean isOpen() {
		return webcam.isOpen();
	}
	
	public boolean isLocked() {
		return webcam.getLock().isLocked();
	}
	
	public boolean isImageNew() {
		return webcam.isImageNew();
	}
	
	public double getFPS() {
		return webcam.getFPS();
	}
	
	public void setViewSize(Dimension size) {
		webcam.setViewSize(size);
	}
}
