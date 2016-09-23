package com.shootoff.camera;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Bounds;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.rules.ErrorCollector;
import org.slf4j.LoggerFactory;

import com.shootoff.config.Configuration;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.plugins.TrainingExerciseBase;

import ch.qos.logback.classic.Logger;

import static org.hamcrest.core.IsEqual.equalTo;

@Ignore
public class ShotDetectionTestor implements VideoFinishedListener {
	private static int ALLOWED_COORD_VARIANCE = 3;

	@BeforeClass
	public static void setUpBaseClass() {
		System.setProperty("shootoff.home", System.getProperty("user.dir"));
		Configuration.disableErrorReporting();
		TrainingExerciseBase.silence(true);

		nu.pattern.OpenCV.loadShared();

		Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		rootLogger.detachAndStopAllAppenders();
	}

	public void checkShots(ErrorCollector collector, final List<Shot> actualShots, List<Shot> requiredShots,
			List<Shot> optionalShots, boolean isColorWarning) {

		List<Shot> mutableActualShots = new ArrayList<Shot>(actualShots);

		for (Shot shot : requiredShots) {
			Optional<Shot> potentialShotMatch = findPotentialShotMatch(mutableActualShots, shot);

			collector.checkThat(String.format("Shot (%.2f, %.2f, %s) not found", shot.getX(), shot.getY(),
					shot.getColor().toString()), potentialShotMatch.isPresent(), equalTo(true));

			if (potentialShotMatch.isPresent()) {
				if (isColorWarning && !potentialShotMatch.get().getColor().equals(shot.getColor())) {
					System.err.println(String.format("Shot (%.2f, %.2f, %s) detected with wrong color", shot.getX(),
							shot.getY(), shot.getColor().toString()));
				} else {
					collector.checkThat(
							String.format("Shot (%.2f, %.2f, %s) has wrong color", shot.getX(), shot.getY(),
									shot.getColor().toString()),
							potentialShotMatch.get().getColor(), equalTo(shot.getColor()));
				}

				mutableActualShots.remove(potentialShotMatch.get());
			}
		}

		for (Shot shot : optionalShots) {
			Optional<Shot> potentialShotMatch = findPotentialShotMatch(mutableActualShots, shot);

			if (potentialShotMatch.isPresent()) {
				if (isColorWarning && !potentialShotMatch.get().getColor().equals(shot.getColor())) {
					System.err.println(String.format("Optional shot (%.2f, %.2f, %s) detected with wrong color",
							shot.getX(), shot.getY(), shot.getColor().toString()));
				} else {
					collector.checkThat(
							String.format("Optional shot (%.2f, %.2f, %s) has wrong color", shot.getX(), shot.getY(),
									shot.getColor().toString()),
							potentialShotMatch.get().getColor(), equalTo(shot.getColor()));
				}

				mutableActualShots.remove(potentialShotMatch.get());
			} else {
				System.err.println(String.format("Optional shot (%.2f, %.2f, %s) not found", shot.getX(), shot.getY(),
						shot.getColor().toString()));
			}
		}

		// If you are getting this failure you're either detecting noise that
		// wasn't detected before
		// or you found a shot that was previously missed and not accounted for,
		// thus you should
		// add it to the required shot list for the respective test.
		StringBuilder reason = new StringBuilder();

		if (mutableActualShots.size() == 1) {
			reason.append(String.format("There was %d shot that was detected that isn't account for: ",
					mutableActualShots.size()));
		} else if (mutableActualShots.size() > 1) {
			reason.append(String.format("There are %d shots that were detected that aren't account for: ",
					mutableActualShots.size()));
		}

		if (!mutableActualShots.isEmpty()) {
			Iterator<Shot> it = mutableActualShots.iterator();

			while (it.hasNext()) {
				Shot s = it.next();
				reason.append(String.format("(%.2f, %.2f, %s)", s.getX(), s.getY(), s.getColor().toString()));
				if (it.hasNext()) reason.append(", ");
			}
		}

		collector.checkThat(reason.toString(), mutableActualShots.isEmpty(), equalTo(true));
	}

	public Optional<Shot> findPotentialShotMatch(List<Shot> actualShots, Shot testedShot) {
		for (Shot shot : actualShots) {
			if (Math.abs(shot.getX() - testedShot.getX()) <= ALLOWED_COORD_VARIANCE
					&& Math.abs(shot.getY() - testedShot.getY()) <= ALLOWED_COORD_VARIANCE) {

				return Optional.of(shot);
			}
		}

		return Optional.empty();
	}

	Object processingLock = new Object();
	protected List<Shot> findShots(String videoPath, Optional<Bounds> projectionBounds, MockCanvasManager mockManager,
			Configuration config, boolean[][] sectorStatuses) {
		
		File videoFile = new File(ShotDetectionTestor.class.getResource(videoPath).getFile());
		MockCameraManager cameraManager = new MockCameraManager(new MockCamera(videoFile), mockManager, 
				sectorStatuses, projectionBounds, this);
		
		cameraManager.start();

		try {
			synchronized (processingLock) {
				processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return mockManager.getShots();
	}

	@Override
	public void videoFinished() {
		synchronized (processingLock) {
			processingLock.notifyAll();
		}
	}

}
