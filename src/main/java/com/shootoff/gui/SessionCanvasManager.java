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
import com.shootoff.targets.Target;
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

/**
 * A canvas to display events that were recorded by the session recorder to a
 * user. This class is where events from a session are actually processed to
 * display their outcomes to the user.
 * 
 * @author phrack
 */
public class SessionCanvasManager {
	private final Group canvas;
	private final Label exerciseLabel = new Label();
	private final Map<Event, TargetView> eventToContainer = new HashMap<Event, TargetView>();
	private final Map<Event, Point2D> eventToPosition = new HashMap<Event, Point2D>();
	private final Map<Event, String> eventToExerciseMessage = new HashMap<Event, String>();
	private final Map<Event, Dimension2D> eventToDimension = new HashMap<Event, Dimension2D>();
	private final List<TargetView> targetViews = new ArrayList<TargetView>();
	private final List<Target> targets = new ArrayList<Target>();
	private final Configuration config;

	public SessionCanvasManager(final Group canvas, final Configuration config) {
		this.canvas = canvas;
		this.config = config;
		canvas.getChildren().add(exerciseLabel);
	}

	public void doEvent(final Event e) {
		switch (e.getType()) {
		case SHOT:
			if (!(e instanceof ShotEvent)) {
				throw new AssertionError("Expected type ShotEvent but got type " + e.getClass().getName());
			}

			final ShotEvent se = (ShotEvent) e;
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

			final TargetRemovedEvent tre = (TargetRemovedEvent) e;
			eventToContainer.put(e, targetViews.get(tre.getTargetIndex()));
			canvas.getChildren().remove(targetViews.get(tre.getTargetIndex()).getTargetGroup());
			targetViews.remove(tre.getTargetIndex());
			targets.remove(tre.getTargetIndex());
			break;

		case TARGET_RESIZED:
			if (!(e instanceof TargetResizedEvent)) {
				throw new AssertionError("Expected type TargetResizedEvent but got type " + e.getClass().getName());
			}

			final TargetResizedEvent trre = (TargetResizedEvent) e;
			eventToDimension.put(e, targetViews.get(trre.getTargetIndex()).getDimension());
			targetViews.get(trre.getTargetIndex()).setDimensions(trre.getNewWidth(), trre.getNewHeight());
			break;

		case TARGET_MOVED:
			if (!(e instanceof TargetMovedEvent)) {
				throw new AssertionError("Expected type TargetMovedEvent but got type " + e.getClass().getName());
			}

			final TargetMovedEvent tme = (TargetMovedEvent) e;
			eventToPosition.put(e, targetViews.get(tme.getTargetIndex()).getPosition());
			targetViews.get(tme.getTargetIndex()).setPosition(tme.getNewX(), tme.getNewY());
			break;

		case EXERCISE_FEED_MESSAGE:
			if (!(e instanceof ExerciseFeedMessageEvent)) {
				throw new AssertionError(
						"Expected type ExerciseFeedMessageEvent but got type " + e.getClass().getName());
			}

			final ExerciseFeedMessageEvent pfme = (ExerciseFeedMessageEvent) e;
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

			final ShotEvent se = (ShotEvent) e;
			canvas.getChildren().remove(se.getShot().getMarker());

			if (se.getTargetIndex().isPresent() && se.getHitRegionIndex().isPresent()) {
				animateTarget(se, true);
			}

			break;

		case TARGET_ADDED:
			canvas.getChildren().remove(eventToContainer.get(e).getTargetGroup());
			targetViews.remove(eventToContainer.get(e));
			targets.remove(eventToContainer.get(e));
			break;

		case TARGET_REMOVED:
			if (!(e instanceof TargetRemovedEvent)) {
				throw new AssertionError("Expected type TargetRemovedEvent but got type " + e.getClass().getName());
			}

			final TargetRemovedEvent tre = (TargetRemovedEvent) e;
			final TargetView oldTarget = eventToContainer.get(e);
			canvas.getChildren().add(oldTarget.getTargetGroup());
			targetViews.add(tre.getTargetIndex(), oldTarget);
			targets.add(tre.getTargetIndex(), oldTarget);
			break;

		case TARGET_RESIZED:
			if (!(e instanceof TargetResizedEvent)) {
				throw new AssertionError("Expected type TargetResizedEvent but got type " + e.getClass().getName());
			}

			final TargetResizedEvent trre = (TargetResizedEvent) e;
			final Dimension2D oldDimension = eventToDimension.get(e);
			targetViews.get(trre.getTargetIndex()).setDimensions(oldDimension.getWidth(), oldDimension.getHeight());
			break;

		case TARGET_MOVED:
			if (!(e instanceof TargetMovedEvent)) {
				throw new AssertionError("Expected type TargetMovedEvent but got type " + e.getClass().getName());
			}

			final TargetMovedEvent tme = (TargetMovedEvent) e;
			final Point2D oldPosition = eventToPosition.get(e);
			targetViews.get(tme.getTargetIndex()).setPosition(oldPosition.getX(), oldPosition.getY());
			break;

		case EXERCISE_FEED_MESSAGE:
			exerciseLabel.setText(eventToExerciseMessage.get(e));
			break;
		}
	}

	private void animateTarget(ShotEvent se, boolean undo) {
		final TargetView target = targetViews.get(se.getTargetIndex().get());
		final TargetRegion region = (TargetRegion) target.getTargetGroup().getChildren()
				.get(se.getHitRegionIndex().get());

		if (!region.tagExists("command")) return;

		TargetView.parseCommandTag(region, (commands, commandName, args) -> {
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
				// like normal
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
					// reset the animated region
					if ("animate".equals(commandName)) {
						if (region.getType() == RegionType.IMAGE) {
							((ImageRegion) region).reset();
						} else {
							final Optional<TargetRegion> t = TargetView
									.getTargetRegionByName(new ArrayList<Target>(targetViews), region, args.get(0));
							if (t.isPresent()) ((ImageRegion) t.get()).reset();
						}
					}
				}
			}
		});
	}

	private void addTarget(final TargetAddedEvent e) {
		final Optional<Group> target = TargetIO.loadTarget(
				new File(System.getProperty("shootoff.home") + File.separator + "targets/" + e.getTargetName()));

		if (target.isPresent()) {
			canvas.getChildren().add(target.get());
			final TargetView targetContainer = new TargetView(target.get(), targets);
			eventToContainer.put(e, targetContainer);
			targetViews.add(targetContainer);
			targets.add(targetContainer);
		}
	}
}