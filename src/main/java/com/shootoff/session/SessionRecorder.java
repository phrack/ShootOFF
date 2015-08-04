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
	
	public void recordShot(String cameraName, Shot shot, Optional<Target> target, Optional<Integer> hitRegionIndex) {
		Optional<Integer> targetIndex = Optional.empty();
		
		if (target.isPresent()) targetIndex = Optional.of(target.get().getTargetIndex());
		
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

	private void collapseTargetEvents(String cameraName, EventType type, Target target) {
		ListIterator<Event> it = getCameraEvents(cameraName).listIterator(getCameraEvents(cameraName).size());
		
		while (it.hasPrevious()) {
			Event e = it.previous();
			
			if (e.getType() != EventType.TARGET_RESIZED && e.getType() != EventType.TARGET_MOVED) {
				break;
			}
			
			if (e.getType() == type) {
				if (type == EventType.TARGET_RESIZED && 
						((TargetResizedEvent)e).getTargetIndex() == target.getTargetIndex()) {
					it.remove();
				} else if (type == EventType.TARGET_MOVED && 
						((TargetMovedEvent)e).getTargetIndex() == target.getTargetIndex()) {
					it.remove();
				}
			}
		}
	}
	
	public void recordTargetResized(String cameraName, Target target, double newWidth, double newHeight) {
		// Remove all resize events immediately before this one
		collapseTargetEvents(cameraName, EventType.TARGET_RESIZED, target);
		
		getCameraEvents(cameraName).add(
				new TargetResizedEvent(cameraName, System.currentTimeMillis() - startTime, 
						target.getTargetIndex(), newWidth, newHeight));
	}
	
	public void recordTargetMoved(String cameraName, Target target, int newX, int newY) {
		// Remove all move events immediately before this one
		collapseTargetEvents(cameraName, EventType.TARGET_MOVED, target);
		
		getCameraEvents(cameraName).add(
				new TargetMovedEvent(cameraName, System.currentTimeMillis() - startTime, target.getTargetIndex(), newX, newY));
	}
	
	public Map<String, List<Event>> getEvents() {
		return events;
	}
}
