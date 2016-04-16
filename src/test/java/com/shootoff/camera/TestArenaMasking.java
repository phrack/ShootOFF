package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.shootoff.camera.arenamask.ArenaMaskManager;
import com.shootoff.camera.arenamask.Mask;
import com.shootoff.camera.shotdetection.ShotDetectionManager;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;

public class TestArenaMasking extends ShotDetectionTestor {
	private Configuration config;
	private MockCanvasManager mockCanvasManager;
	private boolean[][] sectorStatuses;
	protected ArenaMaskManager arenaMaskManager = null;

	@Rule public ErrorCollector collector = new ErrorCollector();

	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();

		config = new Configuration(new String[0]);
		config.setDebugMode(false);
		mockCanvasManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[ShotDetectionManager.SECTOR_ROWS][ShotDetectionManager.SECTOR_COLUMNS];

		for (int x = 0; x < ShotDetectionManager.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotDetectionManager.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}

	IMediaReader reader;
	MockCameraManager cameraManager;

	private List<Shot> arenaMaskingVideo(String videoPath, String maskPath, Bounds videoBounds) {
		Object processingLock = new Object();

		File videoFile = new File(TestCameraManagerLifecam.class.getResource(videoPath).getFile());
		File maskFile = new File(TestCameraManagerLifecam.class.getResource(maskPath).getFile());

		cameraManager = new MockCameraManager(videoFile, processingLock, mockCanvasManager, config, sectorStatuses,
				Optional.empty());

		mockCanvasManager.setCameraManager(cameraManager);

		arenaMaskManager = cameraManager.getArenaMaskManager();

		arenaMaskManager.setDelay(200);

		cameraManager.cameraAutoCalibrated = true;

		cameraManager.setShotDetectionArenaMaskManager();

		cameraManager.setProjectionBounds(videoBounds);

		arenaMaskManager.start((int) videoBounds.getWidth(), (int) videoBounds.getHeight());

		reader = ToolFactory.makeReader(maskFile.getAbsolutePath());
		reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
		reader.addListener(new maskListener());

		reader.readPacket();

		cameraManager.processVideo(new cameraListener());

		try {
			synchronized (processingLock) {
				while (!cameraManager.isVideoProcessed())
					processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return mockCanvasManager.getShots();
	}

	protected class cameraListener extends MediaListenerAdapter {
		@Override
		public void onVideoPicture(IVideoPictureEvent event) {
			// We don't actually want this event, we just use this as
			// a trigger to read another frame on the mask video

			while (cameraManager.currentFrameTimestamp + 200 > currentFrameTimestamp) {
				if (reader.readPacket() == null) break;
			}
		}
	}

	protected long currentFrameTimestamp = -1;
	private long initialSystemTimeAtVideoStart = -1;

	protected class maskListener extends MediaListenerAdapter {
		@Override
		public void onVideoPicture(IVideoPictureEvent event) {
			BufferedImage currentFrame = event.getImage();

			if (initialSystemTimeAtVideoStart == -1) initialSystemTimeAtVideoStart = System.currentTimeMillis();

			currentFrameTimestamp = (event.getTimeStamp() / 1000) + initialSystemTimeAtVideoStart;

			arenaMaskManager.handleMask(new Mask(currentFrame, currentFrameTimestamp));

		}
	}

	@Ignore
	@Test
	public void testArenaMaskingBouncingTargetsFourShots() throws IOException {
		Bounds bounds = new BoundingBox(0, 0, 418, 314);
		List<Shot> shots = arenaMaskingVideo("/arenamask/BouncingTargets-calibrated-2.mp4",
				"/arenamask/BouncingTargets-arena-2.mp4", bounds);

		List<Shot> requiredShots = new ArrayList<Shot>();
		requiredShots.add(new Shot(Color.GREEN, 189.8, 256.0, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 158.2, 266.9, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 152.6, 141.3, 0, 2));
		requiredShots.add(new Shot(Color.GREEN, 256.8, 112.2, 0, 2));

		super.checkShots(collector, shots, requiredShots, new ArrayList<Shot>(), true);
	}

	@Ignore
	@Test
	public void testArenaMaskingBouncingTargetsNoShots() throws IOException {
		Bounds bounds = new BoundingBox(0, 0, 410, 312);
		List<Shot> shots = arenaMaskingVideo("/arenamask/BouncingTargets-calibrated.mp4",
				"/arenamask/BouncingTargets-arena.mp4", bounds);

		assertEquals(true, shots.isEmpty());
	}
}