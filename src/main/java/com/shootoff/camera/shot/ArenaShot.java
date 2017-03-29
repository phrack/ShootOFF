package com.shootoff.camera.shot;

import java.util.Optional;

import javafx.scene.shape.Ellipse;

/**
 * This class encapsulates a DisplayShot which can be adjusted for arena canvases.
 * 
 * @author cbdmaul
 */
public class ArenaShot extends DisplayShot {
	Optional<Double> arenaX = Optional.empty(), arenaY = Optional.empty();
	Ellipse arenaMarker;
	
	public ArenaShot(DisplayShot shot)
	{
		super(shot, shot.getMarker());
		
		if (shot instanceof ArenaShot)
		{
			this.arenaX = ((ArenaShot) shot).arenaX;
			this.arenaY = ((ArenaShot) shot).arenaY;
		}
		
		this.arenaMarker = new Ellipse(getX(), getY(), shot.getMarker().getRadiusX(), shot.getMarker().getRadiusX());
		this.arenaMarker.setFill(colorMap.get(color));
	}

	public void setArenaCoords(double x, double y) {
		arenaX = Optional.of(x);
		arenaY = Optional.of(y);
		
		this.arenaMarker = new Ellipse(getX(), getY(), getMarker().getRadiusX(), getMarker().getRadiusX());
		this.arenaMarker.setFill(colorMap.get(color));
	}
	
	public double getX() {
		if (!arenaX.isPresent())
			return super.getX();
		return arenaX.get();
	}
	public double getY() {
		if (!arenaY.isPresent())
			return super.getY();
		return arenaY.get();
	}
	
	public double getArenaX() {
		if (!arenaX.isPresent())
			return super.getX();
		return arenaX.get();
	}
	public double getArenaY() {
		if (!arenaY.isPresent())
			return super.getY();
		return arenaY.get();
	}
	
	public Ellipse getMarker()
	{
		return this.arenaMarker;
	}
	
	public void setMarker(Ellipse marker)
	{
		this.arenaMarker = marker;
	}
}
