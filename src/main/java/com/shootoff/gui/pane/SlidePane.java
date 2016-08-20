package com.shootoff.gui.pane;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;

public class SlidePane extends Pane {
	private final VBox container = new VBox();
	private final List<Node> savedControls = new ArrayList<>();
	
	private final Pane parent;
	
	public SlidePane(Pane parent) {
		this.parent = parent;
		
		final ImageView backImage = new ImageView(
				new Image(SlidePane.class.getResourceAsStream("/images/back_button.png"), 64.0, 64.0, true, true));
		final Button backButton = new Button("", backImage);
		backButton.setOnAction((Event) -> hide());
		add(backButton);
		
		this.getChildren().add(container);
	}
	
	public void show() {
		if (!savedControls.isEmpty()) return;
		
		savedControls.addAll(parent.getChildren());
		parent.getChildren().clear();
		
		parent.getChildren().add(this);
	}
	
	public void hide() {
		parent.getChildren().remove(this);
		parent.getChildren().addAll(savedControls);
		savedControls.clear();
	}
	
	public void add(Node node) {
		container.getChildren().add(node);
	}
}
