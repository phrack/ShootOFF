/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import java.util.List;

import javafx.scene.Group;

/** 
 * This class implements common training protocol operations. All
 * training protocols should extend it.
 * 
 * @author phrack
 */
public class TrainingProtocolBase {
	@SuppressWarnings("unused")
	private List<Group> targets;

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public TrainingProtocolBase() {}
	
	public TrainingProtocolBase(List<Group> targets) {
		this.targets = targets;
	}
}
