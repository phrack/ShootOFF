package com.shootoff.session;

public class ProtocolFeedMessageEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final String message;
	
	public ProtocolFeedMessageEvent(String cameraName, long timestamp, String message) {
		this.cameraName = cameraName;
		this.timestamp = timestamp;
		this.message = message;
	}

	@Override
	public EventType getType() {
		return EventType.PROTOCOL_FEED_MESSAGE;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}
	
	@Override
	public String toString() {
		return "training protocol feed message";
	}
}
