package com.shootoff.gui;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;

public class ShootOFFController {
	@FXML 
	private MenuBar mainMenu;
	
	@FXML 
	public void preferencesClicked(ActionEvent event) throws IOException {
		new PreferencesController().showPreferences(
				mainMenu.getScene().getWindow());
    }
}
