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

package com.shootoff.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import com.shootoff.Main;
import com.shootoff.camera.Camera;
import com.shootoff.camera.CameraManager;
import com.shootoff.camera.MalfunctionsProcessor;
import com.shootoff.camera.ShotProcessor;
import com.shootoff.camera.VirtualMagazineProcessor;
import com.shootoff.gui.controller.VideoPlayerController;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.session.SessionRecorder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

public class Configuration {
	private static final String FIRST_RUN_PROP = "shootoff.firstrun";
	private static final String ERROR_REPORTING_PROP = "shootoff.errorreporting";
	private static final String IPCAMS_PROP = "shootoff.ipcams";
	private static final String WEBCAMS_PROP = "shootoff.webcams";
	private static final String RECORDING_WEBCAMS_PROP = "shootoff.webcams.recording";
	private static final String MARKER_RADIUS_PROP = "shootoff.markerradius";
	private static final String IGNORE_LASER_COLOR_PROP = "shootoff.ignorelasercolor";
	private static final String USE_RED_LASER_SOUND_PROP = "shootoff.redlasersound.use";
	private static final String RED_LASER_SOUND_PROP = "shootoff.redlasersound";
	private static final String USE_GREEN_LASER_SOUND_PROP = "shootoff.greenlasersound.use";
	private static final String GREEN_LASER_SOUND_PROP = "shootoff.greenlasersound";
	private static final String USE_VIRTUAL_MAGAZINE_PROP = "shootoff.virtualmagazine.use";
	private static final String VIRTUAL_MAGAZINE_CAPACITY_PROP = "shootoff.virtualmagazine.capacity";
	private static final String USE_MALFUNCTIONS_PROP = "shootoff.malfunctions.use";
	private static final String MALFUNCTIONS_PROBABILITY_PROP = "shootoff.malfunctions.probability";
	private static final String ARENA_POSITION_X_PROP = "shootoff.arena.x";
	private static final String ARENA_POSITION_Y_PROP = "shootoff.arena.y";
	private static final String MUTED_CHIME_MESSAGES = "shootoff.diagnosticmessages.chime.muted";

	protected static final String MARKER_RADIUS_MESSAGE = "MARKER_RADIUS has an invalid value: %d. Acceptable values are "
			+ "between 1 and 20.";
	protected static final String LASER_COLOR_MESSAGE = "LASER_COLOR has an invalid value: %s. Acceptable values are "
			+ "\"red\" and \"green\".";
	protected static final String LASER_SOUND_MESSAGE = "LASER_SOUND has an invalid value: %s. Sound file must exist.";
	protected static final String VIRTUAL_MAGAZINE_MESSAGE = "VIRTUAL_MAGAZINE has an invalid value: %d. Acceptable values are "
			+ "between 1 and 45.";
	protected static final String INJECT_MALFUNCTIONS_MESSAGE = "INJECT_MALFUNCTIONS has an invalid value: %f. Acceptable values are "
			+ "between 0.1 and 99.9.";

	private static final String DEFAULT_CONFIG_FILE = "shootoff.properties";

	private static final int DEFAULT_DISPLAY_WIDTH = 640;
	private static final int DEFAULT_DISPLAY_HEIGHT = 480;

	private InputStream configInput;
	private String configName;

