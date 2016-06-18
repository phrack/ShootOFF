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

package com.shootoff.camera.perspective;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;

/*
 * 
 * distance to object (mm) =
 * 
 *  focal length (mm) * real height of the object (mm) * image height (pixels)
 * ---------------------------------------------------------------------------
 *                  object height (pixels) * sensor height (mm)
 * 
 */

public class PerspectiveManager {
	private static final Logger logger = LoggerFactory.getLogger(PerspectiveManager.class);

	private final static int US_LETTER_WIDTH_MM = 279;
	private final static int US_LETTER_HEIGHT_MM = 216;

	// Key = camera name
	private static final Map<String, CameraParameters> cameraParameters = new HashMap<>();

	// All in millimeters
	private double focalLength = -1;
	private double sensorHeight = -1;
	private double sensorWidth = -1;
	private int cameraDistance = -1;
	private int shooterDistance = -1;

	private double pxPerMMhigh = -1;
	private double pxPerMMwide = -1;

	// All in pixels
	private int projectionHeight = -1;
	private int projectionWidth = -1;
	private int cameraHeight = -1;
	private int cameraWidth = -1;
	private int patternHeight = -1;
	private int patternWidth = -1;

	private int projectorResHeight = -1;
	private int projectorResWidth = -1;

	private static class CameraParameters {
		private final double focalLength;
		private final double sensorWidth;
		private final double sensorHeight;

		public CameraParameters(double focalLength, double sensorWidth, double sensorHeight) {
			this.focalLength = focalLength;
			this.sensorWidth = sensorWidth;
			this.sensorHeight = sensorHeight;
		}

		public double getFocalLength() {
			return focalLength;
		}

		public double getSensorWidth() {
			return sensorWidth;
		}

		public double getSensorHeight() {
			return sensorHeight;
		}
	}

	// TODO: Implement a way to load these values from a file
	// so that they can be easily tweaked/added to
	static {
		// !!!! THESE NUMBERS ARE ONLY GOOD FOR 1280x720 !!!!
		cameraParameters.put("C270", new CameraParameters(4.0, 3.58, 2.02));
		cameraParameters.put("C920", new CameraParameters(3.67, 4.80, 3.60));
	}

	// For testing
	protected PerspectiveManager(Bounds arenaBounds) {
		if (logger.isTraceEnabled())
			logger.trace("pattern res w {} h {}", arenaBounds.getWidth(), arenaBounds.getHeight());
		this.patternWidth = (int) arenaBounds.getWidth();
		this.patternHeight = (int) arenaBounds.getHeight();
	}

	// For testing
	protected PerspectiveManager(Bounds arenaBounds,  Dimension2D feedDims, Dimension2D paperBounds) {
		this(arenaBounds);
		setCameraFeedSize((int) feedDims.getWidth(), (int) feedDims.getHeight());
		setProjectionSizeFromLetterPaperPixels(paperBounds);
	}
	
	public PerspectiveManager(String cameraName, Bounds arenaBounds) {
		this(arenaBounds);
		
		if (!setCameraParameters(cameraName)) {
			throw new UnsupportedCameraException(cameraName + " does not support target perspectives because"
					+ " its focal length and sensor parameters are unknown.");
		}
	}
	
	public PerspectiveManager(String cameraName, Bounds arenaBounds, Dimension2D feedDims, Dimension2D paperBounds) {
		this(cameraName, arenaBounds);
		setCameraFeedSize((int) feedDims.getWidth(), (int) feedDims.getHeight());
		setProjectionSizeFromLetterPaperPixels(paperBounds);
	}

	public static boolean isCameraSupported(final String cameraName) {
		for (String supportedName : cameraParameters.keySet()) {
			if (cameraName.contains(supportedName)) return true;
		}

		return false;
	}

	private boolean setCameraParameters(final String cameraName) {
		for (Map.Entry<String, CameraParameters> entry : cameraParameters.entrySet()) {
			if (cameraName.contains(entry.getKey())) {
				CameraParameters cp = entry.getValue();
				focalLength = cp.getFocalLength();
				sensorWidth = cp.getSensorWidth();
				sensorHeight = cp.getSensorHeight();

				return true;
			}
		}

		return false;
	}

