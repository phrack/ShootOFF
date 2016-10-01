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

package com.shootoff.camera.shotdetection;

import org.opencv.core.Mat;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;

public abstract class FrameProcessingShotDetector extends ShotDetector {

	public FrameProcessingShotDetector(CameraManager cameraManager, CameraView cameraView) {
		super(cameraManager, cameraView);
	}

	/**
	 * Process <code>frameBGR</code> to detect shots that appear in it. The
	 * frame is in blue, green, red format, which is the default used by OpenCV
	 * when it reads a frame off of a webcam. The behavior when
	 * <code>isDetecting</code> is <code>false</code> is dependent on the
	 * specific implementation of the shot detection algorithm. Some may perform
	 * no processing in this case, others may still update filters, collect
	 * diagnostic information (e.g. to show users where noise may occur), etc.
	 * 
	 * @param frameBGR
	 *            the frame to process in search of a shot
	 * @param isDetecting
	 *            <code>true</code> if the algorithm should perform the full
	 *            detection process, otherwise stop after collecting
	 *            diagnostic/filter information
	 */
	public abstract void processFrame(Mat frameBGR, boolean isDetecting);

}