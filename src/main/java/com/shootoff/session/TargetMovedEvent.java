package com.shootoff.session;

public class TargetMovedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final int targetIndex;
	private final int newX;
	private final int newY;
	
	public TargetMovedEvent(String cameraName, long timestamp, int targetIndex, int newX, int newY) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
		this.newX = newX;
		this.newY = newY;
	}
	
	@Override
	public String getCameraName() {
		return cameraName;
	}
	
	public int getTargetIndex() {
		return targetIndex;
	}
	
	public int getNewX() {
		return newX;
	}
	
	public int getNewY() {
		return newY;
	}
	
	@Override
	public EventType getType() {
		return EventType.TARGET_MOVED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
		return String.format("target moved (%d, %d)", newX, newY);
	}
}
