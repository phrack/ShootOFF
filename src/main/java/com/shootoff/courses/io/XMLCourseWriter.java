package com.shootoff.courses.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class XMLCourseWriter implements CourseVisitor {
	private final File courseFile;
	private StringBuilder xmlBody = new StringBuilder();
	
	public XMLCourseWriter(File courseFile) {
		this.courseFile = courseFile;
	}

	@Override
	public void visitBackground(String url, boolean isResource) {
		xmlBody.append(String.format("\t<background url=\"%s\" isResource=\"%b\" />%n",
				url, isResource));		
	}

	@Override
	public void visitTarget(File targetFile, double x, double y, double width, double height) {
		xmlBody.append(String.format("\t<target file=\"%s\" x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" />%n",
				targetFile.getPath(), x, y, width, height));
	}

	@Override
	public void visitEnd() {
		try {
			PrintWriter out = new PrintWriter(courseFile, "UTF-8");
			
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<course>");
			out.print(xmlBody.toString());
			out.println("</course>");
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
