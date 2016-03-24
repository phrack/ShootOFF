package com.shootoff.gui.controller;

import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;

public class MockProjectorArenaController extends ProjectorArenaController {

	// Used for testing
	public void init(Configuration config, CanvasManager canvasManager) {
		this.config = config;
		this.canvasManager = canvasManager;

		arenaStage = new Stage();
		arenaAnchor = new AnchorPane(canvasManager.getCanvasGroup());
		Scene scene = new Scene(arenaAnchor, 500, 500);
		arenaStage.setScene(scene);

	}

}
