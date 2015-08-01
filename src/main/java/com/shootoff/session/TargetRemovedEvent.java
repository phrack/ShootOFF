package com.shootoff.session;

public class TargetRemovedEvent implements Event {
	private final long timestamp;
	private final int targetIndex;
	
	public TargetRemovedEvent(long timestamp, int targetIndex) {
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
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
}