	private boolean isFirstRun = false;
	private boolean useErrorReporting = true;
	private Map<String, URL> ipcams = new HashMap<String, URL>();
	private Map<String, String> ipcamCredentials = new HashMap<String, String>();
	private Map<String, Camera> webcams = new HashMap<String, Camera>();
	private int markerRadius = 4;
	private boolean ignoreLaserColor = false;
	private String ignoreLaserColorName = "None";
	private boolean useRedLaserSound = false;
	private File redLaserSound = new File("sounds/walther_ppq.wav");
	private boolean useGreenLaserSound = false;
	private File greenLaserSound = new File("sounds/walther_ppq.wav");
	private boolean useVirtualMagazine = false;
	private int virtualMagazineCapacity = 7;
	private boolean useMalfunctions = false;
	private float malfunctionsProbability = (float) 10.0;
	private boolean debugMode = false;
	private Set<Camera> recordingCameras = new HashSet<Camera>();
	private Set<CameraManager> recordingManagers = new HashSet<CameraManager>();
	private Set<VideoPlayerController> videoPlayers = new HashSet<VideoPlayerController>();
	private Optional<SessionRecorder> sessionRecorder = Optional.empty();
	private TrainingExercise currentExercise = null;
	private Optional<Color> shotRowColor = Optional.empty();
	private Optional<Point2D> arenaPosition = Optional.empty();
	private Set<String> messagesChimeMuted = new HashSet<String>();

	private int displayWidth = DEFAULT_DISPLAY_WIDTH;

	private int displayHeight = DEFAULT_DISPLAY_HEIGHT;

	private boolean debugShotsRecordToFiles = false;

	private final Set<ShotProcessor> shotProcessors = new HashSet<ShotProcessor>();
	private VirtualMagazineProcessor magazineProcessor = null;
	private MalfunctionsProcessor malfunctionsProcessor = null;
	
	// TODO: This is used at the moment as a constant to determine if the (current incomplete)
	// masking solution should be used. This setting will be unnecessary when the masking code
	// is either complete or removed.
	public static final boolean USE_ARENA_MASK = false;

	protected Configuration(InputStream configInputStream, String name) throws IOException, ConfigurationException {
		configInput = configInputStream;
		configName = name;
		readConfigurationFile();

	}

	public Configuration(String name) throws IOException, ConfigurationException {
		configName = name;
		readConfigurationFile();

	}

	protected Configuration(InputStream configInputStream, String name, String[] args)
			throws IOException, ConfigurationException {
		configInput = configInputStream;
		configName = name;
		parseCmdLine(args);
		readConfigurationFile();
		parseCmdLine(args); // Parse twice so that we guarantee debug is set and
							// override config file

	}

	/**
	 * Loads the configuration from a file named <tt>name</tt> and then updates
	 * the configuration using the programs arguments stored in <tt>args</tt>.
	 * 
	 * @param name
	 *            the configuration file to load properties from
	 * @param args
	 *            the command line arguments for this program
	 * @throws IOException
	 *             <tt>name</tt> doesn't exist on the file system
	 * @throws ConfigurationException
	 *             a specific property value is out of spec
	 */
	public Configuration(String name, String[] args) throws IOException, ConfigurationException {
		configName = name;
		parseCmdLine(args);
		readConfigurationFile();
		parseCmdLine(args);

	}

	public Configuration(String[] args) throws ConfigurationException {
		configName = DEFAULT_CONFIG_FILE;
		parseCmdLine(args);
	}

