package com.shootoff.gui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ProjectorArenaController {
	@FXML private Parent arenaPanel;
	@FXML private Canvas arenaCanvas;
	
	public void toggleArena() throws IOException {
		Parent arenaParent = FXMLLoader.load(getClass().getClassLoader().getResource("com/shootoff/gui/ProjectorArena.fxml"));

		Stage arenaStage = new Stage();
		
        arenaStage.setTitle("ProjectorArena");
        arenaStage.setScene(new Scene(arenaParent));
        arenaStage.show();
	}
	
	@FXML
	public void canvasKeyPressed(KeyEvent event) {
		Stage arenaStage = (Stage)arenaPanel.getScene().getWindow();
		
		if (event.getCode() == KeyCode.F11) {
			arenaStage.setFullScreen(!arenaStage.isFullScreen());
		}
	}
	
	@FXML 
	public void canvasMouseEntered(MouseEvent event) throws IOException {
		arenaCanvas.requestFocus();
    }
}
