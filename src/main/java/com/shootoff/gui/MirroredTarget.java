package com.shootoff.gui;

import java.io.File;
import java.util.Map;

import com.shootoff.config.Configuration;

import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class MirroredTarget extends TargetView {
	private MirroredTarget mirroredTarget;

	public MirroredTarget(File targetFile, Group target, Map<String, String> targetTags, Configuration config,
			CanvasManager parent, boolean userDeletable) {
		super(targetFile, target, targetTags, config, parent, userDeletable);
	}

	public void setMirroredTarget(MirroredTarget mirroredTarget) {
		this.mirroredTarget = mirroredTarget;

		mirrorKeyEvents();
		mirrorMouseEvents();
	}

	private void mirrorKeyEvents() {
		final EventHandler<? super KeyEvent> mirroredKeyHandler = mirroredTarget.getTargetGroup().getOnKeyPressed();
		final EventHandler<? super KeyEvent> thisKeyHandler = this.getTargetGroup().getOnKeyPressed();

		mirroredTarget.getTargetGroup().setOnKeyPressed((event) -> {
			mirroredKeyHandler.handle(event);
			thisKeyHandler.handle(event);
		});

		this.getTargetGroup().setOnKeyPressed((event) -> {
			thisKeyHandler.handle(event);
			mirroredKeyHandler.handle(event);
		});
	}

	private void mirrorMouseEvents() {
		final EventHandler<? super MouseEvent> thisMouseDraggedHandler = this.getTargetGroup().getOnMouseDragged();

		this.getTargetGroup().setOnMouseDragged((event) -> {
			thisMouseDraggedHandler.handle(event);

			final Dimension2D targetDimension = this.getDimension();
			mirroredTarget.mirrorSetDimensions(targetDimension.getWidth(), targetDimension.getHeight());
			final Point2D targetPosition = this.getPosition();
			mirroredTarget.mirrorSetPosition(targetPosition.getX(), targetPosition.getY());
		});

		final EventHandler<? super MouseEvent> thisMouseMovedHandler = this.getTargetGroup().getOnMouseMoved();

		this.getTargetGroup().setOnMouseMoved((event) -> {
			thisMouseMovedHandler.handle(event);

			final Dimension2D targetDimension = this.getDimension();
			mirroredTarget.mirrorSetDimensions(targetDimension.getWidth(), targetDimension.getHeight());
			final Point2D targetPosition = this.getPosition();
			mirroredTarget.mirrorSetPosition(targetPosition.getX(), targetPosition.getY());
		});
	}

	public MirroredTarget getMirroredTarget() {
		return mirroredTarget;
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
