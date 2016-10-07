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

package com.shootoff.targets.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import com.shootoff.gui.controller.TargetEditorController;
import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.PolygonRegion;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.animation.GifAnimation;
import com.shootoff.targets.animation.SpriteAnimation;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class XMLTargetReader implements TargetReader {
	private static final Logger logger = LoggerFactory.getLogger(XMLTargetReader.class);

	private final List<Node> targetNodes = new ArrayList<>();
	private final Map<String, String> targetTags = new HashMap<>();
	private final boolean playAnimations;
	private final Optional<ClassLoader> loader;

	public XMLTargetReader(File targetFile, boolean playAnimations) {
		this.playAnimations = playAnimations;
		loader = Optional.empty();

		try (InputStream is = new FileInputStream(targetFile)) {
			load(is);
		} catch (final IOException e) {
			logger.error("Problem initializing target reader from file", e);
		}
	}

	public XMLTargetReader(InputStream targetStream, boolean playAnimations) {
		this.playAnimations = playAnimations;
		loader = Optional.empty();
		load(targetStream);
	}

	public XMLTargetReader(InputStream targetStream, boolean playAnimations, ClassLoader loader) {
		this.playAnimations = playAnimations;
		this.loader = Optional.ofNullable(loader);
		load(targetStream);
	}

	@Override
	public List<Node> getTargetNodes() {
		return targetNodes;
	}

	@Override
	public Map<String, String> getTargetTags() {
		return targetTags;
	}

	private void load(InputStream targetStream) {
		try {
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			final TargetXMLHandler handler = new TargetXMLHandler();
			saxParser.parse(targetStream, handler);

			targetNodes.addAll(handler.getRegions());
			targetTags.putAll(handler.getTags());

			return;
		} catch (IOException | ParserConfigurationException | SAXException e) {
			logger.error("Error reading XML target", e);
		} finally {
			if (targetStream != null) {
				try {
					targetStream.close();
				} catch (final IOException e) {
					logger.error("Error closing XMl target opened for reading", e);
				}
			}
		}
	}

	private class TargetXMLHandler extends DefaultHandler {
		private final Map<String, String> targetTags = new HashMap<>();
		private final List<Node> regions = new ArrayList<>();
		private TargetRegion currentRegion;
		private List<Double> polygonPoints = null;
		private Color polygonFill = null;
		private Map<String, String> currentTags;

		public List<Node> getRegions() {
			return regions;
		}

		public Map<String, String> getTags() {
			return targetTags;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {
			case "target":
				if (attributes.getLength() > 0) {
					for (int i = 0; i < attributes.getLength(); i++) {
						final String key = attributes.getQName(i);
						final String value = attributes.getValue(key);
						targetTags.put(key, value);
					}
				}

				break;

			case "image":
				currentTags = new HashMap<>();

				final File savedFile = new File(attributes.getValue("file"));

				InputStream imageStream = null;
				if ('@' == savedFile.toString().charAt(0) && loader.isPresent()) {
					imageStream = loader.get().getResourceAsStream(savedFile.toString().substring(1).replace("\\", "/"));
				}

				File imageFile;
				if (savedFile.isAbsolute() || '@' == savedFile.toString().charAt(0)) {
					imageFile = savedFile;
				} else {
					imageFile = new File(
							System.getProperty("shootoff.home") + File.separator + attributes.getValue("file"));
				}

				ImageRegion imageRegion;
				if (imageStream != null) {
					imageRegion = new ImageRegion(Double.parseDouble(attributes.getValue("x")),
							Double.parseDouble(attributes.getValue("y")), imageFile, imageStream);
				} else {
					try {
						imageRegion = new ImageRegion(Double.parseDouble(attributes.getValue("x")),
								Double.parseDouble(attributes.getValue("y")), imageFile);
					} catch (final FileNotFoundException e) {
						logger.error("Failed to load target image from file: {}", e);
						return;
					}
				}

				try {
					final int firstDot = imageFile.getName().indexOf('.') + 1;
					final String extension = imageFile.getName().substring(firstDot);

					if (extension.endsWith("gif") && '@' == savedFile.toString().charAt(0) && loader.isPresent()) {
						final InputStream gifStream = loader.get().getResourceAsStream(savedFile.toString().substring(1).replace("\\", "/"));
						final GifAnimation gif = new GifAnimation(imageRegion, gifStream);
						imageRegion.setImage(gif.getFirstFrame());
						if (gif.getFrameCount() > 1) imageRegion.setAnimation(gif);
					} else if (extension.endsWith("gif")) {
						final GifAnimation gif = new GifAnimation(imageRegion, imageRegion.getImageFile());
						imageRegion.setImage(gif.getFirstFrame());
						if (gif.getFrameCount() > 1) imageRegion.setAnimation(gif);
					}

					if (imageRegion.getAnimation().isPresent() && playAnimations) {
						final SpriteAnimation animation = imageRegion.getAnimation().get();
						animation.setCycleCount(1);

						animation.setOnFinished((e) -> {
							animation.reset();
							animation.setOnFinished(null);
						});

						animation.play();
					}
				} catch (final IOException e) {
					logger.error("Error reading animation from XML target", e);
				}

				currentRegion = imageRegion;

				break;
			case "rectangle":
				currentTags = new HashMap<>();
				currentRegion = new RectangleRegion(Double.parseDouble(attributes.getValue("x")),
						Double.parseDouble(attributes.getValue("y")), Double.parseDouble(attributes.getValue("width")),
						Double.parseDouble(attributes.getValue("height")));
				((Shape) currentRegion).setFill(TargetEditorController.createColor(attributes.getValue("fill")));
				break;
			case "ellipse":
				currentTags = new HashMap<>();
				currentRegion = new EllipseRegion(Double.parseDouble(attributes.getValue("centerX")),
						Double.parseDouble(attributes.getValue("centerY")),
						Double.parseDouble(attributes.getValue("radiusX")),
						Double.parseDouble(attributes.getValue("radiusY")));
				((Shape) currentRegion).setFill(TargetEditorController.createColor(attributes.getValue("fill")));
				break;
			case "polygon":
				currentTags = new HashMap<>();
				polygonPoints = new ArrayList<>();
				polygonFill = TargetEditorController.createColor(attributes.getValue("fill"));
				break;
			case "point":
				polygonPoints.add(Double.parseDouble(attributes.getValue("x")));
				polygonPoints.add(Double.parseDouble(attributes.getValue("y")));
				break;
			case "tag":
				currentTags.put(attributes.getValue("name"), attributes.getValue("value"));
				break;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			switch (qName) {
			case "polygon":
				final double[] points = new double[polygonPoints.size()];

				for (int i = 0; i < polygonPoints.size(); i++)
					points[i] = polygonPoints.get(i);

				currentRegion = new PolygonRegion(points);
				((Shape) currentRegion).setFill(polygonFill);
			case "image":
			case "rectangle":
			case "ellipse":
				currentRegion.setTags(currentTags);
				regions.add((Node) currentRegion);
				break;
			}
		}
	}
}
