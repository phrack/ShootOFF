package com.shootoff.gui.pane;

import com.shootoff.gui.controller.SessionViewerController;

import javafx.scene.layout.Pane;

public class SessionViewerSlide extends Slide {
	public SessionViewerSlide(Pane parentControls, Pane parentBody, SessionViewerController sessionViewerController) {
		super(parentControls, parentBody);
		
		addBodyNode(sessionViewerController.getPane());
	}
}
