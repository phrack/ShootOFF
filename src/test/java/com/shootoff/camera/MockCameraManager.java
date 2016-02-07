package com.shootoff.camera;

import com.shootoff.gui.MockCanvasManager;

public class MockCameraManager extends CameraManager {
	public MockCameraManager() {
		super(new MockCamera(), null, new MockCanvasManager(null), null);
	}
}
