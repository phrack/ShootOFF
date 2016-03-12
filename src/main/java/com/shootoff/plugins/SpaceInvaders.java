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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import com.shootoff.camera.Shot;
import com.shootoff.gui.Hit;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;

public class SpaceInvaders extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private static boolean ufoWasStarted = false;
	private final static Logger logger = LoggerFactory.getLogger(SpaceInvaders.class);
	private static Color myColorFontBackground = new Color(0.0, 0.0, 0.0, 0.0);
	private static double fontSize = 45.0;
	final int MAX_TARGETS = 20;
	final int DEFAULT_TARGET_COUNT = 0;
	final int DEFAULT_MAX_ROUNDS = 0;
	final String DEFAULT_TARGET_STRING = "Invader";
	final String DEFAULT_SCALE = "original";
	private final static String POINTS_COL_NAME = "Score";
	private final static int POINTS_COL_WIDTH = 60;
	private boolean fromReset = false;
	private String theScale = "original";
	private int roundCount = 0;// 0 for continuous
	private int timeBetweenTargetMovement = 1;
	private double newScale = 1;
	private int decRoundCount = 1;// have at least one round
	private static ProjectorTrainingExerciseBase thisSuper;
	private static double targetStartingPosX = 0;
	private static double targetStartingPosY = 0;
	private static int soundInc = 1;
	private static boolean movingRight = true;
	private static boolean boundryHit = false;
	private static int hitCount = 0;
	private static boolean firstSpeedIncrease = true;
	private static boolean secondSpeedIncrease = false;
	private static boolean thirdSpeedIncrease = false;
	private static int invaderTotal = 70;
	private static int ufoMove = 1;
	private static int highScore = 0;
	private static int misses = 0;
	static Clip clip = null;
	public long beepTime = 0;
	private static final List<SpaceInvadersTarget> shootTargets = new ArrayList<SpaceInvadersTarget>();
	private static List<SpaceInvadersTarget> shootTargetsRoleCall = new ArrayList<SpaceInvadersTarget>();
	private static Timeline targetAnimation;
	private static int score = 0;
	private int textStartPosX = 250;
	private int textStartPosY = 300;
	private static int leftBoundry = 300;
	private static int rightBoundry = 1200;
	private static double boundryHolder = 0;
	private static boolean ufoAtBoundry = false;

	SpaceInvadersUFO newSpaceInvadersUFO = null;

	public SpaceInvaders() {
	}

	public SpaceInvaders(List<Group> targets) {
		super(targets);
		setThisSuper(super.getInstance());
	}

	@Override
	public void init() {
		initColumn();
		startExercise();
	}// end init

	public long getBeepTime() {
		return beepTime;
	}

	private static void setThisSuper(ProjectorTrainingExerciseBase thisSuper) {
		SpaceInvaders.thisSuper = thisSuper;
	}

	private void initColumn() {
		if (!fromReset) {
			super.addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
		}
	}

	private void startExercise() {
		try {
			AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File("sounds/ufo_highpitch.wav"));
			clip = AudioSystem.getClip();
			clip.open(inputStream);

		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			logger.error("In SpaceInvaders updateTargets:  Error reading sound stream to play", e);
		}

		textStartPosX = (int) (super.getArenaWidth() * .175);
		leftBoundry = (int) (super.getArenaWidth() * .2);
		rightBoundry = (int) (super.getArenaWidth() * .75);

		Optional<Target> newUFO = super.addTarget(new File("targets/UFO.target"), leftBoundry, 200);
		newSpaceInvadersUFO = new SpaceInvadersUFO(newUFO.get());
		for (Node n : newSpaceInvadersUFO.getTarget().getTargetGroup().getChildren()) {
			TargetRegion mytargetRegion = (TargetRegion) n;
			if (mytargetRegion.getType() == RegionType.IMAGE) {
				newSpaceInvadersUFO.UFOImageRegion = (ImageRegion) mytargetRegion;
			} // end if
		} // end for
		newSpaceInvadersUFO.moveUFO_TL = new Timeline(
				new KeyFrame(Duration.millis(1000), e -> moveUFO(newSpaceInvadersUFO)));
		newSpaceInvadersUFO.moveUFO_TL.setCycleCount(Timeline.INDEFINITE);
		newSpaceInvadersUFO.moveUFO_TL.setRate(6);
		newUFO.get().getTargetGroup().setScaleX(2);
		newUFO.get().getTargetGroup().setVisible(false);

		textStartPosY = (int) (super.getArenaHeight() * .25);
		for (int xPos = (int) (super.getArenaWidth() * .2); xPos < 1000; xPos = xPos + 50) {
			addTargets(shootTargets, "targets/invader3.target", 1, xPos, textStartPosY);// 250
			addTargets(shootTargets, "targets/invader2.target", 1, xPos, textStartPosY + 50);
			addTargets(shootTargets, "targets/invader2.target", 1, xPos, textStartPosY + 100);
			addTargets(shootTargets, "targets/invader1.target", 1, xPos, textStartPosY + 150);
			addTargets(shootTargets, "targets/invader1.target", 1, xPos, textStartPosY + 200);
		}

		shootTargetsRoleCall = shootTargets;

		if (score < 0) {
			myColorFontBackground = Color.BLACK;
		} else {
			myColorFontBackground = Color.BLACK;
		}
		thisSuper.showTextOnFeed(
				String.format("SCORE<1>\tHI-SCORE\tKILLS<1>  %n  %d \t\t  %04d \t\t   %d", score, highScore, hitCount),
				textStartPosX, 0, myColorFontBackground, Color.WHITE, new Font("OCR A Extended", fontSize));

		String resourceFilename = "arena/backgrounds/SpaceInvaders_BG.gif";
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceFilename);
		LocatedImage img = new LocatedImage(is, resourceFilename);
		super.setArenaBackground(img);

		targetAnimation = new Timeline(
				new KeyFrame(Duration.millis(timeBetweenTargetMovement * 1000), e -> updateTargets()));

		targetAnimation.setRate(1.0);

		if (roundCount == 0) {
			targetAnimation.setCycleCount(Timeline.INDEFINITE);
		} else {
			targetAnimation.setCycleCount(roundCount + 1);
		}

		pauseShotDetection(false);

		beepTime = System.currentTimeMillis();

		targetAnimation.play();

	}// end start exercise

	private void moveUFO(SpaceInvadersUFO theSpaceInvadersUFO) {
		theSpaceInvadersUFO.moveUFO();
	}

	protected List<SpaceInvadersTarget> getShootTargets() {
		return shootTargets;
	}

	private void moveInvader(SpaceInvadersTarget tt) {
		tt.moveInvader();
	}

	private void addTargets(List<SpaceInvadersTarget> targets, String targetName, int count, int startPosX,
			int startPosY) {
		String targetLeftName = "nothingHere";
		targetLeftName = targetName;

		targetStartingPosY = startPosY;
		targetStartingPosX = startPosX;

		for (int i = 0; i < count; i++) {
			Optional<Target> newLeftTarget = super.addTarget(new File(targetLeftName), targetStartingPosX,
					targetStartingPosY);
			SpaceInvadersTarget newSpaceInvadersTarget = new SpaceInvadersTarget(newLeftTarget.get());
			// so we dont have to cycle through the nodes every time an
			// animation reset is required
			for (Node n : newSpaceInvadersTarget.getLeftTarget().getTargetGroup().getChildren()) {
				TargetRegion mytargetRegion = (TargetRegion) n;
				if (mytargetRegion.getType() == RegionType.IMAGE) {
					newSpaceInvadersTarget.leftImageRegion = (ImageRegion) mytargetRegion;
				} // end if
			} // end for

			newSpaceInvadersTarget.moveInvaderTL = new Timeline(
					new KeyFrame(Duration.millis(1000), e -> moveInvader(newSpaceInvadersTarget)));
			newSpaceInvadersTarget.moveInvaderTL.setCycleCount(Timeline.INDEFINITE);

			if (newLeftTarget.isPresent()) {
				// scale the target
				if (theScale == "one half")
					newScale = 0.5;
				if (theScale == "original")
					newScale = .9;
				if (theScale == "double")
					newScale = 2.0;

				newLeftTarget.get().getTargetGroup().setScaleX(newScale);
				newLeftTarget.get().getTargetGroup().setScaleY(newScale);

				targets.add(newSpaceInvadersTarget);

			} // end if
		} // end for
	}// end function

	// targetAnimation timeline calls updateTargets...controls a round
	private void updateTargets() {
		if (hitCount % 10 == 0 && !ufoWasStarted && hitCount > 0 && !ufoAtBoundry) {
			int startXPos = 300;
			if (getRandom(2, 4) == 3) {
				startXPos = 1200;
				ufoMove = Math.abs(ufoMove) * -1;
			} else {
				startXPos = 300;
				ufoMove = Math.abs(ufoMove);

			}
			newSpaceInvadersUFO.target.setPosition(startXPos, 200);
			newSpaceInvadersUFO.UFOImageRegion.getAnimation().get().reset();
			newSpaceInvadersUFO.getTarget().getTargetGroup().setVisible(true);
			newSpaceInvadersUFO.moveUFO_TL.play();
			clip.loop(Clip.LOOP_CONTINUOUSLY);

			ufoWasStarted = true;
		} // end if hitcount

		if (invaderTotal == 0) {
			targetAnimation.stop();
			decRoundCount = 0;
		} else if (invaderTotal < 2) {
			targetAnimation.setRate(10.0);
		} else if (invaderTotal < 11) {
			targetAnimation.setRate(6.0);
		} else if (invaderTotal < 35) {
			targetAnimation.setRate(3.0);
		}

		// continuous loop if roundCount == 0 ("Continuous")
		if (roundCount != 0)
			decRoundCount--;

		// stop the shooting
		if (decRoundCount == 0) {
			pauseShotDetection(true);
			if (score > highScore)
				highScore = score;
			double hitPercentage = 0;
			if (misses > 0)
				hitPercentage = ((double) hitCount / ((double) misses + hitCount)) * 100;
			thisSuper.showTextOnFeed(
					String.format(
							"SCORE<1>\tHI-SCORE\tKILLS<1>%n  %d \t\t  %04d \t\t   %d %n%n%n%n%n%n%n%n \t\t<GAME OVER>%n\t\tHit %%: %.1f",
							score, highScore, hitCount, hitPercentage),
					textStartPosX, 0, myColorFontBackground, Color.WHITE, new Font("OCR A Extended", fontSize));
			for (SpaceInvadersTarget tt2 : shootTargets) {
				tt2.moveInvaderTL.stop();
			} // end for
			targetAnimation.stop();
			return;
		} // end if
		else {
			playSound("sounds/fastinvader" + soundInc + ".wav");
			if (soundInc <= 3) {
				soundInc++;
			} else {
				soundInc = 1;
			}
		} // end else

		for (SpaceInvadersTarget b : shootTargets) {
			if (invaderTotal < 2 && thirdSpeedIncrease) {
				for (SpaceInvadersTarget b1 : shootTargets) {
					b1.moveInvaderTL.setRate(10.0);
				}
				thirdSpeedIncrease = false;
			} // end if
			else {
				if (invaderTotal < 11 && secondSpeedIncrease) {
					for (SpaceInvadersTarget b1 : shootTargets) {
						b1.moveInvaderTL.setRate(6.0);
					}
					thirdSpeedIncrease = true;
					secondSpeedIncrease = false;
				} // end if
				else {
					if (invaderTotal < 35 && firstSpeedIncrease) {
						for (SpaceInvadersTarget b1 : shootTargets) {
							b1.moveInvaderTL.setRate(3.0);
						}
						secondSpeedIncrease = true;
						firstSpeedIncrease = false;
					} // end if
				} // end else
			} // end else

			if (!b.targetWasHit) {
				if (((b.targetLeft.getPosition().getX() > rightBoundry
						|| b.targetLeft.getPosition().getX() < leftBoundry)) && !boundryHit) {
					movingRight = !movingRight;
					if (!boundryHit && ((boundryHolder != b.targetLeft.getPosition().getX())
							&& (boundryHolder != b.targetLeft.getPosition().getX() - 25)
							&& (boundryHolder != b.targetLeft.getPosition().getX() + 25))) {
						for (SpaceInvadersTarget b1 : shootTargets) {
							if (b.targetLeft.getPosition().getX() < leftBoundry) {
								b1.xMove = Math.abs(b1.xMove);// positive
							} else {
								b1.xMove = Math.abs(b1.xMove) * -1;// negative
							}
							b1.targetLeft.setPosition(b1.targetLeft.getPosition().getX(),
									b1.targetLeft.getPosition().getY() + 25);
							if (b1.targetLeft.getPosition().getY() > super.getArenaHeight() * .675) {
								decRoundCount = 0;
							}
						} // end for

						boundryHit = true;
						boundryHolder = b.targetLeft.getPosition().getX();
						continue;
					} // end if !boundryHit
				} // end if out of bounds
				b.updateInvader();
			} // end if !targetwashit
		} // end for

		// clear boundryFlag
		boundryHit = false;
	}// end updateTargets

	public int getRandom(int from, int to) {
		if (from < to)
			return from + new Random().nextInt(Math.abs(to - from));
		return from - new Random().nextInt(Math.abs(to - from));
	}

	protected static class SpaceInvadersUFO {
		private final Target target;

		public SpaceInvadersUFO(Target newTarget) {
			target = newTarget;
		}

		public Target getTarget() {
			return target;
		}

		private ImageRegion UFOImageRegion = null;
		private boolean targetWasHit = false;
		private Timeline moveUFO_TL = new Timeline(new KeyFrame(Duration.millis(20), e -> moveUFO()));

		public void updateUFO() {
			if (targetWasHit) {
				UFOImageRegion.getAnimation().get().play();
				moveUFO_TL.stop();
				targetWasHit = false;
			} else {
				moveUFO_TL.play();
			} // end else

		}// end updateUFO

		public void moveUFO() {
			target.setPosition(target.getPosition().getX() + 20 * ufoMove, target.getPosition().getY());
			if (target.getPosition().getX() < leftBoundry || target.getPosition().getX() > rightBoundry) {
				moveUFO_TL.stop();
				target.getTargetGroup().setVisible(false);
				clip.stop();
				ufoAtBoundry = true;
				ufoWasStarted = false;
			} // end if
		}// end moveUFO
	}// end class ufo

	protected static class SpaceInvadersTarget {
		private final Target targetLeft;

		public SpaceInvadersTarget(Target target) {
			targetLeft = target;
		}

		public Target getLeftTarget() {
			return targetLeft;
		}

		private ImageRegion leftImageRegion = null;
		private boolean targetWasHit = false;
		private Timeline moveInvaderTL = new Timeline(new KeyFrame(Duration.millis(20), e -> moveInvader()));
		private double moveTargetTime = 0;
		private double xMove = 1;
		private boolean position1 = true;

		public double getMoveTargetTime() {
			return moveTargetTime;
		}

		public void setMoveTargetTime(double newTime) {
			moveTargetTime = newTime;
		}

		// called by updateTargets for every SpaceInvadersTarget
		public void updateInvader() {
			if (targetWasHit) {
				leftImageRegion.getAnimation().get().setFrame(4);
			} else {
				if (position1) {
					leftImageRegion.getAnimation().get().setFrame(1);
					position1 = !position1;
				} else {
					leftImageRegion.getAnimation().get().setFrame(2);
					position1 = !position1;
				} // end else
			} // end else
			moveInvaderTL.play();
		}// end updateInvader

		// called by the Invader's timeline every 20ms
		public void moveInvader() {
			targetLeft.setPosition(targetLeft.getPosition().getX() + 25 * xMove, targetLeft.getPosition().getY());
		}// end moveInvader
	}// end class SpaceInvadersTargets

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("SpaceInvaders", "1.0", "ifly53e",
				"This projector exercise is an adaptation of Space Invaders, the video game from the 80's. "
						+ "So it turns out that the moving blaster canon has malfunctioned and you have to shoot the aliens"
						+ "with your laser trainer.  Who would have thought the aliens are vulnerable to 5mw of red light..."
						+ "The top row of aliens are worth 15 points.  The middle two rows are worth 10 points.  "
						+ "The bottom two rows are worth 5 points. The UFO is worth 20 points."
						+ "Firing and not hitting anything will take away 1 point per shot."
						+ "Win the game by hitting all of the aliens as fast as you can but beware...the aliens will speed up"
						+ "when they feel like they are threatened."
						+ "If the aliens hit the barriers at the bottom of the screen, the game is over"
						+ "and the world as you know it will cease to exist...until you press the reset button.");

		// TODO:
		// debug intermittent delays (Is this a threading conflict? Is it my computer?)
		// green laser for player 2 score instead of kills?
		// add continuous play with faster and lower aliens on subsequent rounds
		// add menu for selecting number of alien columns
		// add menu for setRate multiplier
		// add menu for scale to increase invader size

	}// end metadata

	@Override
	public void shotListener(Shot shot, Optional<Hit> theHit) {
		if (theHit.isPresent()) {
			if (theHit.get().getTarget().equals(newSpaceInvadersUFO.getTarget())) {
				clip.stop();
				newSpaceInvadersUFO.targetWasHit = true;
				ufoWasStarted = false;
				hitCount++;
				if (theHit.get().getHitRegion().tagExists("points")) {
					score = score + Integer.parseInt(theHit.get().getHitRegion().getTag("points"));
				} // end if
			} // end if

			for (SpaceInvadersTarget tt : shootTargets) {
				if (theHit.get().getTarget().equals(tt.targetLeft)) {
					if (!tt.targetWasHit)
						tt.leftImageRegion.getAnimation().get().play();
					tt.targetWasHit = true;
					hitCount++;
					invaderTotal--;
					if (ufoAtBoundry)
						ufoAtBoundry = false;
					if (theHit.get().getHitRegion().tagExists("points")) {
						score = score + Integer.parseInt(theHit.get().getHitRegion().getTag("points"));
					}
					shootTargets.remove(tt);
					break;
				} // end if
			} // end for

			if (score < 0) {
				myColorFontBackground = Color.BLACK;
			} else {
				myColorFontBackground = Color.BLACK;
			}
			thisSuper.showTextOnFeed(
					String.format("SCORE<1>\tHI-SCORE\tKILLS<1>  %n  %d \t\t  %04d \t\t   %d", score, highScore,
							hitCount),
					textStartPosX, 0, myColorFontBackground, Color.WHITE, new Font("OCR A Extended", fontSize));

		} // end if hit is present
		else {
			// the shot was taken but missed the target
			score = score - 1;
			misses++;
			playSound("sounds/shoot.wav");
			if (score < 0) {
				myColorFontBackground = Color.BLACK;
			} else {
				myColorFontBackground = Color.BLACK;
			}
			thisSuper.showTextOnFeed(
					String.format("SCORE<1>\tHI-SCORE\tKILLS<1>  %n  %d \t\t  %04d \t\t   %d", score, highScore,
							hitCount),
					textStartPosX, 0, myColorFontBackground, Color.WHITE, new Font("OCR A Extended", fontSize));
		} // end else
	}// end function

	@Override
	public void reset(List<Group> targets) {
		for (Group theGroups : targets) {
			theGroups.getChildren().clear();
		}
		reset_SI();
	}

	public void reset_SI() {
		clip.stop();
		targetAnimation.stop();

		for (SpaceInvadersTarget b : shootTargets) {
			b.moveInvaderTL.stop();
			b.moveInvaderTL.setRate(1.0);
			super.removeTarget(b.getLeftTarget());
		}

		// will nulling and garbage collecting get rid of all of the space
		// invader objects I created?
		for (SpaceInvadersTarget b4 : shootTargetsRoleCall) {
			super.removeTarget(b4.getLeftTarget());
			b4 = null;
		}

		shootTargets.clear();
		shootTargetsRoleCall.clear();

		System.gc();

		invaderTotal = 70;
		hitCount = 0;
		score = 0;
		misses = 0;
		fromReset = true;
		decRoundCount = 1;
		roundCount = 0;
		firstSpeedIncrease = true;
		secondSpeedIncrease = false;
		thirdSpeedIncrease = false;

		init();
	}// end reset_SI
}// end public class SpaceInvaders
