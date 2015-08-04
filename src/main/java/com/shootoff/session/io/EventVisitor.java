package com.shootoff.session.io;

import java.util.Optional;

import com.shootoff.camera.Shot;

public interface EventVisitor {
	public void visitCamera(String cameraName);
	public void visitCameraEnd();
	public void visitShot(long timestamp, Shot shot, Optional<Integer> targetIndex, Optional<Integer> hitRegionIndex);
	public void visitTargetAdd(long timestamp, String targetName);
	public void visitTargetRemove(long timestamp, int targetIndex);
	public void visitTargetResize(long timestamp, int targetIndex, double newWidth, double newHeight);
	public void visitTargetMove(long timestamp, int targetIndex, int newX, int newY);
	public void visitProtocolFeedMessage(long timestamp, String message);
	public void visitEnd();
}
