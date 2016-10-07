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

package com.shootoff.session.io;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shootoff.session.Event;
import com.shootoff.session.ExerciseFeedMessageEvent;
import com.shootoff.session.SessionRecorder;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

public class SessionIO {
	public static void saveSession(SessionRecorder sessionRecorder, File sessionFile) {
		EventVisitor visitor;

		if (sessionFile.getName().endsWith("xml")) {
			visitor = new XMLSessionWriter(sessionFile);
		} else if (sessionFile.getName().endsWith("json")) {
			visitor = new JSONSessionWriter(sessionFile);
		} else {
			System.err.println("Unknown session file type.");
			return;
		}

		for (final String cameraName : sessionRecorder.getEvents().keySet()) {
			visitor.visitCamera(cameraName);

			for (final Event e : sessionRecorder.getCameraEvents(cameraName)) {
				switch (e.getType()) {
				case SHOT:
					final ShotEvent se = (ShotEvent) e;
					visitor.visitShot(se.getTimestamp(), se.getShot(), se.isMalfunction(), se.isReload(),
							se.getTargetIndex(), se.getHitRegionIndex(), se.getVideoString());
					break;

				case TARGET_ADDED:
					final TargetAddedEvent tae = (TargetAddedEvent) e;
					visitor.visitTargetAdd(tae.getTimestamp(), tae.getTargetName());
					break;

				case TARGET_REMOVED:
					final TargetRemovedEvent tre = (TargetRemovedEvent) e;
					visitor.visitTargetRemove(tre.getTimestamp(), tre.getTargetIndex());
					break;

				case TARGET_RESIZED:
					final TargetResizedEvent trre = (TargetResizedEvent) e;
					visitor.visitTargetResize(trre.getTimestamp(), trre.getTargetIndex(), trre.getNewWidth(),
							trre.getNewHeight());
					break;

				case TARGET_MOVED:
					final TargetMovedEvent tme = (TargetMovedEvent) e;
					visitor.visitTargetMove(tme.getTimestamp(), tme.getTargetIndex(), tme.getNewX(), tme.getNewY());
					break;

				case EXERCISE_FEED_MESSAGE:
					final ExerciseFeedMessageEvent pfme = (ExerciseFeedMessageEvent) e;
					visitor.visitExerciseFeedMessage(pfme.getTimestamp(), pfme.getMessage());
					break;
				}
			}

			visitor.visitCameraEnd();
		}

		visitor.visitEnd();
	}

	public static Optional<SessionRecorder> loadSession(File sessionFile) {
		Map<String, List<Event>> events = null;

		if (sessionFile.getName().endsWith("xml")) {
			events = new XMLSessionReader(sessionFile).load();
		} else if (sessionFile.getName().endsWith("json")) {
			events = new JSONSessionReader(sessionFile).load();
		} else {
			System.err.println("Unknown session file type.");
			return Optional.empty();
		}

		if (events == null) {
			return Optional.empty();
		} else {
			final SessionRecorder sessionRecorder = new SessionRecorder();
			sessionRecorder.addEvents(events);
			return Optional.of(sessionRecorder);
		}
	}
}
