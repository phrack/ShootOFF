package com.shootoff.courses;

import java.util.List;
import java.util.Optional;

import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;

public class Course {
	private final Optional<LocatedImage> background;
	private final List<Target> targets;

	public Course(List<Target> targets) {
		background = Optional.empty();
		this.targets = targets;
	}

	public Course(LocatedImage background, List<Target> targets) {
		this.background = Optional.of(background);
		this.targets = targets;
	}

	public Optional<LocatedImage> getBackground() {
		return background;
	}

	public List<Target> getTargets() {
		return targets;
	}
}
