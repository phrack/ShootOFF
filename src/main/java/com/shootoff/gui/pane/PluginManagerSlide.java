package com.shootoff.gui.pane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.controller.PluginManagerController;
import javafx.scene.layout.Pane;

public class PluginManagerSlide extends Slide {
	private static final Logger logger = LoggerFactory.getLogger(PluginManagerSlide.class);
	
	public PluginManagerSlide(Pane parentControls, Pane parentBody, PluginManagerController pluginManagerController) {
		super(parentControls, parentBody);
		
		addBodyNode(pluginManagerController.getPane());
	}
}
