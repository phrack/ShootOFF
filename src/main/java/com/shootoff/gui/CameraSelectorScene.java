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

package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import com.shootoff.camera.Camera;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

public class CameraSelectorScene extends Stage {
	private final List<Camera> unconfiguredWebcams = new ArrayList<Camera>();
	private final List<Camera> selectedWebcams = new ArrayList<Camera>();
	private final ListView<String> webcamListView = new ListView<String>();
	
	public CameraSelectorScene(Window parent, List<Camera> configuredCameras) {
		super();
		this.initOwner(parent);
		this.initModality(Modality.WINDOW_MODAL);
		
		ObservableList<String> webcams = FXCollections.observableArrayList();
		
		for (Camera webcam : Camera.getWebcams()) {
			if (!configuredCameras.contains(webcam)) {
				unconfiguredWebcams.add(webcam);
				Platform.runLater(() -> { webcams.add(webcam.getName()); });
			}
		}
		
		BorderPane pane = new BorderPane();
		
	    webcamListView.setCellFactory(new Callback<ListView<String>, 
	            ListCell<String>>() {
	                @Override 
	                public ListCell<String> call(ListView<String> list) {
	                    return new ImageCell(unconfiguredWebcams, null);
	                }
	            }
	        );

	    webcamListView.setOnMouseClicked((event) -> {
	    		if (event.getClickCount() == 2) {
	    			addSelection();
	    		}
	    	});
	    
	    webcamListView.setOnKeyPressed((event) -> {
	    		if (event.getCode() == KeyCode.ENTER) {
	    			addSelection();
	    		}
	    	});
	    
	    webcamListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		webcamListView.setItems(webcams);
		
		pane.setTop(webcamListView);
		
		final Button okButton = new Button("OK");
		okButton.setDefaultButton(true);
		okButton.setOnAction((event) -> {
				addSelection();
			});
		
		final Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction((event) -> {
				this.close();
			});
		
		HBox h = new HBox(okButton, cancelButton);
		h.setSpacing(10);
		pane.setRight(h);
		
		Scene scene = new Scene(pane);
		this.setScene(scene);
		this.show();
	}
	
	private void addSelection() {
		final ObservableList<String> selectedNames = 
				webcamListView.getSelectionModel().getSelectedItems();
		
		if (selectedNames.isEmpty()) return;
		
        for (Camera webcam : unconfiguredWebcams) {
        	if (selectedNames.contains(webcam.getName())) {
        		selectedWebcams.add(webcam);
        	}
        }
        
        this.close();
	}
	
	public List<Camera> getSelectedWebcams() {
		return selectedWebcams;
	}
}
