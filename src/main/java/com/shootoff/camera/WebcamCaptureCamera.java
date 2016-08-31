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

package com.shootoff.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import com.github.sarxos.webcam.Webcam;

public class WebcamCaptureCamera extends Camera {
	
	private int cameraIndex = -1;
	private final VideoCapture camera;

	// For testing
	protected WebcamCaptureCamera() {
		camera = null;
	}

	WebcamCaptureCamera(final String cameraName) {
		final List<Webcam> webcams = Webcam.getWebcams();
		int cameraIndex = -1;

		for (int i = 0; i < webcams.size(); i++) {
			if (webcams.get(i).getName().equals(cameraName)) {
				cameraIndex = i;
				break;
			}
		}

		if (cameraIndex < 0) throw new IllegalArgumentException("Camera not found: " + cameraName);

		camera = new VideoCapture();
		this.cameraIndex = cameraIndex;

	}


	@Override
	public Mat getFrame() {
		final Mat frame = new Mat();
		if (!camera.read(frame) || frame.size().height == 0) return null;

		return frame;
	}


	@Override
	public BufferedImage getImage() {
		Mat frame = getFrame();

		if (frame == null) {
			return null;
		} else {
			return Camera.matToBufferedImage(getFrame());
		}
	}


	
	@Override
	public boolean open() {
		boolean open;

		open = camera.open(cameraIndex);
		// Set the max FPS to 60. If we don't set this it defaults
		// to 30, which unnecessarily hampers higher end cameras
		camera.set(5, 60);
		
		if (open) Camera.openCameras.add(this);

		return open;
	}

	
	@Override
	public boolean isOpen() {
		return camera.isOpened();
	}


	@Override
	public boolean close() {
		camera.release();
		Camera.openCameras.remove(this);
		return true;
	}


	@Override
	public String getName() {
		return Webcam.getWebcams().get(cameraIndex).getName();
	}


	@Override
	public boolean isLocked() {
		return isOpen();
	}


	@Override
	public boolean isImageNew() {
		return true;
	}


	@Override
	public void setViewSize(final Dimension size) {
			camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, size.getWidth());
			camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, size.getHeight());
	}
	

	@Override
	public Dimension getViewSize() {
			return new Dimension((int) camera.get(Highgui.CV_CAP_PROP_FRAME_WIDTH),
					(int) camera.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT));
	}


	public void launchCameraSettings() {
		camera.set(Highgui.CV_CAP_PROP_SETTINGS, 1);
	}
}
