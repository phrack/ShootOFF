/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.Group;

import com.shootoff.camera.Shot;
import com.shootoff.gui.Target;
import com.shootoff.targets.TargetRegion;

public class ShootDontShoot extends ProjectorTrainingProtocolBase implements TrainingProtocol {
	private final static String TARGET_COL_NAME = "TARGET";
	private final static int TARGET_COL_WIDTH = 60;
	
	private final static int MIN_TARGETS_PER_ROUND = 0;
	private final static int MAX_TARGETS_PER_ROUND = 4;
	private final static int ROUND_DURATION = 10; // s
	
	private static final int CORE_POOL_SIZE = 2;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
	
	private boolean continueProtocol = true;
	private ProjectorTrainingProtocolBase thisSuper;
	private int missedTargets = 0;
	private int badHits = 0;
	private final List<Target> shootTargets = new ArrayList<Target>();
	private final List<Target> dontShootTargets = new ArrayList<Target>();
	
	public ShootDontShoot() {}
	
	public ShootDontShoot(List<Group> targets) {
		super(targets);
		this.thisSuper = super.getInstance();
	}
	
	@Override
	public void init() {
		super.addShotTimerColumn(TARGET_COL_NAME, TARGET_COL_WIDTH);
		
        addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target");
        addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target");
        super.showTextOnFeed("missed targets: 0\nbad hits: 0");      

        executorService.schedule(new NewRound(), ROUND_DURATION, TimeUnit.SECONDS);
	}
	
	private class NewRound implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			if (continueProtocol) {
				missedTargets += shootTargets.size();
				
				if (shootTargets.size() > 0) {
					TextToSpeech.say(String.format("You missed %d targets.", shootTargets.size()));
				}
				
				thisSuper.showTextOnFeed(String.format("missed targets: %d%nbad hits: %d", 
						missedTargets, badHits)); 
				
				for (Target target : shootTargets) thisSuper.removeTarget(target);
				shootTargets.clear();
				for (Target target : dontShootTargets) thisSuper.removeTarget(target);
				dontShootTargets.clear();
				
		        addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target");
		        addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target");
		        
		        thisSuper.clearShots();
				
				if (continueProtocol) executorService.schedule(new NewRound(), ROUND_DURATION, TimeUnit.SECONDS);
			}
			
			return null;
		}
	}

	@Override
	public ProtocolMetadata getInfo() {
	    return new ProtocolMetadata("Shoot Don't Shoot", "1.0", "phrack",
	 		    	"This protocol randomly puts up targets and gives you 10 seconds "
	 		    	+ "to decide which ones to shoot and which ones to ignore. If "
	 		    	+ "you do not shoot a target you are supposed to shoot, it gets "
	 		    	+ "added to your missed targets counter and the protocol says "
	 		    	+ "how many targets you missed. If you hit a target you were not "
	 		    	+ "supposed to hit, the protocol says 'bad shoot!'. Shoot the targets "
	 		    	+ "with the red ring, don't shoot the other targets.");
	}

	private void addTargets(List<Target> targets, String target) {
		int count = new Random().nextInt((MAX_TARGETS_PER_ROUND - MIN_TARGETS_PER_ROUND) + 1) 
				+ MIN_TARGETS_PER_ROUND;

		for (int i = 0; i < count; i++) {
			int x = new Random().nextInt(((int)super.getArenaWidth() - 100) + 1) + 0;
			int y = new Random().nextInt(((int)super.getArenaHeight() - 100) + 1) + 0;

			Optional<Target> newTarget = super.addTarget(new File(target), x, y);
			if (newTarget.isPresent()) targets.add(newTarget.get());
		}
	}
	
	private void removeTarget(List<Target> targets, TargetRegion region) {
		Iterator<Target> it = targets.iterator();
		
		while (it.hasNext()) {
			Target target = it.next();
			
			if (target.getTargetGroup().getChildren().contains(region)) {
				super.removeTarget(target);
				it.remove();
				return;
			}
		}
	}
	
	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (hitRegion.isPresent()) {
			if (hitRegion.get().tagExists("subtarget")) {
				switch (hitRegion.get().getTag("subtarget")) {
				case "shoot":
					{
						removeTarget(shootTargets, hitRegion.get());
						super.setShotTimerColumnText(TARGET_COL_NAME, "shoot");
					}
					break;
					
				case "dont_shoot":
					{
						removeTarget(dontShootTargets, hitRegion.get());
						badHits++;
						super.setShotTimerColumnText(TARGET_COL_NAME, "dont_shoot");
						TextToSpeech.say("Bad shoot!");
					}
					break;
				}
			}
		}
	}

	@Override
	public void reset(List<Group> targets) {
		continueProtocol = false;
		executorService.shutdownNow();
		
        missedTargets = 0;
        badHits = 0;
        
		for (Target target : shootTargets) super.removeTarget(target);
		shootTargets.clear();
		for (Target target : dontShootTargets) super.removeTarget(target);
		dontShootTargets.clear();
		
        addTargets(shootTargets, "targets/shoot_dont_shoot/shoot.target");
        addTargets(dontShootTargets, "targets/shoot_dont_shoot/dont_shoot.target");
        
        super.showTextOnFeed("missed targets: 0\nbad hits: 0");
        
		executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
		executorService.schedule(new NewRound(), ROUND_DURATION, TimeUnit.SECONDS);
	}
	
	@Override
	public void destroy() {
		super.destroy();
		executorService.shutdownNow();
		continueProtocol = false;
	}
}
