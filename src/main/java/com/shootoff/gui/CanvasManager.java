/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import com.shootoff.camera.Shot;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.config.Configuration;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;import javafx.scene.paint.Color;

public class CanvasManager {
	private final Group canvasGroup;
	private final Configuration config;
	private final ImageView background = new ImageView();
	private final List<Shot> shots = new ArrayList<Shot>();
	
	public CanvasManager(Group canvasGroup, Configuration config) {
		this.canvasGroup = canvasGroup;
		this.config = config;

		if (Platform.isFxApplicationThread()) {
			ProgressIndicator progress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
			progress.setPrefHeight(480);
			progress.setPrefWidth(640);
			canvasGroup.getChildren().add(progress);
		}
		
		// Click to shoot
		if (config.inDebugMode()) {
			canvasGroup.setOnMouseClicked((event) -> {
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
	}
	
	public void updateBackground(Image img) {
		if (!canvasGroup.getChildren().contains(background)) {
			Platform.runLater(() -> {
					canvasGroup.getChildren().clear();
					canvasGroup.getChildren().add(background);
				});
		}
		
		background.setImage(img);
	}
	
	public void reset() {
		Platform.runLater(() -> {
				for (Shot shot : shots) {
					canvasGroup.getChildren().remove(shot.getMarker());
				}
				
				shots.clear();
			}); 
	}
	
	public void addShot(Shot shot) {
		for (ShotProcessor processor : config.getShotProcessors()) {
			if (!processor.processShot(shot)) return;
		}
		
		shots.add(shot);
		shot.drawShot(canvasGroup);
	}
}