	private void readConfigurationFile() throws ConfigurationException, IOException {
		Properties prop = new Properties();

		InputStream inputStream;

		if (configInput != null) {
			inputStream = configInput;
		} else {
			try {
				inputStream = new FileInputStream(configName);
			} catch (FileNotFoundException e) {
				throw new FileNotFoundException("Could not read configuration file " + configName);
			}
		}

		try {
			prop.load(inputStream);
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			inputStream.close();
		}

		if (prop.containsKey(FIRST_RUN_PROP)) {
			setFirstRun(Boolean.parseBoolean(prop.getProperty(FIRST_RUN_PROP)));
		} else {
			setFirstRun(false);
		}

		if (prop.containsKey(ERROR_REPORTING_PROP)) {
			setUseErrorReporting(Boolean.parseBoolean(prop.getProperty(ERROR_REPORTING_PROP)));
		}

		if (prop.containsKey(IPCAMS_PROP)) {
			for (String nameString : prop.getProperty(IPCAMS_PROP).split(",")) {
				String[] names = nameString.split("\\|");
				if (names.length == 2) {
					registerIpCam(names[0], names[1], Optional.empty(), Optional.empty());
				} else if (names.length > 2) {
					registerIpCam(names[0], names[1], Optional.of(names[2]), Optional.of(names[3]));
				}
			}
		}

		if (prop.containsKey(WEBCAMS_PROP)) {
			List<String> webcamNames = new ArrayList<String>();
			List<String> webcamInternalNames = new ArrayList<String>();

			for (String nameString : prop.getProperty(WEBCAMS_PROP).split(",")) {
				String[] names = nameString.split(":");
				if (names.length > 1) {
					webcamNames.add(names[0]);
					webcamInternalNames.add(names[1]);
				}
			}

			for (Camera webcam : Camera.getWebcams()) {
				int cameraIndex = webcamInternalNames.indexOf(webcam.getName());
				if (cameraIndex >= 0) {
					webcams.put(webcamNames.get(cameraIndex), webcam);
				}
			}
		}

		Set<Camera> recordingCameras = new HashSet<Camera>();
		if (prop.containsKey(RECORDING_WEBCAMS_PROP)) {
			for (String nameString : prop.getProperty(RECORDING_WEBCAMS_PROP).split(",")) {
				for (Camera webcam : webcams.values()) {
					if (webcam.getName().equals(nameString)) {
						recordingCameras.add(webcam);
						continue;
					}
				}
			}
		}
		setRecordingCameras(recordingCameras);

		if (prop.containsKey(MARKER_RADIUS_PROP)) {
			setMarkerRadius(Integer.parseInt(prop.getProperty(MARKER_RADIUS_PROP)));
		}

		if (prop.containsKey(IGNORE_LASER_COLOR_PROP)) {
			String colorName = prop.getProperty(IGNORE_LASER_COLOR_PROP);

			if (!colorName.equals("None")) {
				setIgnoreLaserColor(true);
				setIgnoreLaserColorName(colorName);
			}
		}

		if (prop.containsKey(USE_RED_LASER_SOUND_PROP)) {
			setUseRedLaserSound(Boolean.parseBoolean(prop.getProperty(USE_RED_LASER_SOUND_PROP)));
		}

		if (prop.containsKey(RED_LASER_SOUND_PROP)) {
			setRedLaserSound(new File(prop.getProperty(RED_LASER_SOUND_PROP)));
		}

		if (prop.containsKey(USE_GREEN_LASER_SOUND_PROP)) {
			setUseGreenLaserSound(Boolean.parseBoolean(prop.getProperty(USE_GREEN_LASER_SOUND_PROP)));
		}

		if (prop.containsKey(GREEN_LASER_SOUND_PROP)) {
			setGreenLaserSound(new File(prop.getProperty(GREEN_LASER_SOUND_PROP)));
		}

		if (prop.containsKey(USE_VIRTUAL_MAGAZINE_PROP)) {
			setUseVirtualMagazine(Boolean.parseBoolean(prop.getProperty(USE_VIRTUAL_MAGAZINE_PROP)));
		}

		if (prop.containsKey(VIRTUAL_MAGAZINE_CAPACITY_PROP)) {
			setVirtualMagazineCapacity(Integer.parseInt(prop.getProperty(VIRTUAL_MAGAZINE_CAPACITY_PROP)));
		}

		if (prop.containsKey(USE_MALFUNCTIONS_PROP)) {
			setMalfunctions(Boolean.parseBoolean(prop.getProperty(USE_MALFUNCTIONS_PROP)));
		}

		if (prop.containsKey(MALFUNCTIONS_PROBABILITY_PROP)) {
			setMalfunctionsProbability(Float.parseFloat(prop.getProperty(MALFUNCTIONS_PROBABILITY_PROP)));
		}

		if (prop.containsKey(ARENA_POSITION_X_PROP) && prop.containsKey(ARENA_POSITION_Y_PROP)) {
			setArenaPosition(Double.parseDouble(prop.getProperty(ARENA_POSITION_X_PROP)),
					Double.parseDouble(prop.getProperty(ARENA_POSITION_Y_PROP)));
		}

		if (prop.containsKey(MUTED_CHIME_MESSAGES)) {
			for (String message : prop.getProperty(MUTED_CHIME_MESSAGES).split("\\|")) {
				muteMessageChime(message);
			}
		}

		validateConfiguration();
	}

