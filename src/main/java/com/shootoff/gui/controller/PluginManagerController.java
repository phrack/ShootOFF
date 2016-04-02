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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.io.Files;
import com.shootoff.Main;
import com.shootoff.plugins.ExerciseMetadata;
import com.shootoff.plugins.engine.Plugin;
import com.shootoff.plugins.engine.PluginEngine;
import com.shootoff.util.VersionChecker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

public class PluginManagerController {
	private static final Logger logger = LoggerFactory.getLogger(PluginManagerController.class);

	@FXML private TableView<PluginMetadata> pluginsTableView;

	private static final String PLUGIN_METADATA_NAME = "shootoff-plugins.xml";
	private final ObservableList<PluginMetadata> pluginEntries = FXCollections.observableArrayList();

	private PluginEngine pluginEngine;

	public void init(PluginEngine pluginEngine) {
		final Stage pluginManagerStage = (Stage) pluginsTableView.getScene().getWindow();
		final String pluginMetadataAddress = Main.SHOOTOFF_DOMAIN + PLUGIN_METADATA_NAME;

		this.pluginEngine = pluginEngine;

		final TableColumn<PluginMetadata, String> actionCol = new TableColumn<PluginMetadata, String>("Action");
		actionCol.setMinWidth(90);

		actionCol
				.setCellFactory(new Callback<TableColumn<PluginMetadata, String>, TableCell<PluginMetadata, String>>() {
					@Override
					public TableCell<PluginMetadata, String> call(TableColumn<PluginMetadata, String> p) {
						return new ActionTableCell(p);
					}
				});

		final TableColumn<PluginMetadata, String> nameCol = new TableColumn<PluginMetadata, String>("Name");
		nameCol.setMinWidth(160);
		nameCol.setCellValueFactory(new PropertyValueFactory<PluginMetadata, String>("Name"));

		final TableColumn<PluginMetadata, String> versionCol = new TableColumn<PluginMetadata, String>("Version");
		versionCol.setMinWidth(85);
		versionCol.setCellValueFactory(new PropertyValueFactory<PluginMetadata, String>("Version"));

		final TableColumn<PluginMetadata, String> creatorCol = new TableColumn<PluginMetadata, String>("Creator");
		creatorCol.setMinWidth(85);
		creatorCol.setCellValueFactory(new PropertyValueFactory<PluginMetadata, String>("Creator"));

		final TableColumn<PluginMetadata, String> descriptionCol = new TableColumn<PluginMetadata, String>(
				"Description");
		descriptionCol.prefWidthProperty().bind(pluginsTableView.widthProperty()
				.subtract(actionCol.getWidth() + nameCol.getWidth() + versionCol.getWidth() + creatorCol.getWidth()));
		descriptionCol.setCellValueFactory(new PropertyValueFactory<PluginMetadata, String>("Description"));
		descriptionCol
				.setCellFactory(new Callback<TableColumn<PluginMetadata, String>, TableCell<PluginMetadata, String>>() {
					@Override
					public TableCell<PluginMetadata, String> call(TableColumn<PluginMetadata, String> param) {
						final TableCell<PluginMetadata, String> cell = new TableCell<PluginMetadata, String>() {
							@Override
							public void updateItem(String item, boolean empty) {
								super.updateItem(item, empty);
								if (!isEmpty()) {
									final Text text = new Text(item);
									text.wrappingWidthProperty().bind(descriptionCol.widthProperty());
									setGraphic(text);
								}
							}
						};
						return cell;
					}
				});

		pluginsTableView.getColumns().add(actionCol);
		pluginsTableView.getColumns().add(nameCol);
		pluginsTableView.getColumns().add(versionCol);
		pluginsTableView.getColumns().add(creatorCol);
		pluginsTableView.getColumns().add(descriptionCol);

		pluginsTableView.setItems(pluginEntries);

		final Optional<String> pluginMetadata = getPluginMetadataXML(pluginMetadataAddress);

		if (pluginMetadata.isPresent()) {
			final Set<PluginMetadata> parsedMetadata = parsePluginMetadata(pluginMetadata.get());

			if (parsedMetadata.isEmpty()) {
				final Alert metadataAlert = new Alert(AlertType.WARNING);
				metadataAlert.setTitle("No Plugins Found");
				metadataAlert.setHeaderText("Did not find any plugins.");
				metadataAlert.setResizable(true);
				metadataAlert.setContentText("There were no plugins found. This likely means there is a problem with "
						+ "the metadata and you should email us: project.shootoff@gmail.com.");
				metadataAlert.initOwner(pluginManagerStage);
				metadataAlert.showAndWait();
			} else {
				processMetadata(parsedMetadata);
			}
		} else {
			final Alert metadataAlert = new Alert(AlertType.ERROR);
			metadataAlert.setTitle("Failed to Get Exercise Data");
			metadataAlert.setHeaderText("Could not fetch exercise data from the Internet!");
			metadataAlert.setResizable(true);
			metadataAlert.setContentText("ShootOFF could not connect to \n\n" + pluginMetadataAddress + " \n\n"
					+ "to fetch data about known plugins. Please ensure you are connected to the Internet. The "
					+ "exercise manager will now close.");
			metadataAlert.initOwner(pluginManagerStage);
			metadataAlert.showAndWait();

			pluginManagerStage.getOnCloseRequest()
					.handle(new WindowEvent(pluginManagerStage, WindowEvent.WINDOW_CLOSE_REQUEST));
			pluginManagerStage.close();
		}
	}

