package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.cameratypes.CameraEventListener;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.xuggle.mediatool.IMediaListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.ICloseEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;

public class MockCamera extends MediaListenerAdapter implements Camera {
	protected static final Logger logger = LoggerFactory.getLogger(MockCamera.class);

	
	protected File videoFile;
	protected long lastVideoTimestamp = -1;
	protected static final int SECOND_IN_MICROSECONDS = 1000 * 1000;
	protected Optional<CameraEventListener> cameraEventListener = Optional.empty();

	public MockCamera() {
		super();
	}
	
	public MockCamera(File videoFile) {
		super();
		this.videoFile = videoFile;
	}

	@Override
	public boolean isOpen() {
		return true;
	}
	
	@Override
	public String getName()
	{
		return "MockCamera";
	}
	
	public void run() {
		if (videoFile == null)
			return;
		
		IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
		reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		reader.addListener(this);

		logger.trace("opening {}", videoFile.getAbsolutePath());

		while (reader.readPacket() == null)
			do {} while (false);
	}

	public void processVideo(IMediaListener listener) {
		IMediaReader reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
		reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		reader.addListener(listener);

		logger.trace("opening {}", videoFile.getAbsolutePath());

		while (reader.readPacket() == null)
			do {} while (false);
	}

	private long initialSystemTimeAtVideoStart = -1;
	protected long currentFrameTimestamp = -1;
	private int frameCount = 0;
	public static final int DEFAULT_FPS = 30;
	private double webcamFPS = 0.0;

	@Override
	public void onVideoPicture(IVideoPictureEvent event) {
		BufferedImage currentFrame = event.getImage();

		if (initialSystemTimeAtVideoStart == -1) initialSystemTimeAtVideoStart = System.currentTimeMillis();

		currentFrameTimestamp = (event.getTimeStamp() / 1000) + initialSystemTimeAtVideoStart;

		if (frameCount == 0) {
			if (cameraEventListener.isPresent())
				setViewSize(new Dimension(currentFrame.getWidth(), currentFrame.getHeight()));
				cameraEventListener.get().setFeedResolution(currentFrame.getWidth(), currentFrame.getHeight());
		}

		if (lastVideoTimestamp > -1 && (frameCount % 30) == 0) {

			double estimateFPS = (double) SECOND_IN_MICROSECONDS
					/ (double) (event.getTimeStamp() - lastVideoTimestamp);

			setFPS(estimateFPS);
		}
		lastVideoTimestamp = event.getTimeStamp();

		if (cameraEventListener.isPresent())
			cameraEventListener.get().newFrame(Camera.bufferedImageToMat(currentFrame));
		
		frameCount++;
	}
	protected void setFPS(double newFPS) {
		// This just tells us if it's the first FPS estimate
		if (getFrameCount() > DEFAULT_FPS)
			webcamFPS = ((webcamFPS * 4.0) + newFPS) / 5.0;
		else
			webcamFPS = newFPS;
	}
	

	@Override
	public void onClose(ICloseEvent event) {
		if (cameraEventListener.isPresent())
			cameraEventListener.get().cameraClosed();
	}

	@Override
	public Mat getMatFrame() {
		return null;
	}

	@Override
	public BufferedImage getBufferedImage() {
		return null;
	}

	@Override
	public boolean open() {
		return false;
	}

	@Override
	public void close() {
		return;
	}

	@Override
	public void setCameraEventListener(CameraEventListener cameraEventListener) {
		this.cameraEventListener = Optional.of(cameraEventListener);
	}

	@Override
	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	@Override
	public int getFrameCount() {
		return frameCount;
	}

	@Override
	public ShotDetector getPreferredShotDetector(CameraManager cameraManager, CameraView cameraView) {
		if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, cameraView);
		else
			return null;
	}

	@Override
	public boolean isLocked() {
		return false;
	}
	
	private Dimension size = null;

	@Override
	public void setViewSize(Dimension size) {
		this.size = size;
	}

	@Override
	public Dimension getViewSize() {
		return size;
	}

	@Override
	public double getFPS() {
		return webcamFPS;
	}

	@Override
	public boolean setState(CameraState state) {
		return true;
	}
	public CameraState getState()
	{
		return CameraState.DETECTING;
	}

	@Override
	public boolean supportsExposureAdjustment() {

		return false;
	}

	public boolean decreaseExposure() {
		return false;
	}
	
	public void resetExposure()
	{
		return;
	}
	
	@Override
	public boolean limitsFrames()
	{
		return false;
	}
}
