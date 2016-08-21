package com.shootoff.gui.pane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.controller.SessionViewerController;

import javafx.scene.layout.Pane;

public class SessionViewerSlide extends Slide {
	private static final Logger logger = LoggerFactory.getLogger(SessionViewerSlide.class);
	
	private boolean saved = false;
	
	public SessionViewerSlide(Pane parentControls, Pane parentBody, SessionViewerController sessionViewerController) {
		super(parentControls, parentBody);
		
		addBodyNode(sessionViewerController.getPane());
	}
	
	public boolean isSaved() {
		return saved;
	}
}
