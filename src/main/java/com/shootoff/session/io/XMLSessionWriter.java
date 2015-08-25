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

package com.shootoff.session.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import com.shootoff.camera.Shot;

public class XMLSessionWriter implements EventVisitor {
	private final File sessionFile;
	private StringBuilder xmlBody = new StringBuilder();
	
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
	public void visitShot(long timestamp, Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex) {
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
		
		xmlBody.append(String.format("\t\t<shot timestamp=\"%d\" color=\"%s\""
				+ " x=\"%f\" y=\"%f\" shotTimestamp=\"%d\" markerRadius=\"%d\" targetIndex=\"%d\" hitRegionIndex=\"%d\" />%n", 
				timestamp, shot.getColor().toString(), shot.getX(), shot.getY(), shot.getTimestamp(), 
				(int)shot.getMarker().getRadiusX(), targIndex, hitRegIndex));
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
		xmlBody.append(String.format("\t\t<targetResized timestamp=\"%d\" index=\"%d\" "
				+ "newWidth=\"%f\" newHeight=\"%f\" />%n", timestamp, targetIndex, newWidth, newHeight));
	}

	@Override
	public void visitTargetMove(long timestamp, int targetIndex, int newX, int newY) {
		xmlBody.append(String.format("\t\t<targetMoved timestamp=\"%d\" index=\"%d\" "
				+ "newX=\"%d\" newY=\"%d\" />%n", timestamp, targetIndex, newX, newY));
	}
	
	@Override
	public void visitExerciseFeedMessage(long timestamp, String message) {
		xmlBody.append(String.format("\t\t<exerciseFeedMessage timestamp=\"%d\">%s%n\t\t</exerciseFeedMessage>%n", timestamp, message));
	}

	@Override
	public void visitEnd() {
		try {
			File sessionsFolder = new File(System.getProperty("shootoff.home") + File.separator + "sessions");
			if (!sessionsFolder.exists()) sessionsFolder.mkdir();
			
			PrintWriter out = new PrintWriter(sessionFile);
			
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<session>");
			out.print(xmlBody.toString());
			out.println("</session>");
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
