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

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.openimaj.util.parallel.GlobalExecutorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Closeable;
import com.shootoff.Main;
import com.shootoff.camera.CameraErrorView;
import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CameraConfigListener;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.Resetter;
import com.shootoff.gui.ShotEntry;
import com.shootoff.gui.pane.ExerciseSlide;
import com.shootoff.gui.pane.FileSlide;
import com.shootoff.gui.pane.ProjectorSlide;
import com.shootoff.gui.pane.ShotSectorPane;
import com.shootoff.gui.pane.TargetSlide;
import com.shootoff.plugins.ExerciseMetadata;
import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.plugins.TrainingExerciseView;
import com.shootoff.plugins.engine.Plugin;
import com.shootoff.plugins.engine.PluginEngine;
import com.shootoff.targets.CameraViews;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.util.SystemInfo;
import com.shootoff.util.TimerPool;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Shape;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ShootOFFController implements CameraConfigListener, CameraErrorView, CameraViews, Closeable, Resetter,
		TrainingExerciseView, ExerciseListener {
	private Stage shootOFFStage;
	@FXML private VBox shootOffContainer;
	@FXML private HBox controlsContainer;
	@FXML private VBox bodyContainer;
	@FXML private TabPane cameraTabPane;
	@FXML private TableView<ShotEntry> shotTimerTable;
	@FXML private VBox buttonsContainer;
	@FXML private Pane trainingExerciseContainer;
	@FXML private ScrollPane trainingExerciseScrollPane;

	private TargetSlide targetPane;
	private ExerciseSlide exerciseSlide;
	private ProjectorSlide projectorSlide;

	private String defaultWindowTitle;
	private CamerasSupervisor camerasSupervisor;
	private Configuration config;
	private PluginEngine pluginEngine;
	private static final Logger logger = LoggerFactory.getLogger(ShootOFFController.class);
	private final ObservableList<ShotEntry> shotEntries = FXCollections.observableArrayList();
	private final List<Stage> streamDebuggerStages = new ArrayList<>();

	static public double getDpiScaleFactorForScreen() {
		// http://news.kynosarges.org/2015/06/29/javafx-dpi-scaling-fixed/
		// Number of actual horizontal lines (768p)
		final double trueHorizontalLines = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		// Number of scaled horizontal lines. (384p for 200%)
		final double scaledHorizontalLines = Screen.getPrimary().getBounds().getHeight();
		// DPI scale factor.
		final double dpiScaleFactor = trueHorizontalLines / scaledHorizontalLines;

		return dpiScaleFactor;
	}

	@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
	public void init(Configuration config) throws IOException {
		this.config = config;
		camerasSupervisor = new CamerasSupervisor(config);

		shootOFFStage = (Stage) controlsContainer.getScene().getWindow();

		shootOFFStage.setOnShown((event) -> {
			final ObservableList<Screen> shootOffScreens = Screen.getScreensForRectangle(shootOFFStage.getX(),
					shootOFFStage.getY(), 1, 1);

			final Screen shootOffScreen;

			// Automatically maximize ShootOFF on smaller screens
			if (shootOffScreens.size() > 0) {
				shootOffScreen = shootOffScreens.get(0);
			} else {
				shootOffScreen = Screen.getPrimary();
			}

			if (shootOffScreen.getBounds().getWidth() <= 1280 || shootOffScreen.getBounds().getHeight() <= 800) {
				shootOFFStage.setMaximized(true);

				// If the screen has an unusually short display for
				// a modern system, add a scroll bar to the body
				if (shootOffScreen.getBounds().getHeight() < 800) {
					shootOffContainer.getChildren().remove(bodyContainer);
					shootOffContainer.getChildren().add(new ScrollPane(bodyContainer));
				}
			}
		});

		targetPane = new TargetSlide(controlsContainer, bodyContainer, this);
		exerciseSlide = new ExerciseSlide(controlsContainer, bodyContainer, this);
		projectorSlide = new ProjectorSlide(controlsContainer, bodyContainer, this, shootOFFStage,
				trainingExerciseContainer, this, exerciseSlide);

		pluginEngine = new PluginEngine(exerciseSlide);
		pluginEngine.startWatching();

		defaultWindowTitle = shootOFFStage.getTitle();
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

		// This delay is to give the window time to fully show for the first
		// time. If we don't do this, the newValues in the listeners will end
		// up very high initially, making the controls too big. Using
		// Stage.setOnShow doesn't solve the problem because the size will
		// still change after the first call to the listener.
		TimerPool.schedule(() -> {
			shootOFFStage.widthProperty().addListener((observable, oldValue, newValue) -> {
				if (!shootOFFStage.isShowing()) return;

				final double d = newValue.doubleValue() - oldValue.doubleValue();

				cameraTabPane.setPrefWidth(cameraTabPane.getLayoutBounds().getWidth() + d * .35);
				shotTimerTable.setPrefWidth(shotTimerTable.getLayoutBounds().getWidth() + d * .65);
			});

			shootOFFStage.heightProperty().addListener((observable, oldValue, newValue) -> {
				if (!shootOFFStage.isShowing()) return;

				final double d = newValue.doubleValue() - oldValue.doubleValue();

				cameraTabPane.setPrefHeight(cameraTabPane.getLayoutBounds().getHeight() + d * .25);
				trainingExerciseScrollPane
						.setPrefHeight(trainingExerciseScrollPane.getLayoutBounds().getHeight() + d * .75);
			});
		}, 2000);

		addCameraTabs();

		final TableColumn<ShotEntry, String> timeCol = new TableColumn<>("Time");
		timeCol.setMinWidth(85);
		timeCol.setCellValueFactory(new PropertyValueFactory<ShotEntry, String>("timestamp"));

		final TableColumn<ShotEntry, ShotEntry.SplitData> splitCol = new TableColumn<>("Split");
		splitCol.setMinWidth(85);
		splitCol.setCellValueFactory(new PropertyValueFactory<ShotEntry, ShotEntry.SplitData>("split"));

		final TableColumn<ShotEntry, String> laserCol = new TableColumn<>("Laser");
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

		shotTimerTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<ShotEntry>() {
			@Override
			public void onChanged(Change<? extends ShotEntry> change) {
				while (change.next()) {
					for (final ShotEntry unselected : change.getRemoved()) {
						unselected.getShot().getMarker().setFill(unselected.getShot().getPaintColor());
						if (unselected.getShot().getMirroredShot().isPresent()) {
							((DisplayShot) unselected.getShot().getMirroredShot().get()).getMarker()
									.setFill(unselected.getShot().getPaintColor());
						}
					}

					for (final ShotEntry selected : change.getAddedSubList()) {
						if (selected == null) continue;

						selected.getShot().getMarker().setFill(TargetRegion.SELECTED_STROKE_COLOR);

						if (selected.getShot().getMirroredShot().isPresent()) {
							((DisplayShot) selected.getShot().getMirroredShot().get()).getMarker()
									.setFill(TargetRegion.SELECTED_STROKE_COLOR);
						}

						// Move all selected shots to top the of their z-stack
						// to ensure visibility
						for (final CameraView cv : camerasSupervisor.getCameraViews()) {
							final CanvasManager cm = (CanvasManager) cv;

							final Shape marker = selected.getShot().getMarker();
							final int shotIndex = cm.getCanvasGroup().getChildren().indexOf(marker);

							if (shotIndex >= 0 && shotIndex < cm.getCanvasGroup().getChildren().size() - 1) {
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

	@Override
	public void close() {
		shootOFFStage.close();
		camerasSupervisor.closeAll();
		pluginEngine.stopWatching();

		if (config.getExercise().isPresent()) config.getExercise().get().destroy();

		projectorSlide.closeArena();

		for (final Stage streamDebuggerStage : streamDebuggerStages) {
			streamDebuggerStage.close();
		}

		if (config.getSessionRecorder().isPresent()) {
			exerciseSlide.stopRecordingSession();
		}

		TimerPool.close();
		GlobalExecutorPool.getPool().shutdownNow();

		if (!config.getVideoPlayers().isEmpty()) {
			for (final VideoPlayerController videoPlayer : config.getVideoPlayers()) {
				videoPlayer.getStage().close();
			}
		}

		if (!config.inDebugMode()) Main.forceClose(0);
	}

	@Override
	public boolean isArenaViewSelected() {
		return "Arena".equals(cameraTabPane.getSelectionModel().getSelectedItem().getText());
	}

	@Override
	public Optional<CameraView> getArenaView() {
		if (projectorSlide != null && projectorSlide.getArenaPane() != null) {
			return Optional.of(projectorSlide.getArenaPane().getArenaPaneMirror().getCanvasManager());
		}

		return Optional.empty();
	}

	@Override
	public CameraView getSelectedCameraView() {
		if (isArenaViewSelected()) {
			return projectorSlide.getArenaPane().getCanvasManager();
		} else {
			return camerasSupervisor.getCameraView(cameraTabPane.getSelectionModel().getSelectedIndex());
		}
	}

	@Override
	public CameraManager getSelectedCameraManager() {
		return camerasSupervisor.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
	}

	@Override
	public Node getSelectedCameraContainer() {
		return cameraTabPane.getSelectionModel().getSelectedItem().getContent();
	}

	public Stage getStage() {
		return shootOFFStage;
	}

	@Override
	public VBox getButtonsPane() {
		return buttonsContainer;
	}

	@Override
	public ObservableList<ShotEntry> getShotTimerModel() {
		return shotEntries;
	}

	@Override
	public void selectCameraView(CameraView cameraView) {
		final int viewTabIndex = camerasSupervisor.getCameraViews().indexOf(cameraView);

		if (viewTabIndex == -1) {
			// Attempting to show a non-camera view. As of now, this can only
			// be the arena tab
			for (Tab t : cameraTabPane.getTabs()) {
				if ("Arena".equals(t.getText())) {
					cameraTabPane.getSelectionModel().select(t);
					break;
				}
			}
		} else {
			cameraTabPane.getSelectionModel().select(viewTabIndex);
		}
	}

	@Override
	public Pane getTrainingExerciseContainer() {
		return trainingExerciseContainer;
	}

	@Override
	public TableView<ShotEntry> getShotEntryTable() {
		return shotTimerTable;
	}

	@Override
	public void cameraConfigUpdated() {
		config.unregisterAllRecordingCameraManagers();
		addConfiguredCameras();
	}

	private final Map<Tab, CameraManager> cameraManagerTabs = new HashMap<>();

	private void addCameraTabs() {
		if (!config.getWebcams().isEmpty()) {
			addConfiguredCameras();
			return;
		}

		// No configured cameras, attempt to use the system default
		final Optional<Camera> defaultCamera = CameraFactory.getDefault();
		if (!defaultCamera.isPresent()) {
			Main.closeNoCamera();
		}

		if (!addCameraTab("Default", defaultCamera.get())) {
			// Failed to open the default camera. This sometimes happens
			// on Windows when video devices get registered and set as
			// the default camera even though the physical device is not
			// actually present. This seems to happen sometimes with TV
			// tuners and buggy camera drivers. As a workaround, try to
			// fall back to using a different camera as the default.
			final List<Camera> allCameras = CameraFactory.getWebcams();

			if (allCameras.size() <= 1) {
				showCameraLockError(defaultCamera.get(), true);
			} else {
				logger.warn("System default camera is in use, attempting to fall back to a different camera.");

				for (final Camera c : allCameras) {
					if (!c.equals(defaultCamera.get())) {
						if (!addCameraTab("Default", c)) {
							showCameraLockError(c, true);
						}

						break;
					}
				}
			}
		}
	}

	private boolean addCameraTab(String webcamName, Camera cameraInterface) {
		if (cameraInterface.isLocked() && !cameraInterface.isOpen()) {
			return false;
		}

		final Tab cameraTab = new Tab(webcamName);
		final Group cameraCanvasGroup = new Group();
		// 640 x 480
		cameraTab.setContent(new AnchorPane(cameraCanvasGroup));

		final CanvasManager canvasManager = new CanvasManager(cameraCanvasGroup, this, webcamName, shotEntries);
		final Optional<CameraManager> cameraManagerOptional = camerasSupervisor.addCameraManager(cameraInterface, this,
				canvasManager);

		if (!cameraManagerOptional.isPresent()) {
			return false;
		}

		final CameraManager cameraManager = cameraManagerOptional.get();

		cameraManagerTabs.put(cameraTab, cameraManager);

		if (config.getRecordingCameras().contains(cameraInterface)) {
			config.registerRecordingCameraManager(cameraManager);
		}

		canvasManager.setContextMenu(createContextMenu());
		installDebugCoordDisplay(canvasManager);

		return cameraTabPane.getTabs().add(cameraTab);
	}

	private void addConfiguredCameras() {
		Optional<Camera> defaultCam = Optional.empty();
		if (config.getWebcams().isEmpty()) defaultCam = CameraFactory.getDefault();

		for (final Iterator<Entry<Tab, CameraManager>> it = cameraManagerTabs.entrySet().iterator(); it.hasNext();) {
			final Entry<Tab, CameraManager> next = it.next();
			if (config.getWebcams().isEmpty()) {
				if (!defaultCam.isPresent() || next.getValue().getCamera() != defaultCam.get()) {
					cameraTabPane.getTabs().remove(next.getKey());
					camerasSupervisor.clearManager(next.getValue());
					it.remove();
				}
			} else {
				boolean remove = true;
				for (final String webcamName : config.getWebcams().keySet()) {
					final Camera webcam = config.getWebcams().get(webcamName);
					if (next.getValue().getCamera() == webcam && webcam.isOpen()) {
						// Webcam name may have changed, so update it
						next.getKey().setText(webcamName);
						remove = false;
					}
				}
				if (remove) {
					cameraTabPane.getTabs().remove(next.getKey());
					camerasSupervisor.clearManager(next.getValue());
					it.remove();
				}
			}
		}

		if (config.getWebcams().isEmpty()) {
			if (camerasSupervisor.getCameraManagers().size() > 0) return;

			if (defaultCam.isPresent()) {
				if (!addCameraTab("Default", defaultCam.get())) showCameraLockError(defaultCam.get(), true);
			} else {
				logger.error("Default camera was not fetched after clearing camera settings!");
				Main.closeNoCamera();
			}
		} else {
			if (camerasSupervisor.getCameraManagers().size() == config.getWebcams().size()) return;

			int failureCount = 0;

			for (final String webcamName : config.getWebcams().keySet()) {
				final Camera webcam = config.getWebcams().get(webcamName);

				if (camerasSupervisor.getCameraManager(webcam) != null) continue;

				if (!addCameraTab(webcamName, webcam)) {
					failureCount++;
					showCameraLockError(webcam, failureCount == config.getWebcams().size());
				}
			}
		}
	}

	@Override
	public void addNonCameraView(String name, Pane content, CanvasManager canvasManager, boolean select,
			boolean maximizeView) {
		final Tab viewTab = new Tab(name, content);
		cameraTabPane.getTabs().add(viewTab);
		installDebugCoordDisplay(canvasManager);

		if (select) {
			cameraTabPane.getSelectionModel().selectLast();
		}

		// Keep aspect ratio but always match size to the width of the tab
		if (maximizeView) {
			final Runnable translateTabContents = () -> {
				final double widthScale = cameraTabPane.getBoundsInLocal().getWidth()
						/ content.getBoundsInLocal().getWidth();
				content.setScaleX(widthScale);
				content.setScaleY(widthScale);

				// If the arena content is hanging off the bottom of the tab
				// this means the arena is tall and we should scale off the
				// height instead
				if (content.getBoundsInParent().getHeight() > cameraTabPane.getHeight()) {
					final double heightScale = cameraTabPane.getBoundsInLocal().getHeight()
							/ content.getBoundsInLocal().getHeight();
					content.setScaleX(heightScale);
					content.setScaleY(heightScale);
				}

				content.setTranslateX(
						(content.getBoundsInParent().getWidth() - content.getBoundsInLocal().getWidth()) / 2);
				content.setTranslateY(
						(content.getBoundsInParent().getHeight() - content.getBoundsInLocal().getHeight()) / 2);
			};

			// Delay to give auto-placement and calibration a chance to finish
			TimerPool.schedule(translateTabContents, 2000);

			final ChangeListener<? super Number> widthListener = (observable, oldValue, newValue) -> {
				translateTabContents.run();
			};

			content.widthProperty().addListener(widthListener);
			content.heightProperty().addListener(widthListener);

			cameraTabPane.widthProperty().addListener(widthListener);
			cameraTabPane.heightProperty().addListener(widthListener);
		}
	}

	@Override
	public void removeCameraView(String name) {
		Tab viewTab = null;

		for (final Tab t : cameraTabPane.getTabs()) {
			if (t.getText().equals(name)) {
				viewTab = t;
				break;
			}
		}

		if (viewTab != null) cameraTabPane.getTabs().remove(viewTab);
	}

	private void installDebugCoordDisplay(CanvasManager canvasManager) {
		// Show coords of mouse when in canvas during debug mode
		if (config.inDebugMode()) {
			canvasManager.getCanvasGroup().setOnMouseMoved((event) -> {
				shootOFFStage.setTitle(defaultWindowTitle + String.format(" (%.1f, %.1f)", event.getX(), event.getY()));
			});

			canvasManager.getCanvasGroup().setOnMouseExited((event) -> {
				shootOFFStage.setTitle(defaultWindowTitle);
			});
		}
	}

	private ContextMenu createContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem toggleDetectionSectors = new MenuItem("Toggle Shot Detection Sectors");

		toggleDetectionSectors.setOnAction((event) -> {
			final AnchorPane tabAnchor = (AnchorPane) cameraTabPane.getSelectionModel().getSelectedItem().getContent();

			// Only add the pane if it isn't already open
			boolean hasPane = false;
			for (final Node node : tabAnchor.getChildren()) {
				if (node instanceof ShotSectorPane) {
					hasPane = true;
					break;
				}
			}

			if (!hasPane) {
				final CameraManager cameraManager = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());
				new ShotSectorPane(tabAnchor, cameraManager);
			}
		});

		contextMenu.getItems().add(toggleDetectionSectors);

		if (SystemInfo.isWindows()) {
			final MenuItem cameraMenuItem = new MenuItem("Configure Camera");

			cameraMenuItem.setOnAction((event) -> {
				final CameraManager cameraManager = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());

				cameraManager.launchCameraSettings();
			});

			contextMenu.getItems().add(cameraMenuItem);
		}

		if (config.inDebugMode()) {
			final MenuItem startStreamDebuggerMenuItem = new MenuItem("Start Stream Debugger");

			startStreamDebuggerMenuItem.setOnAction((event) -> {
				final FXMLLoader loader = new FXMLLoader(
						getClass().getClassLoader().getResource("com/shootoff/gui/StreamDebugger.fxml"));
				try {
					loader.load();
				} catch (final Exception e) {
					logger.error("Error loading StreamDebugger FXML file", e);
				}

				final Stage streamDebuggerStage = new Stage();
				streamDebuggerStages.add(streamDebuggerStage);

				final String tabName = cameraTabPane.getSelectionModel().getSelectedItem().getText();
				streamDebuggerStage.setTitle(String.format("Stream Debugger -- %s", tabName));
				streamDebuggerStage.setScene(new Scene(loader.getRoot()));
				streamDebuggerStage.show();
				final CameraManager cameraManager = camerasSupervisor
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

			final MenuItem recordMenuItem = new MenuItem("Start Recording");

			recordMenuItem.setOnAction((event) -> {
				final CameraManager cameraManager = camerasSupervisor
						.getCameraManager(cameraTabPane.getSelectionModel().getSelectedIndex());

				if (recordMenuItem.getText().equals("Start Recording")) {
					recordMenuItem.setText("Stop Recording");

					final String tabName = cameraTabPane.getSelectionModel().getSelectedItem().getText();
					final String videoName = tabName + LocalDateTime.now().toString().replaceAll(":", ".") + ".mp4";
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

	@Override
	public List<Target> getTargets() {
		final List<Target> targets = new ArrayList<>();

		for (final CameraManager manager : camerasSupervisor.getCameraManagers()) {
			targets.addAll(((CanvasManager) manager.getCameraView()).getTargets());
		}

		return targets;
	}

	@FXML
	public void fileButtonClicked(MouseEvent event) {
		new FileSlide(controlsContainer, bodyContainer, projectorSlide, this, this, this).showControls();
	}

	@FXML
	public void targetsButtonClicked(MouseEvent event) {
		targetPane.showControls();
	}

	@FXML
	public void trainingButtonClicked(MouseEvent event) {
		exerciseSlide.showControls();
	}

	@FXML
	public void projectorButtonClicked(MouseEvent event) {
		projectorSlide.startArena();
		projectorSlide.showControls();
	}

	@FXML
	public void resetClicked(ActionEvent event) {
		reset();
	}

	@Override
	public void reset() {
		camerasSupervisor.reset();

		if (config.getExercise().isPresent()) {
			final List<Target> knownTargets = new ArrayList<>();
			knownTargets.addAll(getTargets());

			if (projectorSlide.getArenaPane() != null) {
				knownTargets.addAll(projectorSlide.getArenaPane().getCanvasManager().getTargets());
			}

			config.getExercise().get().reset(knownTargets);
		}

		disableShotDetection(1000);
	}

	// Technically the period could be shorter than the previous call
	// and we don't handle that right now. I'm not too worried about that
	// because I don't think the periods are going to be vastly different
	// This is only intended for very short disablement periods
	public void disableShotDetection(int msDuration) {
		// Don't disable the cameras if they are already disabled (e.g. because
		// a training protocol paused shot detection)
		if (!camerasSupervisor.areDetecting()) return;

		// Keep track of cameras that already had shot detection off so that
		// we can ensure they stay off when we re-enable shot detection
		final Set<CameraManager> alreadyOff = new HashSet<>();

		for (final CameraManager cm : camerasSupervisor.getCameraManagers()) {
			if (!cm.isDetecting()) alreadyOff.add(cm);
		}

		camerasSupervisor.setDetectingAll(false);

		final Runnable restartDetection = () -> {
			final Optional<CalibrationManager> calibrationManager = projectorSlide.getCalibrationManager();

			if (!calibrationManager.isPresent()
					|| (calibrationManager.isPresent() && !calibrationManager.get().isCalibrating())) {
				if (alreadyOff.isEmpty()) {
					camerasSupervisor.setDetectingAll(true);
				} else {
					for (final CameraManager cm : camerasSupervisor.getCameraManagers()) {
						if (!alreadyOff.contains(cm)) cm.setDetecting(true);
					}
				}
			} else {
				logger.info("disableShotDetectionTimer did not re-enable shot detection, isCalibrating is true");
			}
		};

		TimerPool.schedule(restartDetection, msDuration);
	}

	@Override
	public void showCameraLockError(Camera webcam, boolean allCamerasFailed) {
		Platform.runLater(() -> {
			final Alert cameraAlert = new Alert(AlertType.ERROR);
			cameraAlert.setTitle("Webcam Locked");
			cameraAlert.setHeaderText("Cannot Open Webcam");
			cameraAlert.setResizable(true);
			cameraAlert.getDialogPane().getScene().getWindow().requestFocus();

			final String messageFormat;

			if (allCamerasFailed) {
				messageFormat = "Cannot open the webcam %s. It is being "
						+ "used by another program or it is an IPCam with the wrong credentials. This "
						+ "is the only configured camera, thus ShootOFF must close.";
			} else {
				messageFormat = "Cannot open the webcam %s. It is being "
						+ "used by another program, it is an IPCam with the wrong credentials, or you "
						+ "have ShootOFF open more than once.";
			}

			final Optional<String> webcamName = config.getWebcamsUserName(webcam);

			cameraAlert.setContentText(
					String.format(messageFormat, webcamName.isPresent() ? webcamName.get() : webcam.getName()));

			if (allCamerasFailed) {
				cameraAlert.showAndWait();
				Main.forceClose(-1);
			} else {
				cameraAlert.show();
			}
		});
	}

	@Override
	public void showMissingCameraError(Camera webcam) {
		Platform.runLater(() -> {
			final Alert cameraAlert = new Alert(AlertType.ERROR);

			final Optional<String> cameraName = config.getWebcamsUserName(webcam);
			final String messageFormat = CameraErrorView.MISSING_ERROR;
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
			final Alert cameraAlert = new Alert(AlertType.WARNING);

			final Optional<String> cameraName = config.getWebcamsUserName(webcam);
			final String messageFormat = CameraErrorView.FPS_WARNING;
			final String message;
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
			final Alert brightnessAlert = new Alert(AlertType.WARNING);

			final Optional<String> cameraName = config.getWebcamsUserName(webcam);
			final String messageFormat = CameraErrorView.BRIGHTNESS_WARNING;
			final String message;
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

	@Override
	public void setExercise(TrainingExercise exercise) {
		try {
			// If there is a current exercise, ensure it is destroyed
			// before starting an new one in case it's a projector
			// exercise that added targets that need to be removed.
			config.setExercise(null);

			if (exercise == null) return;

			final Constructor<?> ctor = exercise.getClass().getConstructor(List.class);

			final List<Target> knownTargets = new ArrayList<>();
			knownTargets.addAll(getTargets());

			if (projectorSlide.getArenaPane() != null) {
				knownTargets.addAll(projectorSlide.getArenaPane().getCanvasManager().getTargets());
			}

			final TrainingExercise newExercise = (TrainingExercise) ctor.newInstance(knownTargets);

			final Optional<Plugin> plugin = pluginEngine.getPlugin(newExercise);
			if (plugin.isPresent()) {
				config.setPlugin(plugin.get());
			} else {
				config.setPlugin(null);
			}

			config.setExercise(newExercise);

			final Runnable initExercise = () -> {
				((TrainingExerciseBase) newExercise).init(camerasSupervisor, this);
				newExercise.init();
			};

			if (Platform.isFxApplicationThread()) {
				initExercise.run();
			} else {
				Platform.runLater(initExercise);
			}
		} catch (final ReflectiveOperationException e) {
			final ExerciseMetadata metadata = exercise.getInfo();
			logger.error("Failed to start exercise " + metadata.getName() + " " + metadata.getVersion(), e);
		}
	}

	@Override
	public void setProjectorExercise(TrainingExercise exercise) {
		try {
			config.setExercise(null);

			final Constructor<?> ctor = exercise.getClass().getConstructor(List.class);
			final TrainingExercise newExercise = (TrainingExercise) ctor
					.newInstance(projectorSlide.getArenaPane().getCanvasManager().getTargets());

			final Optional<Plugin> plugin = pluginEngine.getPlugin(newExercise);
			if (plugin.isPresent()) {
				config.setPlugin(plugin.get());
			} else {
				config.setPlugin(null);
			}

			config.setExercise(newExercise);

			final Runnable initExercise = () -> {
				((ProjectorTrainingExerciseBase) newExercise).init(camerasSupervisor, this,
						projectorSlide.getArenaPane());
				newExercise.init();
			};

			if (Platform.isFxApplicationThread()) {
				initExercise.run();
			} else {
				Platform.runLater(initExercise);
			}

		} catch (final ReflectiveOperationException e) {
			final ExerciseMetadata metadata = exercise.getInfo();
			logger.error("Failed to start projector exercise " + metadata.getName() + " " + metadata.getVersion(), e);
		}
	}

	@Override
	public PluginEngine getPluginEngine() {
		return pluginEngine;
	}
}
