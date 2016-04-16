package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import javafx.geometry.Bounds;

import com.shootoff.camera.arenamask.ArenaMaskManager;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.MockCanvasManager;
import com.xuggle.mediatool.IMediaListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.ICloseEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;

public class MockCameraManager extends CameraManager {
	public MockCameraManager() {
		super(new MockCamera(), null, new MockCanvasManager(null), null);
		this.processingLock = null;
	}

	protected File videoFile;
	protected long lastVideoTimestamp = -1;
	protected static final int SECOND_IN_MICROSECONDS = 1000 * 1000;

	protected final Object processingLock;
	protected boolean processedVideo = false;

	protected MockCameraManager(File videoFile, Object processingLock, CanvasManager canvas, Configuration config,
			boolean[][] sectorStatuses, Optional<Bounds> projectionBounds) {

		super(canvas, config);

		this.processingLock = processingLock;
		this.cameraView.setCameraManager(this);

		setSectorStatuses(sectorStatuses);

		if (projectionBounds.isPresent()) {
			setLimitDetectProjection(true);
			setProjectionBounds(projectionBounds.get());
		}

		this.videoFile = videoFile;

	}

	public void processVideo() {
		Detector detector = new Detector();

		IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
		reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		reader.addListener(detector);

		logger.trace("opening {}", videoFile.getAbsolutePath());

		while (reader.readPacket() == null)
			do {} while (false);
	}

	public void processVideo(IMediaListener listener) {
		Detector detector = new Detector();

		IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
		reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		reader.addListener(listener);
		reader.addListener(detector);

		logger.trace("opening {}", videoFile.getAbsolutePath());

		while (reader.readPacket() == null)
			do {} while (false);
	}

	protected class Detector extends MediaListenerAdapter implements Runnable {

		@Override
		public void run() {}

		/**
		 * From the MediaListenerAdapter. This method is used to get a new frame
		 * from a video that is being played back in a unit test, not to get a
		 * frame from the webcam.
		 */

		private long initialSystemTimeAtVideoStart = -1;

		@Override
		public void onVideoPicture(IVideoPictureEvent event) {
			BufferedImage currentFrame = event.getImage();

			if (initialSystemTimeAtVideoStart == -1) initialSystemTimeAtVideoStart = System.currentTimeMillis();

			currentFrameTimestamp = (event.getTimeStamp() / 1000) + initialSystemTimeAtVideoStart;

			if (getFrameCount() == 0) {
				setFeedResolution(currentFrame.getWidth(), currentFrame.getHeight());
				shotDetectionManager.reInitializeDimensions();
			}

			if (lastVideoTimestamp > -1 && (getFrameCount() % 5) == 0) {

				double estimateFPS = (double) SECOND_IN_MICROSECONDS
						/ (double) (event.getTimeStamp() - lastVideoTimestamp);

				setFPS(estimateFPS);
			}
			lastVideoTimestamp = event.getTimeStamp();

			processFrame(currentFrame);
		}

		@Override
		public void onClose(ICloseEvent event) {
			synchronized (processingLock) {
				processedVideo = true;
				processingLock.notifyAll();
			}
		}

	}

	public boolean isVideoProcessed() {
		return processedVideo;
	}

	public void setShotDetectionArenaMaskManager() {
		shotDetectionManager.setArenaMaskManager(arenaMaskManager);
	}

	protected ArenaMaskManager getArenaMaskManager() {
		return arenaMaskManager;
	}
}
