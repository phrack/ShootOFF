package com.shootoff.courses.io;

import java.io.File;

public interface CourseVisitor {
	public void visitBackground(String url, boolean isResource);
	public void visitTarget(File targetFile, double x, double y, double width, double height);
	public void visitEnd();
}
