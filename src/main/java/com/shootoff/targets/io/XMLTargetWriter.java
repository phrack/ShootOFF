/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.targets.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class XMLTargetWriter implements RegionVisitor {
	private final File targetFile;
	private StringBuilder xmlBody = new StringBuilder();
	
	public XMLTargetWriter(File targetFile) {
		this.targetFile = targetFile;
	}
	
	private void addTags(Map<String, String> tags) {
		for (String name : tags.keySet()) {
			xmlBody.append(String.format("\t\t<tag name=\"%s\" value=\"%s\" />\n",
					name, tags.get(name)));
		}
	}
	
	@Override
	public void visitImageRegion(double x, double y, File imageFile,
			Map<String, String> tags) {
		
		xmlBody.append(String.format(
				"\t<image x=\"%f\" y=\"%f\" file=\"%s\">\n", 
				x, y, imageFile.getPath()));
		
		addTags(tags);
		
		xmlBody.append("\t</image>\n");
	}

	@Override
	public void visitRectangleRegion(double x, double y, double width,
			double height, String fill, Map<String, String> tags) {
	
		xmlBody.append(String.format(
				"\t<rectangle x=\"%f\" y=\"%f\" width=\"%f\" "
				+ "height=\"%f\" fill=\"%s\">\n", 
				x, y, width, height, fill));
		
		addTags(tags);
		
		xmlBody.append("\t</rectangle>\n");
	}

	@Override
	public void visitEllipse(double centerX, double centerY, double radiusX,
			double radiusY, String fill, Map<String, String> tags) {
	
		xmlBody.append(String.format(
				"\t<ellipse centerX=\"%f\" centerY=\"%f\" radiusX=\"%f\" "
				+ "radiusY=\"%f\" fill=\"%s\">\n", 
				centerX, centerY, radiusX, radiusY, fill));
		
		addTags(tags);
		
		xmlBody.append("\t</ellipse>\n");
	}

	@Override
	public void visitPolygonRegion(Double[] points, String fill,
			Map<String, String> tags) {
	
		xmlBody.append(String.format(
				"\t<polygon fill=\"%s\">\n", fill));
		
		for (int i = 0; i < points.length - 1; i += 2) {
			xmlBody.append(String.format("\t\t<point x=\"%f\" y=\"%f\" />\n",
					points[i], points[i+1]));
		}
		
		addTags(tags);
		
		xmlBody.append("\t</polygon>\n");
	}

	@Override
	public void visitEnd() {
		try {
			PrintWriter out = new PrintWriter(targetFile);
			
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<target>");
			out.print(xmlBody.toString());
			out.println("</target>");
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