	private class ActionTableCell extends TableCell<PluginMetadata, String> {
		private TableColumn<PluginMetadata, String> actionColumn;
		private Optional<Task<Boolean>> downloadTask = Optional.empty();

		public ActionTableCell(TableColumn<PluginMetadata, String> actionColumn) {
			this.actionColumn = actionColumn;
			this.setAlignment(Pos.CENTER);
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			if (!empty) {
				final int currentIndex = indexProperty().getValue() < 0 ? 0 : indexProperty().getValue();
				final PluginMetadata metadata = actionColumn.getTableView().getItems().get(currentIndex);
				final Button actionButton = new Button();

				actionButton.setOnAction((e) -> {
					if (downloadTask.isPresent()) {
						downloadTask.get().cancel();
						downloadTask = Optional.empty();
					} else {
						Optional<Plugin> installedPlugin = metadata.findInstalledPlugin(pluginEngine.getPlugins());

						if (installedPlugin.isPresent()) {
							if (uninstallPlugin(installedPlugin.get())) actionButton.setText("Install");
						} else {
							ProgressIndicator progress = new ProgressIndicator();
							progress.setPrefHeight(actionButton.getHeight() - 2);
							progress.setPrefWidth(actionButton.getHeight() - 2);
							progress.setOnMouseClicked((event) -> actionButton.fire());

							actionButton.setGraphic(progress);

							downloadTask = installPlugin(metadata, () -> actionButton.setText("Uninstall"),
									() -> actionButton.setGraphic(null));
						}
					}
				});

				if (metadata.findInstalledPlugin(pluginEngine.getPlugins()).isPresent()) {
					actionButton.setText("Uninstall");
				} else {
					actionButton.setText("Install");
				}

				setGraphic(actionButton);
			}
		}
	};

