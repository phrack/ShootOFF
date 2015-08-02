package com.shootoff.session;

import java.util.Optional;

import com.shootoff.camera.Shot;

public class ShotEvent implements Event {
	private final String cameraName;
	private final long timestamp;
	private final Shot shot;
	private final Optional<Integer> targetIndex;
	private final Optional<Integer> hitRegionIndex;
	
	public ShotEvent(String cameraName, long timestamp, Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex) {
		this.cameraName = cameraName;
		this.timestamp = timestamp; 
		this.shot = shot;
		this.targetIndex = targetIndex;
		this.hitRegionIndex = hitRegionIndex;
	}

	@Override
	public String getCameraName() {
		return cameraName;
	}
	
	public Shot getShot() {
		return shot;
	}
	
	public Optional<Integer> getTargetIndex() {
		return targetIndex;
	}
	
	public Optional<Integer> getHitRegionIndex() {
		return hitRegionIndex;
	}
	
	@Override
	public EventType getType() {
		return EventType.SHOT;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
}
