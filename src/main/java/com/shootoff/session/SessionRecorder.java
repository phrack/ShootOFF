package com.shootoff.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.gui.Target;

public class SessionRecorder {
	private final long startTime;
	private final Map<String, List<Event>> events = new HashMap<String, List<Event>>();
	
	public SessionRecorder() {
		startTime = System.currentTimeMillis();
	}
	
	public void addEvents(Map<String, List<Event>> events) {
		for (String cameraName : events.keySet()) {
			this.events.put(cameraName, events.get(cameraName));
		}
	}
	
	public List<Event> getCameraEvents(String cameraName) {
		if (events.containsKey(cameraName)) {
			return events.get(cameraName);
		} else {
			List<Event> eventList = new ArrayList<Event>();
			events.put(cameraName, eventList);
			return eventList;
		}
	}
	
	public void recordShot(String cameraName, Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex) {
		getCameraEvents(cameraName).add(
				new ShotEvent(cameraName, System.currentTimeMillis() - startTime, shot, targetIndex, hitRegionIndex));
	}
	
	public void recordTargetAdded(String cameraName, Target target) {
		getCameraEvents(cameraName).add(
				new TargetAddedEvent(cameraName, System.currentTimeMillis() - startTime, target.getTargetFile().getName()));
	}
	
	public void recordTargetRemoved(String cameraName, Target target) {
		getCameraEvents(cameraName).add(
				new TargetRemovedEvent(cameraName, System.currentTimeMillis() - startTime, target.getTargetIndex()));
	}

	private void collapseTargetEvents(String cameraName, EventType type, int targetIndex) {
		ListIterator<Event> it = getCameraEvents(cameraName).listIterator(getCameraEvents(cameraName).size());
		
		while (it.hasPrevious()) {
			Event e = it.previous();
			
			if (e.getType() != EventType.TARGET_RESIZED && e.getType() != EventType.TARGET_MOVED) {
				break;
			}
			
			// TODO: Target indexes must be the same
			if (e.getType() == type) {
				if (type == EventType.TARGET_RESIZED && 
						((TargetResizedEvent)e).getTargetIndex() == targetIndex) {
					it.remove();
				} else if (type == EventType.TARGET_MOVED && 
						((TargetMovedEvent)e).getTargetIndex() == targetIndex) {
					it.remove();
				}
			}
		}
	}
	
	public void recordTargetResized(String cameraName, int targetIndex, double newWidth, double newHeight) {
		// Remove all resize events immediately before this one
		collapseTargetEvents(cameraName, EventType.TARGET_RESIZED, targetIndex);
		
		getCameraEvents(cameraName).add(
				new TargetResizedEvent(cameraName, System.currentTimeMillis() - startTime, targetIndex, newWidth, newHeight));
	}
	
	public void recordTargetMoved(String cameraName, int targetIndex, int newX, int newY) {
		// Remove all move events immediately before this one
		collapseTargetEvents(cameraName, EventType.TARGET_MOVED, targetIndex);
		
		getCameraEvents(cameraName).add(
				new TargetMovedEvent(cameraName, System.currentTimeMillis() - startTime, targetIndex, newX, newY));
	}
	
	public Map<String, List<Event>> getEvents() {
		return events;
	}
}
