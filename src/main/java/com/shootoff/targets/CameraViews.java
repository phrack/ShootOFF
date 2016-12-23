package com.shootoff.targets;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.ShotEntry;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public interface CameraViews {
	/**
	 * Add a view to the same GUI widget used to display camera views, but for a
	 * non-camera view (i.e. an arena tab).
	 * 
	 * @param name
	 *            the name of the view (i.e. what will appear in the tab for the
	 *            view)
	 * @param content
	 *            a pane containing the contents of the view
	 * @param canvasManager
	 *            the canvas manager for the view
	 * @param select
	 *            <code>true</code> if this view should be selected (shown) as
	 *            soon as it is added
	 * @param maximizeView
	 *            <code>true</code> if view should maximize within the camera
	 *            view widget
	 */
	void addNonCameraView(String name, Pane content, CanvasManager canvasManager, boolean select, boolean maximizeView);

	void removeCameraView(String name);

	boolean isArenaViewSelected();

	CameraView getSelectedCameraView();

	CameraManager getSelectedCameraManager();

	Node getSelectedCameraContainer();

	void selectCameraView(CameraView cameraView);

	ObservableList<ShotEntry> getShotTimerModel();
}
