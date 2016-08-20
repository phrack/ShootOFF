package com.shootoff.gui.pane;

import com.shootoff.gui.controller.TargetEditorController;

import javafx.scene.layout.Pane;

public class TargetEditorSlide extends Slide {
	
	public TargetEditorSlide(Pane parentControls, Pane parentBody, TargetEditorController editorController) {
		super(parentControls, parentBody);
		
		addBodyNode(editorController.getPane());
	}
}
