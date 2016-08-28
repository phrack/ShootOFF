package com.shootoff.gui;

import java.io.File;
import java.util.Map;

import com.shootoff.config.Configuration;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;

public class MirroredTarget extends TargetView {
	private MirroredTarget mirroredTarget;
	
	public MirroredTarget(File targetFile, Group target, Map<String, String> targetTags, Configuration config,
			CanvasManager parent, boolean userDeletable) {
		super(targetFile, target, targetTags, config, parent, userDeletable);
	}
	
	public void setMirroredTarget(MirroredTarget mirroredTarget) {
		this.mirroredTarget = mirroredTarget;
		
		final EventHandler<? super KeyEvent> mirroredKeyHandler = mirroredTarget.getTargetGroup().getOnKeyPressed();
		final EventHandler<? super KeyEvent> thisKeyHandler = this.getTargetGroup().getOnKeyPressed();
		
		mirroredTarget.getTargetGroup().setOnKeyPressed((event) -> {
			if (thisKeyHandler != null) thisKeyHandler.handle(event);
		});
		
		this.getTargetGroup().setOnKeyPressed((event) -> {
			if (mirroredKeyHandler != null) mirroredKeyHandler.handle(event);
		});
	}

	@Override
	public void setPosition(double x, double y) {
		mirroredTarget.mirrorSetPosition(x, y);
		super.setPosition(x, y);
	}

	public void mirrorSetPosition(double x, double y) {
		super.setPosition(x, y);
	}
	
	@Override
	public void setDimensions(double newWidth, double newHeight) {
		mirroredTarget.mirrorSetDimensions(newWidth, newHeight);
		super.setDimensions(newWidth, newHeight);
	}
	
	public void mirrorSetDimensions(double newWidth, double newHeight) {
		super.setDimensions(newWidth, newHeight);
	}
}
