package com.shootoff.gui;

import java.util.List;

public interface CommandProcessor {
	public void process(List<String> commands, String commandName,
			List<String> args);
}
