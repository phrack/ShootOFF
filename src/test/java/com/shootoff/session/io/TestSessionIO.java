package com.shootoff.session.io;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.Target;
import com.shootoff.session.Event;
import com.shootoff.session.ExerciseFeedMessageEvent;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

import javafx.scene.Group;
import javafx.scene.paint.Color;

public class TestSessionIO {
	private SessionRecorder sessionRecorder;
	private String cameraName1;
	private String cameraName2;
	private Shot redShot;
	private Shot greenShot;
	private String targetName;
	private int targetIndex;
	private int hitRegionIndex;
	private String exerciseMessage;
	
	@Before
	public void setUp() throws ConfigurationException {
		sessionRecorder = new SessionRecorder();
		cameraName1 = "Default";
		cameraName2 = "Another Camera";
		redShot = new Shot(Color.RED, 10, 11, 3, 2);
		greenShot = new Shot(Color.GREEN, 12, 15, 3, 5);
		targetName = "bullseye.target";
		targetIndex = 1;
		exerciseMessage = "This is a\n\t test";
		
		Configuration config = new Configuration(new String[0]);
		Target target = new Target(new File(targetName), new Group(), 
				config, new MockCanvasManager(config), false, targetIndex);
		
		hitRegionIndex = 0;
		
		sessionRecorder.recordTargetAdded(cameraName1, target);
		sessionRecorder.recordTargetAdded(cameraName2, target);
		sessionRecorder.recordTargetResized(cameraName1, target, 10, 20);
		sessionRecorder.recordTargetMoved(cameraName1, target, 4, 3);
		sessionRecorder.recordShot(cameraName1, redShot, Optional.of(target), Optional.of(hitRegionIndex));
		sessionRecorder.recordShot(cameraName1, greenShot, Optional.of(target), Optional.of(hitRegionIndex));
		sessionRecorder.recordTargetRemoved(cameraName1, target);
		sessionRecorder.recordShot(cameraName1, greenShot, Optional.empty(), Optional.empty());
		sessionRecorder.recordExerciseFeedMessage(exerciseMessage);
	}
	
	private void checkSession (Optional<SessionRecorder> sessionRecorder) {
		assertTrue(sessionRecorder.isPresent());
		
		List<Event> events = sessionRecorder.get().getCameraEvents(cameraName1);
		
		assertEquals(8, events.size());
		
		assertEquals(targetName, ((TargetAddedEvent)events.get(0)).getTargetName());
		
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(1)).getTargetIndex());
		assertEquals(10, ((TargetResizedEvent)events.get(1)).getNewWidth(), 1);
		assertEquals(20, ((TargetResizedEvent)events.get(1)).getNewHeight(), 1);
		
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(2)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent)events.get(2)).getNewX());
		assertEquals(3, ((TargetMovedEvent)events.get(2)).getNewY());

		assertEquals(Color.RED, ((ShotEvent)events.get(3)).getShot().getColor());
		assertEquals(redShot.getX(), ((ShotEvent)events.get(3)).getShot().getX(), 1);
		assertEquals(redShot.getY(), ((ShotEvent)events.get(3)).getShot().getY(), 1);
		assertEquals(redShot.getTimestamp(), ((ShotEvent)events.get(3)).getShot().getTimestamp());
		assertEquals(redShot.getMarker().getRadiusX(), ((ShotEvent)events.get(3)).getShot().getMarker().getRadiusX(), 1);
		assertEquals(targetIndex, ((ShotEvent)events.get(3)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(3)).getHitRegionIndex().get().intValue());
		
		assertEquals(Color.GREEN, ((ShotEvent)events.get(4)).getShot().getColor());
		assertEquals(greenShot.getX(), ((ShotEvent)events.get(4)).getShot().getX(), 1);
		assertEquals(greenShot.getY(), ((ShotEvent)events.get(4)).getShot().getY(), 1);
		assertEquals(greenShot.getTimestamp(), ((ShotEvent)events.get(4)).getShot().getTimestamp());
		assertEquals(greenShot.getMarker().getRadiusX(), ((ShotEvent)events.get(4)).getShot().getMarker().getRadiusX(), 1);
		assertEquals(targetIndex, ((ShotEvent)events.get(4)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(4)).getHitRegionIndex().get().intValue());

		assertEquals(targetIndex, ((TargetRemovedEvent)events.get(5)).getTargetIndex());
		
		assertEquals(Color.GREEN, ((ShotEvent)events.get(6)).getShot().getColor());
		assertEquals(greenShot.getX(), ((ShotEvent)events.get(6)).getShot().getX(), 1);
		assertEquals(greenShot.getY(), ((ShotEvent)events.get(6)).getShot().getY(), 1);
		assertEquals(greenShot.getTimestamp(), ((ShotEvent)events.get(6)).getShot().getTimestamp());
		assertEquals(greenShot.getMarker().getRadiusX(), ((ShotEvent)events.get(6)).getShot().getMarker().getRadiusX(), 1);
		assertFalse(((ShotEvent)events.get(6)).getTargetIndex().isPresent());
		assertFalse(((ShotEvent)events.get(6)).getHitRegionIndex().isPresent());
		
		assertEquals(exerciseMessage, ((ExerciseFeedMessageEvent)events.get(7)).getMessage());
		
		events = sessionRecorder.get().getCameraEvents(cameraName2);
		
		assertEquals(2, events.size());
		
		assertEquals(targetName, ((TargetAddedEvent)events.get(0)).getTargetName());
		
		assertEquals(exerciseMessage, ((ExerciseFeedMessageEvent)events.get(1)).getMessage());
	}

	@Test
	public void testXMLSerialization() {
		File tempXMLSession = new File("temp_session.xml");
		SessionIO.saveSession(sessionRecorder, tempXMLSession);
				
		Optional<SessionRecorder> sessionRecorder = SessionIO.loadSession(tempXMLSession);
		checkSession(sessionRecorder);
		
		tempXMLSession.delete();
	}
	
	@Test
	public void testJSONSerialization() {
		File tempJSONSession = new File("temp_session.json");
		SessionIO.saveSession(sessionRecorder, tempJSONSession);
		
		Optional<SessionRecorder> sessionRecorder = SessionIO.loadSession(tempJSONSession);
		checkSession(sessionRecorder);
		
		tempJSONSession.delete();
	}
}
