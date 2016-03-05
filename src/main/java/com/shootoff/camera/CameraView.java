package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import com.shootoff.gui.Target;

import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public interface CameraView {
	public void addShot(Color color, double x, double y);

	public Optional<Target> addTarget(File targetFile);

	public Target addTarget(Target newTarget);

	public Label addDiagnosticMessage(String message, Color backgroundColor);

	public void clearShots();

	public void close();

	public void removeDiagnosticMessage(Label diagnosticLabel);

	public void reset();

	public void setCameraManager(CameraManager cameraManager);

	public void updateBackground(BufferedImage frame, Optional<Bounds> projectionBounds);
}
