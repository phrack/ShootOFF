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

import java.util.List;
import java.util.Optional;

import javafx.scene.Group;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;

public interface TrainingProtocol {
	/** 
	 * Any protocol specific initialization that needs to use protocol API methods
	 * should call them in this method instead of the constructor, otherwise the
	 * API may not be initialized yet.
	 */
	public void init();
	
	/**
	 * Called when a training protocol is first loaded to retrive information
	 * about the plugin that is displayable to the user.
	 * 
	 * @return a <tt>ProtocolMetadata</tt> object initialized with the data for
	 *		   the loaded training protocol
	 */
	public ProtocolMetadata getInfo();
	
	/**
	 * Called whenever a shot is detected. If the shot hit a target,
	 * hitRegion will be set.
	 * 
	 * @param shot		the detect shot
	 * @param hitRegion	empty if no target was hit, otherwise set to the
	 * 					specific region that was hit
	 */
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion);
	

	/**
	 * Called when the reset button is hit or a reset target is shot. The
	 * training protocol should reset to its initial state here.
	 * 
	 * @param targets	a list of all of the targets currently added to 
	 * 					webcam feeds
	 */
	public void reset(List<Group> targets);
	
	/**
	 * Called when a training protocol is being unloaded by the framework.
	 * This method must call super.destroy() otherwise an exception will occur
	 * when protocols are switched.
	 */
    public void destroy();
}
