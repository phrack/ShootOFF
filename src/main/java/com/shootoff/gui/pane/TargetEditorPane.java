package com.shootoff.gui.pane;

import com.shootoff.gui.controller.TargetEditorController;

import javafx.scene.layout.Pane;

public class TargetEditorPane extends SlidePane {
	
	public TargetEditorPane(Pane parent, TargetEditorController editorController) {
		super(parent);
		
		add(editorController.getPane());
	}
}
