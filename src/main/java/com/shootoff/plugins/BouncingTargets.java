/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.shootoff.camera.Shot;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

public class BouncingTargets extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private int shootCount = 4;
	private int dontShootCount = 1;
	private static int maxVelocity = 10;
	private boolean removeShootTargets = false;

	private static final List<BouncingTarget> shootTargets = new ArrayList<>();
	private static final List<BouncingTarget> dontShootTargets = new ArrayList<>();

	private static ProjectorTrainingExerciseBase thisSuper;
	private Timeline targetAnimation;
	private int score = 0;

	public BouncingTargets() {}

	public BouncingTargets(List<Target> targets) {
		super(targets);
		setThisSuper(super.getInstance());
	}

	@Override
	public void targetUpdate(Target target, TargetChange change) {}

	// For testing
	protected void init(int shootCount, int dontShootCount, int maxVelocity) {
		this.shootCount = shootCount;
		this.dontShootCount = dontShootCount;
		setMaxVelocity(maxVelocity);

		shootTargets.clear();
		dontShootTargets.clear();

		startExercise();
	}

	private static void setThisSuper(ProjectorTrainingExerciseBase thisSuper) {
		BouncingTargets.thisSuper = thisSuper;
	}

	private static void setMaxVelocity(int maxVelocity) {
		BouncingTargets.maxVelocity = maxVelocity;
	}

	@Override
	public void init() {
		addSettingControls();

		startExercise();
	}

	private void startExercise() {
		super.showTextOnFeed("Score: 0");

		addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target", shootCount);
		addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target", dontShootCount);

		targetAnimation = new Timeline(new KeyFrame(Duration.millis(20), e -> updateTargets()));
		targetAnimation.setCycleCount(Timeline.INDEFINITE);
		targetAnimation.play();
	}

	private void addSettingControls() {
		final GridPane bouncingTargetsPane = new GridPane();

		final ColumnConstraints cc = new ColumnConstraints(100);
		cc.setHalignment(HPos.CENTER);
		bouncingTargetsPane.getColumnConstraints().addAll(new ColumnConstraints(), cc);

		final int MAX_TARGETS = 10;
		final int MAX_VELOCITY = 30;

		final int SHOOT_DEFAULT_COUNT = 4 - 1;
		final int DONT_SHOOT_DEFAULT_COUNT = 1 - 1;
		final int DEFAULT_MAX_VELOCITY = 10;

		final ObservableList<String> targetCounts = FXCollections.observableArrayList();
		for (int i = 1; i <= MAX_TARGETS; i++)
			targetCounts.add(Integer.toString(i));
		final ComboBox<String> shootTargetsComboBox = new ComboBox<>(targetCounts);
		shootTargetsComboBox.getSelectionModel().select(SHOOT_DEFAULT_COUNT);
		shootTargetsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			shootCount = Integer.parseInt(newValue);
			stopExercise();
			startExercise();
		});
		bouncingTargetsPane.add(new Label("Shoot Targets:"), 0, 0);
		bouncingTargetsPane.add(shootTargetsComboBox, 1, 0);

		final ComboBox<String> dontShootTargetsComboBox = new ComboBox<>(targetCounts);
		dontShootTargetsComboBox.getSelectionModel().select(DONT_SHOOT_DEFAULT_COUNT);
		dontShootTargetsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			dontShootCount = Integer.parseInt(newValue);
			stopExercise();
			startExercise();
		});
		bouncingTargetsPane.add(new Label("Don't Shoot Targets:"), 0, 1);
		bouncingTargetsPane.add(dontShootTargetsComboBox, 1, 1);

		final ObservableList<String> maxVelocity = FXCollections.observableArrayList();
		for (int i = 1; i <= MAX_VELOCITY; i++)
			maxVelocity.add(Integer.toString(i));
		final ComboBox<String> maxVelocityComboBox = new ComboBox<>(maxVelocity);
		maxVelocityComboBox.getSelectionModel().select(DEFAULT_MAX_VELOCITY - 1);
		maxVelocityComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			BouncingTargets.maxVelocity = Integer.parseInt(newValue);
			stopExercise();
			startExercise();
		});
		bouncingTargetsPane.add(new Label("Max Target Speed:"), 0, 2);
		bouncingTargetsPane.add(maxVelocityComboBox, 1, 2);

		final CheckBox removeTargets = new CheckBox();
		removeTargets.setOnAction((event) -> removeShootTargets = removeTargets.isSelected());
		bouncingTargetsPane.add(new Label("Remove Hit Shoot Targets:"), 0, 3);
		bouncingTargetsPane.add(removeTargets, 1, 3);

		super.addExercisePane(bouncingTargetsPane);
	}

	protected List<BouncingTarget> getShootTargets() {
		return shootTargets;
	}

	protected List<BouncingTarget> getDontShootTargets() {
		return dontShootTargets;
	}

	private void updateTargets() {
		for (final BouncingTarget b : shootTargets)
			b.moveTarget();
		for (final BouncingTarget b : dontShootTargets)
			b.moveTarget();
	}

	protected static class BouncingTarget {
		private final Target target;
		private double dx;
		private double dy;

		public BouncingTarget(Target target) {
			this.target = target;

			final Random r = new Random();

			dx = r.nextInt(maxVelocity + 1) + 1;
			dy = r.nextInt(maxVelocity + 1) + 1;

			if (r.nextBoolean()) dx *= -1;
			if (r.nextBoolean()) dy *= -1;
		}

		public Target getTarget() {
			return target;
		}

		private enum CollisionType {
			NONE, COLLISION_X, COLLISION_Y, COLLISION_BOTH;
		}

		private CollisionType checkCollision() {
			final Bounds targetBounds = target.getBoundsInParent();
			List<BouncingTarget> collisionList;

			if (shootTargets.contains(this)) {
				collisionList = shootTargets;
			} else {
				collisionList = dontShootTargets;
			}

			for (final BouncingTarget b : collisionList) {
				if (b.getTarget().equals(target)) continue;

				final Bounds bBounds = b.getTarget().getBoundsInParent();

				if (targetBounds.intersects(bBounds)) {
					final boolean atRight = targetBounds.getMaxX() > bBounds.getMinX()
							&& targetBounds.getMaxX() - bBounds.getMinX() < maxVelocity * 2;
					final boolean atLeft = bBounds.getMaxX() > bBounds.getMinX()
							&& bBounds.getMaxX() - bBounds.getMinX() < maxVelocity * 2;
					final boolean atBottom = targetBounds.getMaxY() > bBounds.getMinY()
							&& targetBounds.getMaxY() - bBounds.getMinY() < maxVelocity * 2;
					final boolean atTop = bBounds.getMaxY() > targetBounds.getMinY()
							&& bBounds.getMaxY() - targetBounds.getMinY() < maxVelocity * 2;

					if ((atRight || atLeft) && (atBottom || atTop)) {
						return CollisionType.COLLISION_BOTH;
					} else if (atRight || atLeft) {
						return CollisionType.COLLISION_X;
					} else if (atBottom || atTop) {
						return CollisionType.COLLISION_Y;
					}
				}
			}

			return CollisionType.NONE;
		}

		public void moveTarget() {
			if (maxVelocity == 0) return;

			final Bounds b = target.getBoundsInParent();
			final Point2D p = target.getPosition();
			final Dimension2D d = target.getDimension();
			final CollisionType ct = checkCollision();

			if (b.getMinX() <= 1 || b.getMinX() + d.getWidth() > thisSuper.getArenaWidth()
					|| ct == CollisionType.COLLISION_X || ct == CollisionType.COLLISION_BOTH) {
				dx *= -1;
			}

			if (b.getMinY() <= 1 || b.getMinY() + d.getHeight() > thisSuper.getArenaHeight()
					|| ct == CollisionType.COLLISION_X || ct == CollisionType.COLLISION_BOTH) {
				dy *= -1;
			}

			target.setPosition(p.getX() + dx, p.getY() + dy);
		}
	}

	private void addTargets(List<BouncingTarget> targets, String target, int count) {
		for (int i = 0; i < count; i++) {
			final Optional<Target> newTarget = super.addTarget(new File(target), 0, 0);

			if (newTarget.isPresent()) {
				// Randomly place the target
				final int maxX = (int) (super.getArenaWidth() - newTarget.get().getDimension().getWidth() - 50);
				final int x = new Random().nextInt(maxX + 1) + 1;

				final int maxY = (int) (super.getArenaHeight() - newTarget.get().getDimension().getHeight() - 50);
				final int y = new Random().nextInt(maxY + 1) + 1;

				newTarget.get().setPosition(x, y);

				targets.add(new BouncingTarget(newTarget.get()));
			}
		}
	}

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Bouncing Targets", "1.0", "phrack",
				"This exercise randomly moves shoot (gray ring) and don't shoot (red ring) targets"
						+ " around the arena. All targets bounce off the arena bounds and targets of the "
						+ "same type can bounce off of each other. Don't shoot targets are always able to "
						+ "overlap shoot targets. This exercise is scored. Your score is the tally of how "
						+ "many shoot targets you have hit since shooting your last don't shoot target.");
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		if (hit.isPresent()) {
			if (hit.get().getHitRegion().tagExists("subtarget")) {
				switch (hit.get().getHitRegion().getTag("subtarget")) {
				case "shoot": {
					score++;
					super.showTextOnFeed(String.format("Score: %d", score));

					if (removeShootTargets) {
						super.removeTarget(hit.get().getTarget());

						if (score == shootTargets.size()) {
							super.playSound("sounds/beep.wav");
							TextToSpeech.say(String.format("Your score was %d", score));
							stopExercise();
							startExercise();
						}
					}
				}
				break;

				case "dont_shoot": {
					super.playSound("sounds/beep.wav");
					TextToSpeech.say(String.format("Your score was %d", score));

					if (removeShootTargets) {
						stopExercise();
						startExercise();
					} else {
						score = 0;
						super.showTextOnFeed("Score: 0");
					}
				}
				break;
				}
			}
		}
	}

	private void stopExercise() {
		targetAnimation.stop();

		for (final BouncingTarget b : shootTargets)
			super.removeTarget(b.getTarget());
		shootTargets.clear();
		for (final BouncingTarget b : dontShootTargets)
			super.removeTarget(b.getTarget());
		dontShootTargets.clear();

		score = 0;
		super.showTextOnFeed("Score: 0");
	}

	@Override
	public void reset(List<Target> targets) {
		stopExercise();

		addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target", shootCount);
		addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target", dontShootCount);

		targetAnimation.play();
	}
}
