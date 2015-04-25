package com.shootoff.targets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
		List<Double> pointsX = new ArrayList<Double>();
		
		for (int i = 0; i < points.size(); i += 2) {
			pointsX.add(points.get(i));
		}
		
		double width = Collections.max(pointsX);
		double scaleFactor = (width + widthDelta) / width;
        
		this.setScaleX(this.getScaleX() * scaleFactor);
	}

	@Override
	public void changeHeight(double heightDelta) {
		ObservableList<Double> points = this.getPoints();
		List<Double> pointsY = new ArrayList<Double>();
		
		for (int i = 1; i < points.size(); i += 2) {
			pointsY.add(points.get(i));
		}
		
		double height = Collections.max(pointsY);
		double scaleFactor = (height + heightDelta) / height;
        
		this.setScaleY(this.getScaleY() * scaleFactor);
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
