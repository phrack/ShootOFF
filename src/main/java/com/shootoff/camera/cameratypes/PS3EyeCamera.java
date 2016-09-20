//written by ifly53e

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
import com.shootoff.camera.shotdetection.OptiTrackShotDetector;
import com.shootoff.camera.shotdetection.ShotDetector;
import com.shootoff.config.Configuration;

public class PS3EyeCamera implements Camera {
	private static final Logger logger = LoggerFactory.getLogger(PS3EyeCamera.class);

	private static boolean initialized = false;
	protected CameraState cameraState;
	protected Optional<CameraEventListener> cameraEventListener = Optional.empty();
	protected long currentFrameTimestamp = -1;
	private Dimension dimension = null;
	private int viewWidth = 0;
	private int viewHeight = 0;

	static int size_x = 640;
	static int size_y = 480;

	static eyecam.ps3eye_t ps3ID = null;
	static boolean opened = false;
	static int frameCount = 0;
	static boolean closed = true;
	static double ps3FPS = 30;
	private long lastCameraTimestamp = -1;
	private long lastFrameCount = 0;

	static eyecam eyecamLib = (eyecam) Native.loadLibrary("eyecam", eyecam.class);
	static byte[] ba = new byte[size_x * size_y * 4];

	public PS3EyeCamera() {
		if (!initialized) {

			init();
		}
	}

	public static void init() {
		if (initialized)
			return;

		eyecamLib.ps3eye_init();

		if (eyecamLib.ps3eye_count_connected() == 1) {
			logger.debug("Opening ps3eye");
			ps3ID = eyecamLib.ps3eye_open(0, size_x, size_y, 75, eyecam.ps3eye_format.PS3EYE_FORMAT_BGR);
			if (ps3ID != null) {
				logger.debug("Received a PS3ID");
				closed = false;
				opened = true;
				initialized = true;
			} else {
				logger.debug("Did not receive a PS3ID");
				closed = true;
				opened = false;
				initialized = false;
			}

		} else {
			initialized = false;
		}

		if (initialized()) {
			logger.debug("Registering PS3Eye camera");
			CameraFactory.registerCamera(new PS3EyeCamera());
		}
	}

	static Label fpsValue = new Label("0");
	static Stage stage = new Stage();

