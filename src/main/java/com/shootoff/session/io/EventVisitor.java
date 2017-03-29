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

import java.util.Optional;

import com.shootoff.camera.shot.DisplayShot;

public interface EventVisitor {
	public void visitCamera(String cameraName);

	public void visitCameraEnd();

	public void visitShot(long timestamp, DisplayShot shot, boolean isMalfunction, boolean isReload,
			Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex, Optional<String> videoString);

	public void visitTargetAdd(long timestamp, String targetName);

	public void visitTargetRemove(long timestamp, int targetIndex);

	public void visitTargetResize(long timestamp, int targetIndex, double newWidth, double newHeight);

	public void visitTargetMove(long timestamp, int targetIndex, int newX, int newY);

	public void visitExerciseFeedMessage(long timestamp, String message);

	public void visitEnd();
}
