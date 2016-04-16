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
