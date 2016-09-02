package com.shootoff.camera.cameratypes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;
import com.xuggle.xuggler.video.ConverterFactory;

public interface Camera extends Runnable {
	public enum CameraState { DISABLED, NORMAL, DETECTING, CALIBRATING };

	Mat getMatFrame();

	BufferedImage getBufferedImage();

	boolean open();

	boolean isOpen();

	boolean close();

	String getName();
	
	void setState(CameraState state);
	
	void setCameraEventListener(CameraEventListener cameraEventListener);
	
	long getCurrentFrameTimestamp();
	
	int getFrameCount();
	
	ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final Configuration config, final CameraView cameraView);
	
	boolean isLocked();

	void setViewSize(Dimension size);

	Dimension getViewSize();	

	static BufferedImage matToBufferedImage(Mat matBGR) {
		BufferedImage image = new BufferedImage(matBGR.width(), matBGR.height(), BufferedImage.TYPE_3BYTE_BGR);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		matBGR.get(0, 0, targetPixels);

		return image;
	}

	static Mat bufferedImageToMat(BufferedImage frame) {
		BufferedImage transformedFrame = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		byte[] pixels = ((DataBufferByte) transformedFrame.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(frame.getHeight(), frame.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);

		return mat;
	}

	double getFPS();

}