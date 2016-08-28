package com.shootoff.targets;

import java.util.List;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

public interface CameraViews {
	List<Target> getTargets();
	
	void addCameraView(String name, Pane pane);

	void removeCameraView(String name);
	
	CameraView getSelectedCameraView();
	
	CameraManager getSelectedCameraManager();
	
	Node getSelectedCameraContainer();
}
