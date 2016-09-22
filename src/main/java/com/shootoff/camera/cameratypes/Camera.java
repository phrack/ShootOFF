/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

package com.shootoff.camera.cameratypes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import com.shootoff.Closeable;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;
import com.xuggle.xuggler.video.ConverterFactory;

public interface Camera extends Runnable, Closeable {
	public enum CameraState {
		CLOSED, NORMAL, DETECTING, CALIBRATING
	};

	Mat getMatFrame();

	BufferedImage getBufferedImage();

	boolean open();

	boolean isOpen();

	void close();

	String getName();

	// Return false if the state change is not allowed
	// for possible future use
	boolean setState(CameraState state);

	CameraState getState();

	void setCameraEventListener(CameraEventListener cameraEventListener);

	long getCurrentFrameTimestamp();

	int getFrameCount();

	ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final Configuration config,
			final CameraView cameraView);

	boolean isLocked();

	void setViewSize(Dimension size);

	Dimension getViewSize();

	boolean supportsExposureAdjustment();

	boolean decreaseExposure();

	void resetExposure();

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

	/**
	 * @return True if the camera limits the number of frames that are delivered
	 *         to CameraManager
	 */
	boolean limitsFrames();

}