	public void launchCameraSettings() {

		logger.debug("launch camera settings called");
		final Color textColor = Color.BLACK;

		final Slider gain = new Slider(0, 63,
				eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN));
		final Slider exposure = new Slider(0, 255,
				eyecamLib.ps3eye_get_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE));

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

		final Label gainCaption = new Label("Gain:");
		final Label exposureCaption = new Label("Exposure:");
		final Label autoGainCaption = new Label("Auto Gain:");
		final Label fpsCaption = new Label("FPS: ");

		final Label gainValue = new Label(Integer.toString((int) gain.getValue()));
		final Label exposureValue = new Label(Integer.toString((int) exposure.getValue()));

		Group root = new Group();
		Scene scene = new Scene(root, 425, 200);
		stage.setScene(scene);
		stage.setTitle("PS3EYE Configuration");
		scene.setFill(Color.WHITESMOKE);

		GridPane grid = new GridPane();
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
				logger.debug("gain set to: " + Math.round(new_val.doubleValue()));
				eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_GAIN,
						(int) Math.round(new_val.doubleValue()));
				gainValue.setText(String.format("%d", (int) Math.round(new_val.doubleValue())));
			}
		});

		exposure.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				eyecamLib.ps3eye_set_parameter(ps3ID, eyecam.ps3eye_parameter.PS3EYE_EXPOSURE,
						(int) Math.round(new_val.doubleValue()));
				logger.debug("exposure level set to: " + Math.round(new_val.doubleValue()));
				exposureValue.setText(String.format("%d", (int) Math.round(new_val.doubleValue())));
			}
		});

		final CheckBox autoGain = new CheckBox("AutoGain");
		boolean isAutoGainSet = false;
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

		stage.show();

		stage.setOnCloseRequest((e) -> {
			configIsOpen = false;
		});

	}// end launchcamerasettings

	public boolean setState(CameraState cameraState) {
		this.cameraState = cameraState;
		switch (cameraState) {
		case DETECTING:
			break;
		case CALIBRATING:
			break;
		case CLOSED:
			close();
			break;
		case NORMAL:
		default:
			break;

		}

		this.cameraState = cameraState;

		return true;
	}

	public CameraState getState() {

		return cameraState;
	}

	public void setCameraEventListener(CameraEventListener cameraEventListener) {
		this.cameraEventListener = Optional.ofNullable(cameraEventListener);
	}

	public long getCurrentFrameTimestamp() {
		return currentFrameTimestamp;
	}

	public String getName() {
		return "PS3Eye";
	}

	private native static void initialize();

	private static boolean cameraAvailableNative() {
		return initialized;
	}

	private int getViewWidth() {
		return 640;
	}

	private int getViewHeight() {
		return 480;
	}

	public static void closeMe() {
		if (configIsOpen) {
			configIsOpen = false;
			stage.close();
		}
		if (closed)
			return;

		eyecamLib.ps3eye_close(ps3ID);
		eyecamLib.ps3eye_uninit();
		logger.debug("ps3eye closed and uninit called");
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
			logger.debug("ps3eye closed and uninit called");
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

	@Override
	public double getFPS() {
		return ps3FPS;
	}

	protected void setFPS(double newFPS) {
		// This just tells us if it's the first FPS estimate
		if (getFrameCount() > 30)
			ps3FPS = ((ps3FPS * 4.0) + newFPS) / 5.0;
		else
			ps3FPS = newFPS;
	}

	public void estimateCameraFPS() {
		if (lastCameraTimestamp > -1) {
			double estimateFPS = ((double) getFrameCount() - (double) lastFrameCount)
					/ (((double) System.currentTimeMillis() - (double) lastCameraTimestamp) / 1000.0);

			setFPS(estimateFPS);

			if (cameraEventListener.isPresent())
				cameraEventListener.get().newFPS(ps3FPS);
		}

		lastCameraTimestamp = System.currentTimeMillis();
		lastFrameCount = getFrameCount();

	}

	@Override
	public int getFrameCount() {
		return frameCount;
	}

	public void setViewSize(final Dimension size) {
		return;
	}

	public Dimension getViewSize() {
		if (dimension != null)
			return dimension;

		dimension = new Dimension(size_x, size_y);

		return dimension;
	}

	private byte[] getImageNative() {

		eyecamLib.ps3eye_grab_frame(ps3ID, ba);

		return ba;

	}

	public Mat translateCameraArrayToMat(byte[] imageBuffer) {
		viewHeight = size_y;
		viewWidth = size_x;

		Mat mat = new Mat(viewHeight, viewWidth, CvType.CV_8UC3);

		mat.put(0, 0, imageBuffer);
		return mat;
	}

	public synchronized boolean open() {
		if (opened)
			return true;
		ps3ID = eyecamLib.ps3eye_open(0, size_x, size_y, 75, eyecam.ps3eye_format.PS3EYE_FORMAT_BGR);
		if (isOpen())
			closed = false;
		return isOpen();
	}

	public Mat getMatFrame() {
		byte[] frame = getImageNative();
		currentFrameTimestamp = System.currentTimeMillis();
		Mat mat = translateCameraArrayToMat(frame);
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
		if (OptiTrackShotDetector.isSystemSupported())
			return new OptiTrackShotDetector(cameraManager, config, cameraView);
		else if (NativeShotDetector.isSystemSupported())
			return new NativeShotDetector(cameraManager, config, cameraView);
		else if (JavaShotDetector.isSystemSupported()) {
			logger.debug("starting javaShotDetector for PS3Eye");
			return new JavaShotDetector(cameraManager, config, cameraView);
		} else
			return null;
	}

	public static boolean initialized() {
		return initialized;
	}

	static boolean configIsOpen = false;

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
					fpsValue.setText((Double.toString(getFPS()).substring(0, 5)));
				});
			}

		}
		if (cameraEventListener.isPresent())
			cameraEventListener.get().cameraClosed();
	}

	private void cameraClosed() {
		if (cameraEventListener.isPresent())
			cameraEventListener.get().cameraClosed();
		close();
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	private Optional<Integer> origExposure = Optional.empty();

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
		logger.debug("curExp[ {} newExp {}", curExp, newExp);

		if (newExp < 20)
			return false;

		setExposure(newExp);
		logger.debug("curExp[ {} newExp {} res {}", curExp, newExp, getExposure());
		return (getExposure() == newExp);
	}

	public void resetExposure() {
		if (origExposure.isPresent())
			setExposure(origExposure.get());
	}

	public boolean limitsFrames() {
		return true;
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

		// a test returns 85
		int hello();

	}// end eyecam interface

}