package com.shootoff.plugins.engine;

import com.shootoff.plugins.TrainingExercise;

public interface PluginListener {
	public void registerExercise(TrainingExercise exercise);

	public void registerProjectorExercise(TrainingExercise exercise);

	public void unregisterExercise(TrainingExercise exercise);
}
