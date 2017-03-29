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

package com.shootoff.gui.targets;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.camera.shot.BoundsShot;
import com.shootoff.config.Configuration;
import com.shootoff.gui.CanvasManager;
import com.shootoff.gui.Resetter;
import com.shootoff.targets.Hit;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

import javafx.geometry.Point2D;
import javafx.util.Pair;

/**
 * This class contains the implementations for tag commands for targets.
 */
public class TargetCommands implements CommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(TargetCommands.class);

	private final List<Target> targets;
	private final Resetter resetter;
	private final Hit hit;
	private final Configuration config;
	private final CanvasManager canvasManager;
	private final boolean isMirroredShot;

	public TargetCommands(CanvasManager canvasManager, List<Target> targets, Resetter resetter, Hit hit,
			boolean isMirroredShot) {
		this.canvasManager = canvasManager;
		this.targets = targets;
		this.resetter = resetter;
		this.hit = hit;
		config = Configuration.getConfig();
		this.isMirroredShot = isMirroredShot;
	}

	@Override
	public void process(List<String> commands, String commandName, List<String> args) {
		switch (commandName) {
		case "reset":
			resetter.reset();
			break;

		case "animate":
			hit.getTarget().animate(hit.getHitRegion(), args);
			break;

		case "reverse":
			hit.getTarget().reverseAnimation(hit.getHitRegion());
			break;

		case "play_sound":
			// If there is a second parameter, we should look to see
			// if it's an image region that is down and if so, don't
			// play the sound
			if (args.size() == 2) {
				final Optional<TargetRegion> namedRegion = TargetView.getTargetRegionByName(targets, hit.getHitRegion(),
						args.get(1));
				if (namedRegion.isPresent() && namedRegion.get().getType() == RegionType.IMAGE) {
					if (!((ImageRegion) namedRegion.get()).onFirstFrame()) break;
				}
			}

			// If the string starts with an @ we are supposed to
			// load the sound as a resource from the current exercises
			// JAR file. This indicates that the target is from
			// a modular exercise
			final String soundPath = args.get(0);
			if (config.getExercise().isPresent() && '@' == soundPath.charAt(0)) {
				final InputStream is = config.getExercise().get().getClass()
						.getResourceAsStream(soundPath.substring(1));
				TrainingExerciseBase.playSound(new BufferedInputStream(is));
			} else if ('@' != soundPath.charAt(0)) {
				TrainingExerciseBase.playSound(soundPath);
			} else {
				logger.error("Can't play {} because it is a resource in an exercise but no exercise is loaded.",
						soundPath);
			}

			break;

		case "poi_adjust":
			if (isMirroredShot || hit.getHitRegion().getType() != RegionType.RECTANGLE) break;
						
			final RectangleRegion reg = (RectangleRegion) hit.getHitRegion();
			final Point2D nodeBounds = ((TargetView)hit.getTarget()).getPosition();
			
			
			if (logger.isTraceEnabled()) {
				logger.trace("reg width {} height {}", reg.getWidth(), reg.getHeight());
				logger.trace("reg boundsinparent {}", reg.getBoundsInParent());
				logger.trace("nodeBounds {}", nodeBounds);
				logger.trace("shot x {} y {}", ((BoundsShot)hit.getShot()).getBoundsX(),
					 ((BoundsShot)hit.getShot()).getBoundsY());
			}
			
			double regcenterx = reg.getWidth() / 2.0;
			double regcentery = reg.getHeight() / 2.0;
			
			// Pair is convenient but it's clearly not the intended use.
			// Refactor it if it bugs you
			Pair<Double, Double> translated = canvasManager.translateCanvasToCameraPoint(nodeBounds.getX() + reg.getBoundsInParent().getMinX() + regcenterx, nodeBounds.getY() + reg.getBoundsInParent().getMinY() + regcentery);
			regcenterx = translated.getKey();
			regcentery = translated.getValue();
			
			double offsetx = ((BoundsShot)hit.getShot()).getBoundsX();
			double offsety = ((BoundsShot)hit.getShot()).getBoundsY();
			
			offsetx = (offsetx - regcenterx) / hit.getTarget().getScaleX();
			offsety = (offsety - regcentery) / hit.getTarget().getScaleY();

			if (logger.isTraceEnabled()) {
				logger.trace("Adjusting POI regcenterx {} regcentery {}", regcenterx, regcentery);
				logger.trace("Adjusting POI scalex {} scaley {}", hit.getTarget().getScaleX(), hit.getTarget().getScaleY());
				logger.trace("Adjusting POI offsetx {} offsety {}", offsetx, offsety);
			}

			if (config.updatePOIAdjustment(offsetx, offsety)) {
				TrainingExerciseBase.playSound("sounds/beep2.wav");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
				TrainingExerciseBase.playSound("sounds/beep2.wav");
			} else if (config.isAdjustingPOI())
				TrainingExerciseBase.playSound("sounds/beep.wav");
			else
				TrainingExerciseBase.playSound("sounds/beep2.wav");
		}
	}
}
