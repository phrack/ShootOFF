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
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.gui.targets.TargetView;
import com.shootoff.gui.controller.TargetEditorController;
import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.PolygonRegion;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class TargetIO {
	private static final Logger logger = LoggerFactory.getLogger(TargetIO.class);

	public static final double DEFAULT_OPACITY = 0.5;

	public static class TargetComponents {
		private final Group targetGroup;
		private final Map<String, String> targetTags;

		public TargetComponents(Group targetGroup, Map<String, String> targetTags) {
			this.targetGroup = targetGroup;
			this.targetTags = targetTags;
		}

		public Group getTargetGroup() {
			return targetGroup;
		}

		public Map<String, String> getTargetTags() {
			return targetTags;
		}
	}

	public static void saveTarget(final Map<String, String> targetTags, final List<Node> regions,
			final File targetFile) {
		RegionVisitor visitor;

		if (targetFile.getName().endsWith("target")) {
			visitor = new XMLTargetWriter(targetFile);
		} else {
			logger.error("Unknown target file type.");
			return;
		}

		final URI baseURI = new File(System.getProperty("user.dir")).toURI();

		for (final Node node : regions) {
			final TargetRegion region = (TargetRegion) node;

			switch (region.getType()) {
			case IMAGE: {
				final ImageRegion img = (ImageRegion) node;

				// Make image path relative to cwd so that image files can be
				// found on different machines
				final URI imgURI = new File(img.getImageFile().getAbsolutePath()).toURI();
				final File relativeImageFile = new File(baseURI.relativize(imgURI).getPath());

				visitor.visitImageRegion(img.getBoundsInParent().getMinX(), img.getBoundsInParent().getMinY(),
						relativeImageFile, img.getAllTags());
			}
			break;
			case RECTANGLE: {
				final RectangleRegion rec = (RectangleRegion) node;
				visitor.visitRectangleRegion(rec.getBoundsInParent().getMinX(), rec.getBoundsInParent().getMinY(),
						rec.getWidth(), rec.getHeight(), TargetEditorController.getColorName((Color) rec.getFill()),
						rec.getAllTags());
			}
			break;
			case ELLIPSE: {
				final EllipseRegion ell = (EllipseRegion) node;
				final double absoluteCenterX = ell.getBoundsInParent().getMinX() + ell.getRadiusX();
				final double absoluteCenterY = ell.getBoundsInParent().getMinY() + ell.getRadiusY();
				visitor.visitEllipse(absoluteCenterX, absoluteCenterY, ell.getRadiusX(), ell.getRadiusY(),
						TargetEditorController.getColorName((Color) ell.getFill()), ell.getAllTags());
			}
			break;
			case POLYGON: {
				final PolygonRegion pol = (PolygonRegion) node;

				final Double[] points = new Double[pol.getPoints().size()];

				for (int i = 0; i < pol.getPoints().size(); i += 2) {
					final Point2D p = pol.localToParent(pol.getPoints().get(i), pol.getPoints().get(i + 1));

					points[i] = p.getX();
					points[i + 1] = p.getY();
				}

				visitor.visitPolygonRegion(points, TargetEditorController.getColorName((Color) pol.getFill()),
						pol.getAllTags());
			}
			break;
			}
		}

		visitor.visitEnd(targetTags);
	}

	public static Optional<TargetComponents> loadTarget(final File targetFile) {
		return loadTarget(targetFile, true);
	}

	// Used for loading targets from resource files for modular exercises
	public static Optional<TargetComponents> loadTarget(final InputStream targetStream, final ClassLoader loader) {
		return loadTarget(targetStream, true, loader);
	}

	public static Optional<TargetComponents> loadTarget(final File targetFile, boolean playAnimations) {
		final TargetReader reader;

		if (targetFile.getName().endsWith("target")) {
			reader = new XMLTargetReader(targetFile, playAnimations);
		} else {
			logger.error("Unknown target file type.");
			return Optional.empty();
		}

		return Optional.of(new TargetComponents(processVisualTags(reader.getTargetNodes()), reader.getTargetTags()));
	}

	// Used for loading targets from resource files for modular exercises
	public static Optional<TargetComponents> loadTarget(final InputStream targetStream, boolean playAnimations,
			final ClassLoader loader) {
		final TargetReader reader = new XMLTargetReader(targetStream, playAnimations, loader);

		return Optional.of(new TargetComponents(processVisualTags(reader.getTargetNodes()), reader.getTargetTags()));
	}

	private static Group processVisualTags(List<Node> regions) {
		final Group targetGroup = new Group();
		for (final Node node : regions) {
			final TargetRegion region = (TargetRegion) node;

			if (region.tagExists(TargetView.TAG_VISIBLE)
					&& !Boolean.parseBoolean(region.getTag(TargetView.TAG_VISIBLE))) {
				node.setVisible(false);
			}

			if (region.getType() != RegionType.IMAGE) {
				if (region.tagExists(TargetView.TAG_OPACITY)) {
					node.setOpacity(Double.parseDouble(region.getTag(TargetView.TAG_OPACITY)));
				} else {
					node.setOpacity(DEFAULT_OPACITY);
				}
			}
			targetGroup.getChildren().add(node);
		}

		return targetGroup;
	}
}