	private Optional<Task<Boolean>> installPlugin(PluginMetadata metadata, Runnable successAction,
			Runnable completionAction) {
		HttpURLConnection connection = null;
		InputStream stream = null;

		try {
			connection = (HttpURLConnection) new URL(metadata.getDownload()).openConnection();
			stream = connection.getInputStream();
		} catch (UnknownHostException e) {
			logger.error("Could not connect to remote host " + e.getMessage() + " to download plugin.", e);
			return Optional.empty();
		} catch (IOException e) {
			if (connection != null) connection.disconnect();

			logger.error("Failed to get stream to download plugin.", e);
			return Optional.empty();
		}

		final InputStream remoteStream = stream;
		final HttpURLConnection con = connection;
		final File downloadedFile = new File(String.format("%s%s%s-%s.jar", System.getProperty("shootoff.home"),
				File.separator, metadata.getName().replaceAll("\\s", "_"), metadata.getCreator()));
		Task<Boolean> downloadTask = new Task<Boolean>() {
			@Override
			public Boolean call() throws InterruptedException {
				final BufferedInputStream bufferedInputStream = new BufferedInputStream(remoteStream);

				try (FileOutputStream fileOutputStream = new FileOutputStream(downloadedFile)) {
					int count;
					byte buffer[] = new byte[1024];

					while (!isCancelled() && (count = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
						fileOutputStream.write(buffer, 0, count);
					}
				} catch (IOException e) {
					logger.error("Failed to download plugin", e);
					return false;
				}

				return true;
			}
		};

		downloadTask.setOnCancelled((e) -> {
			if (completionAction != null) completionAction.run();

			con.disconnect();

			if (!downloadedFile.delete()) {
				logger.warn("Failed to delete {} from cancelled plugin download.", downloadedFile.getPath());
			}
		});

		downloadTask.setOnSucceeded((e) -> {
			if (completionAction != null) completionAction.run();
			if (successAction != null) successAction.run();

			con.disconnect();

			File pluginFile = new File(
					System.getProperty("shootoff.plugins") + File.separator + downloadedFile.getName());

			try {
				Files.move(downloadedFile, pluginFile);
			} catch (Exception e1) {
				logger.error("Failed to move {} to {} after downloading plugin.", downloadedFile.getPath(),
						pluginFile.getPath());
			}
		});

		new Thread(downloadTask).start();

		return Optional.of(downloadTask);
	}

	private boolean uninstallPlugin(final Plugin plugin) {
		if (!plugin.getJarPath().toFile().delete()) {
			logger.error("Failed to uninstall {}", plugin.getJarPath().toString());
			return false;
		}

		return true;
	}

	private Optional<String> getPluginMetadataXML(String metadataAddress) {
		HttpURLConnection connection = null;
		InputStream stream = null;

		try {
			connection = (HttpURLConnection) new URL(metadataAddress).openConnection();
			stream = connection.getInputStream();
		} catch (UnknownHostException e) {
			logger.error("Could not connect to remote host " + e.getMessage() + " to download plugin metadata.", e);
			return Optional.empty();
		} catch (IOException e) {
			if (connection != null) connection.disconnect();

			logger.error("Error downloading plugin metadata", e);
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

			logger.error("Failed to fetch plugin metadata", e);
			return Optional.empty();
		}

		connection.disconnect();

		return Optional.of(versionXML.toString());
	}

	protected Set<PluginMetadata> parsePluginMetadata(final String pluginMedata) {
		PluginMetadataXMLHandler handler = new PluginMetadataXMLHandler();

		try (InputStream xmlInput = new ByteArrayInputStream(pluginMedata.getBytes("UTF-8"))) {
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(xmlInput, handler);
		} catch (SAXException | ParserConfigurationException | IOException e) {
			logger.error("Error reading plugin metadata XML file from website", e);
		}

		return handler.getPluginMetada();
	}

	private boolean isPluginCompatible(final String minShootOFFVersion, final String maxShootOFFVersion) {
		return isPluginCompatible(Main.getVersion(), minShootOFFVersion, maxShootOFFVersion);
	}

	// For testing
	protected boolean isPluginCompatible(final Optional<String> currentShootOFFVersion, final String minShootOFFVersion,
			final String maxShootOFFVersion) {

		return currentShootOFFVersion.isPresent()
				&& VersionChecker.compareVersions(currentShootOFFVersion.get(), minShootOFFVersion) >= 0
				&& VersionChecker.compareVersions(currentShootOFFVersion.get(), maxShootOFFVersion) <= 0;
	}

	protected static class PluginMetadata {
		private final String name;
		private final String version;
		private final String minShootOFFVersion;
		private final String maxShootOFFVersion;
		private final String creator;
		private final String download;
		private final String description;

		public PluginMetadata(String name, String version, String minShootOFFVersion, String maxShootOFFVersion,
				String creator, String download, String description) {
			this.name = name;
			this.version = version;
			this.minShootOFFVersion = minShootOFFVersion;
			this.maxShootOFFVersion = maxShootOFFVersion;
			this.creator = creator;
			this.download = download;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}

		public String getMinShootOFFVersion() {
			return minShootOFFVersion;
		}

		public String getMaxShootOFFVersion() {
			return maxShootOFFVersion;
		}

		public String getCreator() {
			return creator;
		}

		public String getDownload() {
			return download;
		}

		public String getDescription() {
			return description;
		}

		public Optional<Plugin> findInstalledPlugin(final Set<Plugin> plugins) {
			for (final Plugin p : plugins) {
				final ExerciseMetadata exerciseMetadata = p.getExercise().getInfo();

				if (exerciseMetadata.getName().equals(getName())) {
					return Optional.of(p);
				}
			}

			return Optional.empty();
		}
	}

	private static class PluginMetadataXMLHandler extends DefaultHandler {
		private final Set<PluginMetadata> pluginMetadata = new HashSet<PluginMetadata>();

		public Set<PluginMetadata> getPluginMetada() {
			return pluginMetadata;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {
			case "plugin":
				pluginMetadata.add(new PluginMetadata(attributes.getValue("name"), attributes.getValue("version"),
						attributes.getValue("minShootOFFVersion"), attributes.getValue("maxShootOFFVersion"),
						attributes.getValue("creator"), attributes.getValue("download"),
						attributes.getValue("description")));
				break;
			}
		}
	}

	private void processMetadata(Set<PluginMetadata> pluginMetadata) {
		for (final PluginMetadata metadata : pluginMetadata) {
			final Optional<Plugin> installedPlugin = metadata.findInstalledPlugin(pluginEngine.getPlugins());

			if (isPluginCompatible(metadata.getMinShootOFFVersion(), metadata.getMaxShootOFFVersion())) {
				if (installedPlugin.isPresent()
						&& VersionChecker.compareVersions(installedPlugin.get().getExercise().getInfo().getVersion(),
								metadata.getVersion()) < 0) {
					// Plugin is already installed but the installed version is
					// older than the current compatible version, so auto-update
					// it
					if (uninstallPlugin(installedPlugin.get())) {
						installPlugin(metadata, null, () -> pluginEntries.add(metadata));
					}
				} else {
					pluginEntries.add(metadata);
				}
			}
		}
	}
}
