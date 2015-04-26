/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;


import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;import javafx.scene.paint.Color;


public class CanvasManager {
	private final Canvas canvas;
	private final GraphicsContext gc;
	private final List<Shot> shots = new ArrayList<Shot>();
	
	public CanvasManager(Canvas canvas, Configuration config) {
		this.canvas = canvas;
		this.gc = canvas.getGraphicsContext2D();
		
		// Click to shoot
		canvas.setOnMouseClicked((event) -> {
				if (event.getButton() == MouseButton.PRIMARY) {
					if (event.isShiftDown()) {
						addShot(new Shot(Color.RED, event.getX(),
								event.getY(), 0, config.getMarkerRadius()));
					} else if (event.isControlDown()) {
						addShot(new Shot(Color.GREEN, event.getX(),
								event.getY(), 0, config.getMarkerRadius()));
					}
				}
			});
	}
	
	public void updateBackground(Image img) {
		canvas.getGraphicsContext2D().drawImage(img, 0, 0);
		drawShots();
	}
	
	public void addShot(Shot shot) {
		shots.add(shot);
		shot.drawShot(gc);
	}
	
	private void drawShots() {
		for (Shot shot : shots) {
			shot.drawShot(gc);
		}
	}
}
