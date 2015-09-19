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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.DebuggerListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.ICloseEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.util.Pair;

public class CameraManager {
	public static final int FEED_WIDTH = 640;
	public static final int FEED_HEIGHT = 480;
	public static final int MIN_SHOT_DETECTION_FPS = 5;
	
	// These thresholds were calculated using all of the test videos
	public static final int LIGHTING_CONDITION_VERY_BRIGHT_THRESHOLD = 130;
	// Anything below this threshold is considered dark
	public static final int LIGHTING_CONDITION__BRIGHT_THRESHOLD = 90; 	
	
	public static final float IDEAL_R_AVERAGE = 171; // Determined by averaging all of the red pixels per frame
												     // for a video recorded using a webcam with hw settings that
												     // worked well
	public static final float IDEAL_LUM = 136;		 // See comment above
	public static final int INIT_FRAME_COUNT = 5; // Used by current pixel transformer to decide how many frames
												  // to use for initialization

	private final PixelTransformer pixelTransformer;

	private final Logger logger = LoggerFactory.getLogger(CameraManager.class);
	private final Optional<Camera> webcam;
	private final Object processingLock;
	private boolean processedVideo = false;
	private final CanvasManager canvasManager;
	private final Configuration config;
	private Optional<Bounds> projectionBounds = Optional.empty();

	private boolean isStreaming = true;
	private boolean isDetecting = true;
	private boolean cropFeedToProjection = false;
	private boolean limitDetectProjection = false;
	private Optional<Integer> centerApproxBorderSize = Optional.empty();
	private Optional<Integer> minimumShotDimension = Optional.empty();
	private Optional<DebuggerListener> debuggerListener = Optional.empty();

	private boolean recordingStream = false;
	private boolean isFirstStreamFrame = true;
	private IMediaWriter videoWriterStream;
	private long recordingStartTime;
	
	private boolean recordingShots = false;
	private RollingRecorder rollingRecorder;
	private Map<Shot, ShotRecorder> shotRecorders = new ConcurrentHashMap<Shot, ShotRecorder>();
 	
	private boolean[][] sectorStatuses;
	
	public static int TESTING_framecount = 0;

	protected CameraManager(Camera webcam, CanvasManager canvas, Configuration config) {
		this.webcam = Optional.of(webcam);
		processingLock = null;
		this.canvasManager = canvas;
		this.config = config;

		this.pixelTransformer = new ShotSearchingBrightnessPixelTransformer(config, canvasManager, sectorStatuses,
				projectionBounds, cropFeedToProjection);
		
		init(new Detector());
	}

	protected CameraManager(File videoFile, Object processingLock, CanvasManager canvas,
			Configuration config, boolean[][] sectorStatuses, Optional<Bounds> projectionBounds) {
		this.webcam = Optional.empty();
		this.processingLock = processingLock;
		this.canvasManager = canvas;
		this.config = config;
		
		if (projectionBounds.isPresent()) {
			setLimitDetectProjection(true);
			setProjectionBounds(projectionBounds.get());
		}
		
		this.pixelTransformer = new ShotSearchingBrightnessPixelTransformer(config, canvasManager, sectorStatuses,
				projectionBounds, cropFeedToProjection);

		Detector detector = new Detector();
		
	    IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
	    reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
	    reader.addListener(detector);

	    setSectorStatuses(sectorStatuses);
	    
	    while (reader.readPacket() == null)
	      do {} while(false);
	}

	private void init(Detector detector) {
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];

