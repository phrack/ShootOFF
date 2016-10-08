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

package com.shootoff.targets.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLTargetWriter implements RegionVisitor {
	private final Logger logger = LoggerFactory.getLogger(XMLTargetWriter.class);

	private final File targetFile;
	private final StringBuilder xmlBody = new StringBuilder();

	public XMLTargetWriter(File targetFile) {
		this.targetFile = targetFile;
	}

	private void addTags(Map<String, String> tags) {
		for (final Entry<String, String> entry : tags.entrySet()) {
			xmlBody.append(String.format(Locale.US, "\t\t<tag name=\"%s\" value=\"%s\" />%n", entry.getKey(),
					entry.getValue()));
		}
	}

	@Override
	public void visitImageRegion(double x, double y, File imageFile, Map<String, String> tags) {

		xmlBody.append(
				String.format(Locale.US, "\t<image x=\"%f\" y=\"%f\" file=\"%s\">%n", x, y, imageFile.getPath()));

		addTags(tags);

		xmlBody.append("\t</image>\n");
	}

	@Override
	public void visitRectangleRegion(double x, double y, double width, double height, String fill,
			Map<String, String> tags) {

		xmlBody.append(String.format(Locale.US,
				"\t<rectangle x=\"%f\" y=\"%f\" width=\"%f\" " + "height=\"%f\" fill=\"%s\">%n", x, y, width, height,
				fill));

		addTags(tags);

		xmlBody.append("\t</rectangle>\n");
	}

	@Override
	public void visitEllipse(double centerX, double centerY, double radiusX, double radiusY, String fill,
			Map<String, String> tags) {

		xmlBody.append(String.format(Locale.US,
				"\t<ellipse centerX=\"%f\" centerY=\"%f\" radiusX=\"%f\" " + "radiusY=\"%f\" fill=\"%s\">%n", centerX,
				centerY, radiusX, radiusY, fill));

		addTags(tags);

		xmlBody.append("\t</ellipse>\n");
	}

	@Override
	public void visitPolygonRegion(Double[] points, String fill, Map<String, String> tags) {

		xmlBody.append(String.format("\t<polygon fill=\"%s\">%n", fill));

		for (int i = 0; i < points.length - 1; i += 2) {
			xmlBody.append(String.format(Locale.US, "\t\t<point x=\"%f\" y=\"%f\" />%n", points[i], points[i + 1]));
		}

		addTags(tags);

		xmlBody.append("\t</polygon>\n");
	}

	@Override
	public void visitEnd(Map<String, String> targetTags) {
		try {
			final PrintWriter out = new PrintWriter(targetFile, "UTF-8");

			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

			final StringBuilder targetAttributes = new StringBuilder();
			for (final Entry<String, String> entry : targetTags.entrySet()) {
				if (targetAttributes.length() > 0) targetAttributes.append(" ");

				targetAttributes.append(entry.getKey());
				targetAttributes.append("=\"");
				targetAttributes.append(entry.getValue());
				targetAttributes.append("\"");
			}

			if (targetAttributes.length() > 0) {
				out.format("<target %s>%n", targetAttributes.toString());
			} else {
				out.println("<target>");
			}

			out.print(xmlBody.toString());
			out.println("</target>");

			out.close();

		} catch (final IOException e) {
			logger.error("Error writing XML target", e);
		}
	}
}
