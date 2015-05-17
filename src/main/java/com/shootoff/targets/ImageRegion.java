package com.shootoff.targets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.shootoff.targets.animation.SpriteAnimation;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageRegion extends ImageView implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();
	private final File imageFile;
	
	private Optional<SpriteAnimation> animation = Optional.empty();

	public ImageRegion(double x, double y, File imageFile) {
		super();
		
		this.setLayoutX(x);
		this.setLayoutY(y);
		this.imageFile = imageFile;
		
		try {
			this.setImage(new Image(new FileInputStream(imageFile)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean onFirstFrame() {
		if (!animation.isPresent()) {
			return true; 
		} else {
			return this.getImage().equals(animation.get().getFirstFrame());
		}
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
	public void changeWidth(double widthDelta) {
	}

	@Override
	public void changeHeight(double heightDelta) {
	}
	
	@Override
	public RegionType getType() {
		return RegionType.IMAGE;
	}
	
	@Override
	public boolean tagExists(String name) {
		return tags.containsKey(name);
	}
	
	@Override
	public String getTag(String name) {
		return tags.get(name);
	}
	
	@Override
	public Map<String, String> getAllTags() {
		return tags;
	}
	
	@Override
	public void setTags(Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}
}
