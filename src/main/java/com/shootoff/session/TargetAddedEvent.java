package com.shootoff.session;

import java.io.File;

public class TargetAddedEvent implements Event{
	private final String cameraName;
	private final long timestamp;
	private final String targetName;
	
	public TargetAddedEvent(String cameraName, long timestamp, String targetName) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetName = targetName;
	}
	
	@Override
	public String getCameraName() {
		return cameraName;
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
	
	@Override
	public String toString() {
		String target = targetName.substring(targetName.lastIndexOf(File.separator) + 1,
				targetName.lastIndexOf('.'));
		
		return String.format("target added (%s)", target);
	}
}
