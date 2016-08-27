package com.shootoff.gui.pane;

import java.io.File;
import java.io.InputStream;

import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.controller.ProjectorArenaController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArenaBackgroundsSlide extends Slide {
	private final ProjectorArenaController arenaController;
	private final Stage shootOffStage;
	
	public ArenaBackgroundsSlide(Pane parentControls, Pane parentBody, ProjectorArenaController arenaController,
			Stage shootOffStage) {
		super(parentControls, parentBody);
		
		this.arenaController = arenaController;
		this.shootOffStage = shootOffStage;
		
		initDefaultBackgrounds();
	}

	private void initDefaultBackgrounds() {
		addDefaultBackground("Hickok45 Autumn", "/arena/backgrounds/hickok45_autumn.gif");
		addDefaultBackground("Hickok45 Summer", "/arena/backgrounds/hickok45_summer.gif");
		addDefaultBackground("Indoor Range", "/arena/backgrounds/indoor_range.gif");
		addDefaultBackground("Kiang West Savanna", "/arena/backgrounds/kiang_west_savanna.gif");
		addDefaultBackground("Outdoor Range", "/arena/backgrounds/outdoor_range.gif");
		addDefaultBackground("Steel Range Bay", "/arena/backgrounds/steel_range_bay.gif");
	}

	private void addDefaultBackground(String menuName, String resourceName) {
		MenuItem backgroundMenuItem = new MenuItem(menuName);

		backgroundMenuItem.setOnAction((e) -> {
			InputStream is = this.getClass().getResourceAsStream(resourceName);
			LocatedImage img = new LocatedImage(is, resourceName);
			arenaController.setBackground(img);
		});

		//arenaBackgroundMenu.getItems().add(backgroundMenuItem);
	}

	@FXML
	public void clearArenaTargetsMenuItemClicked(ActionEvent event) {
		arenaController.getCanvasManager().clearTargets();
	}

	@FXML
	public void removeArenaBackgroundMenuItemClicked(ActionEvent event) {
		arenaController.setBackground(null);
	}

	@FXML
	public void openArenaBackgroundMenuItemClicked(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Arena Background");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png"),
				new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"));

		File backgroundFile = fileChooser.showOpenDialog(shootOffStage);

		if (backgroundFile != null) {
			LocatedImage img = new LocatedImage(backgroundFile.toURI().toString());
			arenaController.setBackground(img);
		}
	}
}
