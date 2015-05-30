/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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
