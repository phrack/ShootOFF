package com.shootoff.targets;

import javafx.scene.shape.Ellipse;

public class EllipseRegion extends Ellipse implements TargetRegion {
	public EllipseRegion(double centerX, double centerY, 
			double radiusX, double radiusY) {
		
		super(centerX, centerY, radiusX, radiusY);
	}

	@Override
	public void changeWidth(double widthDelta) {
		this.setRadiusX(this.getRadiusX() + widthDelta);
	}

	@Override
	public void changeHeight(double heightDelta) {
		this.setRadiusY(this.getRadiusY() + heightDelta);
	}
}
