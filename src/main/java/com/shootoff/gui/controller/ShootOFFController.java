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
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.openimaj.util.parallel.GlobalExecutorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import marytts.util.io.FileFilter;

import com.shootoff.Main;
import com.shootoff.camera.Camera;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.config.Configuration;
import com.shootoff.courses.Course;
import com.shootoff.courses.io.CourseIO;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.ShotSectorPane;
import com.shootoff.gui.Target;
import com.shootoff.gui.TargetListener;
import com.shootoff.plugins.BouncingTargets;
import com.shootoff.plugins.DuelingTree;
import com.shootoff.plugins.ISSFStandardPistol;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.plugins.RandomShoot;
import com.shootoff.plugins.ShootDontShoot;
import com.shootoff.plugins.ShootForScore;
import com.shootoff.plugins.TimedHolsterDrill;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.io.SessionIO;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ShootOFFController implements CameraConfigListener, TargetListener {
	private Stage shootOFFStage;
	@FXML private MenuBar mainMenu;
	@FXML private Menu addTargetMenu;
	@FXML private Menu editTargetMenu;
	@FXML private Menu trainingMenu;
	@FXML private RadioMenuItem noneTrainingMenuItem;
	@FXML private MenuItem toggleSessionRecordingMenuItem;
	@FXML private MenuItem showSessionViewerMenuItem;
	@FXML private ToggleGroup trainingToggleGroup;
	@FXML private TabPane cameraTabPane;
	@FXML private TableView<ShotEntry> shotTimerTable;
	@FXML private MenuItem startArenaMenuItem;
	@FXML private MenuItem toggleArenaCalibrationMenuItem;
	@FXML private Menu calibrationOptionsMenu;
	@FXML private ToggleGroup calibrationToggleGroup;
	@FXML private Menu addArenaTargetMenu;
	@FXML private Menu arenaBackgroundMenu;
	@FXML private Menu coursesMenu;
	@FXML private MenuItem toggleArenaShotsMenuItem;
	
	private String defaultWindowTitle;
	private CamerasSupervisor camerasSupervisor;
	private Configuration config;
	private final Logger logger = LoggerFactory.getLogger(ShootOFFController.class);
	private final ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
	private final List<Stage> streamDebuggerStages = new ArrayList<Stage>();
	
	private ProjectorArenaController arenaController;
	private Optional<Target> calibrationTarget = Optional.empty();
	private CameraManager arenaCameraManager;
	private List<MenuItem> projectorExerciseMenuItems = new ArrayList<MenuItem>();
	
	private Stage sessionViewerStage;
	
	private boolean isCalibrating = false;
	
	
	private enum CalibrationOption {
		EVERYWHERE, ONLY_IN_BOUNDS, CROP
	}
	
	public void init(Configuration config) {
		this.config = config;
		this.camerasSupervisor = new CamerasSupervisor(config);
		
		findTargets();
		initDefaultBackgrounds();
		registerTrainingExercises();
		registerProjectorExercises();
		
		shootOFFStage = (Stage)mainMenu.getScene().getWindow();
		this.defaultWindowTitle = shootOFFStage.getTitle();
		shootOFFStage.getIcons().addAll(
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_16x16.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_32x32.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_48x48.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_64x64.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_128x128.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_256x256.png"))); 
		
		shootOFFStage.setOnCloseRequest((value) -> {
			camerasSupervisor.closeAll();
			
			if (autoCalibrationTimer != null) {
				autoCalibrationTimer.cancel();
				autoCalibrationTimer = null;
			}
			
			if (disableShotDetectionTimer != null) {
				disableShotDetectionTimer.cancel();
				disableShotDetectionTimer = null;
			}
			
			if (config.getExercise().isPresent()) config.getExercise().get().destroy();
			
			if (arenaController != null) {
				arenaController.getCanvasManager().close();
				arenaController.close();
			}
			
			for (Stage streamDebuggerStage : streamDebuggerStages) {
				streamDebuggerStage.close();
			}
			
			if (config.getSessionRecorder().isPresent()) {
				toggleSessionRecordingMenuItem.fire();
			}
			
			if (showSessionViewerMenuItem.isDisable()) {
				sessionViewerStage.close();
			}
			
			GlobalExecutorPool.getPool().shutdownNow();
			
			if (!config.getVideoPlayers().isEmpty()) {
				for (VideoPlayerController videoPlayer : config.getVideoPlayers()) {
					videoPlayer.getStage().close();
				}
			}
			
			if (!config.inDebugMode()) Main.forceClose(0);
		});
		
		if (config.getWebcams().isEmpty()) {
			Camera defaultCamera = Camera.getDefault();
			if (defaultCamera != null) {
				if (!addCameraTab("Default", defaultCamera)) cameraLockFailure(defaultCamera, true);
			} else {
				Main.closeNoCamera();
			}
		} else {
			addConfiguredCameras();
		}
		
		TableColumn<ShotEntry, String> timeCol = new TableColumn<ShotEntry, String>("Time");
		timeCol.setMinWidth(85);
		timeCol.setCellValueFactory(
                new PropertyValueFactory<ShotEntry, String>("timestamp"));
		
		TableColumn<ShotEntry, ShotEntry.SplitData> splitCol = new TableColumn<ShotEntry, ShotEntry.SplitData>("Split");
		splitCol.setMinWidth(85);
		splitCol.setCellValueFactory(
                new PropertyValueFactory<ShotEntry, ShotEntry.SplitData>("split"));
		
		TableColumn<ShotEntry, String> laserCol = new TableColumn<ShotEntry, String>("Laser");
		laserCol.setMinWidth(85);
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
		
		calibrationToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
		    public void changed(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle newToggle) {
	    			if (newToggle == null) return;
	    	
	    			configureArenaCamera(getSelectedCalibrationOption());
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
		        		
		        		// Move all selected shots to top the of their z-stack to ensure visibility
		        		for (CanvasManager cm : camerasSupervisor.getCanvasManagers()) {
		        			Shape marker = selected.getShot().getMarker();
		        			if (cm.getCanvasGroup().getChildren().indexOf(marker) < cm.getCanvasGroup().getChildren().size() - 1) {
		        				cm.getCanvasGroup().getChildren().remove(marker);
		        				cm.getCanvasGroup().getChildren().add(cm.getCanvasGroup().getChildren().size(), marker);
		        	}
	        	}
	        }
	        	}
	        }
	    });
		
		shotTimerTable.setRowFactory(tableView -> new TableRow<ShotEntry>() {
	            @Override
	            protected void updateItem(ShotEntry item, boolean empty){
	            	super.updateItem(item, empty);
	            	
	                if (item == null || empty) {
	                    setStyle("");
	                    return;
	                }
	            	
	            	if (item.getRowColor().isPresent()) {
	            		setStyle("-fx-background-color: " + CanvasManager.colorToWebCode(item.getRowColor().get()));
	            	} else {
	            		setStyle("");
	            	}
	            }
	    	});
		
		splitCol.setCellFactory(column -> {
		        return new TableCell<ShotEntry, ShotEntry.SplitData>() {
		        	@Override
		            public void updateItem(ShotEntry.SplitData item, boolean empty) {
		                super.updateItem(item, empty);
		                
		                if (item == null || empty) {
		                    setText(null);
		                    setStyle("");
		                    return;
		                }
		                
		                setText(item.getSplit());
		                
		                if (item.hadMalfunction()) {
		                    setStyle("-fx-background-color: orange");
		                } else if (item.hadReload()) {
		                	setStyle("-fx-background-color: lightskyblue");
		                } else if (item.getRowColor().isPresent()) {
		                	setStyle("-fx-background-color: " + CanvasManager.colorToWebCode(item.getRowColor().get()));
		            	} else {
		                    setStyle("");
		                }
		            }
		        };
			});
		
		shotTimerTable.getColumns().add(timeCol);
		shotTimerTable.getColumns().add(splitCol);
		shotTimerTable.getColumns().add(laserCol);
		shotTimerTable.setItems(shotEntries);
		shotTimerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}
	
	private CalibrationOption getSelectedCalibrationOption() {
		Toggle selectedToggle = calibrationToggleGroup.getSelectedToggle();
        if (selectedToggle != null && selectedToggle instanceof RadioMenuItem) {
        	RadioMenuItem selectedOption = (RadioMenuItem)calibrationToggleGroup.getSelectedToggle();
        	
        	switch (selectedOption.getText().toLowerCase(Locale.getDefault())) {
        	case "detect everywhere":
        		return CalibrationOption.EVERYWHERE;
        		
        	case "only detect in projector bounds":
        		return CalibrationOption.ONLY_IN_BOUNDS;
        		
        	case "crop feed to projector bounds":
        		return CalibrationOption.CROP;
        	
    		default:
    			logger.error("Unknown calibration option, defaulting to only in projection bounds: {}", 
    					selectedOption.getText());
    			
    			return CalibrationOption.ONLY_IN_BOUNDS;
        	}
        } else {
        	logger.error("No calibration toggle selected or it's not a RadioMenuItem. This should not be possible.");
        	return CalibrationOption.ONLY_IN_BOUNDS;
        }
	}
	
	public Stage getStage() {
		return shootOFFStage;
	}
	
	@Override
	public void cameraConfigUpdated() {
		config.unregisterAllRecordingCameraManagers();
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
			Main.forceClose(-1);
		} else {
			cameraAlert.show();
		}
	}
	
	private boolean addCameraTab(String webcamName, Camera webcam) {
		if (webcam.isLocked() && !webcam.isOpen()) {
			return false;
		}
		
		// We want the CameraManager to configure the camera, we just try to
		// open and close it here to see if we can. If we hold off on doing this
		// until later it's harder to give the user a good error message.
		String os = System.getProperty("os.name");
		if (os != null && !os.equals("Mac OS X")) {
			if (!webcam.isOpen() && !webcam.open()) {
				return false;
			} else {
				webcam.close();
			}
		}
		
		Tab cameraTab = new Tab(webcamName);
		Group cameraCanvasGroup = new Group();
		// 640 x 480
		cameraTab.setContent(new AnchorPane(cameraCanvasGroup));
		
		CanvasManager canvasManager = new CanvasManager(cameraCanvasGroup, config, camerasSupervisor, 
				webcamName, shotEntries);
		CameraManager cameraManager = camerasSupervisor.addCameraManager(webcam, canvasManager);
		
		if (config.getRecordingCameras().contains(webcam)) {
			config.registerRecordingCameraManager(cameraManager);
		}
		
		canvasManager.setContextMenu(createContextMenu());
		
		// Show coords of mouse when in canvas during debug mode
		if (config.inDebugMode()) {
			canvasManager.getCanvasGroup().setOnMouseMoved((event) -> {
					shootOFFStage.setTitle(defaultWindowTitle + String.format(" (%.1f, %.1f)", event.getX(), event.getY()));
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
						logger.error("Error loading StreamDebugger FXML file", e);
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
						cameraManager.startRecordingStream(new File(videoName));
					} else {
						recordMenuItem.setText("Start Recording");
						cameraManager.stopRecordingStream();
					}
				});
			
			contextMenu.getItems().add(recordMenuItem);
		}
		
		return contextMenu;
	}
	
	private void findTargets() {
		File targetsFolder = new File(System.getProperty("shootoff.home") + File.separator + "targets");
		
		File[] targetFiles = targetsFolder.listFiles(new FileFilter("target"));
		
		if (targetFiles != null) {
			for (File file : targetFiles) {
				newTarget(file);
			}
		} else {
			logger.error("Failed to find target files because a list of files could not be retrieved");
		}
	}
	
	private void registerTrainingExercises() {
		addTrainingExercise(new ISSFStandardPistol());
		addTrainingExercise(new RandomShoot());
		addTrainingExercise(new ShootForScore());
		addTrainingExercise(new TimedHolsterDrill());
	}
	
	private void addTrainingExercise(TrainingExercise exercise) {
		RadioMenuItem exerciseItem = new RadioMenuItem(exercise.getInfo().getName());
		exerciseItem.setToggleGroup(trainingToggleGroup);
		
		exerciseItem.setOnAction((e) -> {
				try {
					Constructor<?> ctor = exercise.getClass().getConstructor(List.class);
					
					List<Group> knownTargets = new ArrayList<Group>();
					knownTargets.addAll(camerasSupervisor.getTargets());
					
					if (arenaController != null) {
						knownTargets.addAll(arenaController.getCanvasManager().getTargetGroups());
					}
					
					TrainingExercise newExercise = (TrainingExercise)ctor.newInstance(knownTargets);
					((TrainingExerciseBase)newExercise).init(config, camerasSupervisor, shotTimerTable);
					newExercise.init();
					config.setExercise(newExercise);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		
		trainingMenu.getItems().add(exerciseItem);
	}
	
	private void registerProjectorExercises() {
		addProjectorTrainingExercise(new BouncingTargets());
		addProjectorTrainingExercise(new DuelingTree());
		addProjectorTrainingExercise(new ShootDontShoot());
	}
	
	private void addProjectorTrainingExercise(TrainingExercise exercise) {
		RadioMenuItem exerciseItem = new RadioMenuItem(exercise.getInfo().getName());
		exerciseItem.setToggleGroup(trainingToggleGroup);	
		if (arenaController == null) exerciseItem.setDisable(true);
		
		exerciseItem.setOnAction((e) -> {
				try {
					Constructor<?> ctor = exercise.getClass().getConstructor(List.class);
					TrainingExercise newExercise = (TrainingExercise)ctor.newInstance(
							arenaController.getCanvasManager().getTargetGroups());
					((ProjectorTrainingExerciseBase)newExercise).init(config, camerasSupervisor, 
							shotTimerTable, arenaController);
					newExercise.init();
					config.setExercise(newExercise);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		
		trainingMenu.getItems().add(exerciseItem);
		projectorExerciseMenuItems.add(exerciseItem);
	}
	
	@FXML 
	public void clickedNoneExercise(ActionEvent event) {
		config.setExercise(null);
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
	public void toggleSessionRecordingMenuItemClicked(ActionEvent event) {
		if (config.getSessionRecorder().isPresent()) {
			for (CameraManager cm : config.getRecordingManagers()) {
				cm.stopRecordingShots();
			}
			
			SessionIO.saveSession(config.getSessionRecorder().get(), 
					new File(System.getProperty("shootoff.home") + File.separator + "sessions/" + 
							config.getSessionRecorder().get().getSessionName() + ".xml"));
			
			config.setSessionRecorder(null);
			
			toggleSessionRecordingMenuItem.setText("Record Session");
		} else {
			config.setSessionRecorder(new SessionRecorder());
			
			for (CameraManager cm : config.getRecordingManagers()) {
				cm.startRecordingShots();
			}
			
			toggleSessionRecordingMenuItem.setText("Stop Recording");
		}
	}
	
	@FXML
	public void showSessionViewerMenuItemClicked(ActionEvent event) throws IOException {
		showSessionViewerMenuItem.setDisable(true);

		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("com/shootoff/gui/SessionViewer.fxml"));
		loader.load();
			
		sessionViewerStage = new Stage();
			
	    sessionViewerStage.setTitle("Session Viewer");
	    sessionViewerStage.setScene(new Scene(loader.getRoot()));
	    sessionViewerStage.show();
	        
	    SessionViewerController sessionViewerController = (SessionViewerController)loader.getController();
        sessionViewerController.init(config);

        sessionViewerStage.setOnCloseRequest((e) -> { 
        		showSessionViewerMenuItem.setDisable(false);
        	});
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
	        arenaController.init(this, config, camerasSupervisor);
	        arenaController.getCanvasManager().setShowShots(false);
	            
	        arenaStage.setOnCloseRequest((e) -> {
	        		if (config.getExercise().isPresent() && 
	        				config.getExercise().get() instanceof ProjectorTrainingExerciseBase) {
	        			noneTrainingMenuItem.setSelected(true);
	        			noneTrainingMenuItem.fire();
	        		}
	        		toggleArenaShotsMenuItem.setText("Show Shot Markers");
	        		if (isCalibrating) {
	        			stopCalibration();
	        		}
	        		toggleProjectorMenus(true);
	        		startArenaMenuItem.setDisable(false);
	        		arenaCameraManager.setProjectionBounds(null);

	        		// We can't remove this until stopCalibration's runlaters finish
	        		Platform.runLater(() -> {
	        					arenaCameraManager = null;
	        					arenaController.setFeedCanvasManager(null);
	        					arenaController = null;
	        				}
	        		);
	        	});
		}
		
		arenaController.toggleArena();
        arenaController.autoPlaceArena();
        
        toggleArenaCalibrationMenuItem.fire();
    }
	
	private void toggleProjectorMenus(boolean isDisabled) {
		toggleArenaCalibrationMenuItem.setDisable(isDisabled);
		calibrationOptionsMenu.setDisable(isDisabled);
		addArenaTargetMenu.setDisable(isDisabled);
		arenaBackgroundMenu.setDisable(isDisabled);
		coursesMenu.setDisable(isDisabled);
		toggleArenaShotsMenuItem.setDisable(isDisabled);
		
		for (MenuItem m : projectorExerciseMenuItems) m.setDisable(isDisabled);
	}
	
	private void toggleArenaCalibrationMenuItemText()
	{
		if (toggleArenaCalibrationMenuItem.getText().equals("Calibrate"))
			toggleArenaCalibrationMenuItem.setText("Stop Calibrating");
		else
			toggleArenaCalibrationMenuItem.setText("Calibrate");
	}
	
	@FXML
	public void toggleArenaCalibrationClicked(ActionEvent event) {
		if (!isCalibrating)
		{
			enableCalibration();			
		} else
		{
			if (calibrationTarget.isPresent())
				calibrate(calibrationTarget.get().getTargetGroup().getBoundsInParent());
			else
				stopCalibration();
		}		
	}
		
	private void enableCalibration() {
		isCalibrating = true;
		
		toggleArenaCalibrationMenuItemText();

		arenaCameraManager = camerasSupervisor.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
		
		arenaController.setFeedCanvasManager(arenaCameraManager.getCanvasManager());
		
		// Sets calibrating and not detecting
		arenaCameraManager.setCalibrating(true);
        
        arenaController.setTargetsVisible(false);
		arenaCameraManager.setProjectionBounds(null);
		
		if (arenaController.isFullScreen())
		{
			enableAutoCalibration();
		}
		else
		{
			showFullScreenRequest();
		}
		
	}
	
	public boolean isCalibrating()
	{
		return isCalibrating;
	}
	
	
	private Label manualCalibrationRequestMessage = null; 
	private volatile boolean showingManualCalibrationRequestMessage = false;
	private void showManualCalibrationRequestMessage()
	{
		if (showingManualCalibrationRequestMessage)
			return;
		
		showingManualCalibrationRequestMessage = true;
		Platform.runLater(() -> {
			manualCalibrationRequestMessage = arenaCameraManager.getCanvasManager().addDiagnosticMessage("Please manually calibrate the projection region", 20000, Color.ORANGE);
		});
	}
	
	private void removeManualCalibrationRequestMessage()
	{
		logger.trace("removeFullScreenRequest {}", manualCalibrationRequestMessage);
		
		if (showingManualCalibrationRequestMessage)
		{
			showingManualCalibrationRequestMessage = false;
			
			Platform.runLater(() -> {
				arenaCameraManager.getCanvasManager().removeDiagnosticMessage(manualCalibrationRequestMessage);
				manualCalibrationRequestMessage = null;
			});
		}
	}

	private Label fullScreenRequestMessage = null; 
	private volatile boolean showingFullScreenRequestMessage = false;
	private void showFullScreenRequest()
	{
		if (showingFullScreenRequestMessage)
			return;
		
		showingFullScreenRequestMessage = true;
		Platform.runLater(() -> {
			fullScreenRequestMessage = arenaCameraManager.getCanvasManager().addDiagnosticMessage("Please move the arena to your projector and hit F11", Color.YELLOW);
		});
	}
	
	private void removeFullScreenRequest()
	{
		logger.trace("removeFullScreenRequest {}", fullScreenRequestMessage);
		
		if (showingFullScreenRequestMessage)
		{
			showingFullScreenRequestMessage = false;
			
			Platform.runLater(() -> {
				arenaCameraManager.getCanvasManager().removeDiagnosticMessage(fullScreenRequestMessage);
				fullScreenRequestMessage = null;
			});
		}
	}
	
	private Timer autoCalibrationTimer = null;
	private final static int autoCalibrationTime = 10 * 1000;
	private void enableAutoCalibration()
	{
		logger.trace("enableAutoCalibration");
		
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("pattern.png");
		LocatedImage img = new LocatedImage(is, "chessboard");
		arenaController.startCalibration();
        arenaController.setCalibrationMessageVisible(false);
        arenaController.saveCurrentBackground();
        arenaController.setBackground(img);
        
		arenaCameraManager.setController(this);
        arenaCameraManager.enableAutoCalibration();
			
        showAutoCalibrationMessage();
			
        cancelAutoCalibrationTimer();

       	autoCalibrationTimer = new Timer("Auto Calibration");
        
		autoCalibrationTimer.schedule(new TimerTask() {
		    public void run() {
		        Platform.runLater(new Runnable() {
		            public void run() {
		            	if (isCalibrating)
		            	{
		            		arenaCameraManager.disableAutoCalibration();
		            		enableManualCalibration();
		            	}
		            }
		        });

		    }
		}, autoCalibrationTime);
	}
	
	private void cancelAutoCalibrationTimer()
	{
        if (autoCalibrationTimer != null)
        	autoCalibrationTimer.cancel();
        autoCalibrationTimer = null;
	}
	
	private Label autoCalibrationMessage = null; 
	private volatile boolean showingAutoCalibrationMessage = false;
	private void showAutoCalibrationMessage()
	{
		logger.trace("showAutoCalibrationMessage - showingAutoCalibrationMessage {} autoCalibrationMessage {}", showingAutoCalibrationMessage, autoCalibrationMessage);
		
		
		if (showingAutoCalibrationMessage)
			return;
		
		showingAutoCalibrationMessage = true;
		Platform.runLater(() -> {
			autoCalibrationMessage = arenaCameraManager.getCanvasManager().addDiagnosticMessage("Attempting autocalibration", 11000, Color.CYAN);
		});
	}
	private void removeAutoCalibrationMessage()
	{
		logger.trace("removeAutoCalibrationMessage - showingAutoCalibrationMessage {} autoCalibrationMessage {}", showingAutoCalibrationMessage, autoCalibrationMessage);
		
		if (showingAutoCalibrationMessage)
		{
			showingAutoCalibrationMessage = false;
			
			Platform.runLater(() -> {
				logger.trace("removeAutoCalibrationMessage {} ", autoCalibrationMessage);
				arenaCameraManager.getCanvasManager().removeDiagnosticMessage(autoCalibrationMessage);
				autoCalibrationMessage = null;
			});

		}
	}
	
	
	public void setFullScreenStatus(boolean fullScreen) {
		logger.trace("setFullScreenStatus - {} {}", fullScreen, isCalibrating);
		
		if (!isCalibrating)
		{
			enableCalibration();
		}
		else if(isCalibrating && !fullScreen)
		{
			arenaCameraManager.disableAutoCalibration();
			
			removeCalibrationTargetIfPresent();
			
			removeAutoCalibrationMessage();
			
			disableManualCalibration();
			
			showFullScreenRequest();
		}
		else
		{
			removeFullScreenRequest();
			enableAutoCalibration();
		}
	}
	
	
	private void enableManualCalibration()
	{
		logger.trace("enableManualCalibration");
		
		final int DEFAULT_DIM = 75;
		final int DEFAULT_POS = 150;
		
		removeAutoCalibrationMessage();
		
		showManualCalibrationRequestMessage();
		
		if (!calibrationTarget.isPresent()) {
			createCalibrationTarget(DEFAULT_DIM, DEFAULT_DIM, DEFAULT_POS, DEFAULT_POS);
		} else {
			arenaCameraManager.getCanvasManager().addTarget(calibrationTarget.get());
		}
	}
	
	private void disableManualCalibration()
	{
		removeCalibrationTargetIfPresent();
		
		removeManualCalibrationRequestMessage();
	}
	
	public void createCalibrationTarget(double x, double y, double width, double height)
	{
		RectangleRegion calibrationRectangle =  new RectangleRegion(x, y, 
				width, height);
		calibrationRectangle.setFill(Color.PURPLE);
		calibrationRectangle.setOpacity(TargetIO.DEFAULT_OPACITY);

		Group calibrationGroup = new Group();
		calibrationGroup.setOnMouseClicked((e) -> { calibrationGroup.requestFocus(); });
		calibrationGroup.getChildren().add(calibrationRectangle);
		
		calibrationTarget = Optional.of(arenaCameraManager.getCanvasManager().addTarget(null, calibrationGroup, false));
				calibrationTarget.get().setKeepInBounds(true);
	}
	
	public void calibrate(Bounds bounds)
	{
		removeCalibrationTargetIfPresent();
		
		createCalibrationTarget(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
		
		configureArenaCamera(getSelectedCalibrationOption(), bounds);
		
		stopCalibration();
	}
	

	
	private void stopCalibration() {
		isCalibrating = false;
		
		arenaCameraManager.disableAutoCalibration();
		
		cancelAutoCalibrationTimer();
		
		toggleArenaCalibrationMenuItemText();
		
		removeFullScreenRequest();
		removeAutoCalibrationMessage();
		removeManualCalibrationRequestMessage();
		removeCalibrationTargetIfPresent();

		arenaController.calibrated();
		
		arenaCameraManager.setCalibrating(false);
		
		// We disable shot detection briefly because the pattern going away can cause false shots
		// This statement applies to all the cam feeds rather than just the arena.  I don't think that should
		// be a problem?
		disableShotDetectionForPeriod(400);
	}
	
	private void removeCalibrationTargetIfPresent()
	{
		if (calibrationTarget.isPresent()) 
			arenaCameraManager.getCanvasManager().removeTarget(calibrationTarget.get());
	}
	
	private void configureArenaCamera(CalibrationOption option, Bounds bounds) {
		arenaCameraManager.getCanvasManager().setProjectorArena(arenaController, bounds);
		configureArenaCamera(option);
		arenaCameraManager.setProjectionBounds(bounds);
	}
	
	private void configureArenaCamera(CalibrationOption option) {
		arenaCameraManager.setCropFeedToProjection(CalibrationOption.CROP.equals(option));
		arenaCameraManager.setLimitDetectProjection(CalibrationOption.ONLY_IN_BOUNDS.equals(option));
	}
	
	private void initDefaultBackgrounds() {
		addDefaultBackground("Indoor Range", "/arena/backgrounds/indoor_range.gif");
		addDefaultBackground("Outdoor Range", "/arena/backgrounds/outdoor_range.gif");
		addDefaultBackground("Kiang West Savanna", "/arena/backgrounds/kiang_west_savanna.gif");
	}
	
	private void addDefaultBackground(String menuName, String resourceName) {
		MenuItem backgroundMenuItem = new MenuItem(menuName);
		
		backgroundMenuItem.setOnAction((e) -> {
				InputStream is = this.getClass().getResourceAsStream(resourceName);
				LocatedImage img = new LocatedImage(is, resourceName);
				arenaController.setBackground(img);
			});
		
		arenaBackgroundMenu.getItems().add(backgroundMenuItem);
	}

	@FXML
	public void removeArenaBackgroundMenuItemClicked(ActionEvent event) {
		arenaController.setBackground(null);
	}
	
	@FXML
	public void openArenaBackgroundMenuItemClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Arena Background");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif")
            );
        
		File backgroundFile = fileChooser.showOpenDialog(shootOFFStage);
		
		if (backgroundFile != null) {
			LocatedImage img = new LocatedImage(backgroundFile.toURI().toString());
			arenaController.setBackground(img);
		}
	}
	
	@FXML
	public void saveCourseMenuItemClicked(ActionEvent event) {
		File coursesDir = new File(System.getProperty("shootoff.courses"));
		
		if (!coursesDir.exists()) {
			if (!coursesDir.mkdirs()) {
				logger.error("Courses folder does not exist and cannot be created: {}", coursesDir.getAbsolutePath());
			}
		}
		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Course");
		fileChooser.setInitialDirectory(coursesDir);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Course File (*.course)", "*.course")
            );
        
		File courseFile = fileChooser.showSaveDialog(shootOFFStage);
		
		if (courseFile != null) {
			String path = courseFile.getPath();
			if (!path.endsWith(".course")) path += ".course";
			
			courseFile = new File(path);
			
			CourseIO.saveCourse(arenaController, courseFile);
		}
	}
	
	@FXML
	public void loadCourseMenuItemClicked(ActionEvent event) {
		File coursesDir = new File(System.getProperty("shootoff.courses"));
		
		if (!coursesDir.exists()) {
			if (!coursesDir.mkdirs()) {
				logger.error("Courses folder does not exist and cannot be created: {}", coursesDir.getAbsolutePath());
			}
		}
		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Course");
		fileChooser.setInitialDirectory(coursesDir);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Course File (*.course)", "*.course")
            );
        
		File courseFile = fileChooser.showOpenDialog(shootOFFStage);
		
		if (courseFile != null) {
			Optional<Course> course = CourseIO.loadCourse(arenaController, courseFile);
			
			if (course.isPresent()) {
				arenaController.setCourse(course.get());
			}
		}
	}
	
	@FXML
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
		resetShotsAndTargets();
	}
	
	
	public void resetShotsAndTargets()
	{
		camerasSupervisor.reset();
		
		if (config.getExercise().isPresent()) {
			List<Group> knownTargets = new ArrayList<Group>();
			knownTargets.addAll(camerasSupervisor.getTargets());
			
			if (arenaController != null) {
				knownTargets.addAll(arenaController.getCanvasManager().getTargetGroups());
			}
			
			config.getExercise().get().reset(knownTargets);
		}
		
		disableShotDetectionForPeriod(500);
	}
	
	private Timer disableShotDetectionTimer = null;
	private volatile boolean disableShotDetectionTimerEnabled = false;
	
	// Technically the period could be shorter than the previous call
	// and we don't handle that right now.  I'm not too worried about that
	// because I don't think the periods are going to be vastly different
	// This is only intended for very short disablement periods
	public void disableShotDetectionForPeriod(int msPeriod)
	{
		if (disableShotDetectionTimerEnabled)
		{
			disableShotDetectionTimer.cancel();
		}
		disableShotDetectionTimerEnabled = true;
		
		camerasSupervisor.setDetectingAll(false);
		
		disableShotDetectionTimer = new Timer("Disable Shot Detect");
		disableShotDetectionTimer.schedule(new TimerTask() {
		    public void run() {
		         Platform.runLater(new Runnable() {
		            public void run() {
		            	if (!isCalibrating)
		            	{
		            		camerasSupervisor.setDetectingAll(true);
		            	}
		            	else
		            	{
		            		logger.info("disableShotDetectionTimer did not re-enable shot detection, isCalibrating is true");
		            	}
		            	disableShotDetectionTimerEnabled = false;
		            }
		        });
		    }
		}, msPeriod);
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
				logger.error("Error saving feed image", e);
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
