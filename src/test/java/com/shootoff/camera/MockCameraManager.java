package com.shootoff.camera;

import java.util.Optional;

import javafx.geometry.Bounds;

import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.cameratypes.CameraEventListener;
import com.shootoff.gui.CanvasManager;

public class MockCameraManager extends CameraManager implements CameraEventListener {
	protected VideoFinishedListener videoFinishedListener = null;
	
	public MockCameraManager()
	{
		super();
	}
	
	protected MockCameraManager(MockCamera camera, CanvasManager canvas,
			boolean[][] sectorStatuses, Optional<Bounds> projectionBounds, VideoFinishedListener videoFinishedListener) {
		
		super(camera, null, canvas);

		this.cameraView.setCameraManager(this);

		setSectorStatuses(sectorStatuses);

		if (projectionBounds.isPresent()) {
			setLimitDetectProjection(true);
			setProjectionBounds(projectionBounds.get());
		}
		
		
		this.videoFinishedListener = videoFinishedListener;

	}

	public AutoCalibrationManager getACM()
	{
		return acm;
	}
	
	public void cameraClosed()
	{
		videoFinishedListener.videoFinished();
	}


}
