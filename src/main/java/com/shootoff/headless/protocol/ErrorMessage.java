package com.shootoff.headless.protocol;

public class ErrorMessage extends Message {
	private final String message;
	private final ErrorType type;

	public ErrorMessage(String message, ErrorType type) {
		this.message = message;
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public ErrorType getType() {
		return type;
	}

	public enum ErrorType {
		CAMERA, EXERCSIE, TARGET
	}
}
