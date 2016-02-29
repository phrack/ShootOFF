/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

import javafx.scene.text.Font;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.geometry.HPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.shootoff.camera.Shot;
import com.shootoff.gui.Hit;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;

public class Trap extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private static Color myColor = new Color(0.0, 0.0, 0.0, 0.0);
	private static double fontSize = 45.0;
	final int MAX_TARGETS = 20;
	final int DEFAULT_TARGET_COUNT = 0;
	final int DEFAULT_MAX_ROUNDS = 0;
	final String DEFAULT_TARGET_STRING = "Claybird";
	final String DEFAULT_SCALE = "original";
	private int default_target_count_reset = DEFAULT_TARGET_COUNT;
	private int default_max_rounds_reset = DEFAULT_MAX_ROUNDS;
	private String addTargetString_reset = DEFAULT_TARGET_STRING;
	private String theScale_reset = DEFAULT_SCALE;
	private final static String POINTS_COL_NAME = "Score";
	private final static int POINTS_COL_WIDTH = 60;
	private boolean fromReset = false;
	private String addTargetString = "nothing here";
	private String theScale = "nothing here";
	private int shootCount = 4;
	private int roundCount = 5;
	private int timeBetweenTargetMovement = 7;
	private double newScale = 0;
	private int decRoundCount = 0;
	private static ProjectorTrainingExerciseBase thisSuper;
	private static double targetStartingPosX = 0;
	private static double targetStartingPosY = 0;
	private static boolean skeet = true;
	private static boolean toggle = true;
	private int skeetChoice_reset = 0;

	public long beepTime = 0;

	private static final List<TrapTarget> shootTargets = new ArrayList<TrapTarget>();

	private static Timeline targetAnimation;
	private static int score = 0;

	private String path = "targets/";
	private String fileType = ".target";

	public Trap() {
	}

	public Trap(List<Group> targets) {
		super(targets);
		setThisSuper(super.getInstance());
	}

	@Override
	public void init() {
		initColumn();
		collectSettings();
		startExercise();
	}// end init

	public long getBeepTime() {
		return beepTime;
	}

	private static void setThisSuper(ProjectorTrainingExerciseBase thisSuper) {
		Trap.thisSuper = thisSuper;
	}

	private void initColumn() {
		if (!fromReset) {
			super.addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
		}
	}

	private void collectSettings() {
		super.pauseShotDetection(true);

		final Stage TrapTargetsStage = new Stage();
		final GridPane TrapTargetsPane = new GridPane();
		final ColumnConstraints cc = new ColumnConstraints(200);
		final ObservableList<String> targetList = FXCollections.observableArrayList();
		final ComboBox<String> targetListComboBox = new ComboBox<String>(targetList);
		final ObservableList<String> skeetChoice = FXCollections.observableArrayList();
		final ObservableList<String> targetCounts = FXCollections.observableArrayList();
		final ObservableList<String> roundCounts = FXCollections.observableArrayList();
		final ComboBox<String> shootTargetsComboBox = new ComboBox<String>(targetCounts);
		final ComboBox<String> skeetComboBox = new ComboBox<String>(skeetChoice);
		final ObservableList<String> maxScale = FXCollections.observableArrayList();
		final ComboBox<String> maxScaleComboBox = new ComboBox<String>(maxScale);
		final ComboBox<String> numberOfRoundsComboBox = new ComboBox<String>(roundCounts);
		final Scene scene = new Scene(TrapTargetsPane);
		final Button okButton = new Button("OK");

		cc.setHalignment(HPos.LEFT);
		TrapTargetsPane.getColumnConstraints().addAll(new ColumnConstraints(), cc);

		skeetChoice.add("No");
		skeetChoice.add("Yes");

		targetList.add("Claybird");
		// targets are not based on the origin so a translation is required to
		// use them if you want to move them around
		// the logic is in place to use them but I did not feel like translating
		// the target coordinates in the .target file
		// example would be: targetList.add("ISSF");

		maxScale.add("one half");
		maxScale.add("original");
		maxScale.add("double");

		roundCounts.add("Continuous");
		for (int i = 1; i <= MAX_TARGETS; i++)
			roundCounts.add(Integer.toString(i));

		for (int i = 1; i <= MAX_TARGETS; i++)
			targetCounts.add(Integer.toString(i));

		skeetComboBox.getSelectionModel().select(skeetChoice_reset);
		TrapTargetsPane.add(new Label("Skeet Mode:"), 0, 0);
		TrapTargetsPane.add(skeetComboBox, 1, 0);

		numberOfRoundsComboBox.getSelectionModel().select(default_max_rounds_reset);
		TrapTargetsPane.add(new Label("Number of Rounds:"), 0, 1);
		TrapTargetsPane.add(numberOfRoundsComboBox, 1, 1);

		shootTargetsComboBox.getSelectionModel().select(default_target_count_reset);
		TrapTargetsPane.add(new Label("Targets per Round:"), 0, 2);
		TrapTargetsPane.add(shootTargetsComboBox, 1, 2);

		targetListComboBox.getSelectionModel().select(addTargetString_reset);
		TrapTargetsPane.add(new Label("Target Type:"), 0, 3);
		TrapTargetsPane.add(targetListComboBox, 1, 3);

		maxScaleComboBox.getSelectionModel().select(theScale_reset);
		TrapTargetsPane.add(new Label("Target Scale:"), 0, 4);
		TrapTargetsPane.add(maxScaleComboBox, 1, 4);

		okButton.setDefaultButton(true);
		TrapTargetsPane.add(okButton, 1, 5);

		okButton.setOnAction((e) -> {
			if (skeetComboBox.getSelectionModel().getSelectedIndex() == 0) {
				skeet = false;
			} else {
				skeet = true;
			}
			addTargetString = targetListComboBox.getSelectionModel().getSelectedItem();
			shootCount = Integer.parseInt(shootTargetsComboBox.getSelectionModel().getSelectedItem());
			theScale = maxScaleComboBox.getSelectionModel().getSelectedItem();
			if (numberOfRoundsComboBox.getSelectionModel().getSelectedItem() == "Continuous") {
				roundCount = 0;
			} else {
				roundCount = Integer.parseInt(numberOfRoundsComboBox.getSelectionModel().getSelectedItem());
			}

			TrapTargetsStage.close();
		});// end OKButton

		TrapTargetsStage.initOwner(super.getShootOFFStage());
		TrapTargetsStage.initModality(Modality.WINDOW_MODAL);
		TrapTargetsStage.setTitle("Trap Target Settings");
		TrapTargetsStage.setScene(scene);
		TrapTargetsStage.showAndWait();

		skeetChoice_reset = skeetComboBox.getSelectionModel().getSelectedIndex();
		addTargetString_reset = addTargetString;
		default_target_count_reset = shootCount - 1;
		theScale_reset = theScale;
		default_max_rounds_reset = roundCount;
		decRoundCount = roundCount + 1;
		score = 0;

	}// end collectSettings

	private void startExercise() {
		targetStartingPosX = (thisSuper.getArenaWidth() / 2) - 50;
		targetStartingPosY = (thisSuper.getArenaHeight() / 2) - 50;

		if (skeet)
			shootCount = shootCount * 2;
		addTargets(shootTargets, path + addTargetString + fileType, shootCount);

		if (score < 0) {
			myColor = Color.RED;
		} else {
			myColor = Color.YELLOW;
		}
		thisSuper.showTextOnFeed(String.format("Score: %d", score), 50, (int) super.getArenaHeight() - 200, myColor,
				Color.BLACK, new Font("TimesRoman", fontSize));

		String resourceFilename = "arena/backgrounds/trap.gif";
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceFilename);
		LocatedImage img = new LocatedImage(is, resourceFilename);
		super.setArenaBackground(img);

		targetAnimation = new Timeline(
				new KeyFrame(Duration.millis(timeBetweenTargetMovement * 1000), e -> updateTargets()));

		if (roundCount == 0) {
			targetAnimation.setCycleCount(Timeline.INDEFINITE);
		} else {
			targetAnimation.setCycleCount(roundCount + 1);
		}

		playSound("sounds/voice/shootoff-makeready.wav");

		pauseShotDetection(false);

		beepTime = System.currentTimeMillis();

		targetAnimation.play();

	}// end start exercise

	protected List<TrapTarget> getShootTargets() {
		return shootTargets;
	}

	private void moveClaybird(TrapTarget tt) {
		tt.moveClaybird();
	}

	private void addTargets(List<TrapTarget> targets, String targetName, int count) {
		String targetLeftName = "nothingHere";
		String targetRightName = "nothingHere";
		if (targetName.equals("targets/Claybird.target")) {
			targetLeftName = "targets/Claybird_left.target";
			targetRightName = "targets/Claybird_right.target";
		} else {
			targetLeftName = targetName;
			targetRightName = targetName;
		}

		for (int i = 0; i < count; i++) {
			// create a left target and a right target...only did this to create
			// target hit tails on the correct sides for the lag
			Optional<Target> newLeftTarget = super.addTarget(new File(targetLeftName), targetStartingPosX,
					targetStartingPosY);
			Optional<Target> newRightTarget = super.addTarget(new File(targetRightName), targetStartingPosX,
					targetStartingPosY);
			TrapTarget newTrapTarget = new TrapTarget(newLeftTarget.get(), newRightTarget.get());
			newTrapTarget.getLeftTarget().getTargetGroup().setVisible(false);
			newTrapTarget.getRightTarget().getTargetGroup().setVisible(false);

			// so we dont have to cycle through the nodes every time an
			// animation reset is required
			for (Node n : newTrapTarget.getLeftTarget().getTargetGroup().getChildren()) {
				TargetRegion mytargetRegion = (TargetRegion) n;
				if (mytargetRegion.getType() == RegionType.IMAGE) {
					newTrapTarget.leftImageRegion = (ImageRegion) mytargetRegion;
				} // end if
			} // end for

			for (Node n : newTrapTarget.getRightTarget().getTargetGroup().getChildren()) {
				TargetRegion mytargetRegion2 = (TargetRegion) n;
				if (mytargetRegion2.getType() == RegionType.IMAGE) {
					newTrapTarget.rightImageRegion = (ImageRegion) mytargetRegion2;
				} // end if
			} // end for

			newTrapTarget.moveClaybirdTL = new Timeline(
					new KeyFrame(Duration.millis(20), e -> moveClaybird(newTrapTarget)));
			newTrapTarget.moveClaybirdTL.setCycleCount(Timeline.INDEFINITE);

			if (newLeftTarget.isPresent() && newRightTarget.isPresent()) {
				// scale the target
				if (theScale == "one half")
					newScale = 0.5;
				if (theScale == "original")
					newScale = .9;
				if (theScale == "double")
					newScale = 2.0;

				newLeftTarget.get().getTargetGroup().setScaleX(newScale);
				newLeftTarget.get().getTargetGroup().setScaleY(newScale);
				newRightTarget.get().getTargetGroup().setScaleX(newScale);
				newRightTarget.get().getTargetGroup().setScaleY(newScale);

				newTrapTarget.origScaleXLeft = newLeftTarget.get().getTargetGroup().getScaleX();
				newTrapTarget.origScaleYLeft = newLeftTarget.get().getTargetGroup().getScaleY();
				newTrapTarget.origScaleXRight = newRightTarget.get().getTargetGroup().getScaleX();
				newTrapTarget.origScaleYRight = newRightTarget.get().getTargetGroup().getScaleY();

				targets.add(newTrapTarget);

			} // end if
		} // end for
	}// end function

	// targetAnimation timeline calls updateTargets...controls a round
	private void updateTargets() {
		// continuous loop if roundCount == 0 ("Continuous")
		if (roundCount != 0)
			decRoundCount--;
		// stop the shooting
		if (decRoundCount == 0) {
			thisSuper.showTextOnFeed(String.format("Last Round Complete %nScore: %d", score), 50,
					(int) super.getArenaHeight() - 200, myColor, Color.BLACK, new Font("TimesRoman", fontSize));
			for (TrapTarget tt2 : shootTargets) {
				tt2.moveClaybirdTL.stop();
			} // end for
			targetAnimation.stop();
			return;
		} // end if
		else {
			playSound("sounds/beep.wav");
		}

		for (TrapTarget b : shootTargets) {
			b.updateClaybird();
		} // end for

	}// end updateTargets

	protected static class TrapTarget {
		private final Target targetLeft;
		private final Target targetRight;

		public TrapTarget(Target target, Target target2) {
			targetLeft = target;
			targetRight = target2;
		}

		public Target getLeftTarget() {
			return targetLeft;
		}

		public Target getRightTarget() {
			return targetRight;
		}

		private ImageRegion leftImageRegion = null;
		private ImageRegion rightImageRegion = null;
		private boolean targetWasHit = false;
		private boolean crashed = false;
		private Timeline moveClaybirdTL = new Timeline(new KeyFrame(Duration.millis(20), e -> moveClaybird()));
		private double rotation = 0;
		private double origScaleXLeft = 1;
		private double origScaleYLeft = 1;
		private double origScaleXRight = 1;
		private double origScaleYRight = 1;
		// launchVelocity seems to control the launch angle
		// better than launch degrees
		private int launchVelocity = 350;
		private double angleOfLaunchDegrees = 10;
		private double gravity = 6;
		private double projectileX = 0;
		private double projectileY = 0;
		private double moveTargetTime = 0;
		private double angleInRads = 0;
		private int randomMinX = 1;// controls the arc
		private int randomMaxX = 11;// controls the arc
		private int randomMinY = 1;// controls the arc
		private int randomMaxY = 2;// controls the arc
		private double targetSpeed = 150.0;// between 100 and 200 works best
		private double xMove = 1;
		private double yMove = 1;
		private double moveTargetTimeCutOff = 0.0;
		private double delayTime = 0.0;
		private double startTime = 0.0;

		public double getMoveTargetTime() {
			return moveTargetTime;
		}

		public void setMoveTargetTime(double newTime) {
			moveTargetTime = newTime;
		}

		public int getRandom(int from, int to) {
			if (from < to)
				return from + new Random().nextInt(Math.abs(to - from));
			return from - new Random().nextInt(Math.abs(to - from));
		}

		// called by updateTargets for every trapTarget
		public void updateClaybird() {
			xMove = getRandom(randomMinX, randomMaxX) * 6;
			if (skeet)
				xMove = 5;
			if (xMove > 7) {
				targetSpeed = getRandom(100, 110);// 100;
				launchVelocity = getRandom(800, 850);// 800;//1100,1000,900,800
				angleOfLaunchDegrees = getRandom(3, 4);// 3;//4,3.75
				gravity = 2;// 6,5,4,3,
				moveTargetTimeCutOff = 40;
			} else {// good for skeet targets
				targetSpeed = getRandom(180, 220);// 200
				launchVelocity = getRandom(390, 410);// 400
				angleOfLaunchDegrees = getRandom(10, 12);// 10
				// 15 for normal 23 for skeet
				if (skeet) {
					moveTargetTimeCutOff = 23;
				} else {
					moveTargetTimeCutOff = 15;
				}
				gravity = 6;
			}
			yMove = getRandom(randomMinY, randomMaxY);
			if (new Random().nextInt(2) + 1 > 1) {
				xMove = xMove * -1;
			}

			// for skeet launch one target from each side
			if (skeet) {
				// make xMove positive first
				xMove = Math.abs(xMove);
				if (toggle) {
					// make xMove negative to go left
					xMove = Math.abs(xMove) * -1;// negative
					toggle = !toggle;
				} else {
					// keep xMove positive to go right
					toggle = !toggle;
				}
			}

			// reset the target before it starts its motion
			moveClaybirdTL.stop();
			int randomDelay = getRandom(10, 20);
			delayTime = randomDelay;
			moveClaybirdTL.setDelay(new Duration((randomDelay * 100)));

			if (rightImageRegion != null) {
				rightImageRegion.getAnimation().get().reset();
			}
			if (leftImageRegion != null) {
				leftImageRegion.getAnimation().get().reset();
			}

			rotation = 0;
			targetLeft.getTargetGroup().setRotate(rotation);
			targetLeft.getTargetGroup().setScaleX(origScaleXLeft);
			targetLeft.getTargetGroup().setScaleY(origScaleYLeft);
			targetRight.getTargetGroup().setRotate(rotation);
			targetRight.getTargetGroup().setScaleX(origScaleXRight);
			targetRight.getTargetGroup().setScaleY(origScaleYRight);

			startTime = System.currentTimeMillis();
			moveTargetTime = 0;
			crashed = false;

			moveClaybirdTL.play();

		}// end updateClaybird

		// called by the claybird's timeline every 20ms
		public void moveClaybird() {

			// crash targets into fence...have to start a little early so the
			// animation will show to the user
			if ((moveTargetTime > moveTargetTimeCutOff - 1.25)
					&& moveClaybirdTL.getStatus() == Animation.Status.RUNNING) {
				if (!crashed && !targetWasHit) {
					if (leftImageRegion != null) {
						leftImageRegion.getAnimation().get().play();
					} else {
						thisSuper.showTextOnFeed("leftimageRegion was null");
					}

					if (rightImageRegion != null) {
						rightImageRegion.getAnimation().get().play();
					} else {
						thisSuper.showTextOnFeed("right imageRegion was null");
					}

					crashed = true;
				} // end if
			} // end if

			// has the target hit the fence? If no, then move it towards the
			// fence

			if ((moveTargetTime > moveTargetTimeCutOff) && moveClaybirdTL.getStatus() == Animation.Status.RUNNING) {
				moveClaybirdTL.stop();
				targetLeft.getTargetGroup().setVisible(false);
				targetRight.getTargetGroup().setVisible(false);

				if (targetWasHit) {
					targetWasHit = false;
					score = score + 1;
					if (leftImageRegion != null)
						leftImageRegion.getAnimation().get().reset();
					if (rightImageRegion != null)
						rightImageRegion.getAnimation().get().reset();
				} // end if targetWasHit

				// target was not hit and no shots fired....
				score = score - 1;
				if (score < 0) {
					myColor = Color.RED;
				} else {
					myColor = Color.YELLOW;
				}
				thisSuper.showTextOnFeed(String.format("Score: %d", score), 50, (int) thisSuper.getArenaHeight() - 200,
						myColor, Color.BLACK, new Font("TimesRoman", fontSize));
			} // if movetime>movetargetcutofftime
			else {
				// move the claybird
				angleInRads = angleOfLaunchDegrees * 3.14159 / 180;

				moveTargetTime = (System.currentTimeMillis() - delayTime * 100 - startTime) / targetSpeed;

				projectileX = (launchVelocity * Math.cos(angleInRads) * moveTargetTime) / xMove;
				projectileY = ((launchVelocity * Math.sin(angleInRads) * moveTargetTime
						- gravity * moveTargetTime * moveTargetTime / 2.0) * -1) / yMove;

				if (xMove < 0) {
					// needs left target
					targetLeft.getTargetGroup().setVisible(true);
					targetRight.getTargetGroup().setVisible(false);
					rotation = rotation - 0.10;
					targetLeft.getTargetGroup().setRotate(rotation);
					targetLeft.getTargetGroup().setScaleX(targetLeft.getTargetGroup().getScaleX() * .9975);
					targetLeft.getTargetGroup().setScaleY(targetLeft.getTargetGroup().getScaleY() * .9975);
					if (skeet) {
						targetLeft.setPosition(thisSuper.getArenaWidth() + projectileX,
								targetStartingPosY + projectileY);
					} else {
						targetLeft.setPosition(targetStartingPosX + projectileX, targetStartingPosY + projectileY);
					} // end else
				} // end if
				else {
					// needs right target
					targetLeft.getTargetGroup().setVisible(false);
					targetRight.getTargetGroup().setVisible(true);
					rotation = rotation + 0.10;
					targetRight.getTargetGroup().setRotate(rotation);
					targetRight.getTargetGroup().setScaleX(targetRight.getTargetGroup().getScaleX() * .9975);
					targetRight.getTargetGroup().setScaleY(targetRight.getTargetGroup().getScaleY() * .9975);
					if (skeet) {
						targetRight.setPosition(projectileX, targetStartingPosY + projectileY);
					} else {
						targetRight.setPosition(targetStartingPosX + projectileX, targetStartingPosY + projectileY);
					} // end else
				} // end else
			} // end else
		}// end moveClaybird
	}// end class TrapTargets

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Trap", "1.01", "ifly53e",
				"This projector exercise randomly throws an orange clay pigeon target from the trap house "
						+ "at the center of the screen."
						+ "The exercise can also play Skeet by throwing two targets from the sides that come "
						+ "close to crossing near the middle of the screen"
						+ "Hit the target/targets as quickly as possible to move to the next randomly thrown clay pigeons."
						+ "The targets will disappear after they have completed their flight."
						+ "You can scale the clay pigeons to be bigger or smaller "
						+ "Scoring:  Hit targets are scored three points per hit."
						+ "A target that is not hit is minus 1 point.  All misses are minus 1 point.");

		// TODO:
		// include a better polygon hit area for the lag
		// implement claybird trajectory change on hit?
		// (ducks)? will need to change the physics settings for a more natural
		// flight path...claybirds are more parabollic motion
		// implement regular skeet and trap rules? 25 target round with 10 left,
		// 10 right, 5 middle
		// add "squad ready", "puller ready", "lets see one", and "pull" voice
		// sounds
	}// end metadata

	@Override
	public void shotListener(Shot shot, Optional<Hit> theHit) {
		if (shot.getColor().equals(Color.GREEN)) {
			return;
		}

		if (shot.getColor().equals(Color.RED)) {
			if (theHit.isPresent()) {
				for (TrapTarget tt : shootTargets) {
					if (theHit.get().getTarget().equals(tt.targetLeft)
							|| theHit.get().getTarget().equals(tt.targetRight)) {
						tt.targetWasHit = true;
						break;
					} // end if
				} // end for

				score = score + 3;
				if (score < 0) {
					myColor = Color.RED;
				} else {
					myColor = Color.YELLOW;
				}
				thisSuper.showTextOnFeed(String.format("Score: %d", score), 50, (int) super.getArenaHeight() - 200,
						myColor, Color.BLACK, new Font("TimesRoman", fontSize));

			} // end if hit is present
			else {
				// the shot was taken but missed the target
				score = score - 1;
				if (score < 0) {
					myColor = Color.RED;
				} else {
					myColor = Color.YELLOW;
				}
				thisSuper.showTextOnFeed(String.format("Score: %d", score), 50, (int) super.getArenaHeight() - 200,
						myColor, Color.BLACK, new Font("TimesRoman", fontSize));
			} // end else
		} // end if red
	}// end function

	@Override
	public void reset(List<Group> targets) {
		reset();
	}

	@Override
	public void reset() {
		targetAnimation.stop();

		for (TrapTarget b : shootTargets) {
			b.moveClaybirdTL.stop();
			super.removeTarget(b.getLeftTarget());
			super.removeTarget(b.getRightTarget());
		}

		shootTargets.clear();
		fromReset = true;
		init();
	}// end reset

}// end public class Trap
