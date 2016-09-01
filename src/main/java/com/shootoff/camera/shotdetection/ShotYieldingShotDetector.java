package com.shootoff.camera.shotdetection;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.config.Configuration;

public abstract class ShotYieldingShotDetector extends ShotDetector {

	public ShotYieldingShotDetector(CameraManager cameraManager, Configuration config, CameraView cameraView) {
		super(cameraManager, config, cameraView);
	}
	
	public abstract void startDetecting();
	public abstract void stopDetecting();

}
