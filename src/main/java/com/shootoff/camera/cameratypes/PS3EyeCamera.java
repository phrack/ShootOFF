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

/*
 * eyeCam32.dll and eyeCam64.dll were compiled by ifly53e using the source from
 * https:\\github.com\inspirit\PS3EYEDriver
 *
 *Follow the instructions to install the proper PS3Eye usb driver from this link:
 *https:\\github.com\cboulay\psmove-ue4\wiki\Windows-PSEye-Setup
 *
 *You will need a program called Zadig to help install the usb driver:
 *http:\\zadig.akeo.ie\downloads\zadig_2.2.exe
 *
 *Missing from the Zadig instructions is to click on the Options menu and
 *click "List All Devices" so that you can see the PS3Eye camera in the first place.
 *
 *Test your setup with ps3eye_sdl.exe found at:
 *https:\\github.com\cboulay\psmove-ue4\tree\master\Binaries\Win64
 *
 *Start ShootOFF and it should see the PS3Eye.
 *
 *You can right click on the feed to bring up the configure camera menu
 *and adjust the gain and exposure.  You can also toggle "auto gain"
 *on and off and see the current FPS there.
 */

package com.shootoff.camera.cameratypes;

import com.sun.jna.Library;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import java.util.Optional;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraFactory;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.camera.shotdetection.NativeShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;

public class PS3EyeCamera extends CalculatedFPSCamera {
	private static final Logger logger = LoggerFactory.getLogger(PS3EyeCamera.class);

	private static boolean initialized = false;
	private Dimension dimension = null;

	private static final int VIEW_WIDTH = 640;
	private static final int VIEW_HEIGHT = 480;

	private static eyecam.ps3eye_t ps3ID = null;
	private static boolean opened = false;
	private static boolean closed = true;

	private Optional<Integer> origExposure = Optional.empty();
	private static boolean configIsOpen = false;

	private static eyecam eyecamLib;
	private static byte[] ba = new byte[getViewWidth() * getViewHeight() * 4];
	private static Label fpsValue = new Label("0");
	private static Stage ps3eyeSettingsStage = new Stage();

	public PS3EyeCamera() {
		if (!initialized) {
			init();
		}
	}

	public static void init() {
		if (initialized) return;

		try {
			final String architecture = System.getProperty("sun.arch.data.model");

			if (logger.isDebugEnabled())
				logger.debug("OS type is: {}", architecture);

			if (architecture != null && "64".equals(architecture)) {
				logger.trace("Trying to load eyeCam64.dll");
				eyecamLib = (eyecam) Native.loadLibrary("eyeCam64", eyecam.class);
				logger.trace("Successfully loaded eyeCam64.dll");
			} else if (architecture != null && "32".equals(architecture)) {
				logger.trace("Trying to load eyeCam32.dll");
				eyecamLib = (eyecam) Native.loadLibrary("eyeCam32", eyecam.class);
				logger.trace("Successfully loaded eyeCam32.dll");
			}
		} catch (UnsatisfiedLinkError exception) {
			logger.error("PS3EyeCamera eyecam ULE, Can't find the eyeCamXX.dll or "
					+ "the Visual Studio Visual C++ runtime files are not installed: ", exception);
			initialized = false;
			return;
		}

		if (eyecamLib == null) {
			logger.info("Architecture not accounted for, PS3Eye not loaded");
			initialized = false;
			return;
		}

		eyecamLib.ps3eye_init();

		if (eyecamLib.ps3eye_count_connected() >= 1) {
			logger.trace("Found a PS3EYE camera, setting up communications with it");
			ps3ID = eyecamLib.ps3eye_open(0, getViewWidth(), getViewHeight(), 75,
					eyecam.ps3eye_format.PS3EYE_FORMAT_BGR);
			if (ps3ID != null) {
				logger.trace("Communications with PS3Eye camera established");
				closed = false;
				opened = true;
				initialized = true;
			} else {
				logger.trace("Communications with PS3Eye camera NOT established");
				closed = true;
				opened = false;
				initialized = false;
			}

		} else {
			initialized = false;
		}

		if (initialized()) {
			if (eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN, 16) == -1) {
				logger.error("Error setting gain on PS3Eye during initialization, "
						+ "shutdown ShootOFF and unplug and re-plug in the PS3Eye to the usb port");
			}
			if (eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE, 50) == -1) {
				logger.error("Error setting exposure on PS3Eye during initialization, "
						+ "shutdown ShootOFF and unplug and re-plug in the PS3Eye to the usb port");
			}

