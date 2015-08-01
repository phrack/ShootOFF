package com.shootoff.session;

public class TargetMovedEvent implements Event {
	private final long timestamp;
	private final int targetIndex;
	private final int newX;
	private final int newY;
	
	public TargetMovedEvent(long timestamp, int targetIndex, int newX, int newY) {
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
		this.newX = newX;
		this.newY = newY;
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
}
