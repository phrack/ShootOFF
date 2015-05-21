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
	private List<Group> shootTargets = new ArrayList<Group>();
	private List<Group> dontShootTargets = new ArrayList<Group>();
	
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
				
				thisSuper.showTextOnFeed(String.format("missed targets: %d\nbad hits: %d", 
						missedTargets, badHits)); 
				
				for (Group target : shootTargets) thisSuper.removeTarget(target);
				shootTargets.clear();
				for (Group target : dontShootTargets) thisSuper.removeTarget(target);
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

	private void addTargets(List<Group> targets, String target) {
		int count = new Random().nextInt((MAX_TARGETS_PER_ROUND - MIN_TARGETS_PER_ROUND) + 1) 
				+ MIN_TARGETS_PER_ROUND;

		for (int i = 0; i < count; i++) {
			int x = new Random().nextInt(((int)super.getArenaWidth() - 100) + 1) + 0;
			int y = new Random().nextInt(((int)super.getArenaHeight() - 100) + 1) + 0;

			Optional<Group> newTarget = super.addTarget(new File(target), x, y);
			if (newTarget.isPresent()) targets.add(newTarget.get());
		}
	}
	
	private Optional<Group> removeTarget(List<Group> targets, TargetRegion region) {
		Iterator<Group> it = targets.iterator();
		
		while (it.hasNext()) {
			Group target = it.next();
			
			if (target.getChildren().contains(region)) {
				super.removeTarget(target);
				it.remove();
				return Optional.of(target);
			}
		}
		
		return Optional.empty();
	}
	
	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (hitRegion.isPresent()) {
			if (hitRegion.get().tagExists("subtarget")) {
				switch (hitRegion.get().getTag("subtarget")) {
				case "shoot":
					{
						Optional<Group> target = removeTarget(shootTargets, hitRegion.get());
						if (target.isPresent()) shootTargets.remove(target);
						super.setShotTimerColumnText(TARGET_COL_NAME, "shoot");
					}
					break;
					
				case "dont_shoot":
					{
						Optional<Group> target = removeTarget(dontShootTargets, hitRegion.get());
						if (target.isPresent()) dontShootTargets.remove(target);
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
        
		for (Group target : shootTargets) super.removeTarget(target);
		shootTargets.clear();
		for (Group target : dontShootTargets) super.removeTarget(target);
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
