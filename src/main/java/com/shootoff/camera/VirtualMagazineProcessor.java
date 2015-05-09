package com.shootoff.camera;

import com.shootoff.config.Configuration;
import com.shootoff.plugins.TextToSpeech;

public class VirtualMagazineProcessor implements ShotProcessor {
	private final Configuration config;
	private boolean useTTS = true;
	private int roundCount = 0;
	
	public VirtualMagazineProcessor(Configuration config) {
		this.config = config;
		roundCount = config.getVirtualMagazineCapacity();
	}
	
	public void setUseTTS(boolean useTTS) {
		this.useTTS = useTTS; 
	}
	
	@Override
	public boolean processShot(Shot shot) {
		if (roundCount == 0) {
			roundCount = config.getVirtualMagazineCapacity();
			if (useTTS) TextToSpeech.say("reload!");
			return false;
		}
		
		roundCount--;
		return true;
	}

}
