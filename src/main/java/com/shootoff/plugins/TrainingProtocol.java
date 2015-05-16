/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import java.util.List;
import java.util.Optional;

import javafx.scene.Group;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;

public interface TrainingProtocol {
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
	 * Called when a training protocol is being unloaded by the framework
	 */
    public void destroy();
}
