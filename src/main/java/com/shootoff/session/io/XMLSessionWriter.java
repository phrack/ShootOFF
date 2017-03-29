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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.shot.DisplayShot;

public class XMLSessionWriter implements EventVisitor {
	private final Logger logger = LoggerFactory.getLogger(XMLSessionWriter.class);
	private final File sessionFile;
	private final StringBuilder xmlBody = new StringBuilder();

	public XMLSessionWriter(File sessionFile) {
		this.sessionFile = sessionFile;
	}

	@Override
	public void visitCamera(String cameraName) {
		xmlBody.append(String.format("\t<camera name=\"%s\">%n", cameraName));
	}

	@Override
	public void visitCameraEnd() {
		xmlBody.append("\t</camera>\n");
	}

	@Override
	public void visitShot(long timestamp, DisplayShot shot, boolean isMalfunction, boolean isReload,
			Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex, Optional<String> videoString) {

		int targIndex;
		if (targetIndex.isPresent()) {
			targIndex = targetIndex.get();
		} else {
			targIndex = -1;
		}

		int hitRegIndex;
		if (hitRegionIndex.isPresent()) {
			hitRegIndex = hitRegionIndex.get();
		} else {
			hitRegIndex = -1;
		}

		if (videoString.isPresent()) {
			xmlBody.append(String.format(Locale.US,
					"\t\t<shot timestamp=\"%d\" color=\"%s\""
							+ " x=\"%f\" y=\"%f\" shotTimestamp=\"%d\" markerRadius=\"%d\" isMalfunction=\"%b\""
							+ " isReload=\"%b\" targetIndex=\"%d\" hitRegionIndex=\"%d\" videos=\"%s\" />%n",
							timestamp, shot.getPaintColor().toString(), shot.getX(), shot.getY(), shot.getTimestamp(),
							(int) shot.getMarker().getRadiusX(), isMalfunction, isReload, targIndex, hitRegIndex,
							videoString.get()));

		} else {
			xmlBody.append(String.format(Locale.US,
					"\t\t<shot timestamp=\"%d\" color=\"%s\""
							+ " x=\"%f\" y=\"%f\" shotTimestamp=\"%d\" markerRadius=\"%d\" isMalfunction=\"%b\""
							+ " isReload=\"%b\" targetIndex=\"%d\" hitRegionIndex=\"%d\" />%n",
							timestamp, shot.getColor().toString(), shot.getX(), shot.getY(), shot.getTimestamp(),
							(int) shot.getMarker().getRadiusX(), isMalfunction, isReload, targIndex, hitRegIndex));
		}
	}

	@Override
	public void visitTargetAdd(long timestamp, String targetName) {
		xmlBody.append(String.format("\t\t<targetAdded timestamp=\"%d\" name=\"%s\" />%n", timestamp, targetName));
	}

	@Override
	public void visitTargetRemove(long timestamp, int targetIndex) {
		xmlBody.append(String.format("\t\t<targetRemoved timestamp=\"%d\" index=\"%d\" />%n", timestamp, targetIndex));
	}

	@Override
	public void visitTargetResize(long timestamp, int targetIndex, double newWidth, double newHeight) {
		xmlBody.append(String.format(Locale.US,
				"\t\t<targetResized timestamp=\"%d\" index=\"%d\" " + "newWidth=\"%f\" newHeight=\"%f\" />%n",
				timestamp, targetIndex, newWidth, newHeight));
	}

	@Override
	public void visitTargetMove(long timestamp, int targetIndex, int newX, int newY) {
		xmlBody.append(String.format("\t\t<targetMoved timestamp=\"%d\" index=\"%d\" " + "newX=\"%d\" newY=\"%d\" />%n",
				timestamp, targetIndex, newX, newY));
	}

	@Override
	public void visitExerciseFeedMessage(long timestamp, String message) {
		xmlBody.append(String.format("\t\t<exerciseFeedMessage timestamp=\"%d\">%s%n\t\t</exerciseFeedMessage>%n",
				timestamp, message));
	}

	@Override
	public void visitEnd() {
		try {
			final File sessionsFolder = new File(System.getProperty("shootoff.sessions"));
			if (!sessionsFolder.exists()) {
				if (!sessionsFolder.mkdir()) {
					logger.error("Failed to make directory to store sessions: {}", sessionsFolder.getPath());
				}
			}

			final PrintWriter out = new PrintWriter(sessionFile, "UTF-8");

			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<session>");
			out.print(xmlBody.toString());
			out.println("</session>");

			out.close();

		} catch (final IOException e) {
			logger.error("Error writing XML session", e);
		}
	}
}
