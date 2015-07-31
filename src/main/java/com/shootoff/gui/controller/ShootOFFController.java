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

package com.shootoff.gui.controller;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import marytts.util.io.FileFilter;

import com.shootoff.camera.Camera;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.CalibrationConfigPane;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.ShotSectorPane;
import com.shootoff.gui.TargetListener;
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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
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
	@FXML private TableView<ShotEntry> shotTimerTable;
	@FXML private MenuItem startArenaMenuItem;
	@FXML private MenuItem toggleArenaCalibrationMenuItem;
	@FXML private Menu addArenaTargetMenu;
	@FXML private MenuItem toggleArenaShotsMenuItem;
	
	private String defaultWindowTitle;
	private CamerasSupervisor camerasSupervisor;
	private Configuration config;
	private final ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
	private final List<Stage> streamDebuggerStages = new ArrayList<Stage>();
	
	private ProjectorArenaController arenaController;
	private CalibrationConfigPane calibrationConfigPane;
	private Group calibrationGroup;
	private CameraManager calibratingManager;
	private List<MenuItem> projectorProtocolMenuItems = new ArrayList<MenuItem>();
	
	public void init(Configuration config) {
		this.config = config;
		this.camerasSupervisor = new CamerasSupervisor(config);
		
		findTargets();
		registerTrainingProtocols();
		registerProjectorProtocols();
		
		shootOFFStage = (Stage)mainMenu.getScene().getWindow();
		this.defaultWindowTitle = shootOFFStage.getTitle();
		shootOFFStage.getIcons().add(
				   new Image(ShootOFFController.class.getResourceAsStream("/images/icon_128x128.png"))); 
		shootOFFStage.setOnCloseRequest((value) -> {
			camerasSupervisor.closeAll();
			if (config.getProtocol().isPresent()) config.getProtocol().get().destroy();
			if (arenaController != null) arenaController.close();
			
			for (Stage streamDebuggerStage : streamDebuggerStages) {
				streamDebuggerStage.close();
			}
		});
		
		if (config.getWebcams().isEmpty()) {
			Camera defaultCamera = Camera.getDefault();
			if (defaultCamera != null) {
				if (!addCameraTab("Default", defaultCamera)) cameraLockFailure(defaultCamera, true);
			} else {
				Alert cameraAlert = new Alert(AlertType.ERROR);
				cameraAlert.setTitle("No Webcams");
				cameraAlert.setHeaderText("No Webcams Found!");
				cameraAlert.setResizable(true);
				cameraAlert.setContentText("ShootOFF needs a webcam to function. Now closing...");
				cameraAlert.showAndWait();
				System.exit(-1);
			}
		} else {
			addConfiguredCameras();
		}
		
		TableColumn<ShotEntry, String> timeCol = new TableColumn<ShotEntry, String>("Time");
		timeCol.setPrefWidth(65);
		timeCol.setCellValueFactory(
                new PropertyValueFactory<ShotEntry, String>("timestamp"));
		
		TableColumn<ShotEntry, String> laserCol = new TableColumn<ShotEntry, String>("Laser");
		laserCol.setPrefWidth(65);
		laserCol.setCellValueFactory(
                new PropertyValueFactory<ShotEntry, String>("color"));
		
		shotEntries.addListener(new ListChangeListener<ShotEntry>() {
	        @Override
	        public void onChanged(Change<? extends ShotEntry> change)
	        {
	        	change.next();
	        	if (change.getAddedSize() < 1) return;
	        	Platform.runLater(() -> {
	        			final int size = shotTimerTable.getItems().size();
	        			if (size > 0) shotTimerTable.scrollTo(size - 1);
	        		});
	        }
	    });
		
		shotTimerTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<ShotEntry>() {
	        @Override
	        public void onChanged(Change<? extends ShotEntry> change)
	        {
	        	while (change.next()) {
		        	for (ShotEntry unselected : change.getRemoved()) {
		        		unselected.getShot().getMarker().setFill(unselected.getShot().getColor());
		        	}
		        	
		        	for (ShotEntry selected : change.getAddedSubList()) {
		        		selected.getShot().getMarker().setFill(TargetRegion.SELECTED_STROKE_COLOR);
		        	}
	        	}
	        }
	    });
		
		shotTimerTable.getColumns().add(timeCol);
		shotTimerTable.getColumns().add(laserCol);
		shotTimerTable.setItems(shotEntries);
		shotTimerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}
	
	@Override
	public void cameraConfigUpdated() {
		addConfiguredCameras();
	}
	
	private void addConfiguredCameras() {
		cameraTabPane.getTabs().clear();
		camerasSupervisor.clearManagers();
		
		if (config.getWebcams().isEmpty()) {
			if (!addCameraTab("Default", Camera.getDefault())) cameraLockFailure(Camera.getDefault(), true);
		} else {
			int failureCount = 0;
			
			for (String webcamName : config.getWebcams().keySet()) {
				Camera webcam = config.getWebcams().get(webcamName);
				
				if (!addCameraTab(webcamName, webcam)) {
					failureCount++;
					cameraLockFailure(webcam, failureCount == config.getWebcams().size());
				}
			}
		}
	}
	
	private void cameraLockFailure(Camera webcam, boolean allCamerasFailed) {
		Alert cameraAlert = new Alert(AlertType.ERROR);
		cameraAlert.setTitle("Webcam Locked");
		cameraAlert.setHeaderText("Cannot Open Webcam");
		cameraAlert.setResizable(true);
		cameraAlert.getDialogPane().getScene().getWindow().requestFocus();
		
		String messageFormat;
		
		if (allCamerasFailed) {
			messageFormat = "Cannot open the webcam %s. It is being "
					+ "used by another program. This is the only configured camera, thus "
					+ "ShootOFF must close.";
		} else {
			messageFormat = "Cannot open the webcam %s. It is being "
					+ "used by another program or you have ShootOFF open more than once.";
		}
		
		Optional<String> webcamName = config.getWebcamsUserName(webcam);
		
		cameraAlert.setContentText(String.format(messageFormat, 
				webcamName.isPresent() ? webcamName.get() : webcam.getName()));
		
		if (allCamerasFailed) {
			cameraAlert.showAndWait();
			System.exit(-1);
		} else {
			cameraAlert.show();
		}
	}
	
	private boolean addCameraTab(String webcamName, Camera webcam) {
		if (webcam.isLocked() && !webcam.isOpen()) {
			return false;
		}
		
		Tab cameraTab = new Tab(webcamName);
		Group cameraCanvasGroup = new Group();
		// 640 x 480
		cameraTab.setContent(new AnchorPane(cameraCanvasGroup));
		
		CanvasManager canvasManager = new CanvasManager(cameraCanvasGroup, config, camerasSupervisor, shotEntries);
		camerasSupervisor.addCameraManager(webcam, canvasManager);
		canvasManager.setContextMenu(createContextMenu());
		
		// Show coords of mouse when in canvas during debug mode
		if (config.inDebugMode()) {
			canvasManager.getCanvasGroup().setOnMouseMoved((event) -> {
					shootOFFStage.setTitle(defaultWindowTitle + String.format("(%.1f, %.1f)", event.getX(), event.getY()));
				});
			
			canvasManager.getCanvasGroup().setOnMouseExited((event) -> {
					shootOFFStage.setTitle(defaultWindowTitle);
				});
		}
		
		return cameraTabPane.getTabs().add(cameraTab);
	}
	
	private ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();
		
		MenuItem toggleDetectionSectors = new MenuItem("Toggle Shot Detection Sectors");
		
		toggleDetectionSectors.setOnAction((event) -> {
				AnchorPane tabAnchor = (AnchorPane)cameraTabPane.getSelectionModel().getSelectedItem().getContent();
				
				// Only add the pane if it isn't already open
				boolean hasPane = false;
				for (Node node : tabAnchor.getChildren()) {
					if (node instanceof ShotSectorPane) {
						hasPane = true;
						break;
					}
				}
				
				if (!hasPane) {
					CameraManager cameraManager = camerasSupervisor.getCameraManager(
							cameraTabPane.getSelectionModel().getSelectedIndex());
					new ShotSectorPane(tabAnchor, cameraManager);
				}
			});
		
		contextMenu.getItems().add(toggleDetectionSectors);
		
		if (config.inDebugMode()) {
			MenuItem startStreamDebuggerMenuItem = new MenuItem("Start Stream Debugger");
			
			startStreamDebuggerMenuItem.setOnAction((event) -> {
					FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/StreamDebugger.fxml"));
					try {
						loader.load();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					Stage streamDebuggerStage = new Stage();
					streamDebuggerStages.add(streamDebuggerStage);

					String tabName = cameraTabPane.getSelectionModel().getSelectedItem().getText();
			        streamDebuggerStage.setTitle(String.format("Stream Debugger -- %s", 
			        		tabName));
			        streamDebuggerStage.setScene(new Scene(loader.getRoot()));
			        streamDebuggerStage.show();
					CameraManager cameraManager = camerasSupervisor.getCameraManager(
							cameraTabPane.getSelectionModel().getSelectedIndex());
			        ((StreamDebuggerController)loader.getController()).init(cameraManager);
			        
			        startStreamDebuggerMenuItem.setDisable(true);
			        
			        streamDebuggerStage.setOnCloseRequest((e) -> {
			        		startStreamDebuggerMenuItem.setDisable(false);
			        		cameraManager.setThresholdListener(null);
			        		streamDebuggerStages.remove(streamDebuggerStage);
			        	});
				});
			
			contextMenu.getItems().add(startStreamDebuggerMenuItem);
			
			MenuItem recordMenuItem = new MenuItem("Start Recording");
			
			recordMenuItem.setOnAction((event) -> {
					CameraManager cameraManager = camerasSupervisor.getCameraManager(
							cameraTabPane.getSelectionModel().getSelectedIndex());
				
					if (recordMenuItem.getText().equals("Start Recording")) {
						recordMenuItem.setText("Stop Recording");
						
						String tabName = cameraTabPane.getSelectionModel().getSelectedItem().getText();
						String videoName = tabName + ".mp4";
						cameraManager.startRecording(new File(videoName));
					} else {
						recordMenuItem.setText("Start Recording");
						cameraManager.stopRecording();
					}
				});
			
			contextMenu.getItems().add(recordMenuItem);
		}
		
		return contextMenu;
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
	        
	        toggleArenaCalibrationMenuItem.fire();
	        
	        arenaStage.setOnCloseRequest((e) -> { 
	        		toggleArenaShotsMenuItem.setText("Show Shot Markers");
	        		if (calibratingManager != null) {
	        			toggleArenaCalibrationMenuItem.fire();
	        		}
	        		toggleProjectorMenus(true);
	        		startArenaMenuItem.setDisable(false);
	        		arenaController = null;
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
			
			final AnchorPane tabAnchor = (AnchorPane)cameraTabPane.getSelectionModel().getSelectedItem().getContent();
			calibrationConfigPane = new CalibrationConfigPane(tabAnchor);
			
			RectangleRegion calibrationRectangle =  new RectangleRegion(DEFAULT_DIM, DEFAULT_DIM, 
					DEFAULT_POS, DEFAULT_POS);
			calibrationRectangle.setFill(Color.PURPLE);
			calibrationRectangle.setOpacity(TargetIO.DEFAULT_OPACITY);

			calibratingManager = camerasSupervisor.getCameraManager(
					cameraTabPane.getSelectionModel().getSelectedIndex());
			calibrationGroup = new Group();
			calibrationGroup.setOnMouseClicked((e) -> { calibrationGroup.requestFocus(); });
			calibrationGroup.getChildren().add(calibrationRectangle);
			
			calibratingManager.setDetecting(false);
			calibratingManager.setProjectionBounds(null);
			calibratingManager.getCanvasManager().addTarget(calibrationGroup, false);
			
		} else {
			toggleArenaCalibrationMenuItem.setText("Calibrate");
			
			calibrationConfigPane.close();
			
			calibratingManager.getCanvasManager().removeTarget(calibrationGroup);
			calibratingManager.getCanvasManager().setProjectorArena(arenaController, calibrationGroup.getBoundsInParent());
			calibratingManager.setCropFeedToProjection(calibrationConfigPane.cropFeed());
			calibratingManager.setLimitDetectProjection(calibrationConfigPane.limitDetectProjection());
			
			if (calibrationConfigPane.cropFeed() || calibrationConfigPane.limitDetectProjection()) {
				calibratingManager.setProjectionBounds(calibrationGroup.getBoundsInParent());
			} else {
				calibratingManager.setProjectionBounds(null);
			}
			
			calibratingManager.setDetecting(true);
			
			calibratingManager = null;
			calibrationGroup = null;
			calibrationConfigPane = null;
			arenaController.calibrated();
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
	
	@FXML
	public void saveFeedClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Feed Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"),
                new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png")
            );
		File feedFile = fileChooser.showSaveDialog(shootOFFStage);
		
		if (feedFile != null) {
			String extension = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).substring(2);
			File imageFile = new File(feedFile.getPath() + "." +  extension);
			RenderedImage renderedImage = SwingFXUtils.fromFXImage(shootOFFStage.getScene().snapshot(null), null);
			try {
				ImageIO.write(renderedImage, extension, imageFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
