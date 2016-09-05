package com.shootoff.camera.cameratypes;

import java.util.Optional;

public abstract class CalculatedFPSCamera implements Camera {
	public static final int DEFAULT_FPS = 30;
	private double webcamFPS = DEFAULT_FPS;
	
	protected CameraState cameraState;
	
	protected int frameCount = 0;
	protected long currentFrameTimestamp = -1;
	private long lastCameraTimestamp = -1;
	private long lastFrameCount = 0;

	
	
	protected Optional<CameraEventListener> cameraEventListener = Optional.empty();
	public void setCameraEventListener(CameraEventListener cameraEventListener)
	{
		this.cameraEventListener = Optional.of(cameraEventListener);
	}

	public void setState(CameraState cameraState)
	{
		this.cameraState = cameraState;
	}
	

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}
	
	public int getFrameCount() {
		return frameCount;
	}
	public double getFPS()
	{
		return webcamFPS;
	}
	
	protected void setFPS(double newFPS) {
		// This just tells us if it's the first FPS estimate
		if (getFrameCount() > DEFAULT_FPS)
			webcamFPS = ((webcamFPS * 4.0) + newFPS) / 5.0;
		else
			webcamFPS = newFPS;
	}

	protected void estimateCameraFPS() {
		if (lastCameraTimestamp > -1) {
			double estimateFPS = ((double) getFrameCount() - (double) lastFrameCount)
					/ (((double) System.currentTimeMillis() - (double) lastCameraTimestamp) / 1000.0);

			setFPS(estimateFPS);
			
			if (cameraEventListener.isPresent())
				cameraEventListener.get().newFPS(webcamFPS);
		}

		lastCameraTimestamp = System.currentTimeMillis();
		lastFrameCount = getFrameCount();



	}
	

	public int hashCode() {
		final int prime = 31;
		int result = this.getName().hashCode();
		result = prime * result;
		return result;
	}

	
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Camera other = (Camera) obj;
		if (!this.getName().equals(other.getName())) return false;
		return true;
	}

}
