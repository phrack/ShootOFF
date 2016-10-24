package com.shootoff.gui;

public enum CalibrationOption {
	EVERYWHERE("Detect Shots Everywhere"), ONLY_IN_BOUNDS("Only detect shots in projector bounds"), CROP(
			"Crop feed to projector bounds");

	private final String text;

	private CalibrationOption(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	public static CalibrationOption fromString(String text) {
		if (text != null) {
			for (final CalibrationOption o : CalibrationOption.values()) {
				if (o.text.equalsIgnoreCase(text)) {
					return o;
				}
			}
		}

		return null;
	}
}
