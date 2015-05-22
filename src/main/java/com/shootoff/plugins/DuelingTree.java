/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.Group;
import javafx.scene.Node;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;

public class DuelingTree extends ProjectorTrainingProtocolBase implements TrainingProtocol {
	private final static String HIT_COL_NAME = "Hit By";
	private final static int HIT_COL_WIDTH = 60;

	private static final int NEW_ROUND_DELAY = 5; // s
	private static final int CORE_POOL_SIZE = 2;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
	private TrainingProtocolBase thisSuper;
	
    private boolean continueProtocol = true;
    private boolean isResetting = false;
    private int leftScore = 0;
    private int rightScore = 0;
    private List<TargetRegion> paddlesOnLeft = new ArrayList<TargetRegion>();
    private List<TargetRegion> paddlesOnRight = new ArrayList<TargetRegion>();
	
	public DuelingTree() {}
	
	public DuelingTree(List<Group> targets) {
		super(targets);
		this.thisSuper = super.getInstance();
		findTargets(targets);
	}
    
	@Override
	public void init() {
        // We need to make sure we start with a clean slate because the position
        // of the plates matter
		super.reset();
		
		super.addShotTimerColumn(HIT_COL_NAME, HIT_COL_WIDTH);
		super.showTextOnFeed("left score: 0\nright score: 0");
	}

	private boolean findTargets(List<Group> targets) {
		boolean foundTarget = false;
		
		// Find the first target with directional subtargets and gets its regions
		for (Group target : targets) {
			if (foundTarget) break;
			
			for (Node node : target.getChildren()) {
				TargetRegion region = (TargetRegion)node;
				
				if (region.tagExists("subtarget")) {
					if (region.getTag("subtarget").startsWith("left_paddle")) {
						paddlesOnLeft.add(region);
						foundTarget = true;
					} else if (region.getTag("subtarget").startsWith("right_paddle")) {
						paddlesOnRight.add(region);
						foundTarget = true;
					}
				}
			}
		}
		
		if (!foundTarget) {
			TextToSpeech.say("This training protocol requires a dueling tree target");	
			continueProtocol = false;
		}
		
		return foundTarget;
	}
	
	@Override
	public ProtocolMetadata getInfo() {
		return new ProtocolMetadata("Dueling Tree", "1.0", "phrack",
	    	    	"This protocol works with the dueling tree target. Challenge "
	    	    	+ "a friend, assign a side (left or right) to each participant, "
	    	    	+ "and try to shoot the plates from your side to your friend's "
	    	    	+ "side. A round ends when all plates are on one person's side.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (!continueProtocol) return;
		
		if (hitRegion.isPresent()) {
			TargetRegion r = hitRegion.get();
			
			if (r.tagExists("subtarget") && 
					(r.getTag("subtarget").startsWith("left_paddle") || 
							r.getTag("subtarget").startsWith("right_paddle"))) {
				
				String hitBy = "";
				
				if (paddlesOnLeft.contains(r)) {
					paddlesOnLeft.remove(r);
                    paddlesOnRight.add(r);
                    hitBy = "left";
				} else if (paddlesOnRight.contains(r)) {
					paddlesOnLeft.add(r);                                 
					paddlesOnRight.remove(r);
                    hitBy = "right";
				}
				
				super.setShotTimerColumnText(HIT_COL_NAME, hitBy);
				
				if (paddlesOnLeft.size() == 6) {
					rightScore++;
					roundOver();
				}
				
				if (paddlesOnRight.size() == 6) {
					leftScore++;
					roundOver();
				}
			}
		}
	}
	
	private void roundOver() {
		if (continueProtocol) {
			thisSuper.showTextOnFeed(String.format("left score: %d\nright score: %d", leftScore, rightScore));
			super.pauseShotDetection(true);
			executorService.schedule(new NewRound(), NEW_ROUND_DELAY, TimeUnit.SECONDS);
		}
	}
	
	
	private class NewRound implements Callable<Void> {
		@Override
		public Void call() {
			isResetting = true;
			thisSuper.reset();
			isResetting = false;
			thisSuper.pauseShotDetection(false);
			
			return null;
		}
	}
		
	@Override
	public void reset(List<Group> targets) {
		if (!isResetting) {
			leftScore = 0;
			rightScore = 0;
			super.showTextOnFeed("left score: 0\nright score: 0");
		}
		
		paddlesOnLeft.clear();
		paddlesOnRight.clear();
		
		findTargets(targets);
	}

	@Override
	public void destroy() {
		continueProtocol = false;
		executorService.shutdownNow();
	}
}
