/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.camera;

import java.util.HashSet;
import java.util.Set;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class CamerasSupervisor {
	private final Configuration config;
	private final Set<CameraManager> managers = new HashSet<CameraManager>();
	
	public CamerasSupervisor(Configuration config) {
		this.config = config;
	}
	
	public void addCameraManager(Webcam webcam, CanvasManager canvasManager) {
		managers.add(new CameraManager(webcam, canvasManager, config));
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
}
