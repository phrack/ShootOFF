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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.targets.animation.SpriteAnimation;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class ImageRegion extends ImageView implements TargetRegion {
	private static final Logger logger = LoggerFactory.getLogger(ImageRegion.class);
	
	private final Map<String, String> tags = new HashMap<>();
	private final File imageFile;

	private Optional<SpriteAnimation> animation = Optional.empty();

	public ImageRegion(final double x, final double y, final File imageFile) throws FileNotFoundException {
		this(x, y, imageFile, new FileInputStream(imageFile));
	}

	public ImageRegion(final double x, final double y, final File imageFile, final InputStream imageStream) {
		super();

		setLayoutX(x);
		setLayoutY(y);
		this.imageFile = imageFile;

		setImage(new Image(imageStream));
	}
	
	public ImageRegion(Image image) {
		super(image);
		this.imageFile = null;
	}

	public boolean onFirstFrame() {
		return animation.isPresent() ? getImage().equals(animation.get().getFirstFrame()) : true;
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
	public void setFill(Color fill) {
		logger.warn("setFill on ImageRegion ignored");
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
