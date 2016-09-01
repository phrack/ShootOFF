package com.shootoff.camera;

import com.shootoff.camera.cameratypes.WebcamCaptureCamera;

public class MockCamera extends WebcamCaptureCamera {
	public MockCamera() {
		super();
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isImageNew() {
		return false;
	}
	
	@Override
	public String getName()
	{
		return "MockCamera";
	}
}
