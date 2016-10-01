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

package com.shootoff.camera.shotdetection;

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.CameraView;

public abstract class ShotYieldingShotDetector extends ShotDetector {


	public ShotYieldingShotDetector(CameraManager cameraManager, CameraView cameraView) {
		super(cameraManager, cameraView);
	}

	public abstract void startDetecting();
}
