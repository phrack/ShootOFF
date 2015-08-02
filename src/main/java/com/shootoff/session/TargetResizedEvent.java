package com.shootoff.session;

public class TargetResizedEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final int targetIndex;
	private final double newWidth;
	private final double newHeight;
	
	public TargetResizedEvent(String cameraName, long timestamp, int targetIndex, double newWidth, double newHeight) {
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
	
	public double getNewWidth() {
		return newWidth;
	}
	
	public double getNewHeight() {
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
	
	@Override
	public String toString() {
		return String.format("target resized (%.2f, %.2f)", newWidth, newHeight);
	}
}
