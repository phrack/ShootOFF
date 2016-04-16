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

package com.shootoff;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.plugins.TextToSpeech;
import com.shootoff.plugins.engine.PluginEngine;
import com.shootoff.util.VersionChecker;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Main extends Application {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static final String SHOOTOFF_DOMAIN = "http://shootoffapp.com/";

	private boolean isJWS = false;
	private static final String RESOURCES_METADATA_NAME = "shootoff-writable-resources.xml";
	private static final String RESOURCES_JAR_NAME = "shootoff-writable-resources.jar";
	private File resourcesMetadataFile;
	private File resourcesJARFile;
	private Stage primaryStage;

	private static final String VERSION_METADATA_NAME = "shootoff-version.xml";
	private static Optional<String> version = Optional.empty();

	protected static class ResourcesInfo {
		private final String version;
		private final long fileSize;
		private final String xml;

		public ResourcesInfo(final String version, final long fileSize, final String xml) {
			this.version = version;
			this.fileSize = fileSize;
			this.xml = xml;
		}

		public String getVersion() {
			return version;
		}

		public long getFileSize() {
			return fileSize;
		}

		public String getXML() {
			return xml;
		}
	}

	private Optional<String> parseField(String metadataXML, String tagName, String fieldName) {
		final String tag = "<" + tagName;
		int tagStart = metadataXML.indexOf(tag);

		if (tagStart == -1) {
			if (logger.isErrorEnabled()) logger.error("Couldn't parse " + tag + " tag from metadata");
			if (isJWS) tryRunningShootOFF();
			return Optional.empty();
		}

		tagStart += tag.length();

		fieldName += "=\"";
		int dataStart = metadataXML.indexOf(fieldName, tagStart);

		if (dataStart == -1) {
			logger.error("Couldn't parse {} field from metadata", fieldName);
			if (isJWS) tryRunningShootOFF();
			return Optional.empty();
		}

		dataStart += fieldName.length();

		final int dataEnd = metadataXML.indexOf("\"", dataStart);

		return Optional.of(metadataXML.substring(dataStart, dataEnd));
	}

	protected Optional<ResourcesInfo> deserializeMetadataXML(final String metadataXML) {
		final Optional<String> version = parseField(metadataXML, "resources", "version");
		final Optional<String> fileSize = parseField(metadataXML, "resources", "fileSize");

		if (version.isPresent() && fileSize.isPresent()) {
			return Optional.of(new ResourcesInfo(version.get(), Long.parseLong(fileSize.get()), metadataXML));
		}

		return Optional.empty();
	}

	private Optional<ResourcesInfo> getWebstartResourcesInfo(final File metadataFile) {
		if (!metadataFile.exists()) {
			logger.error("Local metadata file unavailable");
			return Optional.empty();
		}

		try {
			final String metadataXML = new String(Files.readAllBytes(metadataFile.toPath()), "UTF-8");
			return deserializeMetadataXML(metadataXML);
		} catch (IOException e) {
			logger.error("Error reading metadata XML for JNLP", e);
		}

		return Optional.empty();
	}

	private Optional<ResourcesInfo> getWebstartResourcesInfo(final String metadataAddress) {
		HttpURLConnection connection = null;
		InputStream stream = null;

		try {
			connection = (HttpURLConnection) new URL(metadataAddress).openConnection();
			stream = connection.getInputStream();
		} catch (UnknownHostException e) {
			if (logger.isErrorEnabled())
				logger.error("Could not connect to remote host " + e.getMessage() + " to download writable resources.",
						e);
			tryRunningShootOFF();
			return Optional.empty();
		} catch (IOException e) {
			if (connection != null) connection.disconnect();

			logger.error("Error downloading writable resources file", e);
			tryRunningShootOFF();
			return Optional.empty();
		}

		StringBuilder metadataXML = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (metadataXML.length() > 0) metadataXML.append("\n");
				metadataXML.append(line);
			}
		} catch (IOException e) {
			connection.disconnect();

			logger.error("Failed to read resources metadata", e);
			tryRunningShootOFF();
			return Optional.empty();
		}

		connection.disconnect();

		return deserializeMetadataXML(metadataXML.toString());
	}

	/**
	 * Writable resources (e.g. shootoff.properties, sounds, targets, etc.)
	 * cannot be included in JAR files for a Webstart applications, thus we
	 * download them from a remote URL and extract them locally if necessary.
	 * 
	 * Downloads the file at fileAddress with the assumption that it is a JAR
	 * containing writable resources. If there is an existing JAR with writable
	 * resources we only do the download if the file sizes are different.
	 * 
	 * @param fileAddress
	 *            the url (e.g. http://example.com/file.jar) that contains
	 *            ShootOFF's writable resources
	 */
	private void downloadWebstartResources(ResourcesInfo ri, String fileAddress) {
		HttpURLConnection connection = null;
		InputStream stream = null;

		try {
			connection = (HttpURLConnection) new URL(fileAddress).openConnection();
			stream = connection.getInputStream();
		} catch (UnknownHostException e) {
			logger.error("Could not connect to remote host " + e.getMessage() + " to download writable resources.", e);
			tryRunningShootOFF();
			return;
		} catch (IOException e) {
			if (connection != null) connection.disconnect();

			logger.error("Failed to get stream to download writable resources file", e);
			tryRunningShootOFF();
			return;
		}

		long remoteFileLength = ri.getFileSize();

		if (remoteFileLength == 0) {
			logger.error("Remote writable resources file query returned 0 len.");
			connection.disconnect();
			tryRunningShootOFF();
			return;
		}

		final InputStream remoteStream = stream;
		final Task<Boolean> task = new Task<Boolean>() {
			@Override
			public Boolean call() throws InterruptedException {
				BufferedInputStream bufferedInputStream = new BufferedInputStream(remoteStream);

				try (FileOutputStream fileOutputStream = new FileOutputStream(resourcesJARFile)) {

					long totalDownloaded = 0;
					int count;
					byte buffer[] = new byte[1024];

					while ((count = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
						fileOutputStream.write(buffer, 0, count);
						totalDownloaded += count;
						updateProgress(((double) totalDownloaded / (double) remoteFileLength) * 100, 100);
					}

					updateProgress(100, 100);
				} catch (IOException e) {
					logger.error("Failed to download writable resources file", e);
					return false;
				}

				return true;
			}
		};

		final ProgressDialog progressDialog = new ProgressDialog("Downloading Resources...",
				"Downloading required resources (targets, sounds, etc.)...", task);
		final HttpURLConnection con = connection;
		task.setOnSucceeded((value) -> {
			progressDialog.close();
			con.disconnect();
			if (task.getValue()) {
				try {
					PrintWriter out = new PrintWriter(resourcesMetadataFile, "UTF-8");
					out.print(ri.getXML());
					out.close();
				} catch (IOException e) {
					if (logger.isErrorEnabled()) logger.error("Could't update metadata file: " + e.getMessage(), e);
				}

				extractWebstartResources();
			} else {
				tryRunningShootOFF();
			}
		});

		new Thread(task, "DownloadJNLPResources").start();
	}

	/**
	 * If we could not acquire writable resources for Webstart, see if we have
	 * enough to run anyway.
	 */
	private void tryRunningShootOFF() {
		if (!new File(System.getProperty("shootoff.home") + File.separator + "shootoff.properties").exists()) {
			Alert resourcesAlert = new Alert(AlertType.ERROR);
			resourcesAlert.setTitle("Missing Resources");
			resourcesAlert.setHeaderText("Missing Required Resources!");
			resourcesAlert.setResizable(true);
			resourcesAlert.setContentText("ShootOFF could not acquire the necessary resources to run. Please ensure "
					+ "you have a connection to the Internet and can connect to http://shootoffapp.com and try again.\n\n"
					+ "If you cannot get the browser-launched version of ShootOFF to work, use the standlone version from "
					+ "the website.");
			resourcesAlert.showAndWait();
		} else {
			runShootOFF();
		}
	}

	private void extractWebstartResources() {
		final Task<Boolean> task = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				JarFile jar = null;

				try {
					jar = new JarFile(resourcesJARFile);

					Enumeration<JarEntry> enumEntries = jar.entries();
					int fileCount = 0;
					while (enumEntries.hasMoreElements()) {
						JarEntry entry = (JarEntry) enumEntries.nextElement();
						if (!entry.getName().startsWith("META-INF") && !entry.isDirectory()) fileCount++;
					}

					enumEntries = jar.entries();
					int currentCount = 0;
					while (enumEntries.hasMoreElements()) {
						JarEntry entry = (JarEntry) enumEntries.nextElement();

						if (entry.getName().startsWith("META-INF")) continue;

						File f = new File(System.getProperty("shootoff.home") + File.separator + entry.getName());
						if (entry.isDirectory()) {
							if (!f.exists() && !f.mkdir()) {
								IOException e = new IOException(
										"Failed to make directory while extracting JAR: " + entry.getName());
								logger.error("Error making directory to extract writable JAR contents", e);
								throw e;
							}
						} else {
							InputStream is = jar.getInputStream(entry);
							try (FileOutputStream fos = new FileOutputStream(f)) {
								while (is.available() > 0) {
									fos.write(is.read());
								}
							}
							is.close();

							currentCount++;
							updateProgress(((double) currentCount / (double) fileCount) * 100, 100);
						}
					}

					updateProgress(100, 100);
				} catch (IOException e) {
					logger.error("Error extracting writable resources file for JNLP", e);
					return false;
				} finally {
					try {
						if (jar != null) jar.close();
					} catch (IOException e) {
						logger.error("Error closing writable resources file for JNLP", e);
					}
				}

				return true;
			}
		};

		final ProgressDialog progressDialog = new ProgressDialog("Extracting Resources...",
				"Extracting required resources (targets, sounds, etc.)...", task);
		task.setOnSucceeded((value) -> {
			progressDialog.close();
			if (task.getValue()) {
				runShootOFF();
			} else {
				tryRunningShootOFF();
			}
		});

		new Thread(task, "ExtractJNLPResources").start();
	}

	public static class ProgressDialog {
		private final Stage stage = new Stage();
		private final Label messageLabel = new Label();
		private final ProgressBar pb = new ProgressBar();
		private final ProgressIndicator pin = new ProgressIndicator();

		public ProgressDialog(String dialogTitle, String dialogMessage, final Task<?> task) {
			stage.setTitle(dialogTitle);
			stage.initModality(Modality.APPLICATION_MODAL);

			pb.setProgress(-1F);
			pin.setProgress(-1F);

			messageLabel.setText(dialogMessage);

			final HBox hb = new HBox();
			hb.setSpacing(5);
			hb.setAlignment(Pos.CENTER);
			hb.getChildren().addAll(pb, pin);

			pb.prefWidthProperty().bind(hb.widthProperty().subtract(hb.getSpacing() * 6));

			final BorderPane bp = new BorderPane();
			bp.setTop(messageLabel);
			bp.setBottom(hb);

			final Scene scene = new Scene(bp);

			stage.setScene(scene);
			stage.show();

			pb.progressProperty().bind(task.progressProperty());
			pin.progressProperty().bind(task.progressProperty());
		}

		public void close() {
			stage.close();
		}
	}

	public static void forceClose(final int status) {
		System.exit(status);
	}

	private Optional<String> getVersionXML(final String versionAddress) {
		HttpURLConnection connection = null;
		InputStream stream = null;

		try {
			connection = (HttpURLConnection) new URL(versionAddress).openConnection();
			stream = connection.getInputStream();
		} catch (UnknownHostException e) {
			logger.error("Could not connect to remote host " + e.getMessage() + " to download version metadata.", e);
			return Optional.empty();
		} catch (IOException e) {
			if (connection != null) connection.disconnect();

			logger.error("Error downloading version metadata", e);
			return Optional.empty();
		}

		StringBuilder versionXML = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {

			String line;
			while ((line = br.readLine()) != null) {
				if (versionXML.length() > 0) versionXML.append("\n");
				versionXML.append(line);
			}
		} catch (IOException e) {
			connection.disconnect();

			logger.error("Failed to read version metadata", e);
			return Optional.empty();
		}

		connection.disconnect();

		return Optional.of(versionXML.toString());
	}

	public void checkVersion() {
		final Optional<String> versionXML = getVersionXML(SHOOTOFF_DOMAIN + VERSION_METADATA_NAME);

		if (versionXML.isPresent()) {
			final Optional<String> stableVersion = parseField(versionXML.get(), "stableRelease", "version");

			if (stableVersion.isPresent() && VersionChecker.compareVersions(stableVersion.get(), version.get()) > 0) {
				final Optional<String> downloadLink = parseField(versionXML.get(), "stableRelease", "download");

				final String link;

				if (downloadLink.isPresent())
					link = downloadLink.get();
				else
					link = SHOOTOFF_DOMAIN;

				final Alert shootoffWelcome = new Alert(AlertType.INFORMATION);
				shootoffWelcome.setTitle("ShootOFF Updated");
				shootoffWelcome.setHeaderText("This version of ShootOFF is outdated!");
				shootoffWelcome.setResizable(true);

				final FlowPane fp = new FlowPane();
				final Label lbl = new Label(
						"The current stable release of ShootOFF is " + stableVersion.get() + ", but you are running "
								+ version.get() + ". " + "You can download the current version of ShootOFF here:\n\n");

				final Hyperlink lnk = new Hyperlink(link);

				lnk.setOnAction((event) -> {
					HostServicesDelegate hostServices = HostServicesFactory.getInstance(this);
					hostServices.showDocument(link);
					lnk.setVisited(true);
				});

				fp.getChildren().addAll(lbl, lnk);

				shootoffWelcome.getDialogPane().contentProperty().set(fp);
				shootoffWelcome.showAndWait();
			} else if (stableVersion.isPresent() && stableVersion.get().compareTo(version.get()) < 0) {
				logger.warn("Future version of ShootOFF? stableVersion = {}, this.version = {}", stableVersion.get(),
						version.get());
			} else {
				logger.debug("ShootOFF is up to date");
			}
		}
	}

	public void runShootOFF() {
		final String[] args = getParameters().getRaw().toArray(new String[getParameters().getRaw().size()]);
		Configuration config;
		try {
			config = new Configuration(System.getProperty("shootoff.home") + File.separator + "shootoff.properties",
					args);
		} catch (IOException | ConfigurationException e) {
			logger.error("Error fetching ShootOFF configuration to run ShootOFF", e);
			return;
		}

		if (version.isPresent() && !config.inDebugMode() && !isJWS) checkVersion();

		// This initializes the TTS engine
		TextToSpeech.say("");

		if (config.isFirstRun()) {
			config.setUseErrorReporting(showFirstRunMessage());

			config.setFirstRun(false);
			try {
				config.writeConfigurationFile();
			} catch (ConfigurationException | IOException e) {
				logger.error("Error persisting firstrun = false in config", e);
			}
		}

		// This simply ensures that error reporting is turned off,
		// once it's off it stays off
		if (!config.useErrorReporting() || config.inDebugMode()) {
			Configuration.disableErrorReporting();
			logger.info("Error reporting has been disabled.");
		}

		try {
			final FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/shootoff/gui/ShootOFF.fxml"));
			loader.load();

			final Scene scene = new Scene(loader.getRoot());

			if (version.isPresent())
				primaryStage.setTitle("ShootOFF " + version.get());
			else
				primaryStage.setTitle("ShootOFF");
			primaryStage.setScene(scene);
			final ShootOFFController controller = (ShootOFFController) loader.getController();
			controller.init(config, new PluginEngine(controller));
			primaryStage.show();
		} catch (IOException e) {
			logger.error("Error loading ShootOFF FXML file", e);
			return;
		}
	}

	private boolean showFirstRunMessage() {
		final Alert shootoffWelcome = new Alert(AlertType.INFORMATION);
		shootoffWelcome.setTitle("Welcome to ShootOFF");
		shootoffWelcome.setHeaderText("Please Ensure Your Firearm is Unloaded!");
		shootoffWelcome.setResizable(true);

		final FlowPane fp = new FlowPane();
		final Label lbl = new Label("Thank you for choosing ShootOFF for your training needs.\n"
				+ "Please be careful to ensure your firearm is not loaded\n"
				+ "every time you use ShootOFF. We are not liable for any\n"
				+ "negligent discharges that may result from your use of this\n" + "software.\n\n"
				+ "We upload most errors that cause crashes to our servers to\n"
				+ "help us detect and fix common problems. We do not include any\n"
				+ "personal information in these reports, but you may uncheck\n"
				+ "the box below if you do not want to support this effort.\n\n");
		final CheckBox useErrorReporting = new CheckBox("Allow ShootOFF to Send Error Reports");
		useErrorReporting.setSelected(true);

		fp.getChildren().addAll(lbl, useErrorReporting);

		shootoffWelcome.getDialogPane().contentProperty().set(fp);
		shootoffWelcome.showAndWait();

		return useErrorReporting.isSelected();
	}

	public static void closeNoCamera() {
		final Alert cameraAlert = new Alert(AlertType.ERROR);
		cameraAlert.setTitle("No Webcams");
		cameraAlert.setHeaderText("No Webcams Found!");
		cameraAlert.setResizable(true);
		cameraAlert.setContentText("ShootOFF needs a webcam to function. Now closing...");
		cameraAlert.showAndWait();
		Main.forceClose(-1);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;

		final String os = System.getProperty("os.name");
		if (os != null && "Mac OS X".equals(os) && Camera.getWebcams().isEmpty()) {
			closeNoCamera();
		}

		if (System.getProperty("javawebstart.version", null) != null) {
			isJWS = true;
			File shootoffHome = new File(System.getProperty("user.home") + File.separator + ".shootoff");

			if (!shootoffHome.exists() && !shootoffHome.mkdirs()) {
				final Alert homeAlert = new Alert(AlertType.ERROR);
				homeAlert.setTitle("No ShootOFF Home");
				homeAlert.setHeaderText("Missing ShootOFF's Home Directory!");
				homeAlert.setResizable(true);
				homeAlert.setContentText("ShootOFF's home directory " + shootoffHome.getPath() + " "
						+ "does not exist and could not be created. Now closing...");
				homeAlert.showAndWait();
				return;
			}

			if (System.getProperty("shootoff.home") == null) {
				System.setProperty("shootoff.home", shootoffHome.getAbsolutePath());
			}
			System.setProperty("shootoff.sessions", System.getProperty("shootoff.home") + File.separator + "sessions");
			System.setProperty("shootoff.courses", System.getProperty("shootoff.home") + File.separator + "courses");
			System.setProperty("shootoff.plugins", System.getProperty("shootoff.home") + File.separator + "exercises");

			resourcesMetadataFile = new File(
					System.getProperty("shootoff.home") + File.separator + RESOURCES_METADATA_NAME);
			final Optional<ResourcesInfo> localRI = getWebstartResourcesInfo(resourcesMetadataFile);
			final Optional<ResourcesInfo> remoteRI = getWebstartResourcesInfo(
					SHOOTOFF_DOMAIN + "jws/" + RESOURCES_METADATA_NAME);

			if (!localRI.isPresent() && remoteRI.isPresent()) {
				resourcesJARFile = new File(System.getProperty("shootoff.home") + File.separator + RESOURCES_JAR_NAME);
				downloadWebstartResources(remoteRI.get(), "http://shootoffapp.com/jws/" + RESOURCES_JAR_NAME);
			} else if (localRI.isPresent() && remoteRI.isPresent()) {
				if (localRI.get().getVersion().equals(remoteRI.get().getVersion())) {
					runShootOFF();
				} else {
					System.out.println(String.format("Local version: %s, Remote version: %s",
							localRI.get().getVersion(), remoteRI.get().getVersion()));
					resourcesJARFile = new File(
							System.getProperty("shootoff.home") + File.separator + RESOURCES_JAR_NAME);
					downloadWebstartResources(remoteRI.get(), "http://shootoffapp.com/jws/" + RESOURCES_JAR_NAME);
				}
			} else {
				logger.error("Could not locate local or remote resources metadata");
			}
		} else {
			if (System.getProperty("shootoff.home") == null) {
				System.setProperty("shootoff.home", System.getProperty("user.dir"));
			}
			System.setProperty("shootoff.sessions", System.getProperty("shootoff.home") + File.separator + "sessions");
			System.setProperty("shootoff.courses", System.getProperty("shootoff.home") + File.separator + "courses");
			System.setProperty("shootoff.plugins", System.getProperty("shootoff.home") + File.separator + "exercises");
			runShootOFF();
		}
	}

	public static Optional<String> getVersion() {
		return version;
	}

	public static void main(String[] args) {
		// Check the comment at the top of the Camera class
		// for more information about this hack
		final String os = System.getProperty("os.name");

		if (os != null) {
			if ("Mac OS X".equals(os)) {
				Camera.getDefault();
			} else if (os.startsWith("Windows")) {
				// OpenPNP's OpenCV wrapper for Java does not properly clean up
				// after itself on Windows, thus it can fill the drive with
				// stale temporary files. This hack works around the problem
				// by giving ShootOFF on Windows its own instance of a temp
				// directory that we can safely purge of stale files at
				// the start of each session.
				System.setProperty("java.io.tmpdir", System.getProperty("user.dir") + File.separator + "temp_bins");

				final File tempBinsDir = new File(System.getProperty("java.io.tmpdir"));

				if (tempBinsDir.exists()) {
					try {
						Files.walkFileTree(tempBinsDir.toPath(), new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
									throws IOException {
								if (!Files.isSameFile(tempBinsDir.toPath(), dir)) Files.deleteIfExists(dir);
								return super.postVisitDirectory(dir, e);
							}

							@Override
							public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
									throws IOException {
								Files.deleteIfExists(file);
								return super.visitFile(file, attrs);
							}
						});
					} catch (IOException e) {
						logger.error("Failed walk temp_bins to delete old folders.");
					}
				} else if (!tempBinsDir.mkdir()) {
					logger.error("Failed to create temporary directory to store ShootOFF binaries.");
				}
			}
		}

		nu.pattern.OpenCV.loadShared();

		// Read ShootOFF's version number
		final Properties prop = new Properties();

		try (InputStream inputStream = Main.class.getResourceAsStream("/version.properties")) {
			prop.load(inputStream);
			version = Optional.of(prop.getProperty("version"));
		} catch (IOException ioe) {
			logger.error("Couldn't read version properties", ioe);
		}

		launch(args);
	}
}