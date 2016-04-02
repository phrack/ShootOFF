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

package com.shootoff.plugins;

/**
 * Data about what an exercise is and who wrote it.
 * 
 * @author phrack
 */
public class ExerciseMetadata {
	private final String name;
	private final String version;
	private final String creator;
	private final String description;

	public ExerciseMetadata(final String name, final String version, final String creator, final String description) {
		this.name = name;
		this.version = version;
		this.creator = creator;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getCreator() {
		return creator;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ExerciseMetadata other = (ExerciseMetadata) obj;
		if (creator == null) {
			if (other.creator != null) return false;
		} else if (!creator.equals(other.creator)) return false;
		if (description == null) {
			if (other.description != null) return false;
		} else if (!description.equals(other.description)) return false;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		if (version == null) {
			if (other.version != null) return false;
		} else if (!version.equals(other.version)) return false;
		return true;
	}
}
