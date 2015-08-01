package com.shootoff.session;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import com.shootoff.camera.Shot;

public class SessionRecorder {
	private final long startTime;
	private final List<Event> events = new ArrayList<Event>();
	
	public SessionRecorder() {
		startTime = System.currentTimeMillis();
	}
	
	public void addEvents(List<Event> events) {
		this.events.addAll(events);
	}
	
	public void recordShot(Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex) {
		events.add(new ShotEvent(System.currentTimeMillis() - startTime, shot, targetIndex, hitRegionIndex));
	}
	
	public void recordTargetAdded(String targetName) {
		events.add(new TargetAddedEvent(System.currentTimeMillis() - startTime, targetName));
	}
	
	public void recordTargetRemoved(int targetIndex) {
		events.add(new TargetRemovedEvent(System.currentTimeMillis() - startTime, targetIndex));
	}

	private void collapseTargetEvents(EventType type, int targetIndex) {
		ListIterator<Event> it = events.listIterator(events.size());
		
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
	
	public void recordTargetResized(int targetIndex, int newWidth, int newHeight) {
		// Remove all resize events immediately before this one
		collapseTargetEvents(EventType.TARGET_RESIZED, targetIndex);
		
		events.add(new TargetResizedEvent(System.currentTimeMillis() - startTime, targetIndex, newWidth, newHeight));
	}
	
	public void recordTargetMoved(int targetIndex, int newX, int newY) {
		// Remove all move events immediately before this one
		collapseTargetEvents(EventType.TARGET_MOVED, targetIndex);
		
		events.add(new TargetMovedEvent(System.currentTimeMillis() - startTime, targetIndex, newX, newY));
	}
	
	public List<Event> getEvents() {
		return events;
	}
}
