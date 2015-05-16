/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

public class ProtocolMetadata {
	private final String name;
	private final String version;
	private final String creator;
	private final String description;

	public ProtocolMetadata(String name, String version, String creator,
			String description) {
		this.name = name;
		this.version = version;
		this.creator = creator;
		this.description = description;
	}
	
	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getCreator() {
		return creator;
	}

	public String getDescription() {
		return description;
	}
}