	public boolean writeConfigurationFile() throws ConfigurationException, IOException {
		validateConfiguration();

		if (!new File(configName).canWrite()) {
			Alert writeAlert = new Alert(AlertType.ERROR);
			writeAlert.setTitle("Cannot Persist Preferences");
			writeAlert.setHeaderText("Configuration File Unwritable!");
			writeAlert.setResizable(true);
			writeAlert.setContentText("The file " + configName + " is not writable, thus your preferences"
					+ " cannot be saved. This is likely the case because you placed ShootOFF in a location"
					+ " that only the administrator can write to, but ShootOFF is not running as an"
					+ " administrator. Please either move ShootOFF to a different location or grant write"
					+ " privileges to the file.");
			writeAlert.showAndWait();

			return false;
		}

		Properties prop = new Properties();

		StringBuilder ipcamList = new StringBuilder();
		for (Entry<String, URL> entry : ipcams.entrySet()) {
			if (ipcamList.length() > 0) ipcamList.append(",");
			ipcamList.append(entry.getKey());
			ipcamList.append("|");
			ipcamList.append(entry.getValue().toString());

			if (ipcamCredentials.containsKey(entry.getKey())) {
				ipcamList.append("|");
				ipcamList.append(ipcamCredentials.get(entry.getKey()));
			}
		}

		StringBuilder webcamList = new StringBuilder();
		for (Entry<String, Camera> entry : webcams.entrySet()) {
			if (webcamList.length() > 0) webcamList.append(",");
			webcamList.append(entry.getKey());
			webcamList.append(":");
			webcamList.append(entry.getValue().getName());
		}

		StringBuilder recordingWebcamList = new StringBuilder();
		for (Camera c : recordingCameras) {
			if (recordingWebcamList.length() > 0) recordingWebcamList.append(",");
			recordingWebcamList.append(c.getName());
		}

		StringBuilder mutedChimeMessages = new StringBuilder();
		for (String m : messagesChimeMuted) {
			if (mutedChimeMessages.length() > 0) mutedChimeMessages.append("|");
			mutedChimeMessages.append(m);
		}

		prop.setProperty(FIRST_RUN_PROP, String.valueOf(isFirstRun));
		prop.setProperty(ERROR_REPORTING_PROP, String.valueOf(useErrorReporting));
		prop.setProperty(IPCAMS_PROP, ipcamList.toString());
		prop.setProperty(WEBCAMS_PROP, webcamList.toString());
		prop.setProperty(RECORDING_WEBCAMS_PROP, recordingWebcamList.toString());
		prop.setProperty(MARKER_RADIUS_PROP, String.valueOf(markerRadius));
		prop.setProperty(IGNORE_LASER_COLOR_PROP, ignoreLaserColorName);
		prop.setProperty(USE_RED_LASER_SOUND_PROP, String.valueOf(useRedLaserSound));
		prop.setProperty(RED_LASER_SOUND_PROP, redLaserSound.getPath());
		prop.setProperty(USE_GREEN_LASER_SOUND_PROP, String.valueOf(useGreenLaserSound));
		prop.setProperty(GREEN_LASER_SOUND_PROP, greenLaserSound.getPath());
		prop.setProperty(USE_VIRTUAL_MAGAZINE_PROP, String.valueOf(useVirtualMagazine));
		prop.setProperty(VIRTUAL_MAGAZINE_CAPACITY_PROP, String.valueOf(virtualMagazineCapacity));
		prop.setProperty(USE_MALFUNCTIONS_PROP, String.valueOf(useMalfunctions));
		prop.setProperty(MALFUNCTIONS_PROBABILITY_PROP, String.valueOf(malfunctionsProbability));
		prop.setProperty(MUTED_CHIME_MESSAGES, mutedChimeMessages.toString());

		if (getArenaPosition().isPresent()) {
			Point2D arenaPosition = getArenaPosition().get();

			prop.setProperty(ARENA_POSITION_X_PROP, String.valueOf(arenaPosition.getX()));
			prop.setProperty(ARENA_POSITION_Y_PROP, String.valueOf(arenaPosition.getY()));
		}

		OutputStream outputStream = new FileOutputStream(configName);

		try {
			prop.store(outputStream, "ShootOFF Configuration");
			outputStream.flush();
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			outputStream.close();
		}

		return true;
	}

