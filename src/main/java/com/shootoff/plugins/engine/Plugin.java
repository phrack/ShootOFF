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

package com.shootoff.plugins.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.shootoff.plugins.ProjectorTrainingExerciseBase;
import com.shootoff.plugins.TrainingExercise;
import com.shootoff.plugins.TrainingExerciseBase;

public class Plugin {
	private static final Logger logger = LoggerFactory.getLogger(Plugin.class);

	private URLClassLoader loader;
	private final TrainingExercise mainClass;
	private final Path jarPath;
	private final PluginType type;

	public Plugin(Path jarPath) throws ParserConfigurationException, SAXException, IOException {
		this.jarPath = jarPath;

		AccessController.doPrivileged((PrivilegedAction<Void>) 
			() -> {
				try {
					loader = new URLClassLoader(new URL[] { jarPath.toUri().toURL() }, Plugin.class.getClassLoader());
				} catch (Exception e) {
					logger.error("Malformed jarPath", e);
				}
				return null;
		});
		
		InputStream pluginSettings = loader.getResourceAsStream("shootoff.xml");

		if (pluginSettings == null) {
			throw new IllegalArgumentException(
					String.format("The jarPath %s does not represent a valid ShootOFF plugin", jarPath));
		}

		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		PluginSettionsXMLHandler handler = new PluginSettionsXMLHandler();
		saxParser.parse(pluginSettings, handler);

		if (handler.getMainClass() == null) {
			throw new IllegalArgumentException(
					String.format("Could not fetch main class for newly discovered exercise at %s", jarPath));
		}

		mainClass = handler.getMainClass();
		type = handler.getType();
	}

	private class PluginSettionsXMLHandler extends DefaultHandler {
		private TrainingExercise mainClass;
		private PluginType type;

		public TrainingExercise getMainClass() {
			return mainClass;
		}

		public PluginType getType() {
			return type;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			switch (qName) {
			case "shootoffExercise": {
				String mainClassName = attributes.getValue("exerciseClass");
				Class<?> exerciseClass;
				try {
					exerciseClass = loader.loadClass(mainClassName);
				} catch (ClassNotFoundException e) {
					logger.error("Configured exerciseClass ({}) was not found", mainClassName);
					return;
				}

				// No superclass or not a known training exercise superclass
				boolean isStandardExercise = exerciseClass.getSuperclass().getName()
						.equals(TrainingExerciseBase.class.getName());
				boolean isProjectorOnlyExercise = exerciseClass.getSuperclass().getName()
						.equals(ProjectorTrainingExerciseBase.class.getName());

				if (exerciseClass.getSuperclass() == null || (!isStandardExercise && !isProjectorOnlyExercise)) {
					String superclassName;
					if (exerciseClass.getSuperclass() == null) {
						superclassName = "null";
					} else {
						superclassName = exerciseClass.getSuperclass().getName();
					}

					logger.error(
							"Configured exerciseClass ({}) does not have a known training exercise superclass, type is {}",
							mainClassName, superclassName);
					return;
				}

				if (isStandardExercise) {
					type = PluginType.STANDARD;
				} else if (isProjectorOnlyExercise) {
					type = PluginType.PROJECTOR_ONLY;
				}

				try {
					mainClass = (TrainingExercise) exerciseClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					logger.error("Error instantiating configured exerciseClass", e);
				}
			}
				break;

			default:
				logger.warn("Unrecognized exercise settings tag ignored: {}", qName);
			}
		}
	}

	public URLClassLoader getLoader() {
		return loader;
	}

	public TrainingExercise getMainClass() {
		return mainClass;
	}

	public Path getJarPath() {
		return jarPath;
	}

	public PluginType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jarPath == null) ? 0 : jarPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Plugin other = (Plugin) obj;
		if (jarPath == null) {
			if (other.jarPath != null) return false;
		} else if (!jarPath.equals(other.jarPath)) return false;
		return true;
	}
}