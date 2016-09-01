package com.shootoff.camera.shotdetection;

import org.opencv.core.Mat;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.config.Configuration;

public abstract class FrameProcessingShotDetector extends ShotDetector {

	public FrameProcessingShotDetector(CameraManager cameraManager, Configuration config, CameraView cameraView) {
		super(cameraManager, config, cameraView);
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