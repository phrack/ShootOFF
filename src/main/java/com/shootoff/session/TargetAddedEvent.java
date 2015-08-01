package com.shootoff.session;

public class TargetAddedEvent implements Event{
	private final long timestamp;
	private final String targetName;
	
	public TargetAddedEvent(long timestamp, String targetName) {
		this.timestamp = timestamp;
		this.targetName = targetName;
	}
	
	public String getTargetName() {
		return targetName;
	}
	
	@Override
	public EventType getType() {
		return EventType.TARGET_ADDED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
}
