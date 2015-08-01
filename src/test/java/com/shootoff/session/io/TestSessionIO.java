package com.shootoff.session.io;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.Shot;
import com.shootoff.session.Event;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

import javafx.scene.paint.Color;

public class TestSessionIO {
	private SessionRecorder sessionRecorder;
	private Shot redShot;
	private Shot greenShot;
	private String targetName;
	private int targetIndex;
	private int hitRegionIndex;
	
	@Before
	public void setUp() {
		sessionRecorder = new SessionRecorder();
		redShot = new Shot(Color.RED, 10, 11, 3, 2);
		greenShot = new Shot(Color.GREEN, 12, 15, 3, 5);
		targetName = "bullseye.target";
		targetIndex = 1;
		hitRegionIndex = 0;
		
		sessionRecorder.recordShot(redShot, Optional.of(targetIndex), Optional.of(hitRegionIndex));
		sessionRecorder.recordShot(greenShot, Optional.of(targetIndex), Optional.of(hitRegionIndex));
		sessionRecorder.recordTargetAdded(targetName);
		sessionRecorder.recordTargetResized(targetIndex, 10, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 4, 3);
		sessionRecorder.recordTargetRemoved(targetIndex);
	}

	@Test
	public void testXMLSerialization() {
		File tempXMLTarget = new File("temp_session.xml");
		SessionIO.saveSession(sessionRecorder, tempXMLTarget);
				
		Optional<SessionRecorder> sessionRecorder = SessionIO.loadSession(tempXMLTarget);
		
		assertTrue(sessionRecorder.isPresent());
		
		List<Event> events = sessionRecorder.get().getEvents();
		
		assertEquals(6, events.size());

		assertEquals(Color.RED, ((ShotEvent)events.get(0)).getShot().getColor());
		assertEquals(redShot.getX(), ((ShotEvent)events.get(0)).getShot().getX(), 1);
		assertEquals(redShot.getY(), ((ShotEvent)events.get(0)).getShot().getY(), 1);
		assertEquals(redShot.getTimestamp(), ((ShotEvent)events.get(0)).getShot().getTimestamp());
		assertEquals(redShot.getMarker().getRadiusX(), ((ShotEvent)events.get(0)).getShot().getMarker().getRadiusX(), 1);
		
		assertEquals(Color.GREEN, ((ShotEvent)events.get(1)).getShot().getColor());
		assertEquals(greenShot.getX(), ((ShotEvent)events.get(1)).getShot().getX(), 1);
		assertEquals(greenShot.getY(), ((ShotEvent)events.get(1)).getShot().getY(), 1);
		assertEquals(greenShot.getTimestamp(), ((ShotEvent)events.get(1)).getShot().getTimestamp());
		assertEquals(greenShot.getMarker().getRadiusX(), ((ShotEvent)events.get(1)).getShot().getMarker().getRadiusX(), 1);
		
		assertEquals(targetName, ((TargetAddedEvent)events.get(2)).getTargetName());
		
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(3)).getTargetIndex());
		assertEquals(10, ((TargetResizedEvent)events.get(3)).getNewWidth());
		assertEquals(20, ((TargetResizedEvent)events.get(3)).getNewHeight());
		
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(4)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent)events.get(4)).getNewX());
		assertEquals(3, ((TargetMovedEvent)events.get(4)).getNewY());
		
		assertEquals(targetIndex, ((TargetRemovedEvent)events.get(5)).getTargetIndex());
		
		tempXMLTarget.delete();
	}

}
