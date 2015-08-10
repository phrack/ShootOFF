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

package com.shootoff.session.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.shootoff.camera.Shot;
import com.shootoff.session.Event;
import com.shootoff.session.ProtocolFeedMessageEvent;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

import javafx.scene.paint.Color;

public class JSONSessionReader {
	private final File sessionFile;
	
	public JSONSessionReader(File sessionFile) {
		this.sessionFile = sessionFile;
	}
	
	public Map<String, List<Event>> load() {
		Map<String, List<Event>> events = new HashMap<String, List<Event>>();
		
		try {
			JSONObject session = (JSONObject)new JSONParser().parse(new FileReader(sessionFile));
			
			JSONArray cameras = (JSONArray)session.get("cameras");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> itCameras = cameras.iterator();
			
			while (itCameras.hasNext()) {
				JSONObject camera = itCameras.next();
				
				String cameraName = (String)camera.get("name");
				events.put(cameraName, new ArrayList<Event>());
				
				JSONArray cameraEvents = (JSONArray)camera.get("events");
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> itEvents = cameraEvents.iterator();
				
				while (itEvents.hasNext()) {
					JSONObject event = itEvents.next();
					
					String eventType = (String)event.get("type");
					
					switch(eventType) {
					case "shot":
						Color c;
						
						if (event.get("color").equals("0xff0000ff")) {
							c = Color.RED;
						} else {
							c = Color.GREEN;
						}
						
						Shot shot = new Shot(c, (double)event.get("x"), (double)event.get("y"), 
								(Long)event.get("shotTimestamp"), 
								((Long)event.get("markerRadius")).intValue());
						
						Optional<Integer> targetIndex;
						int index = ((Long)event.get("targetIndex")).intValue(); 
						if (index == -1) {
							targetIndex = Optional.empty();
						} else {
							targetIndex = Optional.of(index);
						}
						
						Optional<Integer> hitRegionIndex;
						index = ((Long)event.get("hitRegionIndex")).intValue(); 
						if (index == -1) {
							hitRegionIndex = Optional.empty();
						} else {
							hitRegionIndex = Optional.of(index);
						}
						
						events.get(cameraName).add(
								new ShotEvent(cameraName, (Long)event.get("timestamp"), shot, 
										targetIndex, hitRegionIndex));
						break;
						
					case "targetAdded":
						events.get(cameraName).add(
								new TargetAddedEvent(cameraName, (Long)event.get("timestamp"),
										(String)event.get("name")));
						break;
						
					case "targetRemoved":
						events.get(cameraName).add(
								new TargetRemovedEvent(cameraName, (Long)event.get("timestamp"),
										((Long)event.get("index")).intValue()));
						break;
						
					case "targetResized":
						events.get(cameraName).add(
								new TargetResizedEvent(cameraName, (Long)event.get("timestamp"),
										((Long)event.get("index")).intValue(),
										(Double)event.get("newWidth"),
										(Double)event.get("newHeight")));
						break;
						
					case "targetMoved":
						events.get(cameraName).add(
								new TargetMovedEvent(cameraName, (Long)event.get("timestamp"),
										((Long)event.get("index")).intValue(),
										((Long)event.get("newX")).intValue(),
										((Long)event.get("newY")).intValue()));
						break;
						
					case "protocolFeedMessage":
						events.get(cameraName).add(
								new ProtocolFeedMessageEvent(cameraName, (Long)event.get("timestamp"),
										(String)event.get("message")));
						break;
					}
				}
			}
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return events;
	}
}
