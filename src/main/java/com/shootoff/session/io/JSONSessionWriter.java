package com.shootoff.session.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.shootoff.camera.Shot;

public class JSONSessionWriter implements EventVisitor {
	private final File sessionFile;
	private final JSONArray cameras = new JSONArray();
	private JSONObject currentCamera;
	private JSONArray currentCameraEvents;
	
	public JSONSessionWriter(File sessionFile) {
		this.sessionFile = sessionFile;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitCamera(String cameraName) {
		currentCamera = new JSONObject();
		currentCamera.put("name", cameraName);
		
		currentCameraEvents = new JSONArray();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitCameraEnd() {
		currentCamera.put("events", currentCameraEvents);
		cameras.add(currentCamera);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitShot(long timestamp, Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex) {
		JSONObject event = new JSONObject();
		event.put("type", "shot");
		event.put("timestamp", timestamp);
		event.put("color", shot.getColor().toString());
		event.put("x", shot.getX());
		event.put("y", shot.getY());
		event.put("shotTimestamp", shot.getTimestamp());		
		event.put("markerRadius", (int)shot.getMarker().getRadiusX());
		
		if (targetIndex.isPresent()) {
			event.put("targetIndex", targetIndex.get());
		} else {
			event.put("targetIndex", -1);
		}
		
		if (hitRegionIndex.isPresent()) {
			event.put("hitRegionIndex", hitRegionIndex.get());
		} else {
			event.put("hitRegionIndex", -1);
		}
		
		currentCameraEvents.add(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitTargetAdd(long timestamp, String targetName) {
		JSONObject event = new JSONObject();
		event.put("type", "targetAdded");
		event.put("timestamp", timestamp);
		event.put("name", targetName);
		
		currentCameraEvents.add(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitTargetRemove(long timestamp, int targetIndex) {
		JSONObject event = new JSONObject();
		event.put("type", "targetRemoved");
		event.put("timestamp", timestamp);
		event.put("index", targetIndex);
		
		currentCameraEvents.add(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitTargetResize(long timestamp, int targetIndex, double newWidth, double newHeight) {	
		JSONObject event = new JSONObject();
		event.put("type", "targetResized");
		event.put("timestamp", timestamp);
		event.put("index", targetIndex);
		event.put("newWidth", newWidth);
		event.put("newHeight", newHeight);
		
		currentCameraEvents.add(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitTargetMove(long timestamp, int targetIndex, int newX, int newY) {
		JSONObject event = new JSONObject();
		event.put("type", "targetMoved");
		event.put("timestamp", timestamp);
		event.put("index", targetIndex);
		event.put("newX", newX);
		event.put("newY", newY);
		
		currentCameraEvents.add(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitProtocolFeedMessage(long timestamp, String message) {
		JSONObject event = new JSONObject();
		event.put("type", "protocolFeedMessage");
		event.put("timestamp", timestamp);
		event.put("message", message);
		
		currentCameraEvents.add(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visitEnd() {
		JSONObject session = new JSONObject();
		session.put("cameras", cameras);
		
		FileWriter file = null;
		
		try {
			file = new FileWriter(sessionFile);
			file.write(session.toJSONString());
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (file != null) file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
					
		}
	}
}
