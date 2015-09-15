package com.shootoff.session;

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

import javafx.scene.Group;
import javafx.scene.paint.Color;

public class TestSessionRecorder {
	private SessionRecorder sessionRecorder;
	private String cameraName ;
	private Shot shot;
	private String targetName;
	private int targetIndex;
	private Target target;
	private int hitRegionIndex;
	private String exerciseMessage;
	
	@Before
	public void setUp() throws ConfigurationException {
		sessionRecorder = new SessionRecorder(); 
		cameraName = "Default";
		shot = new Shot(Color.RED, 0, 0, 0, 2);
		targetName = "bullseye.target";
		targetIndex = 1;
		
		Configuration config = new Configuration(new String[0]);
		
		target = new Target(new File(targetName), new Group(), config, 
				new MockCanvasManager(config), false, targetIndex);
		hitRegionIndex = 0;
		exerciseMessage = "This is a test";
	}
	
	@Test
	public void testOneOfEach() {
		sessionRecorder.recordTargetAdded(cameraName, target);
		sessionRecorder.recordShot(cameraName, shot, false, false, Optional.of(target), Optional.of(hitRegionIndex), Optional.empty());
		sessionRecorder.recordShot(cameraName, shot, true, false, Optional.of(target), Optional.of(hitRegionIndex), Optional.empty());
		sessionRecorder.recordShot(cameraName, shot, false, true, Optional.of(target), Optional.of(hitRegionIndex), Optional.empty());
		sessionRecorder.recordTargetResized(cameraName, target, 10, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 4, 3);
		sessionRecorder.recordTargetRemoved(cameraName, target);
		sessionRecorder.recordExerciseFeedMessage(exerciseMessage);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		final int TARGET_ADDED_INDEX = 0;
		assertEquals(EventType.TARGET_ADDED, events.get(TARGET_ADDED_INDEX).getType());
		assertEquals(cameraName, events.get(TARGET_ADDED_INDEX).getCameraName());
		assertEquals(targetName, ((TargetAddedEvent)events.get(TARGET_ADDED_INDEX)).getTargetName());
		
		final int SHOT_INDEX = 1;
		assertEquals(EventType.SHOT, events.get(SHOT_INDEX).getType());
		assertEquals(cameraName, events.get(SHOT_INDEX).getCameraName());
		assertEquals(shot, ((ShotEvent)events.get(SHOT_INDEX)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(SHOT_INDEX)).getShot().getColor());
		assertFalse(((ShotEvent)events.get(SHOT_INDEX)).isMalfunction());
		assertFalse(((ShotEvent)events.get(SHOT_INDEX)).isReload());
		assertEquals(targetIndex, ((ShotEvent)events.get(SHOT_INDEX)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(SHOT_INDEX)).getHitRegionIndex().get().intValue());
		assertFalse(((ShotEvent)events.get(SHOT_INDEX)).getVideoString().isPresent());
		
		final int SHOT_MALFUNCTION_INDEX = 2;
		assertEquals(EventType.SHOT, events.get(SHOT_MALFUNCTION_INDEX).getType());
		assertEquals(cameraName, events.get(SHOT_MALFUNCTION_INDEX).getCameraName());
		assertEquals(shot, ((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).getShot().getColor());
		assertTrue(((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).isMalfunction());
		assertFalse(((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).isReload());
		assertEquals(targetIndex, ((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).getHitRegionIndex().get().intValue());
		assertFalse(((ShotEvent)events.get(SHOT_MALFUNCTION_INDEX)).getVideoString().isPresent());
		
		final int SHOT_RELOAD_INDEX = 3;
		assertEquals(EventType.SHOT, events.get(SHOT_RELOAD_INDEX).getType());
		assertEquals(cameraName, events.get(SHOT_RELOAD_INDEX).getCameraName());
		assertEquals(shot, ((ShotEvent)events.get(SHOT_RELOAD_INDEX)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(SHOT_RELOAD_INDEX)).getShot().getColor());
		assertFalse(((ShotEvent)events.get(SHOT_RELOAD_INDEX)).isMalfunction());
		assertTrue(((ShotEvent)events.get(SHOT_RELOAD_INDEX)).isReload());
		assertEquals(targetIndex, ((ShotEvent)events.get(SHOT_RELOAD_INDEX)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(SHOT_RELOAD_INDEX)).getHitRegionIndex().get().intValue());
		assertFalse(((ShotEvent)events.get(SHOT_RELOAD_INDEX)).getVideoString().isPresent());
		
		final int TARGET_RESIZED_INDEX = 4;
		assertEquals(EventType.TARGET_RESIZED, events.get(TARGET_RESIZED_INDEX).getType());
		assertEquals(cameraName, events.get(TARGET_RESIZED_INDEX).getCameraName());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(TARGET_RESIZED_INDEX)).getTargetIndex());
		assertEquals(10, ((TargetResizedEvent)events.get(TARGET_RESIZED_INDEX)).getNewWidth(), 1);
		assertEquals(20, ((TargetResizedEvent)events.get(TARGET_RESIZED_INDEX)).getNewHeight(), 1);
		
		final int TARGET_MOVED_INDEX = 5;
		assertEquals(EventType.TARGET_MOVED, events.get(TARGET_MOVED_INDEX).getType());
		assertEquals(cameraName, events.get(TARGET_MOVED_INDEX).getCameraName());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(TARGET_MOVED_INDEX)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent)events.get(TARGET_MOVED_INDEX)).getNewX());
		assertEquals(3, ((TargetMovedEvent)events.get(TARGET_MOVED_INDEX)).getNewY());
		
