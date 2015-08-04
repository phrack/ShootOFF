package com.shootoff.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.shootoff.camera.Shot;
import com.shootoff.gui.Target;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

public class SessionRecorder {
	private final long startTime;
	private final Map<String, List<Event>> events = new HashMap<String, List<Event>>();
	private final Map<String, Set<Target>> seenTargets = new HashMap<String, Set<Target>>();
	
	private volatile boolean ignoreTargetCheck = false;
	
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
	
	// This method ensures we have an add event for a target that is being used,
	// if not the user started recording after adding the target so we should
	// artificially add the target add event then ensure it gets moved and resized
	// to wherever it already is and to however big it already is.
	private void checkTarget(String cameraName, Target target) {
		if (!seenTargets.containsKey(cameraName)) {
			seenTargets.put(cameraName, new HashSet<Target>());
		}
		
		if (!seenTargets.get(cameraName).contains(target)) {
			ignoreTargetCheck = true;
			
			recordTargetAdded(cameraName, target);
			Point2D p = target.getPosition();
			recordTargetMoved(cameraName, target, (int)p.getX(), (int)p.getY());
			Dimension2D d = target.getDimension();
			recordTargetResized(cameraName, target, d.getWidth(), d.getHeight());
			
			ignoreTargetCheck = false;
		}
	}
	
	public void recordShot(String cameraName, Shot shot, Optional<Target> target, Optional<Integer> hitRegionIndex) {
		Optional<Integer> targetIndex = Optional.empty();
		
		if (target.isPresent()) {
			targetIndex = Optional.of(target.get().getTargetIndex());
			if (!ignoreTargetCheck) checkTarget(cameraName, target.get());
		}
		
		getCameraEvents(cameraName).add(
				new ShotEvent(cameraName, System.currentTimeMillis() - startTime, shot, targetIndex, hitRegionIndex));
	}
	
	public void recordTargetAdded(String cameraName, Target target) {
		if (!seenTargets.containsKey(cameraName)) {
			seenTargets.put(cameraName, new HashSet<Target>());
		}
		
		seenTargets.get(cameraName).add(target);
		
		getCameraEvents(cameraName).add(
				new TargetAddedEvent(cameraName, System.currentTimeMillis() - startTime, target.getTargetFile().getName()));
	}
	
	public void recordTargetRemoved(String cameraName, Target target) {
		if (!ignoreTargetCheck) checkTarget(cameraName, target);
		
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
		if (!ignoreTargetCheck) checkTarget(cameraName, target);
		
		// Remove all resize events immediately before this one
		collapseTargetEvents(cameraName, EventType.TARGET_RESIZED, target);
		
		getCameraEvents(cameraName).add(
				new TargetResizedEvent(cameraName, System.currentTimeMillis() - startTime, 
						target.getTargetIndex(), newWidth, newHeight));
	}
	
	public void recordTargetMoved(String cameraName, Target target, int newX, int newY) {
		if (!ignoreTargetCheck) checkTarget(cameraName, target);
		
		// Remove all move events immediately before this one
		collapseTargetEvents(cameraName, EventType.TARGET_MOVED, target);
		
		getCameraEvents(cameraName).add(
				new TargetMovedEvent(cameraName, System.currentTimeMillis() - startTime, target.getTargetIndex(), newX, newY));
	}
	
	public Map<String, List<Event>> getEvents() {
		return events;
	}
}
