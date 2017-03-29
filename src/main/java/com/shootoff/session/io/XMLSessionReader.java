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

package com.shootoff.session.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.shootoff.camera.shot.DisplayShot;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.session.Event;
import com.shootoff.session.ExerciseFeedMessageEvent;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

public class XMLSessionReader {
	private final Logger logger = LoggerFactory.getLogger(XMLSessionReader.class);

	private final File sessionFile;

	private long lastTimestamp;
	private boolean exerciseFeedMessage = false;

	public XMLSessionReader(File sessionFile) {
		this.sessionFile = sessionFile;
	}

	public Map<String, List<Event>> load() {
		InputStream xmlInput = null;
		try {
			xmlInput = new FileInputStream(sessionFile);
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			final SessionXMLHandler handler = new SessionXMLHandler();
			saxParser.parse(xmlInput, handler);

			return handler.getEvents();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			logger.error("Error reading XML session", e);
		} finally {
			if (xmlInput != null) {
				try {
					xmlInput.close();
				} catch (final IOException e) {
					logger.error("Error closing XML session opened for reading", e);
				}
			}
		}

		return new HashMap<>();
	}

	private class SessionXMLHandler extends DefaultHandler {
		private final Map<String, List<Event>> events = new HashMap<>();
		private String currentCameraName = "";

		public Map<String, List<Event>> getEvents() {
			return events;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {
			case "camera":
				currentCameraName = attributes.getValue("name");
				events.put(currentCameraName, new ArrayList<Event>());
				break;

			case "shot":
				ShotColor c;

				if (attributes.getValue("color").equals("0xff0000ff") || attributes.getValue("color").equals("RED")) {
					c = ShotColor.RED;
				}
				else if (attributes.getValue("color").equals("0xffa500ff") || attributes.getValue("color").equals("INFRARED"))
				{
					c = ShotColor.INFRARED;
				} else {
					c = ShotColor.GREEN;
				}

				final DisplayShot shot = new DisplayShot(c, Double.parseDouble(attributes.getValue("x")),
						Double.parseDouble(attributes.getValue("y")),
						Long.parseLong(attributes.getValue("shotTimestamp")),
						Integer.parseInt(attributes.getValue("markerRadius")));

				final boolean isMalfunction = Boolean.parseBoolean(attributes.getValue("isMalfunction"));

				final boolean isReload = Boolean.parseBoolean(attributes.getValue("isReload"));

				Optional<Integer> targetIndex;
				int index = Integer.parseInt(attributes.getValue("targetIndex"));
				if (index == -1) {
					targetIndex = Optional.empty();
				} else {
					targetIndex = Optional.of(index);
				}

				Optional<Integer> hitRegionIndex;
				index = Integer.parseInt(attributes.getValue("hitRegionIndex"));
				if (index == -1) {
					hitRegionIndex = Optional.empty();
				} else {
					hitRegionIndex = Optional.of(index);
				}

				final Optional<String> videoString = Optional.ofNullable(attributes.getValue("videos"));

				events.get(currentCameraName)
				.add(new ShotEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")), shot,
						isMalfunction, isReload, targetIndex, hitRegionIndex, videoString));

				break;

			case "targetAdded":
				events.get(currentCameraName).add(new TargetAddedEvent(currentCameraName,
						Long.parseLong(attributes.getValue("timestamp")), attributes.getValue("name")));

				break;

			case "targetRemoved":
				events.get(currentCameraName)
				.add(new TargetRemovedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")),
						Integer.parseInt(attributes.getValue("index"))));

				break;

			case "targetResized":
				events.get(currentCameraName)
				.add(new TargetResizedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")),
						Integer.parseInt(attributes.getValue("index")),
						Double.parseDouble(attributes.getValue("newWidth")),
						Double.parseDouble(attributes.getValue("newHeight"))));

				break;

			case "targetMoved":
				events.get(currentCameraName)
				.add(new TargetMovedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")),
						Integer.parseInt(attributes.getValue("index")),
						Integer.parseInt(attributes.getValue("newX")),
						Integer.parseInt(attributes.getValue("newY"))));
				break;

			case "exerciseFeedMessage":
				lastTimestamp = Long.parseLong(attributes.getValue("timestamp"));
				exerciseFeedMessage = true;
			}
		}

		@Override
		public void characters(char ch[], int start, int length) throws SAXException {
			if (exerciseFeedMessage) {
				events.get(currentCameraName).add(
						new ExerciseFeedMessageEvent(currentCameraName, lastTimestamp, new String(ch, start, length)));

				exerciseFeedMessage = false;
			}
		}
	}
}
