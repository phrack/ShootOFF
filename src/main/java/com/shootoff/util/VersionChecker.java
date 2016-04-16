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
