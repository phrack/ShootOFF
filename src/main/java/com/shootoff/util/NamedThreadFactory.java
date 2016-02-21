package com.shootoff.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
	private final String namePrefix;
	private final AtomicInteger counter = new AtomicInteger();

	public NamedThreadFactory(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	@Override
	public Thread newThread(Runnable r) {
		final String threadName = String.format("%s-%d", namePrefix, counter.incrementAndGet());
		return new Thread(r, threadName);
	}
}
