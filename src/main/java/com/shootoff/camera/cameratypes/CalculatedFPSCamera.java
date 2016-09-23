/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

	public void setCameraEventListener(CameraEventListener cameraEventListener) {
		this.cameraEventListener = Optional.ofNullable(cameraEventListener);
	}

	public boolean setState(CameraState cameraState) {

		switch (cameraState) {
		case CLOSED:
			if (this.cameraState != CameraState.CLOSED) {
				this.cameraState = cameraState;
				close();
			}
			break;
		case CALIBRATING:
			resetExposure();
		default:
			this.cameraState = cameraState;
			break;
		}

		return true;
	}

	public CameraState getState() {
		return cameraState;
	}

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public double getFPS() {
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

			if (cameraEventListener.isPresent()) cameraEventListener.get().newFPS(webcamFPS);
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
