/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import marytts.util.io.FileFilter;

import com.github.sarxos.webcam.Webcam;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.plugins.DuelingTree;
import com.shootoff.plugins.ISSFStandardPistol;
import com.shootoff.plugins.ProjectorTrainingProtocolBase;
import com.shootoff.plugins.RandomShoot;
import com.shootoff.plugins.ShootDontShoot;
import com.shootoff.plugins.ShootForScore;
import com.shootoff.plugins.TimedHolsterDrill;
import com.shootoff.plugins.TrainingProtocol;
import com.shootoff.plugins.TrainingProtocolBase;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ShootOFFController implements CameraConfigListener, TargetListener {
	private Stage shootOFFStage;
	@FXML private MenuBar mainMenu;
	@FXML private Menu addTargetMenu;
	@FXML private Menu editTargetMenu;
	@FXML private Menu trainingMenu;
	@FXML private ToggleGroup trainingToggleGroup;
	@FXML private TabPane cameraTabPane;
	@FXML private Group defaultCanvasGroup;
	@FXML private TableView<ShotEntry> shotTimerTable;
	@FXML private MenuItem startArenaMenuItem;
	@FXML private MenuItem toggleArenaCalibrationMenuItem;
	@FXML private Menu addArenaTargetMenu;
	@FXML private MenuItem toggleArenaShotsMenuItem;
	
	private CamerasSupervisor camerasSupervisor;
	private Configuration config;
	private final ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
	
	private ProjectorArenaController arenaController;
	private Group calibrationGroup;
	private CanvasManager calibratingManager;
	private List<MenuItem> projectorProtocolMenuItems = new ArrayList<MenuItem>();
	
	public void init(Configuration config) {
		this.config = config;
		this.camerasSupervisor = new CamerasSupervisor(config);
		
		findTargets();
		registerTrainingProtocols();
		registerProjectorProtocols();
		
		if (config.getWebcams().isEmpty()) {
			Webcam defaultCamera = Webcam.getDefault();
			camerasSupervisor.addCameraManager(defaultCamera, 
					new CanvasManager(defaultCanvasGroup, config, camerasSupervisor, shotEntries));
		} else {
			addConfiguredCameras();
		}
		
		shootOFFStage = (Stage)mainMenu.getScene().getWindow();
		shootOFFStage.setOnCloseRequest((value) -> {
			camerasSupervisor.setStreamingAll(false);
			if (config.getProtocol().isPresent()) config.getProtocol().get().destroy();
			if (arenaController != null) arenaController.close();
		});
		
		TableColumn<ShotEntry, String> timeCol = new TableColumn<ShotEntry, String>("Time");
		timeCol.setPrefWidth(65);
		timeCol.setCellValueFactory(
                new PropertyValueFactory<ShotEntry, String>("timestamp"));
		
		TableColumn<ShotEntry, String> laserCol = new TableColumn<ShotEntry, String>("Laser");
		laserCol.setPrefWidth(65);
		laserCol.setCellValueFactory(
                new PropertyValueFactory<ShotEntry, String>("color"));
       
		shotTimerTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<ShotEntry>() {
	        @Override
	        public void onChanged(Change<? extends ShotEntry> change)
	        {
	        	while (change.next()) {
		        	for (ShotEntry unselected : change.getRemoved()) {
		        		unselected.getShot().getMarker().setStroke(TargetRegion.UNSELECTED_STROKE_COLOR);
		        	}
		        	
		        	for (ShotEntry selected : change.getAddedSubList()) {
		        		selected.getShot().getMarker().setStroke(TargetRegion.SELECTED_STROKE_COLOR);
		        	}
	        	}
	        }
	    });

		shotTimerTable.getColumns().add(timeCol);
		shotTimerTable.getColumns().add(laserCol);
		shotTimerTable.setItems(shotEntries);
	}
	
	@Override
	public void cameraConfigUpdated() {
		addConfiguredCameras();
	}
	
	private void addConfiguredCameras() {
		cameraTabPane.getTabs().clear();
		
		if (config.getWebcams().isEmpty()) {
			addCameraTab("Default", Webcam.getDefault());
		} else {
			for (String webcamName : config.getWebcams().keySet()) {
				addCameraTab(webcamName, config.getWebcams().get(webcamName));
			}
		}
	}
	
	private void addCameraTab(String webcamName, Webcam webcam) {
		Tab cameraTab = new Tab(webcamName);
		Group cameraCanvasGroup = new Group();
		// 640 x 480
		cameraTab.setContent(new AnchorPane(cameraCanvasGroup));
		
		camerasSupervisor.addCameraManager(webcam, 
				new CanvasManager(cameraCanvasGroup, config, camerasSupervisor, shotEntries));
		
		cameraTabPane.getTabs().add(cameraTab);
	}
	
	private void findTargets() {
		File targetsFolder = new File("targets");
		
		for (File file : targetsFolder.listFiles(new FileFilter("target"))) {
			newTarget(file);
		}
	}
	
	private void registerTrainingProtocols() {
		addTrainingProtocol(new ISSFStandardPistol());
		addTrainingProtocol(new RandomShoot());
		addTrainingProtocol(new ShootForScore());
		addTrainingProtocol(new TimedHolsterDrill());
	}
	
	private void addTrainingProtocol(TrainingProtocol protocol) {
		RadioMenuItem protocolItem = new RadioMenuItem(protocol.getInfo().getName());
		protocolItem.setToggleGroup(trainingToggleGroup);
		
		protocolItem.setOnAction((e) -> {
				try {
					Constructor<?> ctor = protocol.getClass().getConstructor(List.class);
					
					List<Group> knownTargets = new ArrayList<Group>();
					knownTargets.addAll(camerasSupervisor.getTargets());
					
					if (arenaController != null) {
						knownTargets.addAll(arenaController.getCanvasManager().getTargets());
					}
					
					TrainingProtocol newProtocol = (TrainingProtocol)ctor.newInstance(knownTargets);
					((TrainingProtocolBase)newProtocol).init(config, camerasSupervisor, shotTimerTable);
					newProtocol.init();
					config.setProtocol(newProtocol);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		
		trainingMenu.getItems().add(protocolItem);
	}
	
	private void registerProjectorProtocols() {
		addProjectorTrainingProtocol(new DuelingTree());
		addProjectorTrainingProtocol(new ShootDontShoot());
	}
	
	private void addProjectorTrainingProtocol(TrainingProtocol protocol) {
		RadioMenuItem protocolItem = new RadioMenuItem(protocol.getInfo().getName());
		protocolItem.setToggleGroup(trainingToggleGroup);	
		if (arenaController == null) protocolItem.setDisable(true);
		
		protocolItem.setOnAction((e) -> {
				try {
					Constructor<?> ctor = protocol.getClass().getConstructor(List.class);
					TrainingProtocol newProtocol = (TrainingProtocol)ctor.newInstance(
							arenaController.getCanvasManager().getTargets());
					((ProjectorTrainingProtocolBase)newProtocol).init(config, camerasSupervisor, 
							shotTimerTable, arenaController);
					newProtocol.init();
					config.setProtocol(newProtocol);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		
		trainingMenu.getItems().add(protocolItem);
		projectorProtocolMenuItems.add(protocolItem);
	}
	
	@FXML 
	public void clickedNoneProtocol(ActionEvent event) {
		config.setProtocol(null);
	}
	
	@FXML 
	public void preferencesClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));
		loader.load();
		
		Stage preferencesStage = new Stage();
		
		preferencesStage.initOwner(shootOFFStage);
		preferencesStage.initModality(Modality.WINDOW_MODAL);
        preferencesStage.setTitle("Preferences");
        preferencesStage.setScene(new Scene(loader.getRoot()));
        preferencesStage.show();
        ((PreferencesController)loader.getController()).setConfig(config, this);
    }
	
	@FXML 
	public void startArenaClicked(ActionEvent event) throws IOException {		
		toggleProjectorMenus(false);
		startArenaMenuItem.setDisable(true);
		
		if (arenaController == null) {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/ProjectorArena.fxml"));
			loader.load();
			
			Stage arenaStage = new Stage();
			
	        arenaStage.setTitle("Projector Arena");
	        arenaStage.setScene(new Scene(loader.getRoot()));
	        
	        arenaController = (ProjectorArenaController)loader.getController();
	        arenaController.init(config, camerasSupervisor);
	        arenaController.getCanvasManager().setShowShots(false);
	        
	        arenaStage.setOnCloseRequest((e) -> { 
	        		arenaController = null;
	        		toggleArenaCalibrationMenuItem.setText("Calibrate");
	        		toggleArenaShotsMenuItem.setText("Show Shot Markers");
	        		if (calibratingManager != null) {
	        			calibratingManager.setProjectorArena(null, null);
	        			if (calibrationGroup != null) calibratingManager.removeTarget(calibrationGroup);
	        			calibratingManager = null;
	        		}
	        		toggleProjectorMenus(true);
	        		startArenaMenuItem.setDisable(false);
	        	});
		}
		
		arenaController.toggleArena();
    }
	
	private void toggleProjectorMenus(boolean isDisabled) {
		toggleArenaCalibrationMenuItem.setDisable(isDisabled);
		addArenaTargetMenu.setDisable(isDisabled);
		toggleArenaShotsMenuItem.setDisable(isDisabled);
		
		for (MenuItem m : projectorProtocolMenuItems) m.setDisable(isDisabled);
	}
	
	@FXML
	public void toggleArenaCalibrationClicked(ActionEvent event) {
		final int DEFAULT_DIM = 30;
		final int DEFAULT_POS = 100;
		
		if (toggleArenaCalibrationMenuItem.getText().equals("Calibrate")) {
			toggleArenaCalibrationMenuItem.setText("Stop Calibrating");
			
			RectangleRegion calibrationRectangle =  new RectangleRegion(DEFAULT_DIM, DEFAULT_DIM, 
					DEFAULT_POS, DEFAULT_POS);
			calibrationRectangle.setFill(Color.PURPLE);
			calibrationRectangle.setOpacity(TargetIO.DEFAULT_OPACITY);
			
			calibrationGroup = new Group();
			new TargetContainer(calibrationGroup, config);
			calibrationGroup.getChildren().add(calibrationRectangle);
			
			calibratingManager = camerasSupervisor.getCanvasManager(
					cameraTabPane.getSelectionModel().getSelectedIndex());
			calibratingManager.addTarget(calibrationGroup);
		} else {
			toggleArenaCalibrationMenuItem.setText("Calibrate");
			
			calibratingManager.removeTarget(calibrationGroup);
			calibratingManager.setProjectorArena(arenaController, calibrationGroup.getBoundsInParent());
			calibrationGroup = null;
		}		
	}
	
	public void toggleArenaShotsClicked(ActionEvent event) {
		if (toggleArenaShotsMenuItem.getText().equals("Show Shot Markers")) {
			toggleArenaShotsMenuItem.setText("Hide Shot Markers");
			arenaController.getCanvasManager().setShowShots(true);
		} else {
			toggleArenaShotsMenuItem.setText("Show Shot Markers");
			arenaController.getCanvasManager().setShowShots(false);
		}
	}
	
	@FXML
	public void exitMenuClicked(ActionEvent event) {
		camerasSupervisor.setStreamingAll(false);
		shootOFFStage.close();
	}
	
	@FXML
	public void hideTargetsClicked(ActionEvent event) {
		MenuItem hideTargetMenuItem = (MenuItem)event.getSource();
		
		if (hideTargetMenuItem.getText().equals("Hide Targets")) {
			hideTargetMenuItem.setText("Show Targets");
			
			for (Group target : camerasSupervisor.getTargets()) {
				target.setVisible(false);
			}
		} else {
			hideTargetMenuItem.setText("Hide Targets");
			
			for (Group target : camerasSupervisor.getTargets()) {
				target.setVisible(true);
			}
		}
	}

	@FXML 
	public void createTargetMenuClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = createPreferencesStage();
		
        CameraManager currentCamera = camerasSupervisor.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
		Image currentFrame = currentCamera.getCurrentFrame();
        ((TargetEditorController)loader.getController()).init(currentFrame, this);
	}
	
	private FXMLLoader createPreferencesStage() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/TargetEditor.fxml"));
		loader.load();
		
		Stage preferencesStage = new Stage();
		
		preferencesStage.initOwner(shootOFFStage);
		preferencesStage.initModality(Modality.WINDOW_MODAL);
        preferencesStage.setTitle("TargetEditor");
        preferencesStage.setScene(new Scene(loader.getRoot()));
        preferencesStage.show();
        
        return loader;
	}
	
	@FXML
	public void resetClicked(ActionEvent event) {
		camerasSupervisor.reset();
		
		if (config.getProtocol().isPresent()) {
			List<Group> knownTargets = new ArrayList<Group>();
			knownTargets.addAll(camerasSupervisor.getTargets());
			
			if (arenaController != null) {
				knownTargets.addAll(arenaController.getCanvasManager().getTargets());
			}
			
			config.getProtocol().get().reset(knownTargets);
		}
	}

	@Override
	public void newTarget(File path) {
		String targetPath = path.getPath();
		
		String targetName = targetPath.substring(targetPath.lastIndexOf(File.separator) + 1,
				targetPath.lastIndexOf('.'));
		
		MenuItem addTargetItem = new MenuItem(targetName);
		addTargetItem.setMnemonicParsing(false);
		
		addTargetItem.setOnAction((e) -> {
				camerasSupervisor.getCanvasManager(
						cameraTabPane.getSelectionModel().getSelectedIndex()).addTarget(path);
			});
		
		MenuItem addProjectorTargetItem = new MenuItem(targetName);
		addProjectorTargetItem.setMnemonicParsing(false);
		
		addProjectorTargetItem.setOnAction((e) -> {
				arenaController.getCanvasManager().addTarget(path);
			});
		
		MenuItem editTargetItem = new MenuItem(targetName);
		editTargetItem.setMnemonicParsing(false);
		
		editTargetItem.setOnAction((e) -> {
				try {
					FXMLLoader loader = createPreferencesStage();
					
					CameraManager currentCamera = camerasSupervisor.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
					Image currentFrame = currentCamera.getCurrentFrame();
					((TargetEditorController)loader.getController()).init(currentFrame, this, path);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		
		addTargetMenu.getItems().add(addTargetItem);
		addArenaTargetMenu.getItems().add(addProjectorTargetItem);
		editTargetMenu.getItems().add(editTargetItem);
	}
}
