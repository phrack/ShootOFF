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
		ListView<String> webcamListView = new ListView<String>();
		
	    webcamListView.setCellFactory(new Callback<ListView<String>, 
	            ListCell<String>>() {
	                @Override 
	                public ListCell<String> call(ListView<String> list) {
	                    return new ImageCell(unconfiguredWebcams, null);
	                }
	            }
	        );
		
	    webcamListView.setOnKeyPressed((event) -> {
	    		if (event.getCode() == KeyCode.ENTER) {
	    			ObservableList<String> selectedNames = 
	    					webcamListView.getSelectionModel().getSelectedItems();
	    			
	                for (Webcam webcam : unconfiguredWebcams) {
	                	if (selectedNames.contains(webcam.getName())) {
	                		selectedWebcams.add(webcam);
	                	}
	                }
	                
	                this.close();
	    		}
	    	});
	    
	    webcamListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		webcamListView.setItems(webcams);
		pane.setCenter(webcamListView);
		
		Scene scene = new Scene(pane);
		this.setScene(scene);
		this.show();
	}
	
	public List<Webcam> getSelectedWebcams() {
		return selectedWebcams;
	}
}
