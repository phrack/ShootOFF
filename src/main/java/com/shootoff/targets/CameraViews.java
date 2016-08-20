package com.shootoff.targets;

import java.util.List;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;

import javafx.scene.Node;

public interface CameraViews {
	List<Target> getTargets();
	
	CameraView getSelectedCameraView();
	
	CameraManager getSelectedCameraManager();
	
	Node getSelectedCameraContainer();
}