		final int TARGET_REMOVED_INDEX = 6;
		assertEquals(EventType.TARGET_REMOVED, events.get(TARGET_REMOVED_INDEX).getType());
		assertEquals(cameraName, events.get(TARGET_REMOVED_INDEX).getCameraName());
		assertEquals(targetIndex, ((TargetRemovedEvent)events.get(TARGET_REMOVED_INDEX)).getTargetIndex());
		
		final int EXERCISE_FEED_INDEX = 7;
		assertEquals(EventType.EXERCISE_FEED_MESSAGE, events.get(EXERCISE_FEED_INDEX).getType());
		assertEquals(cameraName, events.get(EXERCISE_FEED_INDEX).getCameraName());
		assertEquals(exerciseMessage, ((ExerciseFeedMessageEvent)events.get(EXERCISE_FEED_INDEX)).getMessage());
		
		assertEquals(8, events.size());
	}
	
	@Test
	public void testCollapseResizesBasic() {
		sessionRecorder.recordTargetResized(cameraName, target, 11, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 13, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First two are an add then a move that were artificially inserted
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(2)).getNewWidth(), 1);
		assertEquals(45, ((TargetResizedEvent)events.get(2)).getNewHeight(), 1);
	}
	
	@Test
	public void testCollapseResizesMultipleCameras() {
		String cameraName2 = "AnotherCamera";
		
		sessionRecorder.recordTargetResized(cameraName, target, 11, 20);
		sessionRecorder.recordTargetResized(cameraName2, target, 50, 10);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 13, 20);
		sessionRecorder.recordTargetResized(cameraName2, target, 55, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First two are an add then a move that were artificially inserted
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(2)).getNewWidth(), 1);
		assertEquals(45, ((TargetResizedEvent)events.get(2)).getNewHeight(), 1);
		
		events = sessionRecorder.getCameraEvents(cameraName2);
		
		// First two are an add then a move that were artificially inserted
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(55, ((TargetResizedEvent)events.get(2)).getNewWidth(), 1);
		assertEquals(20, ((TargetResizedEvent)events.get(2)).getNewHeight(), 1);
	}
	
	@Test
	public void testCollapseResizesShotInMiddle() {
		sessionRecorder.recordTargetResized(cameraName, target, 11, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 20);
		String videoString = "c:test/x.vid";
		sessionRecorder.recordShot(cameraName, shot, false, false, Optional.of(target), Optional.of(hitRegionIndex), Optional.of(videoString));
		sessionRecorder.recordTargetResized(cameraName, target, 13, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First two are an add then a move that were artificially inserted
		assertEquals(5, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(2)).getNewWidth(), 1);
		assertEquals(20, ((TargetResizedEvent)events.get(2)).getNewHeight(), 1);	
		
		assertEquals(EventType.SHOT, events.get(3).getType());
		assertEquals(shot, ((ShotEvent)events.get(3)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(3)).getShot().getColor());
		assertEquals(targetIndex, ((ShotEvent)events.get(3)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(3)).getHitRegionIndex().get().intValue());
		assertTrue(((ShotEvent)events.get(3)).getVideoString().isPresent());
		assertEquals(videoString, ((ShotEvent)events.get(3)).getVideoString().get());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(4).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(4)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(4)).getNewWidth(), 1);
		assertEquals(45, ((TargetResizedEvent)events.get(4)).getNewHeight(), 1);	
	}
	
	@Test
	public void testCollapseResizesMoveInMiddle() {
		sessionRecorder.recordTargetResized(cameraName, target, 11, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 4, 3);
		sessionRecorder.recordTargetResized(cameraName, target, 13, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_ADDED, events.get(0).getType());
		assertEquals(cameraName, events.get(0).getCameraName());
		assertEquals(targetName, ((TargetAddedEvent)events.get(0)).getTargetName());
		
		assertEquals(EventType.TARGET_MOVED, events.get(1).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(1)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent)events.get(1)).getNewX());
		assertEquals(3, ((TargetMovedEvent)events.get(1)).getNewY());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(2)).getNewWidth(), 1);
		assertEquals(45, ((TargetResizedEvent)events.get(2)).getNewHeight(), 1);
	}
	
	@Test
	public void testCollapseMovesBasic() {
		sessionRecorder.recordTargetMoved(cameraName, target, 11, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 13, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First two are an add then a resize that were artificially inserted
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(2)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(2)).getNewY());
	}
	
	@Test
	public void testCollapseMovesShotInMiddle() {
		sessionRecorder.recordTargetMoved(cameraName, target, 11, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 20);
		sessionRecorder.recordShot(cameraName, shot, false, false, Optional.of(target), Optional.of(hitRegionIndex), Optional.empty());
		sessionRecorder.recordTargetMoved(cameraName, target, 13, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First two are an add then a resize that were artificially inserted
		assertEquals(5, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(2)).getNewX());
		assertEquals(20, ((TargetMovedEvent)events.get(2)).getNewY());	
		
		assertEquals(EventType.SHOT, events.get(3).getType());
		assertEquals(shot, ((ShotEvent)events.get(3)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(3)).getShot().getColor());
		assertEquals(targetIndex, ((ShotEvent)events.get(3)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(3)).getHitRegionIndex().get().intValue());
		assertFalse(((ShotEvent)events.get(3)).getVideoString().isPresent());
		
		assertEquals(EventType.TARGET_MOVED, events.get(4).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(4)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(4)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(4)).getNewY());	
	}
	
	@Test
	public void testCollapseMovesResizeInMiddle() {
		sessionRecorder.recordTargetMoved(cameraName, target, 11, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 6, 7);
		sessionRecorder.recordTargetMoved(cameraName, target, 13, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First one is an add that was artificially inserted		
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(1).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(1)).getTargetIndex());
		assertEquals(6, ((TargetResizedEvent)events.get(1)).getNewWidth(), 1);
		assertEquals(7, ((TargetResizedEvent)events.get(1)).getNewHeight(), 1);
		
		assertEquals(EventType.TARGET_MOVED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(2)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(2)).getNewY());
	}
	
	@Test
	public void testCollapseMingledMovesAndResizes() {
		sessionRecorder.recordTargetMoved(cameraName, target, 11, 20);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 6, 7);
		sessionRecorder.recordTargetMoved(cameraName, target, 13, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 13, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 13, 40);
		sessionRecorder.recordTargetMoved(cameraName, target, 12, 45);
		sessionRecorder.recordTargetResized(cameraName, target, 11, 20);
		sessionRecorder.recordTargetResized(cameraName, target, 12, 45);
		
		List<Event> events = sessionRecorder.getCameraEvents(cameraName);
		
		// First one is an add that was artificially inserted
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(1).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(1)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(1)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(1)).getNewY());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(2)).getNewWidth(), 1);
		assertEquals(45, ((TargetResizedEvent)events.get(2)).getNewHeight(), 1);	
	}
}