		// Turn on all shot sectors by default
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}

		new Thread(detector).start();
	}

	public boolean[][] getSectorStatuses() {
		return sectorStatuses;
	}

	public void setSectorStatuses(boolean[][] sectorStatuses) {
		this.sectorStatuses = sectorStatuses;
	}

	public void clearShots() {
		canvasManager.clearShots();
	}

	public void reset() {
		canvasManager.reset();
	}

	public void close() {
		if (webcam.isPresent()) webcam.get().close();
		if (recordingStream) stopRecordingStream();
	}

	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	public void setDetecting(boolean isDetecting) {
		this.isDetecting = isDetecting;
	}

	public void setProjectionBounds(Bounds projectionBounds) {
		this.projectionBounds = Optional.ofNullable(projectionBounds);
	}

	public void setCropFeedToProjection(boolean cropFeed) {
		cropFeedToProjection = cropFeed;
	}

	public void setLimitDetectProjection(boolean limitDetection) {
		limitDetectProjection = limitDetection;
	}

	public void startRecordingStream(File videoFile) {
		logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
		videoWriterStream = ToolFactory.makeWriter(videoFile.getName());
		videoWriterStream.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, FEED_WIDTH, FEED_HEIGHT);
		recordingStartTime = System.currentTimeMillis();
		isFirstStreamFrame = true;

		recordingStream = true;
	}

	public void stopRecordingStream() {
		recordingStream = false;
		videoWriterStream.close();
	}
	
	public void notifyShot(Shot shot) {
		shotRecorders.put(shot, rollingRecorder.fork());
	}
	
	public ShotRecorder getRevelantRecorder(Shot shot) {
		return shotRecorders.get(shot);
	}
	
	public void startRecordingShots() {
		String sessionName = null;
		if (config.getSessionRecorder().isPresent()) {
			sessionName = config.getSessionRecorder().get().getSessionName();
			
			File sessionVideoFolder = new File(System.getProperty("shootoff.home") + File.separator + 
					"sessions" + File.separator + config.getSessionRecorder().get().getSessionName());
			
			if (!sessionVideoFolder.exists()) {
				if (!sessionVideoFolder.mkdirs()) {
					logger.error("Could not create video folder for session: {}", sessionVideoFolder.getAbsolutePath());
				}
			}
		}
		
		String cameraName = "UNNAMED";
		
		if (webcam.isPresent()) {
			Optional<String> userCameraName = config.getWebcamsUserName(webcam.get());
			
			if (userCameraName.isPresent()) {
				cameraName = userCameraName.get();
			} else {
				cameraName = webcam.get().getName();
			}
		}
		
		rollingRecorder = new RollingRecorder(ICodec.ID.CODEC_ID_MPEG4, ".mp4", sessionName, cameraName);
		recordingShots = true;
	}
	
	public void stopRecordingShots() {
		recordingShots = false;
		for (ShotRecorder r : shotRecorders.values()) r.close();
		shotRecorders.clear();
		rollingRecorder.close();
		rollingRecorder = null;
	}
	
	public Image getCurrentFrame() {
		if (webcam.isPresent()) {
			return SwingFXUtils.toFXImage(webcam.get().getImage(), null);
		} else {
			return null;
		}
	}

	public CanvasManager getCanvasManager() {
		return canvasManager;
	}

	public boolean isVideoProcessed() {
		return processedVideo;
	}

	public void setCenterApproxBorderSize(int borderSize) {
		centerApproxBorderSize = Optional.of(borderSize);
		logger.debug("Set the shot center approximation border size to: {}", borderSize);
	}

	public void setMinimumShotDimension(int minDim) {
		minimumShotDimension = Optional.of(minDim);
		logger.debug("Set the minimum dimension for shots to: {}", minDim);
	}
	
	public void setThresholdListener(DebuggerListener thresholdListener) {
		this.debuggerListener = Optional.ofNullable(thresholdListener);
	}

	private class Detector extends MediaListenerAdapter implements Runnable {
		private boolean showedFPSWarning = false;
		private boolean pixelTransformerInitialized = false;
		private int seenFrames = 0;
		private final ExecutorService detectionExecutor = Executors.newFixedThreadPool(200);
		
		@Override
		public void run() {
			if (webcam.isPresent()) {
				if (!webcam.get().isOpen()) {
					webcam.get().setViewSize(new Dimension(FEED_WIDTH, FEED_HEIGHT));
					webcam.get().open();
				}

				streamCameraFrames();
			}
		}

		@Override
		/**
		 * From the MediaListenerAdapter. This method is used to get a new frame
		 * from a video that is being played back in a unit test, not to get
		 * a frame from the webcam.
		 */
		public void onVideoPicture(IVideoPictureEvent event)
		{
			BufferedImage currentFrame = event.getImage();
			//AverageFrameComponents averages = averageFrameComponents(currentFrame);
			
			if (pixelTransformerInitialized == false) {
				seenFrames++;
				if (seenFrames == INIT_FRAME_COUNT) pixelTransformerInitialized = true;
			}
			detectShots(currentFrame, pixelTransformerInitialized);
		}

		@Override
		public void onClose(ICloseEvent event) {
			synchronized (processingLock) {
				processedVideo = true;
				processingLock.notifyAll();
			}
			
			detectionExecutor.shutdown();
		}

		private void streamCameraFrames() {			
			long startDetectionCycle = System.currentTimeMillis();

			while (isStreaming) {
				if (!webcam.isPresent() || !webcam.get().isImageNew()) continue;
				
				BufferedImage currentFrame = webcam.get().getImage();

				if (currentFrame == null && webcam.isPresent() && !webcam.get().isOpen()) {
					showMissingCameraError();
					detectionExecutor.shutdown();
					return;
				}
				
				//final AverageFrameComponents averages = averageFrameComponents(currentFrame);

				detectShots(currentFrame, pixelTransformerInitialized);
				
				if (pixelTransformerInitialized == false) {
					seenFrames++;
					if (seenFrames == INIT_FRAME_COUNT) { 
						/*if (averages.getLightingCondition() == LightingCondition.VERY_BRIGHT) {
							showBrightnessWarning();
						}*/
						
						pixelTransformerInitialized = true;
					} else {
						continue;
					}
				}
				
				if (recordingShots) {
					rollingRecorder.recordFrame(currentFrame);
					
					List<Shot> removeKeys = new ArrayList<Shot>();
					for (Entry<Shot, ShotRecorder> r : shotRecorders.entrySet()) {
						if (r.getValue().isComplete()) {
							r.getValue().close();
							removeKeys.add(r.getKey());
						} else {
							r.getValue().recordFrame(currentFrame);
						}
					}
					
					for (Shot s : removeKeys) shotRecorders.remove(s);
				}

				if (recordingStream) {
					BufferedImage image = ConverterFactory.convertToType(currentFrame, BufferedImage.TYPE_3BYTE_BGR);
					IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

					IVideoPicture frame = converter.toPicture(image,
							(System.currentTimeMillis() - recordingStartTime) * 1000);
					frame.setKeyFrame(isFirstStreamFrame);
					frame.setQuality(0);
					isFirstStreamFrame = false;

					videoWriterStream.encodeVideo(0, frame);
				}

				if (cropFeedToProjection && projectionBounds.isPresent()) {
					Bounds b = projectionBounds.get();
					currentFrame = currentFrame.getSubimage((int)b.getMinX(), (int)b.getMinY(),
							(int)b.getWidth(), (int)b.getHeight());
				}

				Image img = SwingFXUtils.toFXImage(currentFrame, null);

				if (cropFeedToProjection) {
					canvasManager.updateBackground(img, projectionBounds);
				} else {
					canvasManager.updateBackground(img, Optional.empty());
				}

				if (System.currentTimeMillis() -
						startDetectionCycle >= config.getDetectionRate()) {

					startDetectionCycle = System.currentTimeMillis();
					final BufferedImage frame = currentFrame;
					//detectionExecutor.submit(() -> {detectShots(frame, averages);});
					//detectionExecutor.submit(() -> {detectShots(frame, pixelTransformerInitialized);});
					
				}
			}
			
			detectionExecutor.shutdown();
		}

		private class AverageFrameComponents {
			private final float averageLum;
			private final float averageRed;

			public AverageFrameComponents(float lum, float red) {
				averageLum = lum; averageRed = red;
			}
			
			public float getAverageRed() {
				return averageRed;
			}
			
			public LightingCondition getLightingCondition() {
				if (averageLum > LIGHTING_CONDITION_VERY_BRIGHT_THRESHOLD) {
					return LightingCondition.VERY_BRIGHT;
				} else if (averageLum > LIGHTING_CONDITION__BRIGHT_THRESHOLD) {
					return LightingCondition.BRIGHT;
				} else {
					return LightingCondition.DARK;
				} 
			}
		}
		
		private AverageFrameComponents averageFrameComponents(BufferedImage frame) {
			long totalLum = 0;
			long totalRed = 0;
			
			int minX;
			int maxX;
			int minY;
			int maxY;
			
			if (limitDetectProjection && projectionBounds.isPresent()) {
				minX = (int)projectionBounds.get().getMinX();
				maxX = (int)projectionBounds.get().getMaxX();
				minY = (int)projectionBounds.get().getMinY();
				maxY = (int)projectionBounds.get().getMaxY();
			} else {
				minX = 0;
				maxX = frame.getWidth();
				minY = 0;
				maxY = frame.getHeight();
			}
			
			/*for (int x = minX; x < maxX; x++) {
				for (int y = minY; y < maxY; y++) {
					//int rgb = frame.getRGB(x, y);

					pixelTransformer.updateFilter(frame, x, y);

					//totalLum += PixelTransformer.calcLums(rgb);
					//totalRed += (rgb >> 16) & 0xFF;
				}
			}*/

			//float totalPixels = (float)(frame.getWidth() * frame.getHeight());

			//return new AverageFrameComponents((float)(totalLum) / totalPixels,
			//		(float)(totalRed) / totalPixels);
			
			return null;
		}

		/* This is not perfect because it treats color temps as linear.
		 * Essentially we use the difference between the ideal average r
		 * component and the average for the current frame to adjust red
		 * and blue up or down to get roughly the ideal color temperature
		 * for shot detection.
		 */
		private void adjustColorTemperature(BufferedImage frame, int x, int y, float dr, float db) {
			Color c = new Color(frame.getRGB(x, y));

			float r = c.getRed() * dr;
			if (r > 255) r = 255;
			if (r < 0) r = 0;
			float b = c.getBlue() * db;
			if (b > 255) b = 255;
			if (b < 0) b = 0;

			frame.setRGB(x, y, new Color((int)r, c.getGreen(), (int)b).getRGB());
		}
		
		private void detectShots(BufferedImage frame, boolean pixelTransformerInitialized) {
			if (!isDetecting) return;

			BufferedImage workingCopy = frame;
			
			int minX;
			int maxX;
			int minY;
			int maxY;

			if (limitDetectProjection && projectionBounds.isPresent()) {
				Bounds b = projectionBounds.get();
				BufferedImage subFrame = frame.getSubimage((int)b.getMinX(), (int)b.getMinY(),
						(int)b.getWidth(), (int)b.getHeight());
				workingCopy.createGraphics().drawImage(subFrame, (int)b.getMinX(), (int)b.getMinY(), null);
				
				minX = (int)projectionBounds.get().getMinX();
				maxX = (int)projectionBounds.get().getMaxX();
				minY = (int)projectionBounds.get().getMinY();
				maxY = (int)projectionBounds.get().getMaxY();
			} else {
				//workingCopy.createGraphics().drawImage(frame, 0, 0, null);
				
				minX = 0;
				maxX = frame.getWidth();
				minY = 0;
				maxY = frame.getHeight();
			}

			/*float averageRed = averages.getAverageRed();
			float dr = IDEAL_R_AVERAGE / averageRed;
			float db = 1 - (dr - 1);*/
			
			//logger.warn("FRAME: {}", TESTING_framecount);
			TESTING_framecount++;
			
			/*BufferedImage grayScale = new BufferedImage(frame.getWidth(),
					frame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			grayScale.createGraphics().drawImage(workingCopy, 0, 0, null);

			//ShotSearcher shotSearcher = new ShotSearcher(config, canvasManager, sectorStatuses,
			//		frame, grayScale, projectionBounds, cropFeedToProjection);
			((ShotSearchingBrightnessPixelTransformer) pixelTransformer).grayScale = grayScale;*/
			((ShotSearchingBrightnessPixelTransformer) pixelTransformer).currentFrame = frame;
			
			
			for (int x = minX; x < maxX; x++) {
				for (int y = minY; y < maxY; y++) {
			
					// If the color temperatures (using just the red component as an
					// approximation) are only a bit off from ideal step up the heat.
					// We don't want to make big changes or it will blow up in dark
					// rooms by trying to do huge corrections that max r and zero b
					// components in rgb pixels.
					/*if (averageRed < IDEAL_R_AVERAGE && dr < 2f) {
						adjustColorTemperature(workingCopy, x, y, dr, db);
					}*/
					
					//pixelTransformer.applyFilter(workingCopy, x, y, averages.getLightingCondition());
					
					ArrayList<Pair<Integer,Integer>> possibleShots = new ArrayList<Pair<Integer,Integer>>();
					
					if(((ShotSearchingBrightnessPixelTransformer) pixelTransformer).updateFilter(frame, x, y, pixelTransformerInitialized))
					{
						if (possibleShots.size()<20)
							possibleShots.add(new Pair<Integer, Integer>(x,y));
					}
					
					if (possibleShots.size()<20)	
						for (Pair<Integer,Integer> shotxy : possibleShots)
							((ShotSearchingBrightnessPixelTransformer) pixelTransformer).findShotWithFrame(frame, shotxy.getKey(), shotxy.getValue());
				}
			}



			
			if (webcam.isPresent()) {
				double webcamFPS = webcam.get().getFPS();
				if (debuggerListener.isPresent()) {
					//debuggerListener.get().updateFeedData(webcamFPS, averages.getLightingCondition());
				}
				if (webcamFPS < MIN_SHOT_DETECTION_FPS && !showedFPSWarning) {
					logger.warn("[{}] Current webcam FPS is {}, which is too low for reliable shot detection",
							webcam.get().getName(), webcamFPS);
					showFPSWarning(webcamFPS);
					showedFPSWarning = true;
				}
			}

			if (centerApproxBorderSize.isPresent()) {
				((ShotSearchingBrightnessPixelTransformer) pixelTransformer).setCenterApproxBorderSize(centerApproxBorderSize.get());
			}

			if (minimumShotDimension.isPresent()) {
				((ShotSearchingBrightnessPixelTransformer) pixelTransformer).setMinimumShotDimension(minimumShotDimension.get());
			}

			//new Thread(shotSearcher).start();

			if (debuggerListener.isPresent()) {
				debuggerListener.get().updateDebugView(workingCopy);
			}
		}
		
		private void showMissingCameraError() {
			Platform.runLater(() -> {
				Alert cameraAlert = new Alert(AlertType.ERROR);

				Optional<String> cameraName = config.getWebcamsUserName(webcam.get());
				String messageFormat = "ShootOFF can no longer communicate with the webcam %s. Was it unplugged?";
				String message;
				if (cameraName.isPresent()) {
					message = String.format(messageFormat, cameraName.get());
				} else {
					message = String.format(messageFormat, webcam.get().getName());
				}

				cameraAlert.setTitle("Webcam Missing");
				cameraAlert.setHeaderText("Cannot Communicate with Camera!");
				cameraAlert.setResizable(true);
				cameraAlert.setContentText(message);
				cameraAlert.show();
			});
		}
		
		private void showFPSWarning(double fps) {
			Platform.runLater(() -> {
				Alert cameraAlert = new Alert(AlertType.WARNING);

				Optional<String> cameraName = config.getWebcamsUserName(webcam.get());
				String messageFormat = "The FPS from %s has dropped to %f, which is too low for reliable shot detection. Some"
						+ " shots may be missed. You may be able to raise the FPS by closing other applications.";
				String message;
				if (cameraName.isPresent()) {
					message = String.format(messageFormat, cameraName.get(), fps);
				} else {
					message = String.format(messageFormat, webcam.get().getName(), fps);
				}

				cameraAlert.setTitle("Webcam FPS Too Low");
				cameraAlert.setHeaderText("Webcam FPS is too low!");
				cameraAlert.setResizable(true);
				cameraAlert.setContentText(message);
				cameraAlert.show();
			});
		}
		
		private void showBrightnessWarning() {
			Platform.runLater(() -> {
				Alert cameraAlert = new Alert(AlertType.WARNING);

				Optional<String> cameraName = config.getWebcamsUserName(webcam.get());
				String messageFormat = "The camera %s is streaming frames that are very bright. "
						+ " This will increase the odds of shots falsely being detected."
						+ " For best results, please do any mix of the following:\n\n"
						+ "-Turn off auto white balance and auto focus on your webcam and reduce the brightness\n"
						+ "-Remove any bright light sources in the camera's view\n"
						+ "-Turn down your projector's brightness and contrast\n"
						+ "-Dim any lights in the room or turn them off, especially those behind the shooter";
				String message;
				if (cameraName.isPresent()) {
					message = String.format(messageFormat, cameraName.get());
				} else {
					message = String.format(messageFormat, webcam.get().getName());
				}

				cameraAlert.setTitle("Conditions Very Bright");
				cameraAlert.setHeaderText("Webcam detected very bright conditions!");
				cameraAlert.setResizable(true);
				cameraAlert.setContentText(message);
				cameraAlert.show();
			});
		}
	}
}