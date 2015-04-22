package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import com.shootoff.camera.Shot;

import javafx.scene.canvas.Canvas;

public class MockCanvasManager extends CanvasManager {
	private final List<Shot> shots = new ArrayList<Shot>();
	
	public MockCanvasManager() {
		super(new Canvas());
	}

	@Override
	public void addShot(Shot shot) {
		shots.add(shot);
	}
	
	public List<Shot> getShots() {
		return shots;
	}
}
