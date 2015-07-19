/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.github.sarxos.webcam.Webcam;

public class Camera {
	// These are used in a hack to get this code to work on Mac.
	// On Mac several of the webcam-capture API's can only be
	// called on the main thread before a JavaFX thread is started
	// or the library will hopeless hang and take ShootOFF with it.
	// Our solution is to cache the things we need that will hang
	// the program on start-up. This has the side effect that the
	// cameras that are known when ShootOFF starts are the only
	// ones it will ever know on Mac.
	private static final boolean isMac;
	private static final Webcam defaultWebcam;
	private static final List<Camera> knownWebcams;
	
	static {
		String os = System.getProperty("os.name");
		
		if (os != null && os.equals("Mac OS X")) {
			isMac = true;
			defaultWebcam = Webcam.getDefault();
			
			knownWebcams = new ArrayList<Camera>();
				
			for (Webcam w : Webcam.getWebcams()) {
				knownWebcams.add(new Camera(w));
			}
			
		} else {
			isMac = false;
			defaultWebcam = null;
			knownWebcams = null;
		}
	}
	
	private Webcam webcam;
	
	private Camera(Webcam webcam) {
		this.webcam = webcam;
	}
	
	
	public static Camera getDefault() {
		if (isMac)
			return new Camera(defaultWebcam);
		else
			return new Camera(Webcam.getDefault());
	}
	
	public static List<Camera> getWebcams() {
		if (isMac) return knownWebcams;
		
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
		if (isMac) {
			new Thread(() -> { webcam.close(); }).start();
			return true;
		} else {
			return webcam.close();
		}
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
