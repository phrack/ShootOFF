/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

package com.shootoff.courses.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLCourseWriter implements CourseVisitor {
	private static final Logger logger = LoggerFactory.getLogger(XMLCourseWriter.class);

	private final File courseFile;
	private StringBuilder xmlBody = new StringBuilder();

	public XMLCourseWriter(File courseFile) {
		this.courseFile = courseFile;
	}

	@Override
	public void visitBackground(String url, boolean isResource) {
		xmlBody.append(String.format("\t<background url=\"%s\" isResource=\"%b\" />%n", url, isResource));
	}

	@Override
	public void visitTarget(File targetFile, double x, double y, double width, double height) {
		xmlBody.append(
				String.format(Locale.US, "\t<target file=\"%s\" x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" />%n",
						targetFile.getPath(), x, y, width, height));
	}

	@Override
	public void visitResolution(double width, double height) {
		xmlBody.append(String.format(Locale.US, "\t<resolution width=\"%f\" height=\"%f\" />%n", width, height));
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
			logger.error("Error writing XML course", e);
		}
	}
}
