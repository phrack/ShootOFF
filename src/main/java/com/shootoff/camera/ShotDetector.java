package com.shootoff.camera;

import org.opencv.core.Mat;

/**
 * This interface is implemented by classes that act as the entry point to
 * some implementation of shot detection.
 */
public interface ShotDetector {
	/**
	 * Process <code>frameBGR</code> to detect shots that appear in it.
	 * The frame is in blue, green, red format, which is the default
	 * used by OpenCV when it reads a frame off of a webcam. The behavior
	 * when <code>isDetecting</code> is <code>false</code> is dependent
	 * on the specific implementation of the shot detection algorithm.
	 * Some may perform no processing in this case, others may still
	 * update filters, collect diagnostic information (e.g. to show
	 * users where noise may occur), etc.
	 * 
	 * @param frameBGR the frame to process in search of a shot
	 * @param isDetecting <code>true</code> if the algorithm should
	 * perform the full detection process, otherwise stop after
	 * collecting diagnostic/filter information
	 */
	public void processFrame(Mat frameBGR, boolean isDetecting);
	
	/**
	 * Notify the shot detector of the dimensions of webcam frames
	 * (e.g. the webcam's resolution). This method may be called
	 * at any time if the webcam's resolution is changed at runtime.
	 *  
	 * @param width the width of frames in pixels
	 * @param height the height of frames in pixels
	 */
	public void setFrameSize(final int width, final int height);
}
