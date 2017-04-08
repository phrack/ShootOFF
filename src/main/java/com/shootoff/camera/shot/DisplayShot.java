package com.shootoff.camera.shot;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.Shot;

import javafx.scene.shape.Ellipse;


/**
 * This class encapsulates a BoundsShot which can be adjusted for display.
 * It also has the marker for display.
 * 
 * @author cbdmaul
 */
public class DisplayShot extends BoundsShot {
	private static final Logger logger = LoggerFactory.getLogger(DisplayShot.class);
	
	protected Ellipse marker;
	Optional<Double> displayX = Optional.empty();
	Optional<Double> displayY = Optional.empty();
	
	
	public DisplayShot(ShotColor color, double x, double y, long timestamp, int frame, int markerRadius) {
		super(color, x, y, timestamp, frame);
		marker = new Ellipse(x, y, markerRadius, markerRadius);
		marker.setFill(colorMap.get(color));
	}
	public DisplayShot(ShotColor color, double x, double y, long timestamp, int markerRadius) {
		super(color, x, y, timestamp);
		marker = new Ellipse(x, y, markerRadius, markerRadius);
		marker.setFill(colorMap.get(color));
	}
	
	public DisplayShot(Shot shot, int markerRadius) {
		super(shot);
		if (shot instanceof DisplayShot)
		{
			this.displayX = ((DisplayShot) shot).displayX;
			this.displayY = ((DisplayShot) shot).displayY;
		}
		marker = new Ellipse(getX(), getY(), markerRadius, markerRadius);
		marker.setFill(colorMap.get(color));
	}
	
	public DisplayShot(Shot shot, Ellipse marker) {
		super(shot);
		
		if (shot instanceof DisplayShot)
		{
			this.displayX = ((DisplayShot) shot).displayX;
			this.displayY = ((DisplayShot) shot).displayY;
		}
		this.marker = marker;
	}
	
	public Ellipse getMarker() {
		return marker;
	}


	public void setDisplayVals(int displayWidth, int displayHeight, int feedWidth, int feedHeight) {

		final double scaleX = (double) displayWidth / (double) feedWidth;
		final double scaleY = (double) displayHeight / (double) feedHeight;

		double scaledX, scaledY;
		if (displayX.isPresent()) {
			scaledX = displayX.get() * scaleX;
			scaledY = displayY.get() * scaleY;
		}
		else {
			scaledX = super.getX() * scaleX;
			scaledY = super.getY() * scaleY;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("setTranslation {} {} - {} {} to {} {}", scaleX, scaleY, super.getX(), super.getY(), scaledX, scaledY);
		}

		marker = new Ellipse(scaledX, scaledY, marker.radiusXProperty().get(), marker.radiusYProperty().get());
		marker.setFill(colorMap.get(color));

		displayX = Optional.of(scaledX);
		displayY = Optional.of(scaledY);
		
	}
	
	public double getX() {
		if (!displayX.isPresent())
			return super.getX();
		return displayX.get();
	}

	public double getY() {
		if (!displayY.isPresent())
			return super.getY();
		return displayY.get();
	}

	public double getDisplayX() {
		if (!displayX.isPresent())
			return super.getX();
		return displayX.get();
	}

	public double getDisplayY() {
		if (!displayY.isPresent())
			return super.getY();
		return displayY.get();
	}
	
	public Ellipse getDisplayMarker()
	{
		return this.marker;
	}
}
