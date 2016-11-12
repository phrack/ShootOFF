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

package com.shootoff.targets.animation;

import java.awt.image.BufferedImage;

import com.shootoff.util.SwingFXUtils;
import javafx.scene.image.Image;

public class ImageFrame {
	private final int delay;
	private final BufferedImage bufferedImage;
	private final Image image;
	private final String disposal;

	public ImageFrame(BufferedImage image, int delay, String disposal) {
		bufferedImage = image;
		this.image = SwingFXUtils.toFXImage(image, null);
		this.delay = delay;
		this.disposal = disposal;
	}

	public ImageFrame(BufferedImage image) {
		bufferedImage = image;
		this.image = SwingFXUtils.toFXImage(image, null);
		delay = -1;
		disposal = null;
	}

	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	public Image getImage() {
		return image;
	}

	public int getDelay() {
		return delay;
	}

	public String getDisposal() {
		return disposal;
	}
}
