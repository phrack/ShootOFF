package com.shootoff.util;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerPool {
	private static final int CORE_POOL_SIZE = 20;
	private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(CORE_POOL_SIZE,
			new NamedThreadFactory("ShootOFFTimerPool"));

	public static ScheduledFuture<?> schedule(Runnable task, long msDelay) {
		return executorService.schedule(task, msDelay, TimeUnit.MILLISECONDS);
	}

	public static boolean isWaiting(ScheduledFuture<?> future) {
		return future != null && !future.isCancelled() && !future.isDone();
	}

	public static boolean cancelTimer(ScheduledFuture<?> future) {
		if (isWaiting(future)) return future.cancel(false);

		return false;
	}

	public static List<Runnable> close() {
		return executorService.shutdownNow();
	}
}
