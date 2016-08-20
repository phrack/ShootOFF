package com.shootoff.gui.pane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class SlidePane extends Pane {
	private final VBox container = new VBox();
	private final HBox slideControlContainer = new HBox(175);
	private final List<Node> savedControls = new ArrayList<>();
	
	private Optional<SlideHiddenListener> slideHiddenListener = Optional.empty();
	private final Pane parent;
	
	public SlidePane(Pane parent) {
		this.parent = parent;
		
		slideControlContainer.setPadding(new Insets(30, 30, 0, 30));
		
		final ImageView backImage = new ImageView(
				new Image(SlidePane.class.getResourceAsStream("/images/back_button.png"), 80.0, 80.0, true, true));
		 final Button backButton = addSlideControlButton("", (Event) -> hide());
		 backButton.setGraphic(backImage);
		 
		 container.getChildren().add(slideControlContainer);
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
		
		if (slideHiddenListener.isPresent()) slideHiddenListener.get().onSlideHidden();
	}
	
	public void setOnSlideHidden(SlideHiddenListener slideHiddenListener) {
		this.slideHiddenListener = Optional.ofNullable(slideHiddenListener);
	}
	
	// This used to add the buttons that appear across the very top row
	// of the slide. These are intended to control the content
	// that appears on the rest of the slide
	protected Button addSlideControlButton(String text, final EventHandler<ActionEvent> eventHandler) {
		final Button controlButton = new Button(text);
		controlButton.setPrefSize(150, 100);
		controlButton.setOnAction(eventHandler);
		slideControlContainer.getChildren().add(controlButton);
		
		return controlButton;
	}
	
	protected void add(Node node) {
		container.getChildren().add(node);
	}
}
