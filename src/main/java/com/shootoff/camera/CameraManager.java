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

import java.util.Optional;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;


import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;

public class CameraManager {
	private final Webcam webcam;
	private final CanvasManager canvasManager;
	private final Configuration config;
	private final int webcamRefreshDelay; // in milliseconds (ms)
	
	private boolean isStreaming = true;
	private boolean isDetecting = true;
	
	protected CameraManager(Webcam webcam, CanvasManager canvas, Configuration config) {
		this.webcam = webcam;
		this.canvasManager = canvas;
		this.config = config;
		
		if (webcam.getFPS() == 0) {
			webcamRefreshDelay = 30; // ms
		} else {
			webcamRefreshDelay = (int)(1000 / webcam.getFPS());
		}
		
		new Thread(new Detector()).start();
	}

	public void clearShots() {
		canvasManager.clearShots();
	}
	
	public void reset() {
		canvasManager.reset();
	}
	
	public void close() {
		webcam.close();
	}
	
	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}
	
	public void setDetecting(boolean isDetecting) {
		this.isDetecting = isDetecting;
	}
	
	public Image getCurrentFrame() {
		return SwingFXUtils.toFXImage(webcam.getImage(), null);
	}
	
	public CanvasManager getCanvasManager() {
		return canvasManager;
	}
	
	private class Detector implements Runnable {
		private BufferedImage currentFrame;
		
		@Override
		public void run() {
			if (!webcam.isOpen()) {
				webcam.setViewSize(new Dimension(640, 480));
				webcam.open();			
			}
			
			streamCameraFrames();
		}
		
		private void streamCameraFrames() {
			long startDetectionCycle = System.currentTimeMillis();
			
			while (isStreaming) {
				currentFrame = webcam.getImage();
				
				if (currentFrame == null && !webcam.isOpen()) {
					Platform.runLater(() -> {
							Alert cameraAlert = new Alert(AlertType.ERROR);
							
							Optional<String> cameraName = config.getWebcamsUserName(webcam);
							String messageFormat = "ShootOFF can no longer communicate with the webcam %s. Was it unplugged?";
							String message;
							if (cameraName.isPresent()) {
								message = String.format(messageFormat, cameraName.get());
							} else {
								message = String.format(messageFormat, webcam.getName());
							}
							
							cameraAlert.setTitle("Webcam Missing");
							cameraAlert.setHeaderText("Cannot Communicate with Camera!");
							cameraAlert.setResizable(true);
							cameraAlert.setContentText(message);
							cameraAlert.show();
						});
					
					return;
				}
				
				Image img = SwingFXUtils.toFXImage(currentFrame, null);
				
				canvasManager.updateBackground(img);
				
				if (System.currentTimeMillis() - 
						startDetectionCycle >= config.getDetectionRate()) {
					
					startDetectionCycle = System.currentTimeMillis();
					detectShots();
				}
				
				try {
					Thread.sleep(webcamRefreshDelay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void detectShots() {
			if (!isDetecting) return;
			
			BufferedImage currentCopy = new BufferedImage(currentFrame.getWidth(),
					currentFrame.getHeight(), BufferedImage.TYPE_INT_RGB);
			currentCopy.createGraphics().drawImage(currentFrame, 0, 0, null);
			
			BufferedImage grayScale = new BufferedImage(currentCopy.getWidth(),
					currentFrame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			grayScale.createGraphics().drawImage(currentCopy, 0, 0, null);
			
			new Thread(new ShotSearcher(config, canvasManager, 
					currentCopy, grayScale)).start();
		}
	}	
}
