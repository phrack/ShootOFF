package com.shootoff.targets.io;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.shootoff.gui.TargetEditorController;
import com.shootoff.targets.EllipseRegion;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.PolygonRegion;
import com.shootoff.targets.RectangleRegion;
import com.shootoff.targets.TargetRegion;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class TargetIO {
	public static void saveTarget(List<Node> regions, File targetFile) {
		RegionVisitor visitor;
		
		if (targetFile.getName().endsWith("target")) {
			visitor = new XMLTargetWriter(targetFile);
		} else {
			System.err.println("Unknown target file type.");
			return;
		}
		
		for (Node node : regions) {
			TargetRegion region = (TargetRegion)node;
			
			switch (region.getType()) {
			case IMAGE:
				ImageRegion img = (ImageRegion)node;
				visitor.visitImageRegion(img.getX(), img.getY(), 
						img.getImageFile(), img.getAllTags());
				break;
			case RECTANGLE:
				RectangleRegion rec = (RectangleRegion)node;
				visitor.visitRectangleRegion(rec.getX(), rec.getY(), 
						rec.getWidth(), rec.getHeight(), 
						TargetEditorController.getColorName((Color)rec.getFill()), 
						rec.getAllTags());
				break;
			case ELLIPSE:
				EllipseRegion ell = (EllipseRegion)node;
				visitor.visitEllipse(ell.getCenterX(), ell.getCenterY(), 
						ell.getRadiusX(), ell.getRadiusY(), 
						TargetEditorController.getColorName((Color)ell.getFill()), 
						ell.getAllTags());
				break;
			case POLYGON:
				PolygonRegion pol = (PolygonRegion)node;
				visitor.visitPolygonRegion(
						pol.getPoints().toArray(new Double[pol.getPoints().size()]), 
						TargetEditorController.getColorName((Color)pol.getFill()), 
						pol.getAllTags());
				break;
			}
		}
		
		visitor.visitEnd();
	}
	
	public static Optional<Group> loadTarget(File targetFile) {
		List<Node> regions;
		
		if (targetFile.getName().endsWith("target")) {
			regions = new XMLTargetReader(targetFile).load();
		} else {
			System.err.println("Unknown target file type.");
			return Optional.empty();
		}
		
		Group targetGroup = new Group();
		for (Node region : regions) {
			targetGroup.getChildren().add(region);
		}
		
		return Optional.of(targetGroup);
	}
}
