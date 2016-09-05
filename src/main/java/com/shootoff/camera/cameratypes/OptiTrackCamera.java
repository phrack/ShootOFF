package com.shootoff.camera.cameratypes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.NativeShotDetector;
import com.shootoff.camera.shotdetection.OptiTrackShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;

import javafx.application.Platform;

public class OptiTrackCamera implements Camera {
	private static final Logger logger = LoggerFactory.getLogger(OptiTrackCamera.class);
	
	private static boolean initialized = false;
	protected CameraState cameraState;
	protected Optional<CameraEventListener> cameraEventListener = Optional.empty();
	protected long currentFrameTimestamp = -1;
	private Dimension dimension = null;
	private int viewWidth = 0;
	private int viewHeight = 0;
	
	public OptiTrackCamera()
	{
		if (!initialized)
			init();
	}
	
	public static void init()
	{
		if (initialized)
			return;
		
		try {
			File lib = new File(System.mapLibraryName("OptiTrackCamera"));
			System.load(lib.getAbsolutePath());
			initialize();
			initialized = true;
		} catch (UnsatisfiedLinkError exception)
		{
			initialized = false;
		}
		
		
		if (initialized && cameraAvailableNative())
		{
			CameraFactory.registerCamera(new OptiTrackCamera());
		}
	}
	
	public void setState(CameraState cameraState)
	{
		this.cameraState = cameraState;
	}
	
	
	public void setCameraEventListener(CameraEventListener cameraEventListener)
	{
		logger.debug("got event listener");
		this.cameraEventListener = Optional.of(cameraEventListener);
		this.cameraEventListener.get().newFPS(5);
	}
	
	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}
	
	public String getName()
	{
		return "OptiTrack";
	}
	
	private native static void initialize();
	
	
	private native static boolean cameraAvailableNative();

	
	public native boolean open();
	
	public void setViewSize(final Dimension size) {
		// Not supported
		return;
	}
	
	public Dimension getViewSize()
	{
		if (dimension != null)
			return dimension;
		
		dimension = new Dimension(getViewWidth(), getViewHeight());
		
		return dimension;
	}

	private native int getViewWidth();
	private native int getViewHeight();
	
	public native boolean isOpen();

	public Mat translateCameraArrayToMat(byte[] imageBuffer)
	{
		if (viewHeight == 0)
			viewHeight = getViewHeight();
		if (viewWidth == 0)
			viewWidth = getViewWidth();
		
		Mat mat = new Mat(viewHeight, viewWidth, CvType.CV_8UC1);
		Mat dst = new Mat(viewHeight, viewWidth, CvType.CV_8UC3);
		
		mat.put(0,0, imageBuffer);
		Imgproc.cvtColor(mat, dst, Imgproc.COLOR_GRAY2BGR);
		return dst;
	}
	
	public Mat getMatFrame()
	{
		byte[] frame = getImageNative();
		currentFrameTimestamp = System.currentTimeMillis();
		Mat mat = translateCameraArrayToMat(frame);
		return mat;
	}
	
	private native byte[] getImageNative();
	
	public native boolean close();

	@Override
	public BufferedImage getBufferedImage() {
		return Camera.matToBufferedImage(getMatFrame());
	}


	@Override
	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final Configuration config, final CameraView cameraView)
	{
		if (OptiTrackShotDetector.isSystemSupported())
			return new OptiTrackShotDetector(cameraManager, config, cameraView);
		else if (NativeShotDetector.isSystemSupported())
			return new NativeShotDetector(cameraManager, config, cameraView);
		else if (JavaShotDetector.isSystemSupported())
			return new JavaShotDetector(cameraManager, config, cameraView);
		else
			return null;
	}
	

	public static boolean initialized() {
		return initialized;
	}
	
	
	@Override
	public void run() {
	}
	
	private void receiveFrame(byte[] frame)
	{
		currentFrameTimestamp = System.currentTimeMillis();
		Mat mat = translateCameraArrayToMat(frame);
		if (cameraEventListener.isPresent())
		{
			Platform.runLater(() -> cameraEventListener.get().newFrame(mat));
		}
			
	}
	
	private void cameraClosed()
	{
		logger.debug("cameraClosed");
		if (cameraEventListener.isPresent())
			cameraEventListener.get().cameraClosed();
		close();
	}

	@Override
	public boolean isLocked() {
		return false;
	}
	
	@Override
	public native double getFPS();
	
	@Override
	public native int getFrameCount();

}
