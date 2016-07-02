package com.shootoff.targets.io;

import java.util.List;
import java.util.Map;

import javafx.scene.Node;

public interface TargetReader {
	public List<Node> getTargetNodes();
	public Map<String, String> getTargetTags();
}
