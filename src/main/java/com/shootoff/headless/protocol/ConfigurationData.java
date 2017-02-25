/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
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

package com.shootoff.headless.protocol;

import java.io.Serializable;

public class ConfigurationData implements Serializable {
	private static final long serialVersionUID = 1L;
	// Laser settings
	private final int markerRadius;
	private final boolean ignoreLaserColor;
	private final String ignoreLaserColorName;
	private final boolean useVirtualMagazine;
	private final int virtualMagazineCapacity;
	private final boolean useMalfunctions;
	private final float malfunctionsProbability;
	// Projector settings
	private final boolean showArenaShotMarkers;

	public ConfigurationData(int markerRadius, boolean ignoreLaserColor, String ignoreLaserColorName,
			boolean useVirtualMagazine, int virtualMagazineCapacity, boolean useMalfunctions,
			float malfunctionsProbability, boolean showArenaShotMarkers) {
		this.markerRadius = markerRadius;
		this.ignoreLaserColor = ignoreLaserColor;
		this.ignoreLaserColorName = ignoreLaserColorName;
		this.useVirtualMagazine = useVirtualMagazine;
		this.virtualMagazineCapacity = virtualMagazineCapacity;
		this.useMalfunctions = useMalfunctions;
		this.malfunctionsProbability = malfunctionsProbability;
		this.showArenaShotMarkers = showArenaShotMarkers;
	}

	public int getMarkerRadius() {
		return markerRadius;
	}

	public boolean isIgnoreLaserColor() {
		return ignoreLaserColor;
	}

	public String getIgnoreLaserColorName() {
		return ignoreLaserColorName;
	}

	public boolean useVirtualMagazine() {
		return useVirtualMagazine;
	}

	public int getVirtualMagazineCapacity() {
		return virtualMagazineCapacity;
	}

	public boolean useMalfunctions() {
		return useMalfunctions;
	}

	public float getMalfunctionsProbability() {
		return malfunctionsProbability;
	}

	public boolean showArenaShotMarkers() {
		return showArenaShotMarkers;
	}
}
