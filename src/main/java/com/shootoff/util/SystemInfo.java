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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemInfo {
	private static final Logger logger = LoggerFactory.getLogger(SystemInfo.class);
	private static final boolean isLinux;
	private static final boolean isMacOsX;
	private static final boolean isWindows;
	private static final boolean isX86;
	private static final boolean isArm;

	static {
		final String os = System.getProperty("os.name");

		if (os != null) {
			if ("Mac OS X".equals(os)) {
				isMacOsX = true;
				isLinux = false;
				isWindows = false;
			} else if (os.startsWith("Linux")) {
				isLinux = true;
				isMacOsX = false;
				isWindows = false;
			} else if (os.startsWith("Windows")) {
				isWindows = true;
				isLinux = false;
				isMacOsX = false;
			} else {
				logger.warn("Untested operating system {}", os);

				isLinux = false;
				isMacOsX = false;
				isWindows = false;
			}
		} else {
			logger.warn("os.name property does not exist");

			isLinux = false;
			isMacOsX = false;
			isWindows = false;
		}

		final String arch = System.getProperty("os.arch");

		final List<String> x86_32 = Arrays.asList("i386", "i686", "x86");
		final List<String> x86_64 = Arrays.asList("amd64", "x86_64");
		final List<String> arm = Arrays.asList("arm");

		if (arch != null) {
			if (x86_32.contains(arch) || x86_64.contains(arch)) {
				isX86 = true;
				isArm = false;
			} else if (arm.contains(arch)) {
				isArm = true;
				isX86 = false;
			} else {
				logger.warn("Untested architecture {}", arch);

				isX86 = false;
				isArm = false;
			}
		} else {
			logger.warn("os.arch property does not exist");

			isX86 = false;
			isArm = false;
		}
	}

	public static boolean isLinux() {
		return isLinux;
	}

	public static boolean isMacOsX() {
		return isMacOsX;
	}

	public static boolean isWindows() {
		return isWindows;
	}

	public static boolean isX86() {
		return isX86;
	}

	public static boolean isArm() {
		return isArm;
	}
}