	// Used for testing
	protected void setCameraParameters(double focalLength, double sensorWidth, double sensorHeight) {
		if (logger.isTraceEnabled())
			logger.trace("camera params fl {} sw {} sh {}", focalLength, sensorWidth, sensorHeight);

		this.focalLength = focalLength;
		this.sensorHeight = sensorHeight;
		this.sensorWidth = sensorWidth;
	}

	/*
	 * The real world width and height of the projector arena in the camera feed
	 * (in mm)
	 */
	public void setProjectionSize(int width, int height) {
		if (logger.isTraceEnabled()) logger.trace("projection w {} h {}", width, height);

		this.projectionHeight = height;
		this.projectionWidth = width;
	}

	/*
	 * Specify (or find with OpenCV) the number of camera pixels that are
	 * represent a U.S. standard letter, which is 8.5 x 11 inches or 216 x 279mm
	 * We assume that the paper is placed sideways! We could probably adjust for
	 * this though
	 */
	private void setProjectionSizeFromLetterPaperPixels(Dimension2D letterDims) {
		if (logger.isTraceEnabled()) logger.trace("letter w {} h {}", letterDims.getWidth(), letterDims.getHeight());

		if (cameraWidth == -1 || patternWidth == -1) {
			logger.error("Missing cameraWidth or patternWidth for US Letter calculation");
			return;
		}

		// Calculate the size of the whole camera feed using the size of the
		// letter
		final double cameraFeedWidthMM = ((double) cameraWidth / letterDims.getWidth()) * US_LETTER_WIDTH_MM;
		final double cameraFeedHeightMM = ((double) cameraHeight / letterDims.getHeight()) * US_LETTER_HEIGHT_MM;

		// Set the projection width/height in mm
		projectionWidth = (int) (cameraFeedWidthMM * ((double) patternWidth / (double) cameraWidth));
		projectionHeight = (int) (cameraFeedHeightMM * ((double) patternHeight / (double) cameraHeight));
	}

	/* The camera feed width and height (in px) */
	public void setCameraFeedSize(int width, int height) {
		if (logger.isTraceEnabled()) logger.trace("camera feed w {} h {}", width, height);
		this.cameraHeight = height;
		this.cameraWidth = width;
	}

	/* Distance (in mm) camera to screen */
	public void setCameraDistance(int cameraDistance) {
		if (logger.isTraceEnabled()) logger.trace("cameraDistance {}", cameraDistance);
		this.cameraDistance = cameraDistance;
	}

	/* Distance (in mm) camera to shooter */
	public void setShooterDistance(int shooterDistance) {
		if (logger.isTraceEnabled()) logger.trace("shooterDistance {}", shooterDistance);
		this.shooterDistance = shooterDistance;
	}

	public int getCameraDistance() {
		return cameraDistance;
	}

	public int getShooterDistance() {
		return shooterDistance;
	}

	/*
	 * The resolution of the screen the arena is projected on Due to DPI scaling
	 * this might not correspond to the projector's resolution, but it is easier
	 * to think of that way
	 */
	public void setProjectorResolution(int width, int height) {
		if (logger.isTraceEnabled()) logger.trace("projector res w {} h {}", width, height);
		this.projectorResWidth = width;
		this.projectorResHeight = height;
	}

	public int getProjectionWidth() {
		return projectionWidth;
	}

	public int getProjectionHeight() {
		return projectionHeight;
	}

	public double getFocalLength() {
		return focalLength;
	}

	public double getSensorWidth() {
		return sensorWidth;
	}

	public double getSensorHeight() {
		return sensorHeight;
	}

