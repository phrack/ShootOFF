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

package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.processors.ShotProcessor;
import com.shootoff.config.Configuration;

public class CamerasSupervisor {
	private final Configuration config;
	private final List<CameraManager> managers = new ArrayList<CameraManager>();

	private final AtomicBoolean allDetecting = new AtomicBoolean(true);

	public CamerasSupervisor(Configuration config) {
		this.config = config;
	}

	public CameraManager addCameraManager(Camera cameraInterface, CameraErrorView cameraErrorView,
			CameraView cameraView) {
		final CameraManager manager = new CameraManager(cameraInterface, cameraErrorView, cameraView, config);
		managers.add(manager);
		allDetecting.set(true);
		manager.start();
		return manager;
	}

	public void clearManagers() {
		setStreamingAll(false);
		setDetectingAll(false);
		allDetecting.set(false);

		for (final CameraManager manager : managers) {
			manager.close();
		}

		managers.clear();
	}

	public void clearManager(CameraManager manager) {
		manager.close();
		managers.remove(manager);
	}

	public void clearShots() {
		for (final CameraManager manager : managers) {
			manager.clearShots();
		}
	}

	public void reset() {
		for (final CameraManager manager : managers) {
			manager.reset();
		}

		for (final ShotProcessor processor : config.getShotProcessors()) {
			processor.reset();
		}
	}

	public void setStreamingAll(final boolean isStreaming) {
		for (final CameraManager manager : managers) {
			manager.setStreaming(isStreaming);
		}
	}

	public void setDetectingAll(final boolean isDetecting) {
		allDetecting.set(isDetecting);

		for (final CameraManager manager : managers) {
			manager.setDetecting(isDetecting);
		}
	}

	public boolean areDetecting() {
		return allDetecting.get();
	}

	public void closeAll() {
		for (final CameraManager manager : managers) {
			manager.close();
		}

		allDetecting.set(false);
	}

	public List<CameraManager> getCameraManagers() {
		return managers;
	}

	public CameraManager getCameraManager(final int index) {
		return managers.get(index);
	}

	public CameraManager getCameraManager(final Camera camera) {
		for (final CameraManager manager : managers) {
			if (manager.getCamera() == camera) return manager;
		}
		return null;
	}

	public List<CameraView> getCameraViews() {
		final List<CameraView> cameraViews = new ArrayList<CameraView>();

		for (final CameraManager manager : managers) {
			cameraViews.add(manager.getCameraView());
		}

		return cameraViews;
	}

	public CameraView getCameraView(final int index) {
		return managers.get(index).getCameraView();
	}

}
