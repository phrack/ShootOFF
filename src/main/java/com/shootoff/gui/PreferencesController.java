/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class PreferencesController {
	public void showPreferences(Window parent) throws IOException {
		Parent preferencesParent = FXMLLoader.load(getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));

		Stage preferencesStage = new Stage();
		
		preferencesStage.initOwner(parent);
		preferencesStage.initModality(Modality.WINDOW_MODAL);
        preferencesStage.setTitle("Preferences");
        preferencesStage.setScene(new Scene(preferencesParent));
        preferencesStage.show();
	}
}
