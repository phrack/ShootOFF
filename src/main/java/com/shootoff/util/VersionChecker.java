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

public class VersionChecker {
	// -1 if version1 is older than version2
	// 0 if version1 is the same version2
	// 1 if version1 is newer than version2
	public static int compareVersions(String version1, String version2) {
		if (version1.equals(version2)) return 0;
		
		String[] version1Components = version1.split("\\.");
		String[] version2Components = version2.split("\\.");
		
		for (int i = 0; i < Math.max(version1Components.length, version2Components.length); i++) {
			int comp1;
			
			if (i >= version1Components.length) {
				// Assume a 0 where the first version is shorter than the
				// second
				comp1 = 0;
			} else {
				comp1 = Integer.parseInt(version1Components[i]);
			}
			
			int comp2;
			
			if (i >= version2Components.length) {
				// Assume a 0 where the second version is shorter than the
				// first
				comp2 = 0;
			} else {
				comp2 = Integer.parseInt(version2Components[i]);
			}
			
			if (comp2 > comp1) return -1;
			if (comp2 < comp1) return 1;
		}
		
		return 0;
	}
}
