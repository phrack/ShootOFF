package com.shootoff.targets;

import java.util.List;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;

public interface TargetRepository {
	List<Target> getTargets();
	
	CameraView getSelectedCameraView();
	
	CameraManager getSelectedCameraManager();
}
