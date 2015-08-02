package com.shootoff.session.io;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shootoff.session.Event;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

public class SessionIO {
	public static void saveSession(SessionRecorder sessionRecorder, File sessionFile) {
		EventVisitor visitor;
		
		if (sessionFile.getName().endsWith("xml")) {
			visitor = new XMLSessionWriter(sessionFile);
		} else {
			System.err.println("Unknown session file type.");
			return;
		}
		
		for (String cameraName : sessionRecorder.getEvents().keySet()) {
			visitor.visitCamera(cameraName);
			
			for (Event e : sessionRecorder.getCameraEvents(cameraName)) {
				switch (e.getType()) {
				case SHOT:
					ShotEvent se = (ShotEvent)e;
					visitor.visitShot(se.getTimestamp(), se.getShot(), se.getTargetIndex(), se.getHitRegionIndex());
					break;
					
				case TARGET_ADDED:
					TargetAddedEvent tae = (TargetAddedEvent)e;
					visitor.visitTargetAdd(tae.getTimestamp(), tae.getTargetName());
					break;
					
				case TARGET_REMOVED:
					TargetRemovedEvent tre = (TargetRemovedEvent)e;
					visitor.visitTargetRemove(tre.getTimestamp(), tre.getTargetIndex());
					break;
					
				case TARGET_RESIZED:
					TargetResizedEvent trre = (TargetResizedEvent)e;
					visitor.visitTargetResize(trre.getTimestamp(), trre.getTargetIndex(), trre.getNewWidth(), trre.getNewHeight());
					break;
					
				case TARGET_MOVED:
					TargetMovedEvent tme = (TargetMovedEvent)e;
					visitor.visitTargetMove(tme.getTimestamp(), tme.getTargetIndex(), tme.getNewX(), tme.getNewY());				
					break;
				}
			}
			
			visitor.visitCameraEnd();
		}
		
		visitor.visitEnd();
	}
	
	public static Optional<SessionRecorder> loadSession(File sessionFile) {
		Map<String, List<Event>> events = null;
		
		if (sessionFile.getName().endsWith("xml")) {
			events = new XMLSessionReader(sessionFile).load();
		} else {
			System.err.println("Unknown target file type.");
			return Optional.empty();
		}
		
		if (events == null) {
			return Optional.empty();
		} else {
			SessionRecorder sessionRecorder = new SessionRecorder();
			sessionRecorder.addEvents(events);
			return Optional.of(sessionRecorder);
		}
	}
}
