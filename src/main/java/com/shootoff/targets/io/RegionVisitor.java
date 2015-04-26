package com.shootoff.targets.io;

import java.io.File;
import java.util.Map;

public interface RegionVisitor {
	public void visitImageRegion(double x, double y, File imageFile, 
			Map<String, String> tags);
	public void visitRectangleRegion(double x, double y, 
			double width, double height, String fill, Map<String, String> tags);
	public void visitEllipse(double centerX, double centerY, 
			double radiusX, double radiusY,
			String fill, Map<String, String> tags);
	public void visitPolygonRegion(Double[] points, String fill, 
			Map<String, String> tags);
	public void visitEnd();
}
