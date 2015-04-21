package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

import javafx.embed.swing.SwingFXUtils;
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
	
	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}
	
	public void setDetecting(boolean isDetecting) {
		this.isDetecting = isDetecting;
	}
	
	private class Detector implements Runnable {
		private BufferedImage currentFrame;
		
		@Override
		public void run() {
			webcam.setViewSize(new Dimension(640, 480));
			webcam.open();			
			
			streamCameraFrames();
		}
		
		private void streamCameraFrames() {
			long startDetectionCycle = System.currentTimeMillis();
			
			while (isStreaming) {
				currentFrame = webcam.getImage();
				
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
