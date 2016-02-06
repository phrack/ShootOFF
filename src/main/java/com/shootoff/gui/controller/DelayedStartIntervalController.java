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

package com.shootoff.gui.controller;

import com.shootoff.gui.DelayedStartListener;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DelayedStartIntervalController {
	private Stage delayedStartIntervalStage;
	@FXML private TextField minTextField;
	@FXML private TextField maxTextField;

	protected DelayedStartListener listener;

	public void init(DelayedStartListener listener) {
		this.listener = listener;

		minTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue.matches("\\d*")) {
					// int value = Integer.parseInt(newValue);
				} else {
					minTextField.setText(oldValue);
					minTextField.positionCaret(minTextField.getLength());
				}
			}
		});

		maxTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue.matches("\\d*")) {
					// int value = Integer.parseInt(newValue);
				} else {
					maxTextField.setText(oldValue);
					maxTextField.positionCaret(maxTextField.getLength());
				}
			}
		});
	}

	@FXML
	public void okClicked(ActionEvent event) {
		delayedStartIntervalStage = (Stage) minTextField.getScene().getWindow();
		delayedStartIntervalStage.close();
		listener.updatedDelayedStartInterval(Integer.parseInt(minTextField.getText()),
				Integer.parseInt(maxTextField.getText()));
	}
}
