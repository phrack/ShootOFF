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

import java.io.InputStream;

import javafx.scene.image.Image;

public class LocatedImage extends Image {
	private final String url;
	private final boolean isResource;

	public LocatedImage(String url) {
		super(url);
		this.url = url;
		isResource = false;
	}

	public LocatedImage(InputStream is, String resourceName) {
		super(is);
		url = resourceName;
		isResource = true;
	}

	public String getURL() {
		return url;
	}

	public boolean isResource() {
		return isResource;
	}
}
