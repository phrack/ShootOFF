/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	// We set a maximum on the number of control buttons at the top
	// because we want them to always maintain the same alighment and
	// size. If slide had more than the maximum, it would break
	// the size and alignment invariant.
	public static final int MAX_CONTROL_BUTTONS = 4;
	public static final int CONTROL_BUTTON_WIDTH = 150;
	public static final int CONTROL_BUTTON_HEIGHT = 100;
	
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
		// If we have less than the maximum number of control buttons
		// add empty panes as place holders for missing buttons to
		// ensure control button alignment and size is maintained.
		if (controlNodes.size() < MAX_CONTROL_BUTTONS) {
			final int sizeDelta = MAX_CONTROL_BUTTONS - controlNodes.size();
			
			for (int i = 0; i < sizeDelta; i++) {
				Pane placeHolderPane = new Pane();
				placeHolderPane.setPrefSize(CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT);
				controlNodes.add(placeHolderPane);
			}
		}
		
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
		if (controlNodes.size() >= MAX_CONTROL_BUTTONS) {
			throw new AssertionError("The slide already has the maximum number of control buttons");
		}
		
		final Button controlButton = new Button(text);
		controlButton.setPrefSize(CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT);
		controlButton.setOnAction(eventHandler);
		controlNodes.add(controlButton);
		
		return controlButton;
	}
	
	protected void addBodyNode(Node node) {
		bodyNodes.add(node);
	}
}
