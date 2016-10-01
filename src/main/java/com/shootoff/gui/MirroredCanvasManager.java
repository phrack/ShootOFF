package com.shootoff.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;
import com.shootoff.gui.pane.ProjectorArenaPane;
import com.shootoff.targets.Target;
import com.shootoff.targets.io.TargetIO.TargetComponents;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.image.Image;

public class MirroredCanvasManager extends CanvasManager {
	private final ProjectorArenaPane arenaPane;
	private final Configuration config;
	
	private MirroredCanvasManager mirroredManager;
	
	public MirroredCanvasManager(Group canvasGroup, Resetter resetter, String cameraName,
			ObservableList<ShotEntry> shotEntries, ProjectorArenaPane arenaPane) {
		super(canvasGroup, resetter, cameraName, shotEntries);
		
		this.config = Configuration.getConfig();
		this.arenaPane = arenaPane;
	}
 
	public void setMirroredManager(MirroredCanvasManager mirroredManager) {
		this.mirroredManager = mirroredManager;
	}
	
	@Override
	public Optional<Target> addTarget(File targetFile, boolean playAnimations) {
		return mirroredManager.mirrorAddTarget(targetFile, playAnimations);
	}
	
	public Optional<Target> mirrorAddTarget(File targetFile, boolean playAnimations) {
		return super.addTarget(targetFile, playAnimations);
	}
	
	@Override
	public Optional<Target> addTarget(File targetFile) {
		return mirroredManager.mirrorAddTarget(targetFile);
	}
	
	public Optional<Target> mirrorAddTarget(File targetFile) {
		return super.addTarget(targetFile);
	}
	
	@Override
	public Target addTarget(File targetFile, Group targetGroup, Map<String, String> targetTags, boolean userDeletable) {
		final Optional<TargetComponents> targetComponents = super.loadTarget(targetFile, false);
		
		if (targetComponents.isPresent()) {
			final TargetComponents tc = targetComponents.get();
			return mirroredManager.mirrorAddTarget(targetFile, tc.getTargetGroup(), tc.getTargetTags(), userDeletable);
		}
		
		return null;
	}
	
	public Target mirrorAddTarget(File targetFile, Group targetGroup, Map<String, String> targetTags, boolean userDeletable) {
		return super.addTarget(targetFile, targetGroup, targetTags, userDeletable);
	}
	
	@Override
	public Target addTarget(Target newTarget) {
		final MirroredTarget target;
		
		if (newTarget instanceof MirroredTarget) {
			target = (MirroredTarget) newTarget;
		} else {
			target = new MirroredTarget(newTarget.getTargetFile(), ((TargetView) newTarget).getTargetGroup(),
					newTarget.getAllTags(), config, this, ((TargetView) newTarget).isUserDeletable());
		}
		
		final Optional<TargetComponents> targetComponents = super.loadTarget(newTarget.getTargetFile(), false);
		
		if (targetComponents.isPresent()) {
			final TargetComponents tc = targetComponents.get();
			final MirroredTarget t = new MirroredTarget(newTarget.getTargetFile(), tc.getTargetGroup(), tc.getTargetTags(), config, 
					mirroredManager, ((TargetView) newTarget).isUserDeletable());
			final Dimension2D targetDimension = target.getDimension(); 
			t.mirrorSetDimensions(targetDimension.getWidth(), targetDimension.getHeight());
			final Point2D targetPosition = target.getPosition();
			t.mirrorSetPosition(targetPosition.getX(), targetPosition.getY());
			mirroredManager.mirrorAddTarget(t);
			
			target.setMirroredTarget(t);
			t.setMirroredTarget(target);
			
			arenaPane.targetAdded(target);
		}
		
		return super.addTarget(target);
	}
	
	public Target mirrorAddTarget(Target newTarget) {
		return super.addTarget(newTarget);
	}
	
	@Override
	public void removeTarget(Target target) {
		if (!(target instanceof MirroredTarget)) {
			throw new AssertionError("Expected a target passed to removeTarget on the arena pane to be a MirroredTarget");
		}
		
		mirroredManager.mirrorRemoveTarget(((MirroredTarget) target).getMirroredTarget());
		super.removeTarget(target);
		
		arenaPane.targetRemoved(target);
	}
	
	public void mirrorRemoveTarget(Target target) {
		super.removeTarget(target);
	}
	
	@Override
	public void clearTargets() {
		mirroredManager.mirrorClearTargets();
		super.clearTargets();
	}
	
	public void mirrorClearTargets() {
		super.clearTargets();
	}
	
	@Override
	public void setBackgroundFit(double width, double height) {
		mirroredManager.mirrorSetBackgroundFit(width, height);
		super.setBackgroundFit(width, height);
	}
	
	public void mirrorSetBackgroundFit(double width, double height) {
		super.setBackgroundFit(width, height);
	}
	
	@Override
	public void updateBackground(BufferedImage frame, Optional<Bounds> projectionBounds) {
		mirroredManager.mirrorUpdateBackground(frame, projectionBounds);
		super.updateBackground(frame, projectionBounds);
	}
	
	public void mirrorUpdateBackground(BufferedImage frame, Optional<Bounds> projectionBounds) {
		super.updateBackground(frame, projectionBounds);
	}
	
	@Override
	public void updateBackground(Image img) {
		mirroredManager.mirrorUpdateBackground(img);
		super.updateBackground(img);
	}
	
	public void mirrorUpdateBackground(Image img) {
		super.updateBackground(img);
	}
	
	@Override
	public void addShot(Shot shot, boolean isMirroredShot) {
		final Shot mirroredShot = new Shot(shot.getColor(), shot.getX(), shot.getY(), shot.getTimestamp(), (int) shot.getMarker().getRadiusX());
		shot.setMirroredShot(mirroredShot);
		mirroredShot.setMirroredShot(shot);
		mirroredManager.mirrorAddShot(mirroredShot);
		super.addShot(shot, isMirroredShot);
	}
	
	public void mirrorAddShot(Shot shot) {
		super.addShot(shot, true);
	}
	
	@Override
	public boolean addArenaShot(Shot shot, Optional<String> videoString, boolean isMirroredShot) {
		mirroredManager.mirrorAddArenaShot(
				new Shot(shot.getColor(), shot.getX(), shot.getY(), shot.getTimestamp(), (int) shot.getMarker().getRadiusX()),
				videoString);
		return super.addArenaShot(shot, videoString, isMirroredShot);
	}
	
	public boolean mirrorAddArenaShot(Shot shot, Optional<String> videoString) {
		return super.addArenaShot(shot, videoString, true);
	}
	
	@Override
	public void clearShots() {
		mirroredManager.mirrorClearShots();
		super.clearShots();
	}
	
	public void mirrorClearShots() {
		super.clearShots();
	}

	@Override
	public void reset() {
		mirroredManager.mirrorReset();
		super.reset();
	}
	
	public void mirrorReset() {
		super.reset();
	}
}
