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

package com.shootoff.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shootoff.config.Configuration;
import com.shootoff.gui.controller.VideoPlayerController;
import com.shootoff.session.Event;
import com.shootoff.session.ExerciseFeedMessageEvent;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SessionCanvasManager {
	private final Group canvas;
	private final Label exerciseLabel = new Label();
	private final Map<Event, Target> eventToContainer = new HashMap<Event, Target>();
	private final Map<Event, Point2D> eventToPosition = new HashMap<Event, Point2D>();
	private final Map<Event, String> eventToExerciseMessage = new HashMap<Event, String>();
	private final Map<Event, Dimension2D> eventToDimension = new HashMap<Event, Dimension2D>();
	private final List<Target> targets = new ArrayList<Target>();
	private final Configuration config;

	public SessionCanvasManager(Group canvas, Configuration config) {
		this.canvas = canvas;
		this.config = config;
		canvas.getChildren().add(exerciseLabel);
	}

	public void doEvent(Event e) {
		switch (e.getType()) {
		case SHOT:
			if (!(e instanceof ShotEvent)) {
			    throw new AssertionError("Expected type ShotEvent but got type " + e.getClass().getName());
			}
			
			ShotEvent se = (ShotEvent) e;
			canvas.getChildren().add(se.getShot().getMarker());

			if (se.isMalfunction()) {
				se.getShot().getMarker().setFill(Color.ORANGE);
			} else if (se.isReload()) {
				se.getShot().getMarker().setFill(Color.LIGHTSKYBLUE);
			}

			se.getShot().getMarker().setVisible(true);

			if (se.getVideoString().isPresent()) {
				se.getShot().getMarker().setOnMouseClicked((event) -> {
					if (event.getClickCount() < 2) return;

					FXMLLoader loader = new FXMLLoader(
							getClass().getClassLoader().getResource("com/shootoff/gui/VideoPlayer.fxml"));
					try {
						loader.load();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}

					Stage videoPlayerStage = new Stage();

					VideoPlayerController controller = (VideoPlayerController) loader.getController();
					controller.init(se.getVideos());

					videoPlayerStage.setTitle("Video Player");
					videoPlayerStage.setScene(new Scene(loader.getRoot()));
					videoPlayerStage.show();

					config.registerVideoPlayer(controller);
					controller.getStage().setOnCloseRequest((closeEvent) -> {
						config.unregisterVideoPlayer(controller);
					});
				});
			}

			if (se.getTargetIndex().isPresent() && se.getHitRegionIndex().isPresent()) {
				animateTarget(se, false);
			}

			break;

		case TARGET_ADDED:
			if (!(e instanceof TargetAddedEvent)) {
			    throw new AssertionError("Expected type TargetAddedEvent but got type " + e.getClass().getName());
			}
			
			addTarget((TargetAddedEvent) e);
			break;

		case TARGET_REMOVED:
			if (!(e instanceof TargetRemovedEvent)) {
			    throw new AssertionError("Expected type TargetRemovedEvent but got type " + e.getClass().getName());
			}
			
			TargetRemovedEvent tre = (TargetRemovedEvent) e;
			eventToContainer.put(e, targets.get(tre.getTargetIndex()));
			canvas.getChildren().remove(targets.get(tre.getTargetIndex()).getTargetGroup());
			targets.remove(tre.getTargetIndex());
			break;

		case TARGET_RESIZED:
			if (!(e instanceof TargetResizedEvent)) {
			    throw new AssertionError("Expected type TargetResizedEvent but got type " + e.getClass().getName());
			}
			
			TargetResizedEvent trre = (TargetResizedEvent) e;
			eventToDimension.put(e, targets.get(trre.getTargetIndex()).getDimension());
			targets.get(trre.getTargetIndex()).setDimensions(trre.getNewWidth(), trre.getNewHeight());
			break;

		case TARGET_MOVED:
			if (!(e instanceof TargetMovedEvent)) {
			    throw new AssertionError("Expected type TargetMovedEvent but got type " + e.getClass().getName());
			}
			
			TargetMovedEvent tme = (TargetMovedEvent) e;
			eventToPosition.put(e, targets.get(tme.getTargetIndex()).getPosition());
			targets.get(tme.getTargetIndex()).setPosition(tme.getNewX(), tme.getNewY());
			break;

		case EXERCISE_FEED_MESSAGE:
			if (!(e instanceof ExerciseFeedMessageEvent)) {
			    throw new AssertionError("Expected type ExerciseFeedMessageEvent but got type " + e.getClass().getName());
			}
			
			ExerciseFeedMessageEvent pfme = (ExerciseFeedMessageEvent) e;
			eventToExerciseMessage.put(e, exerciseLabel.getText());
			exerciseLabel.setText(pfme.getMessage());
			break;
		}
	}

	public void undoEvent(Event e) {
		switch (e.getType()) {
		case SHOT:
			if (!(e instanceof ShotEvent)) {
			    throw new AssertionError("Expected type ShotEvent but got type " + e.getClass().getName());
			}
			
			ShotEvent se = (ShotEvent) e;
			canvas.getChildren().remove(se.getShot().getMarker());

			if (se.getTargetIndex().isPresent() && se.getHitRegionIndex().isPresent()) {
				animateTarget(se, true);
			}

			break;

		case TARGET_ADDED:
			canvas.getChildren().remove(eventToContainer.get(e).getTargetGroup());
			targets.remove(eventToContainer.get(e));
			break;

		case TARGET_REMOVED:
			if (!(e instanceof TargetRemovedEvent)) {
			    throw new AssertionError("Expected type TargetRemovedEvent but got type " + e.getClass().getName());
			}
			
			TargetRemovedEvent tre = (TargetRemovedEvent) e;
			Target oldTarget = eventToContainer.get(e);
			canvas.getChildren().add(oldTarget.getTargetGroup());
			targets.add(tre.getTargetIndex(), oldTarget);
			break;

		case TARGET_RESIZED:
			if (!(e instanceof TargetResizedEvent)) {
			    throw new AssertionError("Expected type TargetResizedEvent but got type " + e.getClass().getName());
			}
			
			TargetResizedEvent trre = (TargetResizedEvent) e;
			Dimension2D oldDimension = eventToDimension.get(e);
			targets.get(trre.getTargetIndex()).setDimensions(oldDimension.getWidth(), oldDimension.getHeight());
			break;

		case TARGET_MOVED:
			if (!(e instanceof TargetMovedEvent)) {
			    throw new AssertionError("Expected type TargetMovedEvent but got type " + e.getClass().getName());
			}
			
			TargetMovedEvent tme = (TargetMovedEvent) e;
			Point2D oldPosition = eventToPosition.get(e);
			targets.get(tme.getTargetIndex()).setPosition(oldPosition.getX(), oldPosition.getY());
			break;

		case EXERCISE_FEED_MESSAGE:
			exerciseLabel.setText(eventToExerciseMessage.get(e));
			break;
		}
	}

	private void animateTarget(ShotEvent se, boolean undo) {
		Target target = targets.get(se.getTargetIndex().get());
		TargetRegion region = (TargetRegion) target.getTargetGroup().getChildren().get(se.getHitRegionIndex().get());

		if (!region.tagExists("command")) return;

		Target.parseCommandTag(region, (commands, commandName, args) -> {
			if (!undo) {
				switch (commandName) {
				case "animate":
					target.animate(region, args);
					break;

				case "reverse":
					target.reverseAnimation(region);
					break;
				}
			} else {
				// If we are undoing a reverse animation we should just play it
				// like
				// normal
				if (commands.contains("reverse")) {
					switch (commandName) {
					case "animate":
						target.animate(region, args);
						break;

					case "reverse":
						target.reverseAnimation(region);
						break;
					}
				} else {
					// If we are undoing a non-reverse animation we need to
					// reset
					// the animated region
					if (commandName.equals("animate")) {
						if (region.getType() == RegionType.IMAGE) {
							((ImageRegion) region).reset();
						} else {
							Optional<TargetRegion> t = Target.getTargetRegionByName(targets, region, args.get(0));
							if (t.isPresent()) ((ImageRegion) t.get()).reset();
						}
					}
				}
			}
		});
	}

	private void addTarget(TargetAddedEvent e) {
		Optional<Group> target = TargetIO.loadTarget(
				new File(System.getProperty("shootoff.home") + File.separator + "targets/" + e.getTargetName()));

		if (target.isPresent()) {
			canvas.getChildren().add(target.get());
			Target targetContainer = new Target(target.get(), targets);
			eventToContainer.put(e, targetContainer);
			targets.add(targetContainer);
		}
	}
}