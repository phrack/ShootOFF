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
