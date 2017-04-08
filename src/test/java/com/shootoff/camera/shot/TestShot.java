package com.shootoff.camera.shot;

import static org.junit.Assert.*;

import org.junit.Test;

import com.shootoff.camera.Shot;

public class TestShot {
	@Test
	public void testShot() {
		Shot shot = new Shot(ShotColor.GREEN, 100, 100, 50, 0);

		assertEquals(100.0, shot.getX(), .1);
		assertEquals(100.0, shot.getY(), .1);
		assertEquals(50, shot.getTimestamp());
		assertEquals(0, shot.getFrame());
		assertEquals(ShotColor.GREEN, shot.getColor());
		
		shot.adjustPOI(5, 5);
		
		assertEquals(105.0, shot.getX(), .1);
		assertEquals(105.0, shot.getY(), .1);
		
		BoundsShot bshot = new BoundsShot(shot);
		
		assertEquals(105.0, bshot.getX(), .1);
		assertEquals(105.0, bshot.getY(), .1);
		
		bshot.adjustBounds(5, 5);
		
		assertEquals(110.0, bshot.getX(), .1);
		assertEquals(110.0, bshot.getY(), .1);
		
		assertEquals(105.0, bshot.getOrigX(), .1);
		assertEquals(105.0, bshot.getOrigY(), .1);
		
		DisplayShot dshot = new DisplayShot(bshot, 5);
		
		assertEquals(110.0, dshot.getX(), .1);
		assertEquals(110.0, dshot.getY(), .1);
		
		assertEquals(110.0, dshot.getMarker().getCenterX(), .1);
		assertEquals(110.0, dshot.getMarker().getCenterY(), .1);
		
		dshot.setDisplayVals(100, 100, 200, 200);
		
		assertEquals(110.0, dshot.getBoundsX(), .1);
		assertEquals(110.0, dshot.getBoundsY(), .1);
		
		assertEquals(55.0, dshot.getMarker().getCenterX(), .1);
		assertEquals(55.0, dshot.getMarker().getCenterY(), .1);
		
		assertEquals(55.0, dshot.getX(), .1);
		assertEquals(55.0, dshot.getY(), .1);
		
		assertEquals(50, dshot.getTimestamp());
		assertEquals(0, dshot.getFrame());
		assertEquals(ShotColor.GREEN, dshot.getColor());
		
		ArenaShot sshot = new ArenaShot(dshot);
		
		sshot.setArenaCoords(110, 110);
		
		assertEquals(110.0, sshot.getMarker().getCenterX(), .1);
		assertEquals(110.0, sshot.getMarker().getCenterY(), .1);
		
		assertEquals(55.0, sshot.getDisplayMarker().getCenterX(), .1);
		assertEquals(55.0, sshot.getDisplayMarker().getCenterY(), .1);
		
		
		assertEquals(55.0, sshot.getDisplayX(), .1);
		assertEquals(55.0, sshot.getDisplayY(), .1);
		
		assertEquals(110.0, sshot.getX(), .1);
		assertEquals(110.0, sshot.getY(), .1);
		
		assertEquals(110.0, sshot.getBoundsX(), .1);
		assertEquals(110.0, sshot.getBoundsY(), .1);
		
	}
}
