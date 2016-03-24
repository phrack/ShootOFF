/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openimaj.util.parallel.GlobalExecutorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Main;
import com.shootoff.camera.Camera;
import com.shootoff.camera.CameraErrorView;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.arenamask.ArenaMaskManager;
import com.shootoff.config.Configuration;
import com.shootoff.courses.Course;
import com.shootoff.courses.io.CourseIO;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.ShotSectorPane;
import com.shootoff.gui.TargetListener;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.plugins.engine.PluginEngine;
import com.shootoff.plugins.engine.PluginListener;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.io.SessionIO;
import com.shootoff.targets.TargetManager;
import com.shootoff.targets.TargetRegion;
import com.shootoff.util.TimerPool;

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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import marytts.util.io.FileFilter;

public class ShootOFFController implements CameraConfigListener, CameraErrorView, TargetListener, TargetManager,
		PluginListener, CalibrationConfigurator {
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
	@FXML private MenuItem clearArenaTargetsMenuItem;
	@FXML private Menu arenaBackgroundMenu;
	@FXML private Menu coursesMenu;
	@FXML private MenuItem toggleArenaShotsMenuItem;
	@FXML private GridPane buttonsGridPane;

	private String defaultWindowTitle;
	private CamerasSupervisor camerasSupervisor;
	private Configuration config;
	private PluginEngine pluginEngine;
	private Stage pluginManagerStage = null;
	private static final Logger logger = LoggerFactory.getLogger(ShootOFFController.class);
	private final ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
	private final List<Stage> streamDebuggerStages = new ArrayList<Stage>();

	private ProjectorArenaController arenaController;
	private Optional<CalibrationManager> calibrationManager = Optional.empty();
	private List<MenuItem> projectorExerciseMenuItems = new ArrayList<MenuItem>();

	private Stage sessionViewerStage;

	public void init(Configuration config, PluginEngine pluginEngine) {
		this.config = config;
		this.camerasSupervisor = new CamerasSupervisor(config);
		this.pluginEngine = pluginEngine;

		findTargets();
		initDefaultBackgrounds();
		pluginEngine.startWatching();

		shootOFFStage = (Stage) mainMenu.getScene().getWindow();
		this.defaultWindowTitle = shootOFFStage.getTitle();
		shootOFFStage.getIcons().addAll(
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_16x16.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_32x32.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_48x48.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_64x64.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_128x128.png")),
				new Image(ShootOFFController.class.getResourceAsStream("/images/icon_256x256.png")));

		shootOFFStage.setOnCloseRequest((value) -> {
			close();
		});

		if (config.getWebcams().isEmpty()) {
			Optional<Camera> defaultCamera = Camera.getDefault();
			if (defaultCamera.isPresent()) {
				if (!addCameraTab("Default", defaultCamera.get())) cameraLockFailure(defaultCamera.get(), true);
			} else {
				Main.closeNoCamera();
			}
		} else {
			addConfiguredCameras();
		}

		TableColumn<ShotEntry, String> timeCol = new TableColumn<ShotEntry, String>("Time");
		timeCol.setMinWidth(85);
		timeCol.setCellValueFactory(new PropertyValueFactory<ShotEntry, String>("timestamp"));

		TableColumn<ShotEntry, ShotEntry.SplitData> splitCol = new TableColumn<ShotEntry, ShotEntry.SplitData>("Split");
		splitCol.setMinWidth(85);
		splitCol.setCellValueFactory(new PropertyValueFactory<ShotEntry, ShotEntry.SplitData>("split"));

		TableColumn<ShotEntry, String> laserCol = new TableColumn<ShotEntry, String>("Laser");
		laserCol.setMinWidth(85);
		laserCol.setCellValueFactory(new PropertyValueFactory<ShotEntry, String>("color"));

		shotEntries.addListener(new ListChangeListener<ShotEntry>() {
			@Override
			public void onChanged(Change<? extends ShotEntry> change) {
				change.next();
				if (change.getAddedSize() < 1) return;
				Platform.runLater(() -> {
					final int size = shotTimerTable.getItems().size();
					if (size > 0) shotTimerTable.scrollTo(size - 1);
				});
			}
		});

		calibrationToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle newToggle) {
				if (newToggle == null) return;

				if (calibrationManager.isPresent())
					calibrationManager.get().configureArenaCamera(getSelectedCalibrationOption());
			}
		});

		shotTimerTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<ShotEntry>() {
			@Override
			public void onChanged(Change<? extends ShotEntry> change) {
				while (change.next()) {
					for (ShotEntry unselected : change.getRemoved()) {
						unselected.getShot().getMarker().setFill(unselected.getShot().getColor());
					}

					for (ShotEntry selected : change.getAddedSubList()) {
						selected.getShot().getMarker().setFill(TargetRegion.SELECTED_STROKE_COLOR);

						// Move all selected shots to top the of their z-stack
						// to ensure visibility
						for (CameraView cv : camerasSupervisor.getCameraViews()) {
							CanvasManager cm = (CanvasManager) cv;

							Shape marker = selected.getShot().getMarker();
							if (cm.getCanvasGroup().getChildren()
									.indexOf(marker) < cm.getCanvasGroup().getChildren().size() - 1) {
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
			protected void updateItem(ShotEntry item, boolean empty) {
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

	private void close() {
		shootOFFStage.close();
		camerasSupervisor.closeAll();
		pluginEngine.stopWatching();

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

		TimerPool.close();
		GlobalExecutorPool.getPool().shutdownNow();

		if (!config.getVideoPlayers().isEmpty()) {
			for (VideoPlayerController videoPlayer : config.getVideoPlayers()) {
				videoPlayer.getStage().close();
			}
		}

		if (!config.inDebugMode()) Main.forceClose(0);
	}

	@Override
	public CalibrationOption getSelectedCalibrationOption() {
		if (calibrationToggleGroup == null) return CalibrationOption.EVERYWHERE;

		Toggle selectedToggle = calibrationToggleGroup.getSelectedToggle();
		if (selectedToggle != null && selectedToggle instanceof RadioMenuItem) {
			RadioMenuItem selectedOption = (RadioMenuItem) calibrationToggleGroup.getSelectedToggle();

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

	public GridPane getButtonsPane() {
		return buttonsGridPane;
	}

	public TableView<ShotEntry> getShotEntryTable() {
		return shotTimerTable;
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
			Optional<Camera> defaultCam = Camera.getDefault();

			if (defaultCam.isPresent()) {
				if (!addCameraTab("Default", defaultCam.get())) cameraLockFailure(defaultCam.get(), true);
			} else {
				logger.error("Default camera was not fetched after clearing camera settings!");
				Main.closeNoCamera();
			}
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
					+ "used by another program or it is an IPCam with the wrong credentials. This "
					+ "is the only configured camera, thus ShootOFF must close.";
		} else {
			messageFormat = "Cannot open the webcam %s. It is being "
					+ "used by another program, it is an IPCam with the wrong credentials, or you "
					+ "have ShootOFF open more than once.";
		}

		Optional<String> webcamName = config.getWebcamsUserName(webcam);

		cameraAlert.setContentText(
				String.format(messageFormat, webcamName.isPresent() ? webcamName.get() : webcam.getName()));

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

		CanvasManager canvasManager = new CanvasManager(cameraCanvasGroup, config, camerasSupervisor, webcamName,
				shotEntries);
		CameraManager cameraManager = camerasSupervisor.addCameraManager(webcam, this, canvasManager);

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
			AnchorPane tabAnchor = (AnchorPane) cameraTabPane.getSelectionModel().getSelectedItem().getContent();

			// Only add the pane if it isn't already open
			boolean hasPane = false;
			for (Node node : tabAnchor.getChildren()) {
				if (node instanceof ShotSectorPane) {
					hasPane = true;
					break;
				}
			}

			if (!hasPane) {
				CameraManager cameraManager = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
				new ShotSectorPane(tabAnchor, cameraManager);
			}
		});

		contextMenu.getItems().add(toggleDetectionSectors);

		if (config.inDebugMode()) {
			MenuItem startStreamDebuggerMenuItem = new MenuItem("Start Stream Debugger");

			startStreamDebuggerMenuItem.setOnAction((event) -> {
				FXMLLoader loader = new FXMLLoader(
						getClass().getClassLoader().getResource("com/shootoff/gui/StreamDebugger.fxml"));
				try {
					loader.load();
				} catch (Exception e) {
					logger.error("Error loading StreamDebugger FXML file", e);
				}

				Stage streamDebuggerStage = new Stage();
				streamDebuggerStages.add(streamDebuggerStage);

				String tabName = cameraTabPane.getSelectionModel().getSelectedItem().getText();
				streamDebuggerStage.setTitle(String.format("Stream Debugger -- %s", tabName));
				streamDebuggerStage.setScene(new Scene(loader.getRoot()));
				streamDebuggerStage.show();
				CameraManager cameraManager = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
				((StreamDebuggerController) loader.getController()).init(cameraManager);

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
				CameraManager cameraManager = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());

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
			Arrays.sort(targetFiles);
			for (File file : targetFiles) {
				newTarget(file);
			}
		} else {
			logger.error("Failed to find target files because a list of files could not be retrieved");
		}
	}

	@Override
	public List<Group> getTargets() {
		final List<Group> targets = new ArrayList<Group>();

		for (final CameraManager manager : camerasSupervisor.getCameraManagers()) {
			targets.addAll(((CanvasManager) manager.getCameraView()).getTargetGroups());
		}

		return targets;
	}

	@Override
	public void registerExercise(TrainingExercise exercise) {
		RadioMenuItem exerciseItem = new RadioMenuItem(exercise.getInfo().getName());
		exerciseItem.setToggleGroup(trainingToggleGroup);

		exerciseItem.setOnAction((e) -> {
			try {
				Constructor<?> ctor = exercise.getClass().getConstructor(List.class);

				List<Group> knownTargets = new ArrayList<Group>();
				knownTargets.addAll(getTargets());

				if (arenaController != null) {
					knownTargets.addAll(arenaController.getCanvasManager().getTargetGroups());
				}

				TrainingExercise newExercise = (TrainingExercise) ctor.newInstance(knownTargets);
				config.setExercise(newExercise);
				((TrainingExerciseBase) newExercise).init(config, camerasSupervisor, this);
				newExercise.init();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		trainingMenu.getItems().add(exerciseItem);
	}

	@Override
	public void registerProjectorExercise(TrainingExercise exercise) {
		RadioMenuItem exerciseItem = new RadioMenuItem(exercise.getInfo().getName());
		exerciseItem.setToggleGroup(trainingToggleGroup);
		if (arenaController == null) exerciseItem.setDisable(true);

		exerciseItem.setOnAction((e) -> {
			try {
				Constructor<?> ctor = exercise.getClass().getConstructor(List.class);
				TrainingExercise newExercise = (TrainingExercise) ctor
						.newInstance(arenaController.getCanvasManager().getTargetGroups());
				((ProjectorTrainingExerciseBase) newExercise).init(config, camerasSupervisor, this, arenaController);
				newExercise.init();
				config.setExercise(newExercise);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		trainingMenu.getItems().add(exerciseItem);
		projectorExerciseMenuItems.add(exerciseItem);
	}

	@Override
	public void unregisterExercise(TrainingExercise exercise) {
		Platform.runLater(() -> {
			// If we just unregistered the exercise that is on, disable it
			if (config.getExercise().isPresent() && config.getExercise().get().getInfo().equals(exercise.getInfo())) {
				noneTrainingMenuItem.fire();
			}

			Iterator<MenuItem> it = trainingMenu.getItems().iterator();

			while (it.hasNext()) {
				MenuItem m = it.next();

				if (m.getText() != null && m.getText().equals(exercise.getInfo().getName())) {
					it.remove();
				}
			}
		});
	}

	@FXML
	public void clickedNoneExercise(ActionEvent event) {
		config.setExercise(null);
		trainingToggleGroup.selectToggle(noneTrainingMenuItem);
	}

	@FXML
	public void preferencesClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/Preferences.fxml"));
		loader.load();

		Stage preferencesStage = new Stage();

		preferencesStage.initOwner(shootOFFStage);
		preferencesStage.initModality(Modality.WINDOW_MODAL);
		preferencesStage.setTitle("Preferences");
		preferencesStage.setScene(new Scene(loader.getRoot()));
		preferencesStage.show();
		((PreferencesController) loader.getController()).setConfig(config, this);
	}

	@FXML
	public void getExercisesMenuItemClicked(ActionEvent event) throws IOException {
		if (pluginManagerStage == null) {
			FXMLLoader loader = new FXMLLoader(
					getClass().getClassLoader().getResource("com/shootoff/gui/PluginManager.fxml"));
			loader.load();

			pluginManagerStage = new Stage();

			pluginManagerStage.initOwner(shootOFFStage);
			pluginManagerStage.setTitle("Exercise Manager");
			pluginManagerStage.setScene(new Scene(loader.getRoot()));
			pluginManagerStage.show();
			pluginManagerStage.setOnCloseRequest((e) -> {
				pluginManagerStage = null;
			});
			((PluginManagerController) loader.getController()).init(pluginEngine);
		} else {
			pluginManagerStage.show();
			pluginManagerStage.toFront();
		}
	}

	@FXML
	public void toggleSessionRecordingMenuItemClicked(ActionEvent event) {
		if (config.getSessionRecorder().isPresent()) {
			for (CameraManager cm : config.getRecordingManagers()) {
				cm.stopRecordingShots();
			}

			SessionIO.saveSession(config.getSessionRecorder().get(), new File(System.getProperty("shootoff.home")
					+ File.separator + "sessions/" + config.getSessionRecorder().get().getSessionName() + ".xml"));

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

		FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/SessionViewer.fxml"));
		loader.load();

		sessionViewerStage = new Stage();

		sessionViewerStage.setTitle("Session Viewer");
		sessionViewerStage.setScene(new Scene(loader.getRoot()));
		sessionViewerStage.show();

		SessionViewerController sessionViewerController = (SessionViewerController) loader.getController();
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
			FXMLLoader loader = new FXMLLoader(
					getClass().getClassLoader().getResource("com/shootoff/gui/ProjectorArena.fxml"));
			loader.load();

			Stage arenaStage = new Stage();

			arenaStage.setTitle("Projector Arena");
			arenaStage.setScene(new Scene(loader.getRoot()));

			arenaController = (ProjectorArenaController) loader.getController();
			CameraManager calibratingCameraManager = camerasSupervisor
					.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
			arenaController.init(this.getStage(), config, camerasSupervisor);
			calibrationManager = Optional.of(new CalibrationManager(this, calibratingCameraManager, arenaController));
			arenaController.setCalibrationManager(calibrationManager.get());
			arenaController.getCanvasManager().setShowShots(false);

			arenaStage.setOnCloseRequest((e) -> {
				if (config.getExercise().isPresent()
						&& config.getExercise().get() instanceof ProjectorTrainingExerciseBase) {
					noneTrainingMenuItem.setSelected(true);
					noneTrainingMenuItem.fire();
				}
				toggleArenaShotsMenuItem.setText("Show Shot Markers");
				if (calibrationManager.isPresent()) {
					if (calibrationManager.get().isCalibrating()) {
						calibrationManager.get().stopCalibration();
					} else {
						calibrationManager.get().arenaClosing();
					}
				}
				toggleProjectorMenus(true);
				startArenaMenuItem.setDisable(false);

				// We can't remove this until stopCalibration's runlaters finish
				Platform.runLater(() -> {
					arenaController.setFeedCanvasManager(null);
					arenaController = null;
				});
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
		clearArenaTargetsMenuItem.setDisable(isDisabled);
		arenaBackgroundMenu.setDisable(isDisabled);
		coursesMenu.setDisable(isDisabled);
		toggleArenaShotsMenuItem.setDisable(isDisabled);

		for (MenuItem m : projectorExerciseMenuItems)
			m.setDisable(isDisabled);
	}

	@Override
	public void toggleCalibrating() {
		if (toggleArenaCalibrationMenuItem.getText().equals("Calibrate"))
			toggleArenaCalibrationMenuItem.setText("Stop Calibrating");
		else
			toggleArenaCalibrationMenuItem.setText("Calibrate");
	}

	@FXML
	public void toggleArenaCalibrationClicked(ActionEvent event) {
		if (!calibrationManager.isPresent()) return;

		if (!calibrationManager.get().isCalibrating()) {
			calibrationManager.get().enableCalibration();
		} else {
			calibrationManager.get().stopCalibration();
		}
	}

	private void initDefaultBackgrounds() {
		addDefaultBackground("Hickok45 Autumn", "/arena/backgrounds/hickok45_autumn.gif");
		addDefaultBackground("Hickok45 Summer", "/arena/backgrounds/hickok45_summer.gif");
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
	public void clearArenaTargetsMenuItemClicked(ActionEvent event) {
		arenaController.getCanvasManager().clearTargets();
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
				new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"));

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
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Course File (*.course)", "*.course"));

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
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Course File (*.course)", "*.course"));

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
		close();
	}

	@FXML
	public void hideTargetsClicked(ActionEvent event) {
		MenuItem hideTargetMenuItem = (MenuItem) event.getSource();

		if (hideTargetMenuItem.getText().equals("Hide Targets")) {
			hideTargetMenuItem.setText("Show Targets");

			for (Group target : getTargets()) {
				target.setVisible(false);
			}
		} else {
			hideTargetMenuItem.setText("Hide Targets");

			for (Group target : getTargets()) {
				target.setVisible(true);
			}
		}
	}

	@FXML
	public void createTargetMenuClicked(ActionEvent event) throws IOException {
		FXMLLoader loader = createPreferencesStage();

		CameraManager currentCamera = camerasSupervisor
				.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
		Image currentFrame = currentCamera.getCurrentFrame();
		((TargetEditorController) loader.getController()).init(currentFrame, this);
	}

	private FXMLLoader createPreferencesStage() throws IOException {
		FXMLLoader loader = new FXMLLoader(
				getClass().getClassLoader().getResource("com/shootoff/gui/TargetEditor.fxml"));
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

	public void resetShotsAndTargets() {
		camerasSupervisor.reset();

		if (config.getExercise().isPresent()) {
			List<Group> knownTargets = new ArrayList<Group>();
			knownTargets.addAll(getTargets());

			if (arenaController != null) {
				knownTargets.addAll(arenaController.getCanvasManager().getTargetGroups());
			}

			config.getExercise().get().reset(knownTargets);
		}

		disableShotDetection(600);
	}

	// Technically the period could be shorter than the previous call
	// and we don't handle that right now. I'm not too worried about that
	// because I don't think the periods are going to be vastly different
	// This is only intended for very short disablement periods
	@Override
	public void disableShotDetection(int msDuration) {
		// Don't disable the cameras if they are already disabled (e.g. because
		// a training protocol paused shot detection)
		if (!camerasSupervisor.areDetecting()) return;

		camerasSupervisor.setDetectingAll(false);

		Runnable restartDetection = () -> {
			if (!calibrationManager.isPresent()
					|| (calibrationManager.isPresent() && !calibrationManager.get().isCalibrating())) {
				camerasSupervisor.setDetectingAll(true);
			} else {
				logger.info("disableShotDetectionTimer did not re-enable shot detection, isCalibrating is true");
			}
		};

		TimerPool.schedule(restartDetection, msDuration);
	}

	@FXML
	public void saveFeedClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Feed Image");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"),
				new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png"));
		File feedFile = fileChooser.showSaveDialog(shootOFFStage);

		if (feedFile != null) {
			String extension = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).substring(2);
			File imageFile = new File(feedFile.getPath() + "." + extension);
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

		String targetName = targetPath
				.substring(targetPath.lastIndexOf(File.separator) + 1, targetPath.lastIndexOf('.')).replace("_", " ");

		MenuItem addTargetItem = new MenuItem(targetName);
		addTargetItem.setMnemonicParsing(false);

		addTargetItem.setOnAction((e) -> {
			camerasSupervisor.getCameraView(cameraTabPane.getSelectionModel().getSelectedIndex()).addTarget(path);
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

				CameraManager currentCamera = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
				Image currentFrame = currentCamera.getCurrentFrame();
				((TargetEditorController) loader.getController()).init(currentFrame, this, path);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		addTargetMenu.getItems().add(addTargetItem);
		addArenaTargetMenu.getItems().add(addProjectorTargetItem);
		editTargetMenu.getItems().add(editTargetItem);
	}

	public void setArenaMaskManager(ArenaMaskManager arenaMaskManager) {
		arenaController.setArenaMaskManager(arenaMaskManager);
	}

	@Override
	public void showMissingCameraError(Camera webcam) {
		Platform.runLater(() -> {
			Alert cameraAlert = new Alert(AlertType.ERROR);

			Optional<String> cameraName = config.getWebcamsUserName(webcam);
			String messageFormat = "ShootOFF can no longer communicate with the webcam %s. Was it unplugged?";
			String message;
			if (cameraName.isPresent()) {
				message = String.format(messageFormat, cameraName.get());
			} else {
				message = String.format(messageFormat, webcam.getName());
			}

			cameraAlert.setTitle("Webcam Missing");
			cameraAlert.setHeaderText("Cannot Communicate with Camera!");
			cameraAlert.setResizable(true);
			cameraAlert.setContentText(message);
			cameraAlert.initOwner(getStage());
			cameraAlert.show();
		});
	}

	@Override
	public void showFPSWarning(Camera webcam, double fps) {
		Platform.runLater(() -> {
			Alert cameraAlert = new Alert(AlertType.WARNING);

			Optional<String> cameraName = config.getWebcamsUserName(webcam);
			String messageFormat = "The FPS from %s has dropped to %f, which is too low for reliable shot detection. Some"
					+ " shots may be missed. You may be able to raise the FPS by closing other applications.";
			String message;
			if (cameraName.isPresent()) {
				message = String.format(messageFormat, cameraName.get(), fps);
			} else {
				message = String.format(messageFormat, webcam.getName(), fps);
			}

			cameraAlert.setTitle("Webcam FPS Too Low");
			cameraAlert.setHeaderText("Webcam FPS is too low!");
			cameraAlert.setResizable(true);
			cameraAlert.setContentText(message);
			cameraAlert.initOwner(getStage());
			cameraAlert.show();
		});
	}

	@Override
	public void showBrightnessWarning(Camera webcam) {
		Platform.runLater(() -> {
			Alert brightnessAlert = new Alert(AlertType.WARNING);

			Optional<String> cameraName = config.getWebcamsUserName(webcam);
			String messageFormat = "The camera %s is streaming frames that are very bright. "
					+ " This will increase the odds of shots falsely being detected."
					+ " For best results, please do any mix of the following:\n\n"
					+ "-Turn off auto white balance and auto focus on your webcam and reduce the brightness\n"
					+ "-Remove any bright light sources in the camera's view\n"
					+ "-Turn down your projector's brightness and contrast";
			String message;
			if (cameraName.isPresent()) {
				message = String.format(messageFormat, cameraName.get());
			} else {
				message = String.format(messageFormat, webcam.getName());
			}

			brightnessAlert.setTitle("Conditions Very Bright");
			brightnessAlert.setHeaderText("Webcam detected very bright conditions!");
			brightnessAlert.setResizable(true);
			brightnessAlert.setContentText(message);
			brightnessAlert.initOwner(getStage());
			brightnessAlert.show();
		});
	}
}
