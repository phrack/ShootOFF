/* Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

import java.util.HashMap;
import java.util.Map;

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
        nameCol.setCellValueFactory(new PropertyValueFactory<Tag, String>("name"));
        nameCol.setCellFactory(TextFieldTableCell.<Tag>forTableColumn());
        nameCol.setOnEditCommit(
        	    new EventHandler<CellEditEvent<Tag, String>>() {
        	        @Override
        	        public void handle(CellEditEvent<Tag, String> t) {
        	            ((Tag)t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).setName(t.getNewValue());
        	        }
        	    }
        	);
        
        TableColumn<Tag, String> valueCol = new TableColumn<Tag, String>("Value");
        valueCol.setCellFactory(TextFieldTableCell.<Tag>forTableColumn());
        valueCol.setCellValueFactory(new PropertyValueFactory<Tag, String>("value"));
        valueCol.setOnEditCommit(
        	    new EventHandler<CellEditEvent<Tag, String>>() {
        	        @Override
        	        public void handle(CellEditEvent<Tag, String> t) {
        	            ((Tag)t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).setValue(t.getNewValue());
        	        }
        	    }
        	);
        
        tagTable.getColumns().addAll(nameCol, valueCol);
        ObservableList<Tag> data = FXCollections.observableArrayList();
        
        for (String name : tags.keySet())
        	data.add(new Tag(name, tags.get(name)));
        
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
	
	public class Tag {
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