	private void parseCmdLine(String[] args) throws ConfigurationException {
		Options options = new Options();

		options.addOption("d", "debug", false, "turn on debug log messages");
		options.addOption("m", "marker-radius", true, "sets the radius of shot markers in pixels [1,20]");
		options.addOption("c", "ignore-laser-color", true,
				"sets the color of laser that should be ignored by ShootOFF (green "
						+ "or red). No color is ignored by default");
		options.addOption("u", "use-virtual-magazine", true,
				"turns on the virtual magazine and sets the number rounds it holds [1,45]");
		options.addOption("f", "use-malfunctions", true,
				"turns on malfunctions and sets the probability of them happening");

		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("d")) setDebugMode(true);

			if (cmd.hasOption("m")) setMarkerRadius(Integer.parseInt(cmd.getOptionValue("m")));

			if (cmd.hasOption("c")) {
				setIgnoreLaserColor(true);
				setIgnoreLaserColorName(cmd.getOptionValue("c"));
			}

			if (cmd.hasOption("u")) {
				setUseVirtualMagazine(true);
				setVirtualMagazineCapacity(Integer.parseInt(cmd.getOptionValue("u")));
			}

			if (cmd.hasOption("f")) {
				setMalfunctions(true);
				setMalfunctionsProbability(Float.parseFloat(cmd.getOptionValue("f")));
			}
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("com.shootoff.Main", options);
			Main.forceClose(-1);
		}

		validateConfiguration();
	}

	protected void validateConfiguration() throws ConfigurationException {
		if (markerRadius < 1 || markerRadius > 20) {
			throw new ConfigurationException(String.format(MARKER_RADIUS_MESSAGE, markerRadius));
		}

		if (!redLaserSound.isAbsolute())
			redLaserSound = new File(System.getProperty("shootoff.home") + File.separator + redLaserSound.getPath());

		if (useRedLaserSound && !redLaserSound.exists()) {
			throw new ConfigurationException(String.format(LASER_SOUND_MESSAGE, redLaserSound.getPath()));
		}

		if (!greenLaserSound.isAbsolute()) greenLaserSound = new File(
				System.getProperty("shootoff.home") + File.separator + greenLaserSound.getPath());

		if (useGreenLaserSound && !greenLaserSound.exists()) {
			throw new ConfigurationException(String.format(LASER_SOUND_MESSAGE, greenLaserSound.getPath()));
		}

		if (ignoreLaserColor && !ignoreLaserColorName.equals("red") && !ignoreLaserColorName.equals("green")) {
			throw new ConfigurationException(String.format(LASER_COLOR_MESSAGE, ignoreLaserColorName));
		}

		if (virtualMagazineCapacity < 1 || virtualMagazineCapacity > 45) {
			throw new ConfigurationException(String.format(VIRTUAL_MAGAZINE_MESSAGE, virtualMagazineCapacity));
		}

		if (malfunctionsProbability < (float) 0.1 || malfunctionsProbability > (float) 99.9) {
			throw new ConfigurationException(String.format(INJECT_MALFUNCTIONS_MESSAGE, malfunctionsProbability));
		}
	}

	public int getDisplayWidth() {
		return displayWidth;
	}

	public int getDisplayHeight() {
		return displayHeight;
	}

	public void setDisplayResolution(int displayWidth, int displayHeight) {
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
	}

	public boolean isFirstRun() {
		return isFirstRun;
	}

	public void setFirstRun(boolean isFirstRun) {
		this.isFirstRun = isFirstRun;
	}

	public boolean useErrorReporting() {
		return useErrorReporting;
	}

	public void setUseErrorReporting(boolean useErrorReporting) {
		this.useErrorReporting = useErrorReporting;
	}

	public static void disableErrorReporting() {
		Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		setLogConsoleAppender(rootLogger, loggerContext);
	}

	public void registerVideoPlayer(VideoPlayerController videoPlayer) {
		videoPlayers.add(videoPlayer);
	}

	public void unregisterVideoPlayer(VideoPlayerController videoPlayer) {
		videoPlayers.remove(videoPlayer);
	}

	public Set<VideoPlayerController> getVideoPlayers() {
		return videoPlayers;
	}

	public Optional<Camera> registerIpCam(String cameraName, String cameraURL, Optional<String> username,
			Optional<String> password) {
		try {
			URL url = new URL(cameraURL);
			Camera cam = Camera.registerIpCamera(cameraName, url, username, password);
			ipcams.put(cameraName, url);

			if (username.isPresent() && password.isPresent()) {
				ipcamCredentials.put(cameraName, username.get() + "|" + password.get());
			}

			return Optional.of(cam);
		} catch (MalformedURLException | URISyntaxException ue) {
			Alert ipcamURLAlert = new Alert(AlertType.ERROR);
			ipcamURLAlert.setTitle("Malformed URL");
			ipcamURLAlert.setHeaderText("IPCam URL is Malformed!");
			ipcamURLAlert.setResizable(true);
			ipcamURLAlert.setContentText("IPCam URL is not valid: \n\n" + ue.getMessage());
			ipcamURLAlert.showAndWait();
		} catch (UnknownHostException uhe) {
			Alert ipcamHostAlert = new Alert(AlertType.ERROR);
			ipcamHostAlert.setTitle("Unknown Host");
			ipcamHostAlert.setHeaderText("IPCam URL Unknown!");
			ipcamHostAlert.setResizable(true);
			ipcamHostAlert.setContentText("The IPCam at " + cameraURL
					+ " cannot be resolved. Ensure the URL is correct "
					+ "and that you are either connected to the internet or on the same network as the camera.");
			ipcamHostAlert.showAndWait();
		} catch (TimeoutException te) {
			Alert ipcamTimeoutAlert = new Alert(AlertType.ERROR);
			ipcamTimeoutAlert.setTitle("IPCam Timeout");
			ipcamTimeoutAlert.setHeaderText("Connection to IPCam Reached Timeout!");
			ipcamTimeoutAlert.setResizable(true);
			ipcamTimeoutAlert.setContentText("Could not communicate with the IP at " + cameraURL
					+ ". Please check the following:\n\n" + "-The IPCam URL is correct\n"
					+ "-You are connected to the Internet (for external cameras)\n"
					+ "-You are connected to the same network as the camera (for local cameras)");
			ipcamTimeoutAlert.showAndWait();
		}

		return Optional.empty();
	}

	public void unregisterIpCam(String cameraName) {
		if (Camera.unregisterIpCamera(cameraName)) {
			ipcams.remove(cameraName);
			ipcamCredentials.remove(cameraName);
		}
	}

	public void setWebcams(List<String> webcamNames, List<Camera> webcams) {
		this.webcams.clear();

		for (int i = 0; i < webcamNames.size(); i++) {
			this.webcams.put(webcamNames.get(i), webcams.get(i));
		}
	}

	public void setMarkerRadius(int markRadius) {
		this.markerRadius = markRadius;
	}

	public void setIgnoreLaserColor(boolean ignoreLaserColor) {
		this.ignoreLaserColor = ignoreLaserColor;
	}

	public void setIgnoreLaserColorName(String ignoreLaserColorName) {
		this.ignoreLaserColorName = ignoreLaserColorName;
	}

	public void setUseRedLaserSound(Boolean useRedLaserSound) {
		this.useRedLaserSound = useRedLaserSound;
	}

	public void setRedLaserSound(File redLaserSound) {
		this.redLaserSound = redLaserSound;
	}

	public void setUseGreenLaserSound(Boolean useGreenLaserSound) {
		this.useGreenLaserSound = useGreenLaserSound;
	}

	public void setGreenLaserSound(File greenLaserSound) {
		this.greenLaserSound = greenLaserSound;
	}

	public void setUseVirtualMagazine(boolean useVirtualMagazine) {
		this.useVirtualMagazine = useVirtualMagazine;

		if (!useVirtualMagazine && magazineProcessor != null) {
			shotProcessors.remove(magazineProcessor);
			magazineProcessor = null;
		}
	}

	public void setVirtualMagazineCapacity(int virtualMagazineCapacity) {
		this.virtualMagazineCapacity = virtualMagazineCapacity;

		if (useVirtualMagazine) {
			if (magazineProcessor != null) {
				shotProcessors.remove(magazineProcessor);
			}

			magazineProcessor = new VirtualMagazineProcessor(this);
			shotProcessors.add(magazineProcessor);
		}
	}

	public void setMalfunctions(boolean injectMalfunctions) {
		this.useMalfunctions = injectMalfunctions;

		if (!useMalfunctions && malfunctionsProcessor != null) {
			shotProcessors.remove(malfunctionsProcessor);
			malfunctionsProcessor = null;
		}
	}

	public void setMalfunctionsProbability(float injectMalfunctionsProbability) {
		this.malfunctionsProbability = injectMalfunctionsProbability;

		if (useMalfunctions) {
			if (malfunctionsProcessor != null) {
				shotProcessors.remove(malfunctionsProcessor);
			}

			malfunctionsProcessor = new MalfunctionsProcessor(this);
			shotProcessors.add(malfunctionsProcessor);
		}
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;

		if (debugMode) {
			// Ignore first run operations if we are running in debug mode
			setFirstRun(false);
		}

		Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

		if (debugMode) {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			setLogConsoleAppender(rootLogger, loggerContext);

			if (rootLogger.getLevel().equals(Level.TRACE)) {
				return;
			}

			rootLogger.setLevel(Level.DEBUG);

			// Ensure webcam-capture logger stays at info because it is quite
			// noisy
			// and doesn't output information we care about.
			Logger webcamCaptureLogger = (Logger) loggerContext.getLogger("com.github.sarxos");
			webcamCaptureLogger.setLevel(Level.INFO);
		} else {
			rootLogger.setLevel(Level.WARN);
		}
	}

	private static void setLogConsoleAppender(Logger rootLogger, LoggerContext loggerContext) {
		PatternLayoutEncoder ple = new PatternLayoutEncoder();

		ple.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
		ple.setContext(loggerContext);
		ple.start();
		ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<ILoggingEvent>();
		consoleAppender.setEncoder(ple);
		consoleAppender.setContext(loggerContext);
		consoleAppender.start();

		rootLogger.detachAndStopAllAppenders();
		rootLogger.setAdditive(false);
		rootLogger.addAppender(consoleAppender);
	}

	public void setRecordingCameras(Set<Camera> recordingCameras) {
		this.recordingCameras = recordingCameras;
	}

	public void setShotTimerRowColor(Color c) {
		shotRowColor = Optional.ofNullable(c);
	}

	public void muteMessageChime(String message) {
		messagesChimeMuted.add(message);
	}

	public void unmuteMessageChime(String message) {
		messagesChimeMuted.remove(message);
	}

	public Set<Camera> getRecordingCameras() {
		return recordingCameras;
	}

	public void registerRecordingCameraManager(CameraManager cm) {
		recordingManagers.add(cm);
	}

	public void unregisterRecordingCameraManager(CameraManager cm) {
		recordingManagers.remove(cm);
	}

	public void unregisterAllRecordingCameraManagers() {
		recordingManagers.clear();
	}

	public void setSessionRecorder(SessionRecorder sessionRecorder) {
		this.sessionRecorder = Optional.ofNullable(sessionRecorder);
	}

	public void setExercise(TrainingExercise exercise) {
		if (currentExercise != null) currentExercise.destroy();

		currentExercise = exercise;
	}

	public void setArenaPosition(double x, double y) {
		arenaPosition = Optional.of(new Point2D(x, y));
	}

	public Map<String, URL> getRegistedIpCams() {
		return ipcams;
	}

	public Map<String, Camera> getWebcams() {
		return webcams;
	}

	public Optional<String> getWebcamsUserName(Camera webcam) {
		for (Entry<String, Camera> entry : webcams.entrySet()) {
			if (entry.getValue().equals(webcam)) return Optional.of(entry.getKey());
		}

		return Optional.empty();
	}

	public int getMarkerRadius() {
		return markerRadius;
	}

	public boolean ignoreLaserColor() {
		return ignoreLaserColor;
	}

	public Optional<Color> getIgnoreLaserColor() {
		if (ignoreLaserColorName.equals("red")) {
			return Optional.of(Color.RED);
		} else if (ignoreLaserColorName.equals("green")) {
			return Optional.of(Color.GREEN);
		}

		return Optional.empty();
	}

	public String getIgnoreLaserColorName() {
		return ignoreLaserColorName;
	}

	public boolean useRedLaserSound() {
		return useRedLaserSound;
	}

	public File getRedLaserSound() {
		if (!redLaserSound.isAbsolute())
			redLaserSound = new File(System.getProperty("shootoff.home") + File.separator + redLaserSound.getPath());

		return redLaserSound;
	}

	public boolean useGreenLaserSound() {
		return useGreenLaserSound;
	}

	public File getGreenLaserSound() {
		if (!greenLaserSound.isAbsolute()) greenLaserSound = new File(
				System.getProperty("shootoff.home") + File.separator + greenLaserSound.getPath());

		return greenLaserSound;
	}

	public boolean useVirtualMagazine() {
		return useVirtualMagazine;
	}

	public int getVirtualMagazineCapacity() {
		return virtualMagazineCapacity;
	}

	public boolean useMalfunctions() {
		return useMalfunctions;
	}

	public float getMalfunctionsProbability() {
		return malfunctionsProbability;
	}

	public boolean inDebugMode() {
		return debugMode;
	}

	public Optional<SessionRecorder> getSessionRecorder() {
		return sessionRecorder;
	}

	public Set<CameraManager> getRecordingManagers() {
		return recordingManagers;
	}

	public Set<ShotProcessor> getShotProcessors() {
		return shotProcessors;
	}

	public Optional<TrainingExercise> getExercise() {
		if (currentExercise == null) return Optional.empty();

		return Optional.of(currentExercise);
	}

	public Optional<Color> getShotTimerRowColor() {
		return shotRowColor;
	}

	public boolean isDebugShotsRecordToFiles() {
		return debugShotsRecordToFiles;
	}

	public Optional<Point2D> getArenaPosition() {
		return arenaPosition;
	}

	public boolean isChimeMuted(String message) {
		return messagesChimeMuted.contains(message);
	}
}
