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

package com.shootoff.headless.protocol;

import java.io.File;
import java.util.UUID;

public class AddedTargetMessage extends TargetMessage {
	private final File targetFile;
	private final double x;
	private final double y;
	private final double width;
	private final double height;
	private final double arenaWidth;
	private final double arenaHeight;

	public AddedTargetMessage(UUID uuid, File targetFile, double x, double y, double width, double height,
			double arenaWidth, double arenaHeight) {
		super(uuid);
		this.targetFile = targetFile;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.arenaWidth = arenaWidth;
		this.arenaHeight = arenaHeight;
	}

	public File getTargetFile() {
		return targetFile;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	public double getArenaWidth() {
		return arenaWidth;
	}

	public double getArenaHeight() {
		return arenaHeight;
	}
}
