package com.shootoff.camera;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import com.shootoff.camera.perspective.PerspectiveManager;
import com.shootoff.config.ConfigurationException;

import javafx.util.Pair;

public class TestPerspectiveManager {

	@Test
	@Ignore
	public void test1() throws ConfigurationException {
		PerspectiveManager pm = new PerspectiveManager();
		
		pm.setCameraParams(PerspectiveManager.C270_FOCAL_LENGTH, PerspectiveManager.C270_SENSOR_WIDTH, PerspectiveManager.C270_SENSOR_HEIGHT);
		pm.setCameraFeedSize(1280, 720);
		pm.setPatternSize(1753, 1299);
		pm.setCameraDistance(3406);
		pm.setShooterDistance(3406);
		
		pm.calculateUnknown();
		
		assertEquals(736.0,pm.getProjectionWidth(), 1);
		assertEquals(543.0,pm.getProjectionHeight(), 1);

		Pair<Double, Double> pair = pm.calculateObjectSize(584, 889, 22860, 11430);
		
		assertEquals(0.0, pair.getKey(), 1);
		assertEquals(0.0, pair.getValue(), 1);
	}
}