			CameraFactory.registerCamera(new PS3EyeCamera());
			logger.trace("PS3Eye adjusted and registered");
		}

	}// end init

	public void launchCameraSettings() {
		logger.trace("Launch camera settings called");
		final CheckBox autoGain = new CheckBox("AutoGain");
		boolean isAutoGainSet = false;
		final Color textColor = Color.BLACK;

		final Slider gain = new Slider(0, 63,
				eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN));
		final Slider exposure = new Slider(0, 255,
				eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE));

		final Label gainCaption = new Label("Gain:");
		final Label exposureCaption = new Label("Exposure:");
		final Label autoGainCaption = new Label("Auto Gain:");
		final Label fpsCaption = new Label("FPS: ");

		final Label gainValue = new Label(Integer.toString((int) gain.getValue()));
		final Label exposureValue = new Label(Integer.toString((int) exposure.getValue()));

		gain.setShowTickLabels(true);
		gain.setShowTickMarks(true);
		gain.setMajorTickUnit(9);// 63
		gain.setMinorTickCount(9);
		gain.setBlockIncrement(1);
		gain.setSnapToTicks(true);

		exposure.setShowTickLabels(true);
		exposure.setShowTickMarks(true);
		exposure.setMajorTickUnit(50);// 255
		exposure.setMinorTickCount(25);
		exposure.setBlockIncrement(1);
		exposure.setSnapToTicks(true);

		final Group root = new Group();
		final Scene scene = new Scene(root, 425, 200);
		ps3eyeSettingsStage.setScene(scene);
		ps3eyeSettingsStage.setTitle("PS3EYE Configuration");
		scene.setFill(Color.WHITESMOKE);

		final GridPane grid = new GridPane();
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setVgap(10);
		grid.setHgap(70);

		scene.setRoot(grid);

		gainCaption.setTextFill(textColor);
		GridPane.setConstraints(gainCaption, 0, 1);
		grid.getChildren().add(gainCaption);

		exposureCaption.setTextFill(textColor);
		GridPane.setConstraints(exposureCaption, 0, 2);
		grid.getChildren().add(exposureCaption);

		GridPane.setConstraints(autoGainCaption, 0, 4);
		grid.getChildren().add(autoGainCaption);

		GridPane.setConstraints(fpsCaption, 0, 5);
		grid.getChildren().add(fpsCaption);

		GridPane.setConstraints(gain, 1, 1);
		grid.getChildren().add(gain);

		GridPane.setConstraints(exposure, 1, 2);
		grid.getChildren().add(exposure);

		gainValue.setTextFill(textColor);
		GridPane.setConstraints(gainValue, 2, 1);
		grid.getChildren().add(gainValue);

		exposureValue.setTextFill(textColor);
		GridPane.setConstraints(exposureValue, 2, 2);
		grid.getChildren().add(exposureValue);

		GridPane.setConstraints(fpsValue, 1, 5);
		grid.getChildren().add(fpsValue);

		configIsOpen = true;

		gain.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				if (logger.isTraceEnabled()) logger.trace("gain set to: {}", Math.round(new_val.doubleValue()));
				eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN,
						(int) Math.round(new_val.doubleValue()));
				gainValue.setText(String.format("%d", (int) Math.round(new_val.doubleValue())));
			}
		});

		exposure.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE,
						(int) Math.round(new_val.doubleValue()));
				if (logger.isTraceEnabled()) logger.trace("exposure level set to: {}", Math.round(new_val.doubleValue()));
				exposureValue.setText(String.format("%d", (int) Math.round(new_val.doubleValue())));
			}
		});

		if (eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_AUTO_GAIN) == 0) {
			isAutoGainSet = false;
			autoGain.setText("Off");
			gain.setDisable(false);
			exposure.setDisable(false);
		} else {
			isAutoGainSet = true;
			autoGain.setText("On");
			gain.setDisable(true);
			exposure.setDisable(true);
		}

		autoGain.setSelected(isAutoGainSet);

		GridPane.setConstraints(autoGain, 1, 4);
		grid.getChildren().add(autoGain);

		autoGain.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
				if (new_val) {
					autoGain.setText("On");
					gain.setValue(eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN));
					gain.setDisable(true);
					exposure.setDisable(true);
					eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_AUTO_GAIN, 1);
				} else {
					eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_AUTO_GAIN, 0);
					autoGain.setText("Off");
					gain.setValue(eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN));
					gain.setDisable(false);
					exposure.setDisable(false);
				}
			}
		});

		ps3eyeSettingsStage.show();

		ps3eyeSettingsStage.setOnCloseRequest((e) -> {
			configIsOpen = false;
		});
	}// end launchcamerasettings

	public String getName() {
		return "PS3Eye";
	}

	private static int getViewWidth() {
		return VIEW_WIDTH;
	}

	private static int getViewHeight() {
		return VIEW_HEIGHT;
	}

	public static void closeMe() {
		if (configIsOpen) {
			configIsOpen = false;
			ps3eyeSettingsStage.close();
		}
		if (closed)
			return;

		eyecamLib.ps3eye_close(ps3ID);
		eyecamLib.ps3eye_uninit();
		logger.debug("PS3Eye camera closed");
		ps3ID = null;
		closed = true;
		opened = false;
	}

	public synchronized void close() {

		closeMe();
		if (closed) {
			return;
		} else {
			eyecamLib.ps3eye_close(ps3ID);
			eyecamLib.ps3eye_uninit();
			logger.debug("PS3Eye camera closed");
			ps3ID = null;
		}
	}

	public boolean isOpen() {
		if (ps3ID != null) {
			opened = true;
			return true;
		} else {
			opened = false;
			return false;
		}
	}

	public void setViewSize(final Dimension size) {
		return;
	}

	public Dimension getViewSize() {
		if (dimension != null)
			return dimension;

		dimension = new Dimension(getViewWidth(), getViewHeight());

		return dimension;
	}

	private byte[] getImageNative() {
		eyecamLib.ps3eye_grab_frame(ps3ID, ba);

		return ba;
	}

	public Mat translateCameraArrayToMat(byte[] imageBuffer) {
		final Mat mat = new Mat(getViewHeight(), getViewWidth(), CvType.CV_8UC3);

		mat.put(0, 0, imageBuffer);
		return mat;
	}

	public synchronized boolean open() {
		if (opened)
			return true;
		ps3ID = eyecamLib.ps3eye_open(0, getViewWidth(), getViewHeight(), 75, eyecam.ps3eye_format.PS3EYE_FORMAT_BGR);
		if (isOpen())
			closed = false;
		return isOpen();
	}

	public Mat getMatFrame() {
		final byte[] frame = getImageNative();
		currentFrameTimestamp = System.currentTimeMillis();
		final Mat mat = translateCameraArrayToMat(frame);
		frameCount++;
		return mat;
	}

	@Override
	public BufferedImage getBufferedImage() {
		return Camera.matToBufferedImage(getMatFrame());
	}

	@Override
	public ShotDetector getPreferredShotDetector(final CameraManager cameraManager, final Configuration config,
			final CameraView cameraView) {
		if (NativeShotDetector.isSystemSupported()) {
			return new NativeShotDetector(cameraManager, config, cameraView);
		} else if (JavaShotDetector.isSystemSupported()) {
			logger.trace("starting javaShotDetector for PS3Eye");
			return new JavaShotDetector(cameraManager, config, cameraView);
		} else
			return null;
	}

	public static boolean initialized() {
		return initialized;
	}

	@Override
	public void run() {
		while (isOpen()) {
			if (cameraEventListener.isPresent())
				cameraEventListener.get().newFrame(getMatFrame());

			if (((int) (getFrameCount() % Math.min(getFPS(), 5)) == 0) && cameraState != CameraState.CALIBRATING) {
				estimateCameraFPS();
			}

			if (configIsOpen) {
				Platform.runLater(() -> {
					String theFPS = Double.toString(getFPS());
					if (theFPS.length() >= 6)
						fpsValue.setText((Double.toString(getFPS()).substring(0, 5)));
				});
			}
		}

		if (cameraEventListener.isPresent())
			cameraEventListener.get().cameraClosed();
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	private int getExposure() {
		return eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE);
	}

	private void setExposure(int exposure) {
		eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE, exposure);
	}

	@Override
	public boolean supportsExposureAdjustment() {
		if (!origExposure.isPresent())
			origExposure = Optional.of(getExposure());
		return true;
	}

	@Override
	public boolean decreaseExposure() {
		final int curExp = getExposure();
		final int newExp = (int) (curExp - (.1 * (double) curExp));
		logger.trace("curExp[ {} newExp {}", curExp, newExp);

		if (newExp < 17)
			return false;

		setExposure(newExp);
		logger.trace("curExp[ {} newExp {} res {}", curExp, newExp, getExposure());
		return (getExposure() == newExp);
	}

	public void resetExposure() {
		if (origExposure.isPresent())
			setExposure(origExposure.get());
	}

	public boolean limitsFrames() {
		return false;
	}

	public interface eyecam extends Library {
		public static class ps3eye_t extends PointerType {
			public ps3eye_t() {
			}

			protected ps3eye_t(Pointer ps3eye_t) {
				super(ps3eye_t);
			}
		}

		public static interface ps3eye_parameter {
			public static final int PS3EYE_AUTO_GAIN = 0; // [false, true]
			public static final int PS3EYE_GAIN = 1; // [0, 63]
			public static final int PS3EYE_AUTO_WHITEBALANCE = 2; // [false,
																	// true]
			public static final int PS3EYE_EXPOSURE = 3; // [0, 255]
			public static final int PS3EYE_SHARPNESS = 4; // [0 63]
			public static final int PS3EYE_CONTRAST = 5; // [0, 255]
			public static final int PS3EYE_BRIGHTNESS = 6; // [0, 255]
			public static final int PS3EYE_HUE = 7; // [0, 255]
			public static final int PS3EYE_REDBALANCE = 8; // [0, 255]
			public static final int PS3EYE_BLUEBALANCE = 9; // [0, 255]
			public static final int PS3EYE_GREENBALANCE = 10; // [0, 255]
			public static final int PS3EYE_HFLIP = 11; // [false, true]
			public static final int PS3EYE_VFLIP = 12; // [false, true]
		};

		public static interface ps3eye_format {
			public static final int PS3EYE_FORMAT_BAYER = 0;
			public static final int PS3EYE_FORMAT_BGR = 1;
			public static final int PS3EYE_FORMAT_RGB = 2;
		}

		/**
		 * Initialize and enumerate connected cameras. Needs to be called once
		 * before all other API functions.
		 **/
		void ps3eye_init();

		/**
		 * De-initialize the library and free resources. If a pseye_t * object
		 * is still opened, nothing happens.
		 **/
		void ps3eye_uninit();

		/**
		 * Return the number of PSEye cameras connected via USB.
		 **/
		int ps3eye_count_connected();

		/**
		 * Open a PSEye camera device by id. The id is zero-based, and must be
		 * smaller than the count. width and height should usually be 640x480 or
		 * 320x240 fps is the target frame rate, 60 usually works fine here
		 **/
		ps3eye_t ps3eye_open(int id, int width, int height, int fps, int outputFormat);

		/**
		 * Get the string that uniquely identifies this camera Returns 0 on
		 * success, -1 on failure
		 **/
		int ps3eye_get_unique_identifier(ps3eye_t eye, char[] out_identifier, int max_identifier_length);

		/**
		 * Grab the next frame as YUV422 blob. YUV422 4 bytes per 2 pixels ( 8
		 * bytes per 4 pixels) A pointer to the buffer will be passed back. The
		 * buffer will only be valid until the next call, or until the eye is
		 * closed again with ps3eye_close(). If stride is not NULL, the byte
		 * offset between two consecutive lines in the frame will be written to
		 * *stride.
		 **/
		void ps3eye_grab_frame(ps3eye_t eye, byte[] ba);

		/**
		 * Close a PSEye camera device and free allocated resources. To really
		 * close the library, you should also call ps3eye_uninit().
		 **/
		void ps3eye_close(ps3eye_t eye);

		/**
		 * Set a ps3eye_parameter to a value. Returns -1 if there is an error,
		 * otherwise 0.
		 **/
		int ps3eye_set_parameter(ps3eye_t eye, int param, int value);

		/**
		 * Get a ps3eye_parameter value. Returns -1 if there is an error,
		 * otherwise returns the parameter value int.
		 **/
		int ps3eye_get_parameter(ps3eye_t eye, int ps3eyeGain);

	}// end eyecam interface
}