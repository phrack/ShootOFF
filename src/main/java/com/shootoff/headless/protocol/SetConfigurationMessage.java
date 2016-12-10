package com.shootoff.headless.protocol;

public class SetConfigurationMessage extends Message {
	private final ConfigurationData configurationData;

	public SetConfigurationMessage(ConfigurationData configurationData) {
		this.configurationData = configurationData;
	}

	public ConfigurationData getConfigurationData() {
		return configurationData;
	}
}
