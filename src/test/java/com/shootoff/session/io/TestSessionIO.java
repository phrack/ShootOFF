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
import com.shootoff.gui.TargetView;
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
	private String videoString;
	private Shot redShot;
	private Shot greenShot;
	private String targetName;
	private int hitRegionIndex;
	private String exerciseMessage;

	@Before
	public void setUp() throws ConfigurationException {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		System.setProperty("shootoff.sessions", System.getProperty("shootoff.home") + File.separator + "sessions");

		sessionRecorder = new SessionRecorder();
		cameraName1 = "Default";
		cameraName2 = "Another Camera";
		videoString = "camera1:test/file.mp4,camera2:what/ax.vid";
		redShot = new Shot(Color.RED, 10, 11, 3, 2);
		greenShot = new Shot(Color.GREEN, 12, 15, 3, 5);
		targetName = "bullseye.target";
		exerciseMessage = "This is a\n\t test";

		Configuration config = new Configuration(new String[0]);
		MockCanvasManager canvasManager = new MockCanvasManager(config);
		TargetView target = new TargetView(new File(targetName), new Group(), config, canvasManager, false);
		canvasManager.addTarget(target);

		hitRegionIndex = 0;

		sessionRecorder.recordTargetAdded(cameraName1, target);
		sessionRecorder.recordTargetAdded(cameraName2, target);
		sessionRecorder.recordTargetResized(cameraName1, target, 10, 20);
		sessionRecorder.recordTargetMoved(cameraName1, target, 4, 3);
		sessionRecorder.recordShot(cameraName1, redShot, false, false, Optional.of(target), Optional.of(hitRegionIndex),
				Optional.of(videoString));
		sessionRecorder.recordShot(cameraName1, greenShot, true, false, Optional.of(target),
				Optional.of(hitRegionIndex), Optional.of(videoString));
		sessionRecorder.recordTargetRemoved(cameraName1, target);
		sessionRecorder.recordShot(cameraName1, greenShot, false, true, Optional.empty(), Optional.empty(),
				Optional.empty());
		sessionRecorder.recordExerciseFeedMessage(exerciseMessage);
	}

	private void checkSession(Optional<SessionRecorder> sessionRecorder) {
		assertTrue(sessionRecorder.isPresent());

		List<Event> events = sessionRecorder.get().getCameraEvents(cameraName1);

		assertEquals(8, events.size());

		final int CAM1_TARGET_ADDED_INDEX = 0;
		assertEquals(targetName, ((TargetAddedEvent) events.get(CAM1_TARGET_ADDED_INDEX)).getTargetName());

		final int CAM1_TARGET_RESIZED_INDEX = 1;
		assertEquals(0, ((TargetResizedEvent) events.get(CAM1_TARGET_RESIZED_INDEX)).getTargetIndex());
		assertEquals(10, ((TargetResizedEvent) events.get(CAM1_TARGET_RESIZED_INDEX)).getNewWidth(), 1);
		assertEquals(20, ((TargetResizedEvent) events.get(CAM1_TARGET_RESIZED_INDEX)).getNewHeight(), 1);

		final int CAM1_TARGET_MOVED_INDEX = 2;
		assertEquals(0, ((TargetMovedEvent) events.get(CAM1_TARGET_MOVED_INDEX)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent) events.get(CAM1_TARGET_MOVED_INDEX)).getNewX());
		assertEquals(3, ((TargetMovedEvent) events.get(CAM1_TARGET_MOVED_INDEX)).getNewY());

		final int CAM1_SHOT_RED_INDEX = 3;
		assertEquals(Color.RED, ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getShot().getColor());
		assertEquals(redShot.getX(), ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getShot().getX(), 1);
		assertEquals(redShot.getY(), ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getShot().getY(), 1);
		assertEquals(redShot.getTimestamp(), ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getShot().getTimestamp());
		assertEquals(redShot.getMarker().getRadiusX(),
				((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getShot().getMarker().getRadiusX(), 1);
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).isMalfunction());
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).isReload());
		assertEquals(0, ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex,
				((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getHitRegionIndex().get().intValue());
		assertEquals(videoString, ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getVideoString().get());
		assertEquals(2, ((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getVideos().size());
		assertEquals(new File("sessions/test/file.mp4"),
				((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getVideos().get("camera1"));
		assertEquals(new File("sessions/what/ax.vid"),
				((ShotEvent) events.get(CAM1_SHOT_RED_INDEX)).getVideos().get("camera2"));

		final int CAM1_SHOT_GREEN_ONE_INDEX = 4;
		assertEquals(Color.GREEN, ((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getShot().getColor());
		assertEquals(greenShot.getX(), ((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getShot().getX(), 1);
		assertEquals(greenShot.getY(), ((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getShot().getY(), 1);
		assertEquals(greenShot.getTimestamp(),
				((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getShot().getTimestamp());
		assertEquals(greenShot.getMarker().getRadiusX(),
				((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getShot().getMarker().getRadiusX(), 1);
		assertTrue(((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).isMalfunction());
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).isReload());
		assertEquals(0, ((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex,
				((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getHitRegionIndex().get().intValue());
		assertEquals(videoString, ((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getVideoString().get());
		assertEquals(2, ((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getVideos().size());
		assertEquals(new File("sessions/test/file.mp4"),
				((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getVideos().get("camera1"));
		assertEquals(new File("sessions/what/ax.vid"),
				((ShotEvent) events.get(CAM1_SHOT_GREEN_ONE_INDEX)).getVideos().get("camera2"));

		final int CAM1_TARGET_REMOVED_INDEX = 5;
		assertEquals(0, ((TargetRemovedEvent) events.get(CAM1_TARGET_REMOVED_INDEX)).getTargetIndex());

		final int CAM1_SHOT_GREEN_TWO_INDEX = 6;
		assertEquals(Color.GREEN, ((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getShot().getColor());
		assertEquals(greenShot.getX(), ((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getShot().getX(), 1);
		assertEquals(greenShot.getY(), ((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getShot().getY(), 1);
		assertEquals(greenShot.getTimestamp(),
				((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getShot().getTimestamp());
		assertEquals(greenShot.getMarker().getRadiusX(),
				((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getShot().getMarker().getRadiusX(), 1);
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).isMalfunction());
		assertTrue(((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).isReload());
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getTargetIndex().isPresent());
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getHitRegionIndex().isPresent());
		assertFalse(((ShotEvent) events.get(CAM1_SHOT_GREEN_TWO_INDEX)).getVideoString().isPresent());

		final int CAM1_EXERCISE_MESSAGE_INDEX = 7;
		assertEquals(exerciseMessage,
				((ExerciseFeedMessageEvent) events.get(CAM1_EXERCISE_MESSAGE_INDEX)).getMessage());

		events = sessionRecorder.get().getCameraEvents(cameraName2);

		assertEquals(2, events.size());

		final int CAM2_ADD_TARGET_INDEX = 0;
		assertEquals(targetName, ((TargetAddedEvent) events.get(CAM2_ADD_TARGET_INDEX)).getTargetName());

		final int CAM2_EXERCISE_MESSAGE_INDEX = 1;
		assertEquals(exerciseMessage,
				((ExerciseFeedMessageEvent) events.get(CAM2_EXERCISE_MESSAGE_INDEX)).getMessage());
	}

	@Test
	public void testXMLSerialization() {
		File tempXMLSession = new File("temp_session.xml");
		SessionIO.saveSession(sessionRecorder, tempXMLSession);

		Optional<SessionRecorder> sessionRecorder = SessionIO.loadSession(tempXMLSession);
		checkSession(sessionRecorder);

		if (!tempXMLSession.delete()) System.err.println("Failed to delete " + tempXMLSession.getPath());
	}

	@Test
	public void testJSONSerialization() {
		File tempJSONSession = new File("temp_session.json");
		SessionIO.saveSession(sessionRecorder, tempJSONSession);

		Optional<SessionRecorder> sessionRecorder = SessionIO.loadSession(tempJSONSession);
		checkSession(sessionRecorder);

		if (!tempJSONSession.delete()) System.err.println("Failed to delete " + tempJSONSession.getPath());
	}
}
