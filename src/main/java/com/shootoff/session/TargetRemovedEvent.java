package com.shootoff.session;

public class TargetRemovedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final int targetIndex;
	
	public TargetRemovedEvent(String cameraName, long timestamp, int targetIndex) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
	}
	
	@Override
	public String getCameraName() {
		return cameraName;
	}
	
	public int getTargetIndex() {
		return targetIndex;
	}
	
	@Override
	public EventType getType() {
		return EventType.TARGET_REMOVED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
		return "target removed";
	}
}
