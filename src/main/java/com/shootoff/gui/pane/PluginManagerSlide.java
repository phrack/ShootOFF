package com.shootoff.gui.pane;

import com.shootoff.gui.controller.PluginManagerController;
import javafx.scene.layout.Pane;

public class PluginManagerSlide extends Slide {
	public PluginManagerSlide(Pane parentControls, Pane parentBody, PluginManagerController pluginManagerController) {
		super(parentControls, parentBody);
		
		addBodyNode(pluginManagerController.getPane());
	}
}
