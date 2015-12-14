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
		if (!newValue.matches("[0-9]*\\.?[0-9]+")) {
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
