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

package com.shootoff.targets.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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

public class XMLTargetReader {
	private final File targetFile;
	
	public XMLTargetReader(File targetFile) {
		this.targetFile = targetFile;
	}
	
	public List<Node> load() {
		try {
			InputStream xmlInput = new FileInputStream(targetFile);
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			TargetXMLHandler handler   = new TargetXMLHandler();
			saxParser.parse(xmlInput, handler);
			
			return handler.getRegions();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		
		return new ArrayList<Node>();
	}
	
	private static class TargetXMLHandler extends DefaultHandler {
		List<Node> regions = new ArrayList<Node>();
		TargetRegion currentRegion;
		List<Double> polygonPoints = null;
		Color polygonFill = null;
		Map<String, String> currentTags;
		
		public List<Node> getRegions() {
			return regions;
		}
		
		public void startElement(String uri, String localName,String qName, 
                Attributes attributes) throws SAXException {
			
			switch (qName) {
			case "image":
				currentTags = new HashMap<String, String>();
				
				File imageFile = new File(attributes.getValue("file"));
				
				ImageRegion imageRegion = new ImageRegion(
						Double.parseDouble(attributes.getValue("x")),
						Double.parseDouble(attributes.getValue("y")),
						imageFile);
				try {
					int firstDot = imageFile.getName().indexOf('.') + 1;
					String extension = imageFile.getName().substring(firstDot);
					
					if (extension.endsWith("gif")) {
						GifAnimation gif = new GifAnimation(imageRegion, imageRegion.getImageFile());
						imageRegion.setImage(gif.getFirstFrame());
						if (gif.getFrameCount() > 1) imageRegion.setAnimation(gif);
					}
					
					if (imageRegion.getAnimation().isPresent()) {
						SpriteAnimation animation = imageRegion.getAnimation().get();
						animation.setCycleCount(1);
						
						animation.setOnFinished((e) -> {
								animation.reset();
								animation.setOnFinished(null);
							});
						
						animation.play();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				currentRegion = imageRegion;
				
				break;
			case "rectangle":
				currentTags = new HashMap<String, String>();
				currentRegion = new RectangleRegion(
						Double.parseDouble(attributes.getValue("x")),
						Double.parseDouble(attributes.getValue("y")),
						Double.parseDouble(attributes.getValue("width")),
						Double.parseDouble(attributes.getValue("height")));
				((Shape)currentRegion).setFill(TargetEditorController
						.createColor(attributes.getValue("fill")));
				break;
			case "ellipse":
				currentTags = new HashMap<String, String>();
				currentRegion = new EllipseRegion(
						Double.parseDouble(attributes.getValue("centerX")),
						Double.parseDouble(attributes.getValue("centerY")),
						Double.parseDouble(attributes.getValue("radiusX")),
						Double.parseDouble(attributes.getValue("radiusY")));
				((Shape)currentRegion).setFill(TargetEditorController
						.createColor(attributes.getValue("fill")));
				break;
			case "polygon":
				currentTags = new HashMap<String, String>();
				polygonPoints = new ArrayList<Double>();
				polygonFill = TargetEditorController
						.createColor(attributes.getValue("fill"));
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
		
		public void endElement(String uri, String localName,
				String qName) throws SAXException {
			switch (qName) {
			case "polygon":
				double[] points = new double[polygonPoints.size()];
				
				for (int i = 0; i < polygonPoints.size(); i++) 
					points[i] = polygonPoints.get(i);
					
				currentRegion = new PolygonRegion(points);
				((Shape)currentRegion).setFill(polygonFill);
			case "image":
			case "rectangle":
			case "ellipse":
				currentRegion.setTags(currentTags);
				regions.add((Node)currentRegion);
				break;
			}
		}
	}
}
