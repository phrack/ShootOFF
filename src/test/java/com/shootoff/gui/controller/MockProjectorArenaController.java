package com.shootoff.gui.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;

import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.pane.ProjectorArenaPane;

public class MockProjectorArenaController extends ProjectorArenaPane {
	// Used for testing
	public MockProjectorArenaController(Configuration config, CanvasManager canvasManager) {
		super(config, canvasManager);

		arenaStage = new Stage();
		Scene scene = new Scene(this, 500, 500);
		arenaStage.setScene(scene);

	}

}
