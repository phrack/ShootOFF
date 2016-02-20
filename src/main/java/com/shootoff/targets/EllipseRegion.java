/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.targets;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.shape.Ellipse;

public class EllipseRegion extends Ellipse implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();

	public EllipseRegion(double centerX, double centerY, double radiusX,
			double radiusY) {

		super(centerX, centerY, radiusX, radiusY);
	}
	private int regionImpactX = 0;
	private int regionImpactY = 0;
	
	@Override
	public void setRegionImpactX(int newImpactX){
		this.regionImpactX = newImpactX;
	}

	@Override
	public void setRegionImpactY(int newImpactY){
		this.regionImpactY = newImpactY;
	}

	@Override
	public int getRegionImpactX(){
		return this.regionImpactX;
	}
	@Override
	public int getRegionImpactY(){
		return this.regionImpactY;
	}

	@Override
	public void changeWidth(double widthDelta) {
		this.setRadiusX(this.getRadiusX() + widthDelta);
	}

	@Override
	public void changeHeight(double heightDelta) {
		this.setRadiusY(this.getRadiusY() + heightDelta);
	}

	@Override
	public RegionType getType() {
		return RegionType.ELLIPSE;
	}

	@Override
	public boolean tagExists(String name) {
		return tags.containsKey(name);
	}

	@Override
	public String getTag(String name) {
		return tags.get(name);
	}

	@Override
	public Map<String, String> getAllTags() {
		return tags;
	}

	@Override
	public void setTags(Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}

}
