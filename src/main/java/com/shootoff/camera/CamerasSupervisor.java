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

import javafx.scene.Group;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class CamerasSupervisor {
	private final Configuration config;
	private final List<CameraManager> managers = new ArrayList<CameraManager>();

	private volatile boolean allDetecting = true;

	public CamerasSupervisor(Configuration config) {
		this.config = config;
	}

	public CameraManager addCameraManager(Camera webcam, CameraErrorView cameraErrorView, CanvasManager canvasManager) {
		final CameraManager manager = new CameraManager(webcam, cameraErrorView, canvasManager, config);
		managers.add(manager);
		allDetecting = true;
		return manager;
	}

	public void clearManagers() {
		setStreamingAll(false);
		setDetectingAll(false);
		allDetecting = false;

		for (CameraManager manager : managers) {
			manager.close();
		}

		managers.clear();
	}

	public void clearShots() {
		for (CameraManager manager : managers) {
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
		allDetecting = isDetecting;

		for (final CameraManager manager : managers) {
			manager.setDetecting(isDetecting);
		}
	}

	public boolean areDetecting() {
		return allDetecting;
	}

	public void closeAll() {
		for (final CameraManager manager : managers) {
			manager.close();
		}

		allDetecting = false;
	}

	public List<CameraManager> getCameraManagers() {
		return managers;
	}

	public CameraManager getCameraManager(final int index) {
		return managers.get(index);
	}

	public List<CanvasManager> getCanvasManagers() {
		final List<CanvasManager> canvasManagers = new ArrayList<CanvasManager>();

		for (final CameraManager manager : managers) {
			canvasManagers.add(manager.getCanvasManager());
		}

		return canvasManagers;
	}

	public CanvasManager getCanvasManager(final int index) {
		return managers.get(index).getCanvasManager();
	}

	public List<Group> getTargets() {
		final List<Group> targets = new ArrayList<Group>();

		for (final CameraManager manager : managers) {
			targets.addAll(manager.getCanvasManager().getTargetGroups());
		}

		return targets;
	}
}
