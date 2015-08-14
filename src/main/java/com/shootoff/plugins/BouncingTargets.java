package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.util.Duration;

import com.shootoff.camera.Shot;
import com.shootoff.gui.Target;
import com.shootoff.targets.TargetRegion;

public class BouncingTargets extends ProjectorTrainingProtocolBase implements TrainingProtocol {
	private final int SHOOT_COUNT = 4;
	private final int DONT_SHOOT_COUNT = 1;
	private static final int MAX_VELOCITY = 10;

	private static final List<BouncingTarget> shootTargets = new ArrayList<BouncingTarget>();
	private static final List<BouncingTarget> dontShootTargets = new ArrayList<BouncingTarget>();
	
	private static ProjectorTrainingProtocolBase thisSuper;
	private Timeline targetAnimation;
	private int score = 0;
	
	public BouncingTargets() {}
	
	public BouncingTargets(List<Group> targets) {
		super(targets);
		thisSuper = super.getInstance();
	}
	
	@Override
	public void init() {
		// TODO: ask use to configure # of shoot and don't shoot targets and velocity
		super.showTextOnFeed("Score: 0");
		
        addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target", SHOOT_COUNT);
        addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target", DONT_SHOOT_COUNT);

        targetAnimation = new Timeline(new KeyFrame(Duration.millis(20), e -> updateTargets()));
        targetAnimation.setCycleCount(Timeline.INDEFINITE);
        targetAnimation.play();
	}
	
	private void updateTargets() {
		for (BouncingTarget b : shootTargets) b.moveTarget();
		for (BouncingTarget b : dontShootTargets) b.moveTarget();
	}
	
	private static class BouncingTarget {
		private final Target target;
		private double dx;
		private double dy;
		
		public BouncingTarget(Target target) {
			this.target = target;
			
			Random r = new Random();
			
			dx = r.nextInt(MAX_VELOCITY + 1);
			dy = r.nextInt(MAX_VELOCITY + 1);
			
			if (r.nextBoolean()) dx *= -1;
			if (r.nextBoolean()) dy *= -1;	
		}
		
		public Target getTarget() {
			return target;
		}
		
		private enum CollisionType {
			NONE, COLLISION_X, COLLISION_Y, COLLISION_BOTH;
		}
		
		private CollisionType checkCollision() {
			final Bounds targetBounds = target.getTargetGroup().getBoundsInParent();
			List<BouncingTarget> collisionList;
			
			if (shootTargets.contains(this)) {
				collisionList = shootTargets;
			} else {
				collisionList = dontShootTargets;
			}
			
			for (BouncingTarget b : collisionList) {
				if (b.getTarget().equals(target)) continue;
				
				final Bounds bBounds = b.getTarget().getTargetGroup().getBoundsInParent();
				
				if (targetBounds.intersects(bBounds)) {
					final boolean atRight = targetBounds.getMaxX() > bBounds.getMinX() &&
							targetBounds.getMaxX() - bBounds.getMinX() < MAX_VELOCITY * 2;
					final boolean atLeft = bBounds.getMaxX() > bBounds.getMinX() &&
							bBounds.getMaxX() - bBounds.getMinX() < MAX_VELOCITY * 2;
					final boolean atBottom = targetBounds.getMaxY() > bBounds.getMinY() &&
							targetBounds.getMaxY() - bBounds.getMinY() < MAX_VELOCITY * 2;
					final boolean atTop = bBounds.getMaxY() > targetBounds.getMinY() &&
							bBounds.getMaxY() - targetBounds.getMinY() < MAX_VELOCITY * 2;
		      
					if ((atRight || atLeft) && (atBottom || atTop)) {
						return CollisionType.COLLISION_BOTH;
					} else if (atRight || atLeft) {
						return CollisionType.COLLISION_X;
					} else if (atBottom || atTop) {
						return CollisionType.COLLISION_Y;
					}
				}
			}
			
			return CollisionType.NONE;
		}
		
	    public void moveTarget() {
	        Point2D p = target.getPosition();
	        Dimension2D d = target.getDimension();
	        CollisionType ct = checkCollision();
	        
	        if (p.getX() <= 1 || p.getX() + d.getWidth() > thisSuper.getArenaWidth() || 
	        		ct == CollisionType.COLLISION_X || ct == CollisionType.COLLISION_BOTH) {
	        	dx *= -1;
	        }
	        
	        if (p.getY() <= 1 || p.getY() + d.getHeight() > thisSuper.getArenaHeight() || 
	        		ct == CollisionType.COLLISION_X || ct == CollisionType.COLLISION_BOTH) {
	        	dy *= -1;
	        }
	    	
	        target.setPosition(p.getX() + dx, p.getY() + dy);
	    }
	}
	
	private void addTargets(List<BouncingTarget> targets, String target, int count) {
		for (int i = 0; i < count; i++) {
			Optional<Target> newTarget = super.addTarget(new File(target), 0, 0);
			
			if (newTarget.isPresent()) {
				// Randomly place the target
				int maxX = (int)(super.getArenaWidth() - newTarget.get().getDimension().getWidth());
				int x = new Random().nextInt(maxX + 1) + 1;
				
				int maxY = (int)(super.getArenaHeight()  - newTarget.get().getDimension().getHeight());
				int y = new Random().nextInt(maxY + 1) + 1;
			
				newTarget.get().setPosition(x, y);
				
				targets.add(new BouncingTarget(newTarget.get()));
			}
		}
	}

	@Override
	public ProtocolMetadata getInfo() {
	    return new ProtocolMetadata("Bouncing Targets", "1.0", "phrack",
 		    	"This protocol randomly moves shoot and don't shoot targets around "
 		    	+ " the arena. All targets bounce off the arena bounds and targets of "
 		    	+ " the same type bounce off of each other. Don't shoot targets are "
 		    	+ " always able to overlap shoot targets. Your score is how many shoot "
 		    	+ " targets you have hit since shooting your last don't shoot target.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (hitRegion.isPresent()) {
			if (hitRegion.get().tagExists("subtarget")) {
				switch (hitRegion.get().getTag("subtarget")) {
				case "shoot":
					{
						score++;
						super.showTextOnFeed(String.format("Score: %d", score));
					}
					break;
					
				case "dont_shoot":
					{
						super.playSound("sounds/beep.wav");
						score = 0;
						super.showTextOnFeed("Score: 0");
					}
					break;
				} 
			}
		}
	}

	@Override
	public void reset(List<Group> targets) {
		// TODO: re-prompt for config
		// TODO: remove existing targets and add new ones
		// TODO: stop old animation and make new one
		score = 0;
		super.showTextOnFeed("Score: 0");
	}
}
