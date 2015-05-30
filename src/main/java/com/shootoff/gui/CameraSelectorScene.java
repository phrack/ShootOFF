/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import com.github.sarxos.webcam.Webcam;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

public class CameraSelectorScene extends Stage {
	private final List<Webcam> unconfiguredWebcams = new ArrayList<Webcam>();
	private final List<Webcam> selectedWebcams = new ArrayList<Webcam>();
	private final ListView<String> webcamListView = new ListView<String>();
	
	public CameraSelectorScene(Window parent, List<Webcam> configuredCameras) {
		super();
		this.initOwner(parent);
		this.initModality(Modality.WINDOW_MODAL);
		
		ObservableList<String> webcams = FXCollections.observableArrayList();
		
		for (Webcam webcam : Webcam.getWebcams()) {
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
		pane.setCenter(webcamListView);
		
		Scene scene = new Scene(pane);
		this.setScene(scene);
		this.show();
	}
	
	private void addSelection() {
		ObservableList<String> selectedNames = 
				webcamListView.getSelectionModel().getSelectedItems();
		
		if (selectedNames.isEmpty()) return;
		
        for (Webcam webcam : unconfiguredWebcams) {
        	if (selectedNames.contains(webcam.getName())) {
        		selectedWebcams.add(webcam);
        	}
        }
        
        this.close();
	}
	
	public List<Webcam> getSelectedWebcams() {
		return selectedWebcams;
	}
}
