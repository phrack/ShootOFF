package com.shootoff.targets;

import java.util.List;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.ShotEntry;

import javafx.collections.ObservableList;
import javafx.scene.Node;

public interface CameraViews {
	List<Target> getTargets();
	
	void addCameraView(String name, Node content, CanvasManager canvasManager, boolean select);

	void removeCameraView(String name);
	
	CameraView getSelectedCameraView();
	
	CameraManager getSelectedCameraManager();
	
	Node getSelectedCameraContainer();
	
	void selectCameraView(CameraView cameraView);
	
	ObservableList<ShotEntry> getShotTimerModel();
}
