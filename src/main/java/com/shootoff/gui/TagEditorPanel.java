/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

package com.shootoff.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Pane;

public class TagEditorPanel extends Pane {
	private TableView<Tag> tagTable = new TableView<Tag>();

	@SuppressWarnings("unchecked")
	public TagEditorPanel(Map<String, String> tags) {
		tagTable.setEditable(true);
		tagTable.setPrefHeight(200);
		tagTable.setPrefWidth(200);

		TableColumn<Tag, String> nameCol = new TableColumn<Tag, String>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory<Tag, String>(
				"name"));
		nameCol.setCellFactory(TextFieldTableCell.<Tag> forTableColumn());
		nameCol.setOnEditCommit(new EventHandler<CellEditEvent<Tag, String>>() {
			@Override
			public void handle(CellEditEvent<Tag, String> t) {
				((Tag) t.getTableView().getItems()
						.get(t.getTablePosition().getRow())).setName(t
						.getNewValue());
			}
		});

		TableColumn<Tag, String> valueCol = new TableColumn<Tag, String>(
				"Value");
		valueCol.setCellFactory(TextFieldTableCell.<Tag> forTableColumn());
		valueCol.setCellValueFactory(new PropertyValueFactory<Tag, String>(
				"value"));
		valueCol.setOnEditCommit(new EventHandler<CellEditEvent<Tag, String>>() {
			@Override
			public void handle(CellEditEvent<Tag, String> t) {
				((Tag) t.getTableView().getItems()
						.get(t.getTablePosition().getRow())).setValue(t
						.getNewValue());
			}
		});

		tagTable.getColumns().addAll(nameCol, valueCol);
		ObservableList<Tag> data = FXCollections.observableArrayList();

		for (Entry<String, String> entry : tags.entrySet())
			data.add(new Tag(entry.getKey(), entry.getValue()));

		tagTable.setItems(data);

		tagTable.setOnMouseClicked((event) -> {
			if (event.getClickCount() == 2) {
				data.add(new Tag("", ""));
			}
		});

		this.getChildren().add(tagTable);
	}

	public Map<String, String> getTags() {
		ObservableList<Tag> tags = tagTable.getItems();

		Map<String, String> tagMap = new HashMap<String, String>();

		for (Tag tag : tags) {
			if (!tag.getName().isEmpty() && !tag.getValue().isEmpty()) {
				tagMap.put(tag.getName(), tag.getValue());
			}
		}

		return tagMap;
	}

	public static class Tag {
		private String name;
		private String value;

		public Tag(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
