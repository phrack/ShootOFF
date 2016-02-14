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

package com.shootoff.gui.controller;

import com.shootoff.gui.ParListener;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ParIntervalController extends DelayedStartIntervalController {
	@FXML private TextField parTextField;

	public void init(ParListener listener) {
		super.init(listener);

		parTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("^\\d*\\.?\\d*$")) {
					parTextField.setText(oldValue);
					parTextField.positionCaret(parTextField.getLength());
				}
			}
		});
	}

	@FXML
	@Override
	public void okClicked(ActionEvent event) {
		super.okClicked(event);
		((ParListener) listener).updatedParInterval(Double.parseDouble(parTextField.getText()));
	}
}
