/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff;

import java.io.IOException;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controllers.ShootOFFController;
import com.shootoff.plugins.TextToSpeech;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) throws IOException, ConfigurationException {
		String[] args = getParameters().getRaw().toArray(new String[getParameters().getRaw().size()]);
		Configuration config = new Configuration("shootoff.properties", args);
		
		// This initializes the TTS engine
		TextToSpeech.say("");
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/shootoff/gui/ShootOFF.fxml"));
	    loader.load();   
		
		Scene scene = new Scene(loader.getRoot());
		
		primaryStage.setTitle("ShootOFF");
		primaryStage.setScene(scene);
		((ShootOFFController)loader.getController()).init(config);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}