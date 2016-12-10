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

package com.shootoff.headless;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraErrorView;
import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CamerasSupervisor;
import com.shootoff.camera.cameratypes.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CalibrationConfigurator;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.CalibrationOption;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.ExerciseListener;
import com.shootoff.gui.Resetter;
import com.shootoff.gui.TargetView;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.headless.protocol.AddTargetMessage;
import com.shootoff.headless.protocol.ConfigurationData;
import com.shootoff.headless.protocol.CurrentConfigurationMessage;
import com.shootoff.headless.protocol.GetConfigurationMessage;
import com.shootoff.headless.protocol.Message;
import com.shootoff.headless.protocol.MessageListener;
import com.shootoff.headless.protocol.MoveTargetMessage;
import com.shootoff.headless.protocol.RemoveTargetMessage;
import com.shootoff.headless.protocol.ResetMessage;
import com.shootoff.headless.protocol.ResizeTargetMessage;
import com.shootoff.headless.protocol.SetConfigurationMessage;
import com.shootoff.headless.protocol.TargetMessage;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.engine.PluginEngine;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.Target;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class HeadlessController implements CameraErrorView, Resetter, ExerciseListener, CalibrationConfigurator,
		QRCodeListener, ConnectionListener, MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(HeadlessController.class);

	private final Configuration config;
	private final CamerasSupervisor camerasSupervisor;
	private final Map<UUID, Target> targets = new HashMap<>();

	private CanvasManager arenaCanvasManager;
	private Target qrCodeTarget;

	private Optional<HeadlessServer> server = Optional.empty();

	public HeadlessController() {
		config = Configuration.getConfig();
		camerasSupervisor = new CamerasSupervisor(config);

		final Optional<Camera> defaultCamera = CameraFactory.getDefault();
		if (defaultCamera.isPresent()) {
			final Camera c = defaultCamera.get();

			if (c.isLocked() && !c.isOpen()) {
				logger.error("Default camera is locked, cannot proceed");
				return;
			}

			final CanvasManager canvasManager = new CanvasManager(new Group(), this, "Default", null);
			final CameraManager cameraManager = camerasSupervisor.addCameraManager(c, this, canvasManager);

			final Stage arenaStage = new Stage();
			// TODO: Pass controls added to this pane to the device controlling
			// SBC
			final Pane trainingExerciseContainer = new Pane();

			final ProjectorArenaPane arenaPane = new ProjectorArenaPane(arenaStage, null, trainingExerciseContainer,
					this, null);

			arenaCanvasManager = arenaPane.getCanvasManager();

			arenaStage.setTitle("Projector Arena");
			arenaStage.setScene(new Scene(arenaPane));
			arenaStage.setFullScreenExitHint("");

			// TODO: Camera views to non-null value to handle calibration issues
			final CalibrationManager calibrationManager = new CalibrationManager(this, cameraManager, arenaPane, null,
					this);

			arenaPane.setCalibrationManager(calibrationManager);
			arenaPane.toggleArena();
			arenaPane.autoPlaceArena();

			calibrationManager.enableCalibration();
		}
	}

	@Override
	public void reset() {
		camerasSupervisor.reset();
	}

	@Override
	public void showCameraLockError(Camera webcam, boolean allCamerasFailed) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void showMissingCameraError(Camera webcam) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void showFPSWarning(Camera webcam, double fps) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void showBrightnessWarning(Camera webcam) {
		// TODO: Send to device controlling SBC
	}

	@Override
	public void setProjectorExercise(TrainingExercise exercise) {
		// TODO: Set exercise
	}

	@Override
	public void setExercise(TrainingExercise exercise) {
		// TODO: Set exercise
	}

	@Override
	public Configuration getConfiguration() {
		return config;
	}

	@Override
	public PluginEngine getPluginEngine() {
		// TODO: Does this need to be implemented?
		return null;
	}

	@Override
	public CalibrationOption getCalibratedFeedBehavior() {
		return CalibrationOption.ONLY_IN_BOUNDS;
	}

	@Override
	public void calibratedFeedBehaviorsChanged() {}

	@Override
	public void toggleCalibrating(boolean isCalibrating) {
		if (!isCalibrating) {
			final HeadlessServer headlessServer = new BluetoothServer(this);
			server = Optional.of(headlessServer);
			headlessServer.startReading(this, this);
		}
	}

	@Override
	public void qrCodeCreated(Image qrCodeImage) {
		final Group targetGroup = new Group();
		final ImageRegion qrCodeRegion = new ImageRegion(qrCodeImage);
		qrCodeRegion.getAllTags().put(TargetView.TAG_IGNORE_HIT, "true");

		targetGroup.getChildren().add(qrCodeRegion);

		qrCodeTarget = arenaCanvasManager.addTarget(null, targetGroup, new HashMap<String, String>(), false);
	}

	@Override
	public void connectionEstablished() {
		if (qrCodeTarget != null) {
			arenaCanvasManager.removeTarget(qrCodeTarget);
			qrCodeTarget = null;
		}
	}

	@Override
	public void messageReceived(Message message) {
		if (message instanceof GetConfigurationMessage) {
			sendConfiguration();
		} else if (message instanceof ResetMessage) {
			reset();
		} else if (message instanceof SetConfigurationMessage) {
			final SetConfigurationMessage configMessage = (SetConfigurationMessage) message;
			setConfiguration(configMessage.getConfigurationData());
		} else if (message instanceof TargetMessage) {
			handleTargetMessage((TargetMessage) message);
		}
	}

	private void sendConfiguration() {
		if (server.isPresent()) {
			final ConfigurationData configurationData = new ConfigurationData(config.getMarkerRadius(),
					config.ignoreLaserColor(), config.getIgnoreLaserColorName(), config.useVirtualMagazine(),
					config.getVirtualMagazineCapacity(), config.useMalfunctions(), config.getMalfunctionsProbability(),
					config.showArenaShotMarkers());
			server.get().sendMessage(new CurrentConfigurationMessage(configurationData));
		}
	}

	private void setConfiguration(ConfigurationData configurationData) {
		config.setMarkerRadius(configurationData.getMarkerRadius());

		config.setIgnoreLaserColor(configurationData.isIgnoreLaserColor());
		config.setIgnoreLaserColorName(configurationData.getIgnoreLaserColorName());

		config.setUseVirtualMagazine(configurationData.useVirtualMagazine());
		config.setVirtualMagazineCapacity(configurationData.getVirtualMagazineCapacity());

		config.setMalfunctions(configurationData.useMalfunctions());
		config.setMalfunctionsProbability(configurationData.getMalfunctionsProbability());

		config.setShowArenaShotMarkers(configurationData.showArenaShotMarkers());
	}

	private void handleTargetMessage(TargetMessage message) {
		if (message instanceof AddTargetMessage) {
			final AddTargetMessage addTarget = (AddTargetMessage) message;
			final Optional<Target> target = arenaCanvasManager.addTarget(addTarget.getTargetFile());

			if (target.isPresent()) {
				targets.put(addTarget.getUuid(), target.get());
			}
		} else {
			final UUID targetUuid = message.getUuid();
			if (!targets.containsKey(targetUuid)) {
				// TODO: Send error to tablet
				logger.error("A target with UUID {} does not exist to perform operation {}", targetUuid,
						message.getClass().getName());
				return;
			}

			final Target t = targets.get(targetUuid);

			if (message instanceof MoveTargetMessage) {
				final MoveTargetMessage moveTarget = (MoveTargetMessage) message;
				t.setPosition(moveTarget.getNewX(), moveTarget.getNewY());
			} else if (message instanceof ResizeTargetMessage) {
				final ResizeTargetMessage resizeTarget = (ResizeTargetMessage) message;
				t.setDimensions(resizeTarget.getNewWidth(), resizeTarget.getNewHeight());
			} else if (message instanceof RemoveTargetMessage) {
				arenaCanvasManager.removeTarget(t);
				targets.remove(targetUuid);
			}
		}
	}
}
