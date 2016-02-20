package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.shootoff.camera.Shot;
import com.shootoff.courses.Course;
import com.shootoff.gui.Hit;
import com.shootoff.gui.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.util.NamedThreadFactory;

import javafx.scene.Group;
import javafx.scene.Node;

public class SteelChallenge extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private final static String LENGTH_COL_NAME = "Length";
	private final static int LENGTH_COL_WIDTH = 60;
	private final static String HIT_COL_NAME = "Hit";
	private final static int HIT_COL_WIDTH = 60;
	private final static int START_DELAY = 4; // s
	private final static int PAUSE_DELAY = 1; // s
	private static final int CORE_POOL_SIZE = 2;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE,
			new NamedThreadFactory("SteelChallengeExercise"));
	private TrainingExerciseBase thisSuper;
	private List<Group> targets;
	private Set<Group> roundTargets;
	private long startTime = 0;
	private boolean repeatExercise = true;
	private boolean testing = false;

	public SteelChallenge() {}

	public SteelChallenge(List<Group> targets) {
		super(targets);

		thisSuper = super.getInstance();
		this.targets = targets;

		if (checkTargets(targets)) startRound();
	}

	@Override
	public void init() {
		super.pauseShotDetection(true);

		super.addShotTimerColumn(LENGTH_COL_NAME, LENGTH_COL_WIDTH);
		super.addShotTimerColumn(HIT_COL_NAME, HIT_COL_WIDTH);
	}

	// For testing
	public void init(final Course course) {
		testing = true;
		thisSuper = super.getInstance();

		targets = new ArrayList<Group>();
		for (Target t : course.getTargets()) {
			targets.add(t.getTargetGroup());
		}

		if (checkTargets(targets)) startRound();
	}

	private boolean checkTargets(final List<Group> targets) {
		boolean hasStopTarget = false;

		for (final Group t : targets) {
			for (final Node node : t.getChildren()) {
				final TargetRegion r = (TargetRegion) node;

				if (r.tagExists("subtarget") && r.getTag("subtarget").equalsIgnoreCase("stop_target")) {
					hasStopTarget = true;
					break;
				}
			}

			if (hasStopTarget) break;
		}

		if (!hasStopTarget) {
			List<File> errorMessages = new ArrayList<File>();
			errorMessages.add(new File("sounds/voice/shootoff-lay-out-own-course.wav"));
			errorMessages.add(new File("sounds/voice/shootoff-add-stop-target.wav"));

			TrainingExerciseBase.playSounds(errorMessages);
		}

		return hasStopTarget;
	}

	private void startRound() {
		if (!repeatExercise) return;

		this.roundTargets = new HashSet<Group>();
		this.roundTargets.addAll(targets);

		if (testing) {
			new AreYouReady().run();
		} else {
			executorService.schedule(new AreYouReady(), START_DELAY, TimeUnit.SECONDS);
		}
	}

	private class AreYouReady implements Runnable {
		@Override
		public void run() {
			if (!repeatExercise) return;

			TrainingExerciseBase.playSound("sounds/voice/shootoff-are-you-ready.wav");

			if (!testing) {
				executorService.schedule(new Standby(), PAUSE_DELAY, TimeUnit.SECONDS);
			} else {
				new Standby().run();
			}

		}
	}

	private class Standby implements Runnable {
		@Override
		public void run() {
			if (!repeatExercise) return;

			TrainingExerciseBase.playSound("sounds/voice/shootoff-standby.wav");

			if (testing) {
				new BeginTimer().run();
			} else {
				executorService.schedule(new BeginTimer(), START_DELAY, TimeUnit.SECONDS);
			}

		}
	}

	private class BeginTimer implements Runnable {
		@Override
		public void run() {
			if (!repeatExercise) return;

			TrainingExerciseBase.playSound("sounds/beep.wav");
			thisSuper.pauseShotDetection(false);
			startTime = System.currentTimeMillis();
		}
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Steel Challenge", "1.0", "phrack",
				"This exercise assumes you will load one of the provided steel challenge courses "
						+ "or lay out your own. When the beep sounds you must shoot every target at least once, "
						+ "ending with the stop target (target with an 's' on it). After you hit the stop target, "
						+ "ShootOFF will tell you your time and how many targets you missed. After you hit the stop "
						+ "target a new round will automatically start.");
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		final long elapsedTime = System.currentTimeMillis() - startTime;
		final String elapsedTimeSeconds;

		if (testing) {
			elapsedTimeSeconds = "0.00";
		} else {
			elapsedTimeSeconds = String.format("%.2f", (double) elapsedTime / (double) 1000);
		}

		super.setShotTimerColumnText(LENGTH_COL_NAME, elapsedTimeSeconds);

		if (hit.isPresent()) {
			final TargetRegion r = hit.get().getHitRegion();

			// Ignore tagless regions
			if (r.getAllTags().size() == 0) {
				super.setShotTimerColumnText(HIT_COL_NAME, "No");
				return;
			}

			super.setShotTimerColumnText(HIT_COL_NAME, "Yes");

			final Iterator<Group> it = roundTargets.iterator();

			while (it.hasNext()) {
				final Group g = it.next();

				if (g.getChildren().contains(r)) {
					it.remove();
					break;
				}
			}

			if (r.tagExists("subtarget") && r.getTag("subtarget").equalsIgnoreCase("stop_target")) {
				super.pauseShotDetection(true);

				String roundAnnouncement;

				if (roundTargets.size() > 0) {
					roundAnnouncement = String.format("Your time was %s seconds. You missed %d targets!",
							elapsedTimeSeconds, roundTargets.size());
				} else {
					roundAnnouncement = String.format("Your time was %s seconds", elapsedTimeSeconds);
				}

				TextToSpeech.say(roundAnnouncement);

				if (testing) {
					startRound();
				} else {
					executorService.schedule(() -> startRound(), START_DELAY, TimeUnit.SECONDS);
				}
			}
		} else {
			super.setShotTimerColumnText(HIT_COL_NAME, "No");
		}
	}

	@Override
	public void reset(List<Group> targets) {
		super.pauseShotDetection(true);

		repeatExercise = false;
		executorService.shutdownNow();

		repeatExercise = true;
		executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE,
				new NamedThreadFactory("SteelChallengeExercise"));

		this.targets = targets;

		if (checkTargets(targets)) startRound();
	}

	@Override
	public void destroy() {
		repeatExercise = false;
		executorService.shutdownNow();
		super.destroy();
	}
}
