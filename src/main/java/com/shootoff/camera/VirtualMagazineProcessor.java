package com.shootoff.camera;

import com.shootoff.config.Configuration;

public class VirtualMagazineProcessor implements ShotProcessor {
	private final Configuration config;
	private int roundCount = 0;
	
	public VirtualMagazineProcessor(Configuration config) {
		this.config = config;
		roundCount = config.getVirtualMagazineCapacity();
	}
	
	@Override
	public boolean processShot(Shot shot) {
		if (roundCount == 0) {
			roundCount = config.getVirtualMagazineCapacity();
			return false;
		}
		
		roundCount--;
		return true;
	}

}
