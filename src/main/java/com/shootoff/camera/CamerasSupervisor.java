/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.camera;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class CamerasSupervisor {
	private final Configuration config;
	private final List<CameraManager> managers = new ArrayList<CameraManager>();
	
	public CamerasSupervisor(Configuration config) {
		this.config = config;
	}
	
	public void addCameraManager(Webcam webcam, CanvasManager canvasManager) {
		managers.add(new CameraManager(webcam, canvasManager, config));
	}
	
	public void clearManagers() {
		setStreamingAll(false);
		setDetectingAll(false);
		managers.clear();
	}

	public void clearShots() {
		for (CameraManager manager : managers) {
			manager.clearShots();
		}
	}
	
	public void reset() {
		for (CameraManager manager : managers) {
			manager.reset();
		}
	}
	
	public void setStreamingAll(boolean isStreaming) {
		for (CameraManager manager : managers) {
			manager.setStreaming(isStreaming);
		}
	}
	
	public void setDetectingAll(boolean isDetecting) {
		for (CameraManager manager : managers) {
			manager.setDetecting(isDetecting);
		}
	}
	
	public List<CameraManager> getCameraManagers() {
		return managers;
	}
	
	public CameraManager getCameraManager(int index) {
		return managers.get(index);
	}
	
	public List<CanvasManager> getCanvasManagers() {
		List<CanvasManager> canvasManagers = new ArrayList<CanvasManager>();
		
		for (CameraManager manager : managers) {
			canvasManagers.add(manager.getCanvasManager());
		}
		
		return canvasManagers;
	}
	
	public CanvasManager getCanvasManager(int index) {
		return managers.get(index).getCanvasManager();
	}
	
	public List<Group> getTargets() {
		List<Group> targets = new ArrayList<Group>();
		
		for (CameraManager manager : managers) {
			targets.addAll(manager.getCanvasManager().getTargets());
		}
		
		return targets;
	}
}
