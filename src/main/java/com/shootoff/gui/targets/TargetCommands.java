package com.shootoff.gui.targets;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.plugins.TrainingExerciseBase;
import com.shootoff.config.Configuration;
import com.shootoff.gui.Resetter;
import com.shootoff.targets.Hit;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

public class TargetCommands implements CommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(TargetCommands.class);
	
	private final List<Target> targets;
	private final Resetter resetter;
	private final Hit hit;
	private final Configuration config;
	
	public TargetCommands(List<Target> targets, Resetter resetter, Hit hit) {
		this.targets = targets;
		this.resetter = resetter;
		this.hit = hit;
		config = Configuration.getConfig();
	}
	
	@Override
	public void process(List<String> commands, String commandName, List<String> args) {
		switch (commandName) {
		case "reset":
			resetter.reset();
			break;

		case "animate":
			hit.getTarget().animate(hit.getHitRegion(), args);
			break;

		case "reverse":
			hit.getTarget().reverseAnimation(hit.getHitRegion());
			break;

		case "play_sound":
			// If there is a second parameter, we should look to see
			// if it's an image region that is down and if so, don't
			// play the sound
			if (args.size() == 2) {
				final Optional<TargetRegion> namedRegion = TargetView.getTargetRegionByName(targets,
						hit.getHitRegion(), args.get(1));
				if (namedRegion.isPresent() && namedRegion.get().getType() == RegionType.IMAGE) {
					if (!((ImageRegion) namedRegion.get()).onFirstFrame()) break;
				}
			}

			// If the string starts with an @ we are supposed to
			// load the sound as a resource from the current exercises
			// JAR file. This indicates that the target is from
			// a modular exercise
			final String soundPath = args.get(0);
			if (config.getExercise().isPresent() && '@' == soundPath.charAt(0)) {
				final InputStream is = config.getExercise().get().getClass()
						.getResourceAsStream(soundPath.substring(1));
				TrainingExerciseBase.playSound(new BufferedInputStream(is));
			} else if ('@' != soundPath.charAt(0)) {
				TrainingExerciseBase.playSound(soundPath);
			} else {
				logger.error("Can't play {} because it is a resource in an exercise but no exercise is loaded.",
						soundPath);
			}

			break;
		}	
	}
}
