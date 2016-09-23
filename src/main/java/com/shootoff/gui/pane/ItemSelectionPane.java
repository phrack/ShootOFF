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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextAlignment;

public class ItemSelectionPane<T> extends ScrollPane {
	private static final Logger logger = LoggerFactory.getLogger(ItemSelectionPane.class);
	private static final int DEFAULT_COLUMNS = 6;
	// Above MAX_COLUMNS and you end up having to move your mouse side to side
	// too much
	private static final int MAX_COLUMNS = 8; 
	private static final int ITEM_DIMS = 150;

	private final Map<Object, ButtonBase> items = new HashMap<>();
	private final TilePane subContainer = new TilePane(30, 30);
	private boolean toggleable;
	
	private ToggleGroup toggleGroup = null;
	private T defaultSelection = null;
	private T currentSelection = null;

	private ItemSelectionListener<T> itemListener = null;

	public ItemSelectionPane(boolean toggleItems, ItemSelectionListener<T> itemListener) {
		super();

		this.itemListener = itemListener;
		this.toggleable = toggleItems;

		if (toggleable) {
			toggleGroup = new ToggleGroup();
		}

		subContainer.setPrefColumns(DEFAULT_COLUMNS);
		subContainer.setPadding(new Insets(0, 65, 65, 65));
		
		this.widthProperty().addListener((observable, oldValue, newValue) -> {
			final Insets padding = subContainer.getPadding();
			final int hgap = (int) subContainer.getHgap();
			final int columnCount = (newValue.intValue() - (int) (padding.getLeft() + padding.getRight()) + hgap) / 
					(ITEM_DIMS + hgap);
			
			if (columnCount <= MAX_COLUMNS) subContainer.setPrefColumns(columnCount);
		});
		
		this.setStyle(
				"-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color:transparent;");
		this.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		this.setHbarPolicy(ScrollBarPolicy.NEVER);
		this.setFitToHeight(true);
		this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		this.setContent(subContainer);
	}

	public ItemSelectionPane(ToggleGroup toggleGroup, ItemSelectionListener<T> itemListener) {
		this(true, itemListener);
		
		this.toggleGroup = toggleGroup;
	}
	
	public ButtonBase addButton(T ref, String text, Optional<Node> graphic, Optional<Tooltip> tooltip) {
		final ButtonBase button;
		if (toggleable) {
			if (defaultSelection == null) defaultSelection = ref;

			button = new ToggleButton(text);

			((ToggleButton) button).setToggleGroup(toggleGroup);
		} else {
			button = new Button(text);
		}

		button.setContentDisplay(ContentDisplay.TOP);
		button.setTextAlignment(TextAlignment.CENTER);
		button.setPrefSize(ITEM_DIMS, ITEM_DIMS);
		button.setWrapText(true);

		if (graphic.isPresent()) button.setGraphic(graphic.get());
		if (tooltip.isPresent()) button.setTooltip(tooltip.get());

		button.setOnAction((event) -> {
			itemListener.onItemClicked(ref);

			if (toggleable) toggleGroup.selectToggle((Toggle) button);
		});

		subContainer.getChildren().add(button);

		items.put(ref, button);

		return button;
	}

	public ButtonBase addButton(T ref, String text) {
		return this.addButton(ref, text, Optional.empty(), Optional.empty());
	}

	public void setDefault(T ref) {
		if (!toggleable) {
			logger.error("setDefault only applies to toggleable item selection");
			return;
		}

		if (items.containsKey(ref)) {
			if (defaultSelection == null && currentSelection == null) {
				currentSelection = ref;
				toggleGroup.selectToggle((Toggle) items.get(ref));
			}
			defaultSelection = ref;
		} else
			logger.error("setDefault on non-existing ref - %s", ref);
	}

	public void removeButton(T ref) {
		if (!items.containsKey(ref)) {
			logger.error("removeButton on non-existing ref - %s", ref);
			return;
		}

		final Node item = items.remove(ref);

		if (Platform.isFxApplicationThread()) {
			subContainer.getChildren().remove(item);
		} else {
			Platform.runLater(() -> subContainer.getChildren().remove(item));
		}

		if (toggleable && ref == defaultSelection) {
			defaultSelection = null;
		}
		
		if (toggleable && ref == currentSelection && defaultSelection != null) {
			currentSelection = defaultSelection;
			itemListener.onItemClicked(currentSelection);
			toggleGroup.selectToggle((Toggle) items.get(currentSelection));
		}
	}

	public Object getCurrentSelection() {
		return currentSelection;
	}

	public ToggleGroup getToggleGroup() {
		return toggleGroup;
	}
	
	public void setSelection(T ref) {
		if (items.containsKey(ref)) {
			currentSelection = ref;
			toggleGroup.selectToggle((Toggle) items.get(currentSelection));
		} else {
			if (logger.isWarnEnabled()) logger.warn("setSelection on non-existing ref - {}", ref);
		}
	}
}
