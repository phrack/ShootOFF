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

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SpriteAnimation extends Transition {
	public static final int DEFAULT_DELAY = 100;

	private final ImageView imageView;
	private final ImageFrame[] frames;
	private final int count;

	private int lastIndex;
	private boolean isReversed = false;

	public SpriteAnimation(ImageView imageView, ImageFrame[] frames) {
		this.imageView = imageView;
		this.frames = new ImageFrame[frames.length];
		System.arraycopy(frames, 0, this.frames, 0, frames.length);
		count = frames.length;
		setInterpolator(Interpolator.LINEAR);
	}

	public Image getFrame(int frameNumber) {
		return frames[frameNumber].getImage();
	}

	public void setCurrentFrame(int frameNumber) {
		imageView.setImage(getFrame(frameNumber));
	}

	public Image getFirstFrame() {
		return isReversed ? frames[frames.length - 1].getImage() : frames[0].getImage();
	}

	public int getFrameCount() {
		return frames.length;
	}

	public void reset() {
		if (getStatus() == Status.RUNNING) stop();

		isReversed = false;
		lastIndex = 0;
		setRate(Math.abs(getRate()));
		imageView.setImage(getFirstFrame());
	}

	@Override
	protected void interpolate(double k) {
		final int index = Math.min((int) Math.floor(k * count), count - 1);
		if (index != lastIndex) {
			imageView.setImage(frames[index].getImage());
			lastIndex = index;
		}
	}

	public void reverse() {
		isReversed = !isReversed;
		setRate(getRate() * -1);
	}
}