package com.shootoff.targets;

import java.util.Map;

public interface TargetRegion {
	public void changeWidth(double widthDelta);
	public void changeHeight(double heightDelta);
	public RegionType getType();
	public boolean tagExists(String name);
	public String getTag(String name);
	public Map<String, String> getAllTags();
	public void replaceAllTags(Map<String, String> newTags);
}
