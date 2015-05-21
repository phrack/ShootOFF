/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.gui;

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
	
	private DelayedStartListener listener;
	
	public void init(DelayedStartListener listener) {
		this.listener = listener;
		
		minTextField.textProperty().addListener(new ChangeListener<String>() {
		    @Override public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        if (newValue.matches("\\d*")) {
		         //   int value = Integer.parseInt(newValue);
		        } else {
		        	minTextField.setText(oldValue);
		        	minTextField.positionCaret(minTextField.getLength());
		        }
		    }
		});
		
		maxTextField.textProperty().addListener(new ChangeListener<String>() {
		    @Override public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        if (newValue.matches("\\d*")) {
		        //    int value = Integer.parseInt(newValue);
		        } else {
		        	maxTextField.setText(oldValue);
		        	maxTextField.positionCaret(maxTextField.getLength());
		        }
		    }
		});
	}
	
	@FXML
	public void okClicked(ActionEvent event) {
		delayedStartIntervalStage = (Stage)minTextField.getScene().getWindow();
		delayedStartIntervalStage.close();
		listener.updatedDelayedStartInterval(Integer.parseInt(minTextField.getText()), 
				Integer.parseInt(maxTextField.getText()));
	}
}
