package com.shootoff.session;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.shootoff.camera.Shot;

import javafx.scene.paint.Color;

public class TestSessionRecorder {
	private SessionRecorder sessionRecorder;
	private Shot shot;
	private String targetName;
	private int targetIndex;
	private int hitRegionIndex;
	
	@Before
	public void setUp() {
		sessionRecorder = new SessionRecorder(); 
		shot = new Shot(Color.RED, 0, 0, 0, 2);
		targetName = "bullseye.target";
		targetIndex = 1;
		hitRegionIndex = 0;
	}
	
	@Test
	public void testOneOfEach() {
		sessionRecorder.recordShot(shot, Optional.of(targetIndex), Optional.of(hitRegionIndex));
		sessionRecorder.recordTargetAdded(targetName);
		sessionRecorder.recordTargetResized(targetIndex, 10, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 4, 3);
		sessionRecorder.recordTargetRemoved(targetIndex);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(EventType.SHOT, events.get(0).getType());
		assertEquals(shot, ((ShotEvent)events.get(0)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(0)).getShot().getColor());
		assertEquals(targetIndex, ((ShotEvent)events.get(0)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(0)).getHitRegionIndex().get().intValue());

		assertEquals(EventType.TARGET_ADDED, events.get(1).getType());
		assertEquals(targetName, ((TargetAddedEvent)events.get(1)).getTargetName());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(10, ((TargetResizedEvent)events.get(2)).getNewWidth());
		assertEquals(20, ((TargetResizedEvent)events.get(2)).getNewHeight());
		
		assertEquals(EventType.TARGET_MOVED, events.get(3).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(3)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent)events.get(3)).getNewX());
		assertEquals(3, ((TargetMovedEvent)events.get(3)).getNewY());
		
		assertEquals(EventType.TARGET_REMOVED, events.get(4).getType());
		assertEquals(targetIndex, ((TargetRemovedEvent)events.get(4)).getTargetIndex());
		
		assertEquals(5, events.size());
	}
	
	@Test
	public void testCollapseResizesBasic() {
		sessionRecorder.recordTargetResized(targetIndex, 11, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 20);
		sessionRecorder.recordTargetResized(targetIndex, 13, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(1, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(0)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(0)).getNewWidth());
		assertEquals(45, ((TargetResizedEvent)events.get(0)).getNewHeight());
	}
	
	@Test
	public void testCollapseResizesShotInMiddle() {
		sessionRecorder.recordTargetResized(targetIndex, 11, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 20);
		sessionRecorder.recordShot(shot, Optional.of(targetIndex), Optional.of(hitRegionIndex));
		sessionRecorder.recordTargetResized(targetIndex, 13, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(0)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(0)).getNewWidth());
		assertEquals(20, ((TargetResizedEvent)events.get(0)).getNewHeight());	
		
		assertEquals(EventType.SHOT, events.get(1).getType());
		assertEquals(shot, ((ShotEvent)events.get(1)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(1)).getShot().getColor());
		assertEquals(targetIndex, ((ShotEvent)events.get(1)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(1)).getHitRegionIndex().get().intValue());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(2)).getNewWidth());
		assertEquals(45, ((TargetResizedEvent)events.get(2)).getNewHeight());	
	}
	
	@Test
	public void testCollapseResizesMoveInMiddle() {
		sessionRecorder.recordTargetResized(targetIndex, 11, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 4, 3);
		sessionRecorder.recordTargetResized(targetIndex, 13, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(2, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(0)).getTargetIndex());
		assertEquals(4, ((TargetMovedEvent)events.get(0)).getNewX());
		assertEquals(3, ((TargetMovedEvent)events.get(0)).getNewY());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(1).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(1)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(1)).getNewWidth());
		assertEquals(45, ((TargetResizedEvent)events.get(1)).getNewHeight());
	}
	
	@Test
	public void testCollapseMovesBasic() {
		sessionRecorder.recordTargetMoved(targetIndex, 11, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 13, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(1, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(0)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(0)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(0)).getNewY());
	}
	
	@Test
	public void testCollapseMovesShotInMiddle() {
		sessionRecorder.recordTargetMoved(targetIndex, 11, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 20);
		sessionRecorder.recordShot(shot, Optional.of(targetIndex), Optional.of(hitRegionIndex));
		sessionRecorder.recordTargetMoved(targetIndex, 13, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(3, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(0)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(0)).getNewX());
		assertEquals(20, ((TargetMovedEvent)events.get(0)).getNewY());	
		
		assertEquals(EventType.SHOT, events.get(1).getType());
		assertEquals(shot, ((ShotEvent)events.get(1)).getShot());
		assertEquals(Color.RED, ((ShotEvent)events.get(1)).getShot().getColor());
		assertEquals(targetIndex, ((ShotEvent)events.get(1)).getTargetIndex().get().intValue());
		assertEquals(hitRegionIndex, ((ShotEvent)events.get(1)).getHitRegionIndex().get().intValue());
		
		assertEquals(EventType.TARGET_MOVED, events.get(2).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(2)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(2)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(2)).getNewY());	
	}
	
	@Test
	public void testCollapseMovesResizeInMiddle() {
		sessionRecorder.recordTargetMoved(targetIndex, 11, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 20);
		sessionRecorder.recordTargetResized(targetIndex, 6, 7);
		sessionRecorder.recordTargetMoved(targetIndex, 13, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(2, events.size());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(0)).getTargetIndex());
		assertEquals(6, ((TargetResizedEvent)events.get(0)).getNewWidth());
		assertEquals(7, ((TargetResizedEvent)events.get(0)).getNewHeight());
		
		assertEquals(EventType.TARGET_MOVED, events.get(1).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(1)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(1)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(1)).getNewY());
	}
	
	@Test
	public void TestCollapseMingledMovesAndResizes() {
		sessionRecorder.recordTargetMoved(targetIndex, 11, 20);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 20);
		sessionRecorder.recordTargetResized(targetIndex, 6, 7);
		sessionRecorder.recordTargetMoved(targetIndex, 13, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 20);
		sessionRecorder.recordTargetResized(targetIndex, 13, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 20);
		sessionRecorder.recordTargetResized(targetIndex, 13, 40);
		sessionRecorder.recordTargetMoved(targetIndex, 12, 45);
		sessionRecorder.recordTargetResized(targetIndex, 11, 20);
		sessionRecorder.recordTargetResized(targetIndex, 12, 45);
		
		List<Event> events = sessionRecorder.getEvents();
		
		assertEquals(2, events.size());
		
		assertEquals(EventType.TARGET_MOVED, events.get(0).getType());
		assertEquals(targetIndex, ((TargetMovedEvent)events.get(0)).getTargetIndex());
		assertEquals(12, ((TargetMovedEvent)events.get(0)).getNewX());
		assertEquals(45, ((TargetMovedEvent)events.get(0)).getNewY());
		
		assertEquals(EventType.TARGET_RESIZED, events.get(1).getType());
		assertEquals(targetIndex, ((TargetResizedEvent)events.get(1)).getTargetIndex());
		assertEquals(12, ((TargetResizedEvent)events.get(1)).getNewWidth());
		assertEquals(45, ((TargetResizedEvent)events.get(1)).getNewHeight());	
		
	}
}