	public void calculateUnknown() {
		int unknownCount = 0;

		final double wValues[] = { focalLength, patternWidth, cameraWidth, projectionWidth, sensorWidth, cameraDistance,
				projectorResWidth };

		for (int i = 0; i < wValues.length; i++) {
			if (wValues[i] == -1) {
				if (logger.isTraceEnabled()) logger.trace("Unknown: {}", i);
				unknownCount++;
			}
		}

		if (unknownCount > 1) {
			// We're okay with two unknowns if they're these two.
			if (!(focalLength == -1 && sensorWidth == -1 && unknownCount == 2)) {
				logger.error("More than one unknown");
				return;
			}
		} else if (unknownCount == 0) {
			logger.error("No unknown found");
			return;
		}

		if (projectionWidth == -1) {
			projectionWidth = (int) (((double) cameraDistance * (double) patternWidth * sensorWidth)
					/ (focalLength * (double) cameraWidth));
			projectionHeight = (int) (((double) cameraDistance * (double) patternHeight * sensorHeight)
					/ (focalLength * (double) cameraHeight));

			if (logger.isTraceEnabled()) logger.trace("({} *  {} * {}) / ({} * {})", cameraDistance, patternWidth,
					sensorWidth, focalLength, cameraWidth);

		} else if (sensorWidth == -1) {
			// Fix focalLength at 1 since we do not know it and we can only
			// calculate 1 unknown
			if (focalLength == -1) focalLength = 1;

			sensorWidth = ((projectionWidth * focalLength * (double) cameraWidth)
					/ ((double) cameraDistance * (double) patternWidth));
			sensorHeight = ((projectionHeight * focalLength * (double) cameraHeight)
					/ ((double) cameraDistance * (double) patternHeight));

			if (logger.isTraceEnabled()) {
				logger.trace("({} *  {} * {}) / ({} * {})", projectionWidth, focalLength, cameraWidth, cameraDistance,
						patternWidth);
				logger.trace("New camera params: focalLength {} sensorWidth {} sensorHeight {}", focalLength,
						sensorWidth, sensorHeight);
			}
		} else if (cameraDistance == -1) {
			final int cameraDistanceH = (int) (((double) projectionHeight * focalLength * (double) cameraHeight)
					/ ((double) patternHeight * sensorHeight));
			final int cameraDistanceW = (int) (((double) projectionWidth * focalLength * (double) cameraWidth)
					/ ((double) patternWidth * sensorWidth));

			if (logger.isTraceEnabled()) {
				logger.trace("{} = ({} * {} * {}) / ({} *  {})", cameraDistanceH, projectionHeight, focalLength,
						cameraHeight, patternHeight, sensorHeight);
				logger.trace("{} = ({} * {} * {}) / ({} *  {})", cameraDistanceW, projectionWidth, focalLength,
						cameraWidth, patternWidth, sensorWidth);
			}

			cameraDistance = (cameraDistanceH + cameraDistanceW) / 2;
		}

		else {
			logger.error("Unknown not supported");
			return;
		}

		pxPerMMwide = ((double) projectorResWidth / (double) projectionWidth);
		pxPerMMhigh = ((double) projectorResHeight / (double) projectionHeight);

		if (logger.isTraceEnabled())
			logger.trace("pW {} pH {} - pxW {} pxH {}", projectionWidth, projectionHeight, pxPerMMwide, pxPerMMhigh);
	}

	// Parameters in mm, return in px
	public Optional<Dimension2D> calculateObjectSize(double realWidth, double realHeight, double realDistance,
			double desiredDistance) {
		if (projectionWidth == -1 || projectionHeight == -1 || shooterDistance == -1) {
			logger.error("projectionWidth or projectionHeight or shooterDistance unknown");
			return Optional.empty();
		}

		// Make it appropriate size for the shooter
		double distRatio = realDistance / shooterDistance;

		// Make it appropriate size for the desired distance
		distRatio *= cameraDistance / desiredDistance;

		final double adjWidthmm = realWidth * distRatio;
		final double adjHeightmm = realHeight * distRatio;

		final double adjWidthpx = adjWidthmm * pxPerMMwide;
		final double adjHeightpx = adjHeightmm * pxPerMMhigh;

		if (logger.isTraceEnabled()) logger.trace("rD {} dD {} sD {} dR {} - adjmm {} {} adjpx {} {}", realDistance,
				desiredDistance, shooterDistance, distRatio, adjWidthmm, adjHeightmm, adjWidthpx, adjHeightpx);

		return Optional.of(new Dimension2D(adjWidthpx, adjHeightpx));
	}

	public boolean isCameraParamsKnown() {
		return sensorWidth == -1 && focalLength == -1;
	}
}
