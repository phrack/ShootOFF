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
	public static final int FEED_WIDTH = 640;
	public static final int FEED_HEIGHT = 480;
	
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
	
	
	protected static BufferedImage threshold(Configuration config, BufferedImage grayScale) {
		BufferedImage threshholdedImg = new BufferedImage(grayScale.getWidth(),
				grayScale.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		for (int y = 0; y < grayScale.getHeight(); y++) {
			for (int x = 0; x < grayScale.getWidth(); x++) {
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
	
	private static int mixColor(int red, int green, int blue) {
		return red << 16 | green << 8 | blue;
	}
	
	protected static byte[][] getFrameCount(BufferedImage img) {
		byte[][] newCount = new byte[FEED_HEIGHT][FEED_WIDTH];
		
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = img.getRGB(x, y) & 0xFF;
				if (pixel == 255) newCount[y][x] = 1;
			}
		}
		
		return newCount;
	}
	
	private class Detector implements Runnable {
		private BufferedImage currentFrame;
		private final int BLOOM_COUNT = 10;
		private int oldestFrame = 0;
		private List<Object> counts = new ArrayList<Object>();
		private byte[][] bloomFilter = new byte[FEED_HEIGHT][FEED_WIDTH];
		
		@Override
		public void run() {
			if (!webcam.isOpen()) {
				webcam.setViewSize(new Dimension(FEED_WIDTH, FEED_HEIGHT));
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
				
				//Image img = SwingFXUtils.toFXImage(currentFrame, null);
				//canvasManager.updateBackground(img);
				
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
		
		private void addFrameCount(byte[][] count) {
			for (int y = 0; y < FEED_HEIGHT; y++) {
				for (int x = 0; x < FEED_WIDTH; x++) {
					bloomFilter[y][x] += count[y][x];
				}
			}
		}
		
		private void subFrameCount(byte[][] count) {
			for (int y = 0; y < FEED_HEIGHT; y++) {
				for (int x = 0; x < FEED_WIDTH; x++) {
					if (bloomFilter[y][x] >= 1)
						bloomFilter[y][x] -= count[y][x];
				}
			}
		}
		
		private BufferedImage countToImage(byte[][] count) {
			BufferedImage img = new BufferedImage(count[0].length,
					count.length, BufferedImage.TYPE_BYTE_GRAY);
			
			for (int y = 0; y < img.getHeight(); y++) {
				for (int x = 0; x < img.getWidth(); x++) {
					if (count[y][x] == 1) {
						img.setRGB(x, y, mixColor(255, 255, 255));
					} else {
						img.setRGB(x, y, mixColor(0, 0, 0));
					}
				}
			}
			
			return img;
		}
		
		private byte[][] generateMask() {
			byte[][] mask = new byte[FEED_HEIGHT][FEED_WIDTH];
			
			for (int y = 0; y < FEED_HEIGHT; y++) {
				for (int x = 0; x < FEED_WIDTH; x++) {
					if (bloomFilter[y][x] == 0) mask[y][x] = 0;
					else mask[y][x] = 1;
				}
			}
			
			return mask;	
		}
		
		private byte not(byte value) {
			if (value == 1) return 0;
			else return 1;
		}
		
		private byte[][] getShotFrame(byte[][] mask, byte[][] currentFrame) {
			byte[][] shotFrame = new byte[FEED_HEIGHT][FEED_WIDTH];
			
			for (int y = 0; y < FEED_HEIGHT; y++) {
				for (int x = 0; x < FEED_WIDTH; x++) {
					shotFrame[y][x] = (byte) (not(mask[y][x]) & currentFrame[y][x]);
				}
			}
			
			return shotFrame;	
		}
		
		private void detectShots() {
			if (!isDetecting) return;
			
			BufferedImage currentCopy = new BufferedImage(currentFrame.getWidth(),
					currentFrame.getHeight(), BufferedImage.TYPE_INT_RGB);
			currentCopy.createGraphics().drawImage(currentFrame, 0, 0, null);
			
			BufferedImage grayScale = new BufferedImage(currentCopy.getWidth(),
					currentFrame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			grayScale.createGraphics().drawImage(currentCopy, 0, 0, null);
			
			BufferedImage threshed = threshold(config, grayScale);
			
			if (counts.size() == BLOOM_COUNT) {
				byte[][] currentFrame = getFrameCount(threshed);
				byte[][] shotFrame = getShotFrame(generateMask(), currentFrame);
				
				new Thread(new ShotSearcher(config, canvasManager, 
						currentCopy, shotFrame)).start();
				
				Image img = SwingFXUtils.toFXImage(countToImage(shotFrame), null);
				canvasManager.updateBackground(img);
				
				// Update the bloom filter by removing the oldest frame
				// and adding the current one
				subFrameCount((byte[][])counts.get(oldestFrame));
				counts.set(oldestFrame, currentFrame);
				addFrameCount(currentFrame);
				oldestFrame = (oldestFrame + 1) % BLOOM_COUNT;
			} else {
				counts.add(getFrameCount(threshed));
				
				if (counts.size() == BLOOM_COUNT) {
					for (Object count : counts) addFrameCount((byte[][])count);
				}
			}
		}
	}	
}