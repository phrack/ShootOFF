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

package com.shootoff.targets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.targets.animation.SpriteAnimation;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageRegion extends ImageView implements TargetRegion {
	private static final Logger logger = LoggerFactory.getLogger(ImageRegion.class);

	private final Map<String, String> tags = new HashMap<String, String>();
	private final File imageFile;

	private Optional<SpriteAnimation> animation = Optional.empty();

	public ImageRegion(final double x, final double y, final File imageFile) {
		super();

		this.setLayoutX(x);
		this.setLayoutY(y);
		this.imageFile = imageFile;

		try {
			this.setImage(new Image(new FileInputStream(imageFile)));
		} catch (IOException e) {
			logger.error("Error reading image file to set image target region's picture", e);
		}
	}

	public boolean onFirstFrame() {
		return animation.isPresent() ? this.getImage().equals(animation.get().getFirstFrame()) : true;
	}

	public File getImageFile() {
		return imageFile;
	}

	public void setAnimation(SpriteAnimation animation) {
		this.animation = Optional.of(animation);
	}

	public Optional<SpriteAnimation> getAnimation() {
		return animation;
	}

	public void reset() {
		if (animation.isPresent()) animation.get().reset();
	}

	@Override
	public void changeWidth(final double widthDelta) {}

	@Override
	public void changeHeight(final double heightDelta) {}

	@Override
	public RegionType getType() {
		return RegionType.IMAGE;
	}

	@Override
	public boolean tagExists(final String name) {
		return tags.containsKey(name);
	}

	@Override
	public String getTag(final String name) {
		return tags.get(name);
	}

	@Override
	public Map<String, String> getAllTags() {
		return tags;
	}

	@Override
	public void setTags(final Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}
}
