package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import com.shootoff.targets.Target;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 * Implemented by {@link com.shootoff.gui.CanvasManager} to display camera
 * frames, shots, and targets.
 * 
 * @author phrack
 */
public interface CameraView {
	/**
	 * Add control to the GUI displaying camera and shot detection data.
	 * 
	 * @param c
	 *            the control to add to the GUI
	 * @return <tt>true</tt> if the GUI did not already contain <tt>c</tt>
	 */
	public boolean addChild(Node c);

	public void addShot(Color color, double x, double y);

	public Optional<Target> addTarget(File targetFile);

	public Target addTarget(Target newTarget);

	public Label addDiagnosticMessage(String message, Color backgroundColor);

	public void clearShots();

	public void close();

	public boolean removeChild(Node c);

	public void removeDiagnosticMessage(Label diagnosticLabel);

	public void reset();

	public void setCameraManager(CameraManager cameraManager);

	public void updateBackground(BufferedImage frame, Optional<Bounds> projectionBounds);
}
