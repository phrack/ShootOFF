package com.shootoff.gui.controller;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.SessionCanvasManager;
import com.shootoff.session.Event;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.io.SessionIO;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import marytts.util.io.FileFilter;

public class SessionViewerController {
	@FXML private ListView<File> sessionListView;
	@FXML private TabPane cameraTabPane;
	@FXML private Slider timeSlider;
	@FXML private Label timeLabel;
	@FXML private ListView<Event> eventsListView;
	
	private final Logger logger = LoggerFactory.getLogger(SessionViewerController.class);
	private final ObservableList<File> sessionEntries = FXCollections.observableArrayList();
	private final ObservableList<Event> eventEntries = FXCollections.observableArrayList();
	private final Map<String, SessionCanvasManager> cameraGroups = new HashMap<String, SessionCanvasManager>();
	private final Map<Tab, Integer> eventSelectionsPerTab = new HashMap<Tab, Integer>();
	
	private boolean refreshFromSlider = true;
	private boolean refreshFromSelection = true;
	private SessionRecorder currentSession;
	
	public void init() {
		sessionEntries.addAll(findSessions());
		sessionListView.setItems(sessionEntries);
		
		sessionListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<File>() {
				public void changed(ObservableValue<? extends File> ov, File oldFile, File newFile) {
					Optional<SessionRecorder> session = SessionIO.loadSession(newFile);
					
					if (session.isPresent()) {
						refreshFromSlider = false;
						timeSlider.setValue(0);
						refreshFromSlider = true;
						
						currentSession = session.get();
						updateCameraTabs();
						
						Tab selectedTab = cameraTabPane.getSelectionModel().getSelectedItem();
						
						if (selectedTab != null) {
							String cameraName = selectedTab.getText();
							listCameraEvents(cameraName);
						} else {
							eventEntries.clear();
						}
					}
				}
            });
		
		cameraTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			public void changed(ObservableValue<? extends Tab> ot, Tab oldTab, Tab newTab) {	
				if (newTab == null) return;
				
				eventSelectionsPerTab.put(oldTab, eventsListView.getSelectionModel().getSelectedIndex());
				
				listCameraEvents(newTab.getText());
				
				List<Event> cameraEvents = currentSession.getCameraEvents(newTab.getText());
				refreshFromSlider = false;
				timeSlider.setValue(0);
				timeSlider.setMax(cameraEvents.get(cameraEvents.size() - 1).getTimestamp());
				refreshFromSlider = true;
				
				if (eventSelectionsPerTab.containsKey(newTab)) {
					refreshFromSelection = false;
					eventsListView.getSelectionModel().select(eventSelectionsPerTab.get(newTab));
					refreshFromSelection = true;
				}
			}
        });
		
		eventsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Event>() {
			public void changed(ObservableValue<? extends Event> oe, Event oldEvent, Event newEvent) {
				if (newEvent == null) return;
				
				refreshFromSlider = false;
				timeSlider.setValue(newEvent.getTimestamp());
				refreshFromSlider = true;
				
				if (!refreshFromSelection) return;
				
				int oldIndex = eventEntries.indexOf(oldEvent);
				int newIndex = eventEntries.indexOf(newEvent);
				
				if (oldIndex <= newIndex) {
					updateEvents(oldIndex, newIndex, EventsUpdate.DO);
				} else {
					updateEvents(oldIndex, newIndex, EventsUpdate.UNDO);
				}
			}
        });
		
		eventsListView.setItems(eventEntries);
		
		timeSlider.valueProperty().addListener(new ChangeListener<Number>() {
		      @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
		        if (newValue == null) {
		          timeLabel.setText("");
		          return;
		        }
		        
		        setTime(newValue.longValue());
		        
		        if (!refreshFromSlider) return;
		        
		        for (Event e : eventEntries) {
		        	if (e.getTimestamp() >= newValue.longValue()) {
		        		eventsListView.getSelectionModel().select(e);
		        		break;
		        	}
		        }
		      }
		    });
	}
	
	private void setTime(long timestamp /* ms */) {
		Date date = new Date(timestamp);
		DateFormat formatter = new SimpleDateFormat("mm:ss:SSS");
		timeLabel.setText(formatter.format(date));
	}
	
	private List<File> findSessions () {
		File targetsFolder = new File("sessions");
		List<File> sessions = new ArrayList<File>();
		
		File[] sessionFiles = targetsFolder.listFiles(new FileFilter("xml"));
		
		if (sessionFiles != null) {
			for (File file : sessionFiles) {
				sessions.add(file);
			}
		} else {
			logger.error("Failed to find session files because a list of files could not be retrieved");
		}
		
		return sessions;
	}
	
	private void updateCameraTabs() {
		cameraTabPane.getTabs().clear();
		cameraGroups.clear();
		eventSelectionsPerTab.clear();
		
		for (String cameraName : currentSession.getEvents().keySet()) {
			Group canvas = new Group();
			cameraGroups.put(cameraName, new SessionCanvasManager(canvas));
			
			Tab cameraTab = new Tab(cameraName);
			cameraTab.setContent(new AnchorPane(canvas));
			cameraTabPane.getTabs().add(cameraTab);
		}
	}
	
	private void listCameraEvents(String cameraName) {
		eventEntries.clear();
		eventEntries.addAll(currentSession.getCameraEvents(cameraName));
	}
	
	private enum EventsUpdate {
		DO, UNDO
	}
	
	private void updateEvents(int oldIndex, int newIndex, EventsUpdate updateType) {
		List<Event> events; 
		
		if (updateType == EventsUpdate.DO) {
			events = new ArrayList<Event>(eventEntries.subList(oldIndex + 1, newIndex + 1));
		} else {
			events = new ArrayList<Event>(eventEntries.subList(newIndex + 1, oldIndex + 1));
			Collections.reverse(events);
		}
		
		SessionCanvasManager currentCanvasManager = 
				cameraGroups.get(cameraTabPane.getSelectionModel().getSelectedItem().getText());
		
		for (Event e : events) {
			if (updateType == EventsUpdate.DO) {
				currentCanvasManager.doEvent(e);
			} else {
				currentCanvasManager.undoEvent(e);
			}
		}
	}
	
	@FXML
	public void nextButtonClicked(ActionEvent event) {
		int selectedIndex = eventsListView.getSelectionModel().getSelectedIndex();
		
		if (selectedIndex >= 0) {
			if (selectedIndex < eventEntries.size() - 1) {
				eventsListView.getSelectionModel().select(++selectedIndex);
			} else {
				eventsListView.getSelectionModel().select(0);
			}
		} else {
			eventsListView.getSelectionModel().select(0);
		}
	}
	
	@FXML
	public void previousButtonClicked(ActionEvent event) {
		int selectedIndex = eventsListView.getSelectionModel().getSelectedIndex();

		if (selectedIndex >= 0) {
			if (selectedIndex == 0) {
				eventsListView.getSelectionModel().select(eventEntries.size() - 1);
			} else {
				eventsListView.getSelectionModel().select(--selectedIndex);
			}
		} else {
			eventsListView.getSelectionModel().select(eventEntries.size() - 1);
		}
	}
} 