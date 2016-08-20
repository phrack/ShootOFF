package com.shootoff.gui.pane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public abstract class Slide {
	private final List<Node> controlNodes = new ArrayList<>();
	private final List<Node> bodyNodes = new ArrayList<>();
	private final List<Node> savedControls = new ArrayList<>();
	private final List<Node> savedBody = new ArrayList<>();
	
	private Optional<SlideHiddenListener> slideHiddenListener = Optional.empty();
	private final Pane parentControls;
	private final Pane parentBody;
	
	public Slide(Pane parentControls, Pane parentBody) {
		this.parentControls = parentControls;
		this.parentBody = parentBody;
		
		final ImageView backImage = new ImageView(
				new Image(Slide.class.getResourceAsStream("/images/back_button.png"), 60.0, 60.0, true, true));
		 final Button backButton = addSlideControlButton("", (Event) -> hide());
		 backButton.setGraphic(backImage);
	}
	
	public void showControls() {
		show(parentControls, controlNodes, savedControls);
	}
	
	public void showBody() {
		show(parentBody, bodyNodes, savedBody);
	}
	
	public void hide() {
		hide(parentBody, bodyNodes, savedBody);
		hide(parentControls, controlNodes, savedControls);
		
		// Assumption that body will never show without the controls
		if (slideHiddenListener.isPresent()) slideHiddenListener.get().onSlideHidden();
	}
	
	private void show(Pane parentContainer, List<Node> slideList, List<Node> savedList) {
		// Do not show twice
		if (!savedList.isEmpty()) return;
		
		savedList.addAll(parentContainer.getChildren());
		parentContainer.getChildren().setAll(slideList);
	}
	
	private void hide(Pane parentContainer, List<Node> slideList, List<Node> savedList) {
		parentContainer.getChildren().removeAll(slideList);
		parentContainer.getChildren().addAll(savedList);
		savedList.clear();
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
		controlNodes.add(controlButton);
		
		return controlButton;
	}
	
	protected void addBodyNode(Node node) {
		bodyNodes.add(node);
	}
}
