package com.shootoff.session;

public interface Event {
	public EventType getType();
	public long getTimestamp();
}