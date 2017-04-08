package com.shootoff.camera.shot;

import java.util.Optional;

import com.shootoff.camera.Shot;


/**
 * This class encapsulates a Shot which can be adjusted for bounds
 * 
 * @author cbdmaul
 */
public class BoundsShot extends Shot {
	
	private Optional<Double> boundsX = Optional.empty();
	private Optional<Double> boundsY = Optional.empty();

	public BoundsShot(ShotColor color, double x, double y, long timestamp, int frame) {
		super(color, x, y, timestamp, frame);
	}

	public BoundsShot(ShotColor color, double x, double y, long timestamp) {
		super(color, x, y, timestamp);
	}

	public BoundsShot(Shot shot) {
		super(shot);
		if (shot instanceof BoundsShot)
		{
			this.boundsX = ((BoundsShot) shot).boundsX;
			this.boundsY = ((BoundsShot) shot).boundsY;
		}
	}

	public void adjustBounds(double adjX, double adjY) {
		boundsX = Optional.of(super.getX() + adjX);
		boundsY = Optional.of(super.getY() + adjY);
	}
	
	public double getBoundsX() {
		if (!boundsX.isPresent())
			return super.getX();
		return boundsX.get();
	}


	public double getBoundsY() {
		if (!boundsY.isPresent())
			return super.getY();
		return boundsY.get();
	}

	public double getX() {
		if (!boundsX.isPresent())
			return super.getX();
		return boundsX.get();
	}


	public double getY() {
		if (!boundsY.isPresent())
			return super.getY();
		return boundsY.get();
	}

}