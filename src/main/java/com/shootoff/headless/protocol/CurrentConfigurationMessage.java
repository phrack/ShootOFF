package com.shootoff.headless.protocol;

public class CurrentConfigurationMessage extends Message {
	private final ConfigurationData configurationData;

	public CurrentConfigurationMessage(ConfigurationData configurationData) {
		this.configurationData = configurationData;
	}

	public ConfigurationData getConfigurationData() {
		return configurationData;
	}
}
