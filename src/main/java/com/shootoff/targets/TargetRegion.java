package com.shootoff.targets;

import java.util.HashMap;
import java.util.Map;

public interface TargetRegion {
	public void changeWidth(double widthDelta);
	public void changeHeight(double heightDelta);

	final Map<String, String> tags = new HashMap<String, String>();
	
	default public void setTag(String name, String value) {
		tags.put(name, value);
	}
	
	default public String getTag(String name) {
		return tags.get(name);
	}
	
	default public void deleteTag(String name) {
		tags.remove(name);
	}
}
