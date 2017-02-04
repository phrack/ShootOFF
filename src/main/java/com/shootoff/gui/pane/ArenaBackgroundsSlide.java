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

package com.shootoff.gui.pane;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.shootoff.gui.LocatedImage;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArenaBackgroundsSlide extends Slide implements ItemSelectionListener<LocatedImage> {
	public static final Map<String, String> DEFAULT_BACKGROUNDS;
	
	static {
		final Map<String, String> defaultBackgrounds = new HashMap<>();
		defaultBackgrounds.put("Hickok45 Autumn", "/arena/backgrounds/hickok45_autumn.gif");
		defaultBackgrounds.put("Hickok45 Summer", "/arena/backgrounds/hickok45_summer.gif");
		defaultBackgrounds.put("Indoor Range", "/arena/backgrounds/indoor_range.gif");
		defaultBackgrounds.put("Kiang West Savanna", "/arena/backgrounds/kiang_west_savanna.gif");
		defaultBackgrounds.put("Oradour-sur-Glane", "/arena/backgrounds/oradour-sur-glane.gif");
		defaultBackgrounds.put("Outdoor Range", "/arena/backgrounds/outdoor_range.gif");
		defaultBackgrounds.put("Steel Range Bay", "/arena/backgrounds/steel_range_bay.gif");
		defaultBackgrounds.put("Subterranean Parking Lot", "/arena/backgrounds/subterranean_parking_lot.gif");
		
		DEFAULT_BACKGROUNDS = Collections.unmodifiableMap(defaultBackgrounds);
	}
	
	private final ItemSelectionPane<LocatedImage> itemPane = new ItemSelectionPane<>(true, this);
	private final ProjectorArenaPane arenaPane;
	private final Stage shootOffStage;

	private boolean choseBackground = false;

	public ArenaBackgroundsSlide(Pane parentControls, Pane parentBody, ProjectorArenaPane arenaPane,
			Stage shootOffStage) {
		super(parentControls, parentBody);

		this.arenaPane = arenaPane;
		this.shootOffStage = shootOffStage;

		addNoneButton();
		initDefaultBackgrounds();

		addBodyNode(itemPane);
	}

	private ButtonBase addNoneButton() {
		final LocatedImage none = new LocatedImage("/images/blank_page.png");

		final InputStream isThumbnail = ArenaBackgroundsSlide.class.getResourceAsStream("/images/blank_page.png");
		final ImageView thumbnailView = new ImageView(new Image(isThumbnail, 60, 60, true, true));

		final ToggleButton noneButton = (ToggleButton) itemPane.addButton(none, "None", Optional.of(thumbnailView),
				Optional.empty());
		noneButton.setSelected(true);
		itemPane.setDefault(none);

		return noneButton;
	}

	private void initDefaultBackgrounds() {
		new Thread(() -> {
			addDefaultBackground("Select Local Image...", "/images/normal_folder.png");
			
			for (Map.Entry<String, String> background : DEFAULT_BACKGROUNDS.entrySet())
				addDefaultBackground(background.getKey(), background.getValue());
		}).start();
	}

	private void addDefaultBackground(String buttonName, String resourceName) {
		final InputStream is = ArenaBackgroundsSlide.class.getResourceAsStream(resourceName);
		final LocatedImage img = new LocatedImage(is, resourceName);
		final InputStream isThumbnail = ArenaBackgroundsSlide.class.getResourceAsStream(resourceName);
		final ImageView thumbnailView = new ImageView(new Image(isThumbnail, 60, 60, true, true));

		itemPane.addButton(img, buttonName, Optional.of(thumbnailView), Optional.empty());
	}

	public void selectedLocalImage() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Arena Background");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Portable Network Graphic (*.png)", "*.png"),
				new FileChooser.ExtensionFilter("Graphics Interchange Format (*.gif)", "*.gif"));

		final File backgroundFile = fileChooser.showOpenDialog(shootOffStage);

		if (backgroundFile != null) {
			final LocatedImage img = new LocatedImage(backgroundFile.toURI().toString());
			arenaPane.setArenaBackground(img);
		}
	}

	@Override
	public void onItemClicked(LocatedImage selectedImage) {
		if (selectedImage.getURL().equals("/images/blank_page.png")) {
			arenaPane.setArenaBackground(null);
		} else if (selectedImage.getURL().equals("/images/normal_folder.png")) {
			selectedLocalImage();
		} else {
			arenaPane.setArenaBackground(selectedImage);
		}

		choseBackground = true;

		hide();
	}

	public void setChoseBackground(boolean choseBackground) {
		this.choseBackground = choseBackground;
	}

	public boolean choseBackground() {
		return choseBackground;
	}
}
