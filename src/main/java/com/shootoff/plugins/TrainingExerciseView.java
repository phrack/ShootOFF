package com.shootoff.plugins;

import java.util.List;
import java.util.Optional;

import com.shootoff.camera.CameraView;
import com.shootoff.gui.ShotEntry;
import com.shootoff.targets.Target;

import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public interface TrainingExerciseView {
	Pane getTrainingExerciseContainer();

	TableView<ShotEntry> getShotEntryTable();
	
	VBox getButtonsPane();
	
	Optional<CameraView> getArenaView();
	
	List<Target> getTargets();
}
