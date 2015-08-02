package com.shootoff.session;

public class TargetResizedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final int targetIndex;
	private final int newWidth;
	private final int newHeight;
	
	public TargetResizedEvent(String cameraName, long timestamp, int targetIndex, int newWidth, int newHeight) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.targetIndex = targetIndex;
		this.newWidth = newWidth;
		this.newHeight = newHeight;
	}
	
	@Override
	public String getCameraName() {
		return cameraName;
	}
	
	public int getTargetIndex() {
		return targetIndex;
	}
	
	public int getNewWidth() {
		return newWidth;
	}
	
	public int getNewHeight() {
		return newHeight;
	}
	
	@Override
	public EventType getType() {
		return EventType.TARGET_RESIZED;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
}
