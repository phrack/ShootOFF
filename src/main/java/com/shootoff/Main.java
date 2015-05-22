/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff;

import java.io.IOException;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.ShootOFFController;
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