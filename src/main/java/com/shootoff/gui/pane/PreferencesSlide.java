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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.PreferencesController;

import javafx.scene.layout.Pane;

public class PreferencesSlide extends Slide {
	private static final Logger logger = LoggerFactory.getLogger(PreferencesSlide.class);
	
	private boolean saved = false;
	
	public PreferencesSlide(Pane parentControls, Pane parentBody, PreferencesController preferencesController) {
		super(parentControls, parentBody);
		
		addSlideControlButton("Save", (event) -> {
			try {
				preferencesController.save();
			} catch (ConfigurationException | IOException e) {
				logger.error("Failed to save preferences", e);
			}
			
			saved = true;
			hide();
		});
		
		preferencesController.prepareToShow();
		
		addBodyNode(preferencesController.getPane());
	}
	
	public boolean isSaved() {
		return saved;
	}
}
