package com.shootoff.headless.protocol;

public class ConfigurationData {
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
