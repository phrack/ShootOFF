package com.shootoff.targets;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.Shot;

import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * A potentially animated target that the user can shoot, reposition, and
 * resize. This interface should be implemented by a class that implements the
 * GUI code to allow users to see and interact with targets.
 * 
 * @author phrack
 */
public interface Target {
	public static final String TAG_IGNORE_HIT = "ignoreHit";
	public static final String TAG_RESIZABLE = "isResizable";
	public static final String TAG_OPACITY = "opacity";
	public static final String TAG_VISIBLE = "visible";
	public static final String TAG_DEFAULT_PERCEIVED_WIDTH = "defaultPerceivedWidth";
	public static final String TAG_DEFAULT_PERCEIVED_HEIGHT = "defaultPerceivedHeight";
	public static final String TAG_DEFAULT_PERCEIVED_DISTANCE = "defaultDistance";
	public static final String TAG_CURRENT_PERCEIVED_WIDTH = "currentPerceivedWidth";
	public static final String TAG_CURRENT_PERCEIVED_HEIGHT = "currentPerceivedHeight";
	public static final String TAG_CURRENT_PERCEIVED_DISTANCE = "currentDistance";
	public static final String TAG_SHOOTER_DISTANCE = "shooterDistance";
	
	public File getTargetFile();

	public int getTargetIndex();

	public void addTargetChild(Node child);

	public void removeTargetChild(Node child);

	public List<TargetRegion> getRegions();

	/**
	 * Check whether or not this target contains a particular region.
	 * 
	 * @param region
	 *            a region that may exist in this target
	 * @return <tt>true</tt> if the target contains <tt>region</tt>
	 */
	public boolean hasRegion(TargetRegion region);

	public void setVisible(boolean isVisible);

	public void setPosition(double x, double y);

	public Point2D getPosition();

	public void setDimensions(double newWidth, double newHeight);

	public Dimension2D getDimension();

	public Bounds getBoundsInParent();

	public void animate(TargetRegion region, List<String> args);

	public void reverseAnimation(TargetRegion region);

	public Optional<Hit> isHit(Shot shot);

	public boolean tagExists(String name);

	public String getTag(String name);

	public Map<String, String> getAllTags();
}
