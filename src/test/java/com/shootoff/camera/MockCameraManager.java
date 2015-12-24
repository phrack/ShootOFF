package com.shootoff.camera;

import com.shootoff.gui.MockCanvasManager;

public class MockCameraManager extends CameraManager {

	public MockCameraManager() {		
		super(new MockCamera(), new MockCanvasManager(null), null);
	}

}
