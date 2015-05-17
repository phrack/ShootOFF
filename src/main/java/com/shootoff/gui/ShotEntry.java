package com.shootoff.gui;

import javafx.scene.paint.Color;
import com.shootoff.camera.Shot;

public class ShotEntry {
	private final Shot shot;
	private final String color;
	private final String timestamp;
	
	public ShotEntry(Shot shot) {
		this.shot = shot;
		
		if (shot.getColor().equals(Color.RED)) {
			color = "red";
		} else {
			color = "green";
		}
		
		timestamp = String.format("%.2f", ((float)shot.getTimestamp()) / (float)1000);
	}
	
	public String getColor() {
		return color;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public Shot getShot() {
		return shot;
	}
}
