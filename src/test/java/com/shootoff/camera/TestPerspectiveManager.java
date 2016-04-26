package com.shootoff.camera;

import static org.junit.Assert.*;

import org.junit.Test;

import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.ConfigurationException;

import javafx.util.Pair;

public class TestPerspectiveManager {

	@Test
	public void testOne() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setCameraParams(PerspectiveManager.C270_FOCAL_LENGTH, PerspectiveManager.C270_SENSOR_WIDTH, PerspectiveManager.C270_SENSOR_HEIGHT);
		pm.setCameraFeedSize(1280, 720);
		pm.setPatternSize(736, 544);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		pm.setProjectorResolution(1024, 768);
		
		pm.calculateUnknown();
		
		assertEquals(1753.0,pm.getProjectionWidth(), 1);
		assertEquals(1299.0,pm.getProjectionHeight(), 1);
		
		Pair<Double, Double> pair = pm.calculateObjectSize(300, 200, 3406, 3406);
		
		assertEquals(175.3, pair.getKey(), 1);
		assertEquals(118.2, pair.getValue(), 1);

		pair = pm.calculateObjectSize(300, 200, 3406, 3406*2);
		
		assertEquals(87.7, pair.getKey(), 1);
		assertEquals(59.1, pair.getValue(), 1);
		
		
		pair = pm.calculateObjectSize(300, 200, 3406*2, 3406);
		
		assertEquals(350.7, pair.getKey(), 1);
		assertEquals(236.5, pair.getValue(), 1);
		
		pm.setShooterDistance(3406*2);
		pair = pm.calculateObjectSize(300, 200, 3406, 3406);
		assertEquals(87.7, pair.getKey(), 1);
		assertEquals(59.1, pair.getValue(), 1);
		
		


	}
}
