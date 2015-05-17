/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;

import javafx.scene.Group;
import javafx.scene.Node;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;

public class RandomShoot extends TrainingProtocolBase implements TrainingProtocol {
	private final List<String> subtargets = new ArrayList<String>();
	private final Stack<Integer> currentSubtargets = new Stack<Integer>();
	
	public RandomShoot() {}
	
	public RandomShoot(List<Group> targets) {
		super(targets);
		if (fetchSubtargets(targets)) startRound();
	}

	private void startRound() {
		pickSubtargets();
		saySubtargets();
	}
	
	/**
	 * Returns the list of all known subtargets. This method exists to make this
	 * protocol easier to test.
	 * 
	 * @return	a list of all known subtargets
	 */
	protected List<String> getSubtargets() {
		return subtargets;
	}
	
	/**
	 * Returns the current subtarget stack. This method exists to make this
	 * protocol easier to test.
	 * 
	 * @return	current subtarget stack
	 */
	protected Stack<Integer> getCurrentSubtargets() {
		return currentSubtargets;
	}
	
    /**
     * Finds the first target with subtargets and gets its regions. If
     * there is no target with substargets, this method uses TTS
     * to tell the user
     * 
     * @param targets	a list of all targets known to this protocolo
     * @return	<tt>true</tt> if we found subtargets, <tt>false</tt> otherwise
     */
	private boolean fetchSubtargets(List<Group> targets) {
    	subtargets.clear();
    	
    	boolean foundTarget = false;
		for (Group target : targets) {
			for (Node node : target.getChildren()) {
				TargetRegion region = (TargetRegion)node;
				
				if (region.getAllTags().containsKey("subtarget")) {
					subtargets.add(region.getTag("subtarget"));
					foundTarget = true;
				}
			}
			
			if (foundTarget) break;
		}
		
		if (foundTarget && subtargets.size() > 0) {
			return true;
		} else {
			TextToSpeech.say("This training protocol requires a target with subtargets");
	        return false;		
		}	
	}
	
	private void pickSubtargets() {
		currentSubtargets.clear();
		
        int count = new Random().nextInt((subtargets.size() - 1) + 1) + 1;
        for (int i : new Random().ints(count, 0, subtargets.size()).toArray()) {
        	currentSubtargets.push(new Integer(i));
        }
	}
        
	private void saySubtargets() {
        StringBuilder sentence = new StringBuilder("shoot subtarget ");
 
        sentence.append(subtargets.get(currentSubtargets.get(currentSubtargets.size() - 1)));
        
        for (int i = currentSubtargets.size() - 2; i >= 0; i--) {
        	sentence.append(" then ");
        	sentence.append(subtargets.get(currentSubtargets.get(i)));
        }
        
        TextToSpeech.say(sentence.toString());
    }
	
	private void sayCurrentSubtarget() {
		String sentence = "shoot " + subtargets.get(currentSubtargets.peek());
		TextToSpeech.say(sentence);
	}

	@Override
	public ProtocolMetadata getInfo() {
		return new ProtocolMetadata("Random Shoot", "1.0", "phrack",
		    	   "This protocol works with targets that have subtarget tags "
		    	    	   + "assigned to some regions. Subtargets are selected at random "
		    	    	   + "and the shooter is asked to shoot those subtargets in order. "
		    	    	   + "If a subtarget is shot out of order or the shooter misses, the "
		    	    	   + "name of the subtarget that should have been shot is repeated.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (hitRegion.isPresent()) {
			String subtargetValue = hitRegion.get().getTag("subtarget");
			if (subtargetValue != null && subtargetValue.equals(subtargets.get(currentSubtargets.peek()))) {
				currentSubtargets.pop();
			} else {
				sayCurrentSubtarget();
			}
			
			if (currentSubtargets.isEmpty()) startRound();
		} else {
			sayCurrentSubtarget();
		}		
	}

	@Override
	public void reset(List<Group> targets) {
		if (fetchSubtargets(targets)) startRound();
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}