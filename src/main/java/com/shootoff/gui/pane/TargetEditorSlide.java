package com.shootoff.gui.pane;

import com.shootoff.gui.controller.TargetEditorController;

import javafx.scene.layout.Pane;

public class TargetEditorSlide extends SlidePane {
	
	public TargetEditorSlide(Pane parent, TargetEditorController editorController) {
		super(parent);
		
		add(editorController.getPane());
	}
}
