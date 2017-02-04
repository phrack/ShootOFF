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

package com.shootoff.headless.protocol;

import java.util.List;
import java.util.Map;

public class CurrentCoursesMessage extends Message {
	private final Map<String, List<String>> courses;

	public CurrentCoursesMessage(Map<String, List<String>> courses) {
		this.courses = courses;
	}

	public Map<String, List<String>> getCourses() {
		return courses;
	}
}
