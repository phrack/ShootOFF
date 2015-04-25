package com.shootoff.targets;

import java.util.HashMap;
import java.util.Map;

import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;

public class PolygonRegion extends Polygon implements TargetRegion {
	private final Map<String, String> tags = new HashMap<String, String>();
	
	public PolygonRegion(double... points) {
		super(points);
	}

	@Override
	public void changeWidth(double widthDelta) {
		ObservableList<Double> points = this.getPoints();
		for (int i = 2; i < points.size(); i += 2) {
			points.set(i, points.get(i) + widthDelta);
		}
	}

	@Override
	public void changeHeight(double heightDelta) {
	//	this.setHeight(this.getHeight() + heightDelta);
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
	public void replaceAllTags(Map<String, String> newTags) {
		tags.clear();
		tags.putAll(newTags);
	}
}
