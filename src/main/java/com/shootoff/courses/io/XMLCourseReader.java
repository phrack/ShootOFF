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

package com.shootoff.courses.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.shootoff.courses.Course;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.TargetView;
import com.shootoff.gui.controller.ProjectorArenaController;
import com.shootoff.targets.Target;
import com.shootoff.targets.io.TargetIO;

import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class XMLCourseReader {
	private static final Logger logger = LoggerFactory.getLogger(XMLCourseReader.class);

	private final ProjectorArenaController arenaController;
	private final File courseFile;

	public XMLCourseReader(ProjectorArenaController arenaController, File courseFile) {
		this.arenaController = arenaController;
		this.courseFile = courseFile;
	}

	public Optional<Course> load() {
		InputStream xmlInput = null;
		try {
			xmlInput = new FileInputStream(courseFile);
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			CourseXMLHandler handler = new CourseXMLHandler();
			saxParser.parse(xmlInput, handler);

			Course c;

			if (handler.getResolution().isPresent()) {
				c = new Course(handler.getBackground(), handler.getTargets(), handler.getResolution().get());
			} else if (handler.getBackground().isPresent()) {
				c = new Course(handler.getBackground().get(), handler.getTargets());
			} else {
				c = new Course(handler.getTargets());
			}

			return Optional.of(c);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			logger.error("Error reading XML course", e);
		} finally {
			if (xmlInput != null) {
				try {
					xmlInput.close();
				} catch (IOException e) {
					logger.error("Error closing XML course opened for reading", e);
				}
			}
		}

		return Optional.empty();
	}

	private class CourseXMLHandler extends DefaultHandler {
		private Optional<LocatedImage> background = Optional.empty();
		private final List<Target> targets = new ArrayList<Target>();
		private Optional<Dimension2D> resolution = Optional.empty();

		public Optional<LocatedImage> getBackground() {
			return background;
		}

		public List<Target> getTargets() {
			return targets;
		}

		public Optional<Dimension2D> getResolution() {
			return resolution;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			switch (qName) {
			case "background": {
				boolean isResource = Boolean.parseBoolean(attributes.getValue("isResource"));

				LocatedImage background;

				if (isResource) {
					InputStream is = this.getClass().getResourceAsStream(attributes.getValue("url"));
					background = new LocatedImage(is, attributes.getValue("url"));
				} else {
					background = new LocatedImage(attributes.getValue("url"));
				}

				this.background = Optional.of(background);
			}
				break;

			case "target": {
				File targetFile = new File(attributes.getValue("file"));
				Optional<Group> targetNodes = TargetIO.loadTarget(targetFile);

				if (targetNodes.isPresent()) {
					TargetView t = new TargetView(targetFile, targetNodes.get(), arenaController.getConfiguration(),
							arenaController.getCanvasManager(), true);

					t.setPosition(Double.parseDouble(attributes.getValue("x")),
							Double.parseDouble(attributes.getValue("y")));

					t.setDimensions(Double.parseDouble(attributes.getValue("width")),
							Double.parseDouble(attributes.getValue("height")));

					targets.add(t);
				} else {
					showTargetError(targetFile.getPath());
				}
			}
				break;

			case "resolution": {
				resolution = Optional.of(new Dimension2D(Double.parseDouble(attributes.getValue("width")),
						Double.parseDouble(attributes.getValue("height"))));
			}
				break;
			}
		}

		private void showTargetError(String targetPath) {
			Platform.runLater(() -> {
				Alert targetAlert = new Alert(AlertType.ERROR);

				String message = String.format(
						"The course %s requires the target %s, but the "
								+ "target file is missing. This target will not appear in your projector arena.",
						courseFile.getName(), targetPath);

				targetAlert.setTitle("Missing Target");
				targetAlert.setHeaderText("Missing Required Target File");
				targetAlert.setResizable(true);
				targetAlert.setContentText(message);
				targetAlert.show();
			});
		}
	}
}
