package com.shootoff.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shootoff.session.Event;
import com.shootoff.session.ProtocolFeedMessageEvent;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import com.shootoff.targets.io.TargetIO;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;

public class SessionCanvasManager {
	private final Group canvas;
	private final Label protocolLabel = new Label();
	private final Map<Event, Target> eventToContainer = new HashMap<Event, Target>();
	private final Map<Event, Point2D> eventToPosition = new HashMap<Event, Point2D>();
	private final Map<Event, String> eventToProtocolMessage = new HashMap<Event, String>();
	private final Map<Event, Dimension2D> eventToDimension = new HashMap<Event, Dimension2D>();
	private final List<Target> targets = new ArrayList<Target>();
	
	public SessionCanvasManager(Group canvas) {
		this.canvas = canvas;
		canvas.getChildren().add(protocolLabel);
	}
	
	public void doEvent(Event e) {
		switch (e.getType()) {
		case SHOT:
			ShotEvent se = (ShotEvent)e;
			canvas.getChildren().add(se.getShot().getMarker());
			se.getShot().getMarker().setVisible(true);
			
			if (se.getTargetIndex().isPresent() && se.getHitRegionIndex().isPresent()) {
				animateTarget(se, false);
			}
			
			break;
			
		case TARGET_ADDED:
			addTarget((TargetAddedEvent)e);
			break;
			
		case TARGET_REMOVED:
			TargetRemovedEvent tre = (TargetRemovedEvent)e;
			eventToContainer.put(e, targets.get(tre.getTargetIndex()));
			canvas.getChildren().remove(targets.get(tre.getTargetIndex()).getTargetGroup());
			targets.remove(tre.getTargetIndex());
			break;
			
		case TARGET_RESIZED:
			TargetResizedEvent trre = (TargetResizedEvent)e;
			eventToDimension.put(e, targets.get(trre.getTargetIndex()).getDimension());
			targets.get(trre.getTargetIndex()).setDimensions(trre.getNewWidth(), trre.getNewHeight());
			break;
			
		case TARGET_MOVED:
			TargetMovedEvent tme = (TargetMovedEvent)e;
			eventToPosition.put(e, targets.get(tme.getTargetIndex()).getPosition());
			targets.get(tme.getTargetIndex()).setPosition(tme.getNewX(), tme.getNewY());
			break;
			
		case PROTOCOL_FEED_MESSAGE:
			ProtocolFeedMessageEvent pfme = (ProtocolFeedMessageEvent)e;
			eventToProtocolMessage.put(e, protocolLabel.getText());
			protocolLabel.setText(pfme.getMessage());
			break;
		}
	}
	
	public void undoEvent(Event e) {
		switch (e.getType()) {
		case SHOT:
			ShotEvent se = (ShotEvent)e;
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
			TargetRemovedEvent tre = (TargetRemovedEvent)e;
			Target oldTarget = eventToContainer.get(e);
			canvas.getChildren().add(oldTarget.getTargetGroup());
			targets.add(tre.getTargetIndex(), oldTarget);
			break;
			
		case TARGET_RESIZED:
			TargetResizedEvent trre = (TargetResizedEvent)e;
			Dimension2D oldDimension = eventToDimension.get(e);
			targets.get(trre.getTargetIndex()).setDimensions(oldDimension.getWidth(), oldDimension.getHeight());
			break;
			
		case TARGET_MOVED:
			TargetMovedEvent tme = (TargetMovedEvent)e;
			Point2D oldPosition = eventToPosition.get(e);
			targets.get(tme.getTargetIndex()).setPosition(oldPosition.getX(), oldPosition.getY());
			break;
			
		case PROTOCOL_FEED_MESSAGE:
			protocolLabel.setText(eventToProtocolMessage.get(e));
			break;
		}
	}
	
	private void animateTarget(ShotEvent se, boolean undo) {
		Target target = targets.get(se.getTargetIndex().get());
		TargetRegion region = (TargetRegion)target.getTargetGroup().getChildren().get(se.getHitRegionIndex().get());
		
		if (!region.tagExists("command")) return;
		
		String commandsSource = region.getTag("command");
		List<String> commands = Arrays.asList(commandsSource.split(";"));		
		
		for (String command : commands) {
			int openParen = command.indexOf('(');
			String commandName;
			String args[];
			
			if (openParen > 0) {
				commandName = command.substring(0, openParen);
				args = command.substring(openParen + 1, command.indexOf(')')).split(",");
			} else {
				commandName = command;
				args = null;
			}
			
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
				// If we are undoing a reverse animation we should just play it like
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
					// If we are undoing a non-reverse animation we need to reset
					// the animated region
					if (commandName.equals("animate")) {
						if (region.getType() == RegionType.IMAGE) {
							((ImageRegion)region).reset();
						} else {
							Optional<TargetRegion> t = Target.getTargetRegionByName(targets, region, args[0]);
							if (t.isPresent()) ((ImageRegion)t.get()).reset();
						}
					}
				}
			}
		}
	}
	
	private void addTarget(TargetAddedEvent e) {
		Optional<Group> target = TargetIO.loadTarget(new File("targets/" + e.getTargetName()));
		
		if (target.isPresent()) {		
			canvas.getChildren().add(target.get());
			Target targetContainer = new Target(target.get(), targets);
			eventToContainer.put(e, targetContainer);
			targets.add(targetContainer);
		}
	}
}