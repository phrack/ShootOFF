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

import com.shootoff.camera.CameraManager;
import com.shootoff.camera.shotdetection.JavaShotDetector;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class ShotSectorPane extends BorderPane {
	private final CheckBox[][] sectors;

	public ShotSectorPane(Pane parent, CameraManager cameraManager) {
		final GridPane checkboxGrid = new GridPane();
		sectors = new CheckBox[JavaShotDetector.SECTOR_ROWS][JavaShotDetector.SECTOR_COLUMNS];

		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < JavaShotDetector.SECTOR_ROWS; y++) {
				final CheckBox sector = new CheckBox();
				sectors[y][x] = sector;
				sector.setSelected(cameraManager.isSectorOn(x, y));

				sector.setOnAction((event) -> {
					cameraManager.setSectorStatuses(getSectorStatuses());
				});

				checkboxGrid.add(sector, x, y);
			}
		}

		final Button doneButton = new Button("Done");

		doneButton.setOnAction((event) -> {
			parent.getChildren().remove(this);
		});

		setTop(checkboxGrid);
		setLeft(doneButton);

		parent.getChildren().add(this);
	}

	private boolean[][] getSectorStatuses() {
		final boolean[][] sectorStatuses = new boolean[JavaShotDetector.SECTOR_ROWS][JavaShotDetector.SECTOR_COLUMNS];

		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < JavaShotDetector.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = sectors[y][x].isSelected();
			}
		}

		return sectorStatuses;
	}
}
