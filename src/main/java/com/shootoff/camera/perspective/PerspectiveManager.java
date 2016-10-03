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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;

/**
 * This class is used to resize targets on the projector arena to real world
 * dimensions. This allows users and training exercises to take virtual targets
 * and set them to the sizes they are in real life are specific distances. For
 * example, ISSF targets are 10 cm x 10 cm at 25 m for most handgun
 * competitions. This class lets you resize an ISSF target on a projector arena
 * to 10 cm by 10 cm as if it were at 25 m. Given defaults like the ISSF case,
 * this class can resize a target to appear as if it is at any distance.
 * 
 * All units for this class are in millimeters because those units make the
 * calculations nicer. In particular, we use the following formula to calculate
 * unknown camera and distance parameters and target dimensions:
 * 
 * distance to object (mm) =
 * 
 * focal length (mm) * real height of the object (mm) * image height (pixels)
 * ---------------------------------------------------------------------------
 * object height (pixels) * sensor height (mm)
 * 
 * @author cbdmaul
 */

public class PerspectiveManager {
	private static final Logger logger = LoggerFactory.getLogger(PerspectiveManager.class);

	private final static int US_LETTER_WIDTH_MM = 279;
	private final static int US_LETTER_HEIGHT_MM = 216;
	
	private final static int DEFAULT_SHOOTER_DISTANCE = 3000;

	private String calibratedCameraName;

	// Key = camera name
	private static final List<CameraParameters> cameraParameters = new ArrayList<CameraParameters>();

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
		private final String cameraName;
		private final double focalLength;
		private final double sensorWidth;
		private final double sensorHeight;
		private final Dimension2D validDims;

		public CameraParameters(String cameraName, double focalLength, double sensorWidth, double sensorHeight,
				Dimension2D validDims) {
			this.focalLength = focalLength;
			this.sensorWidth = sensorWidth;
			this.sensorHeight = sensorHeight;
			this.validDims = validDims;
			this.cameraName = cameraName;
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

		public String getName() {
			return cameraName;
		}

		public Dimension2D getValidDimensions() {
			return validDims;
		}
	}

	// TODO: Implement a way to load these values from a file
	// so that they can be easily tweaked/added to
	static {
		cameraParameters.add(new CameraParameters("C270", 4.0, 3.58, 2.02, new Dimension2D(1280, 720)));
		cameraParameters.add(new CameraParameters("C270", 4.0, 3.60, 2.712, new Dimension2D(800, 600)));
		cameraParameters.add(new CameraParameters("C270", 4.0, 3.145, 2.343, new Dimension2D(640, 480)));

		cameraParameters.add(new CameraParameters("C920", 3.67, 4.80, 2.70, new Dimension2D(1280, 720)));

		cameraParameters.add(new CameraParameters("HD-3000", 4, 3.787, 2.864, new Dimension2D(640, 480)));
	}

	// For testing
	protected PerspectiveManager(Bounds arenaBounds) {
		if (logger.isTraceEnabled())
			logger.trace("pattern res w {} h {}", arenaBounds.getWidth(), arenaBounds.getHeight());
		this.patternWidth = (int) arenaBounds.getWidth();
		this.patternHeight = (int) arenaBounds.getHeight();
	}

	public PerspectiveManager(Bounds arenaBounds, Dimension2D feedDims, Dimension2D paperBounds,
			Dimension2D projectorRes) {
		this(arenaBounds);
		setCameraFeedSize((int) feedDims.getWidth(), (int) feedDims.getHeight());
		this.setProjectorResolution(projectorRes);

		setProjectionSizeFromLetterPaperPixels(paperBounds);

		if (cameraDistance == -1)
			cameraDistance = DEFAULT_SHOOTER_DISTANCE;
		if (shooterDistance == -1)
			shooterDistance = DEFAULT_SHOOTER_DISTANCE;
		
		calculateRealWorldSize();
		
	}

	public PerspectiveManager(String cameraName, Bounds arenaBounds, Dimension2D feedDims, Dimension2D projectorRes) {
		this(cameraName, feedDims, arenaBounds);
		this.setProjectorResolution(projectorRes);
		
		if (cameraDistance == -1)
			cameraDistance = DEFAULT_SHOOTER_DISTANCE;
		if (shooterDistance == -1)
			shooterDistance = DEFAULT_SHOOTER_DISTANCE;

		
		calculateRealWorldSize();
	}

	public PerspectiveManager(String cameraName, Dimension2D resolution, Bounds arenaBounds) {
		this(arenaBounds);

		calibratedCameraName = cameraName;

		if (!setCameraParameters(cameraName, resolution)) {
			throw new UnsupportedCameraException(cameraName + " does not support target perspectives because"
					+ " its focal length and sensor parameters are unknown.");
		}

		setCameraFeedSize(resolution);

	}

	public PerspectiveManager(String cameraName, Bounds arenaBounds, Dimension2D feedDims, Dimension2D paperBounds,
			Dimension2D projectorRes) {
		this(cameraName, feedDims, arenaBounds);
		setProjectionSizeFromLetterPaperPixels(paperBounds);
		this.setProjectorResolution(projectorRes);

		// Camera distance is unknown
		calculateUnknown();

		if (cameraDistance == -1)
			cameraDistance = DEFAULT_SHOOTER_DISTANCE;
		if (shooterDistance == -1)
			shooterDistance = DEFAULT_SHOOTER_DISTANCE;
		
		calculateRealWorldSize();
	}

	public static boolean isCameraSupported(final String cameraName, Dimension2D desiredResolution) {
		for (CameraParameters cam : cameraParameters) {
			if (cameraName.contains(cam.getName())
					&& Math.abs(cam.getValidDimensions().getWidth() - desiredResolution.getWidth()) < .001
					&& Math.abs(cam.getValidDimensions().getHeight() - desiredResolution.getHeight()) < .001) {
				return true;
			}
		}

		return false;
	}

	private boolean setCameraParameters(final String cameraName, Dimension2D desiredResolution) {
		for (CameraParameters cam : cameraParameters) {
			if (cameraName.contains(cam.getName())
					&& Math.abs(cam.getValidDimensions().getWidth() - desiredResolution.getWidth()) < .001
					&& Math.abs(cam.getValidDimensions().getHeight() - desiredResolution.getHeight()) < .001) {
				focalLength = cam.getFocalLength();
				sensorWidth = cam.getSensorWidth();
				sensorHeight = cam.getSensorHeight();

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
	 * (in mm) -- currently only used for testing. Could ask the user to set
	 * this manually, but this is enough of a pain that for now we just ask them
	 * to calibrate with paper
	 */
	protected void setProjectionSize(int width, int height) {
		if (logger.isTraceEnabled())
			logger.trace("projection w {} h {}", width, height);

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
		if (logger.isTraceEnabled())
			logger.trace("letter w {} h {}", letterDims.getWidth(), letterDims.getHeight());

		if (cameraWidth == -1 || patternWidth == -1) {
			logger.error("Missing cameraWidth or patternWidth for US Letter calculation");
			return;
		}

		// Calculate the size of the whole camera feed using the size of the
		// letter
		final double cameraFeedWidthMM = ((double) cameraWidth / letterDims.getWidth()) * US_LETTER_WIDTH_MM;
		final double cameraFeedHeightMM = ((double) cameraHeight / letterDims.getHeight()) * US_LETTER_HEIGHT_MM;

		if (logger.isTraceEnabled()) {
			logger.trace("{} = ({} / {}) * {}", cameraFeedWidthMM, cameraWidth, letterDims.getWidth(),
					US_LETTER_WIDTH_MM);
			logger.trace("{} = ({} / {}) * {}", cameraFeedHeightMM, cameraHeight, letterDims.getHeight(),
					US_LETTER_HEIGHT_MM);
		}

		// Set the projection width/height in mm
		projectionWidth = (int) (cameraFeedWidthMM * ((double) patternWidth / (double) cameraWidth));
		projectionHeight = (int) (cameraFeedHeightMM * ((double) patternHeight / (double) cameraHeight));

		if (logger.isTraceEnabled()) {
			logger.trace("{} = ({} / {}) * {}", projectionWidth, cameraFeedWidthMM, patternWidth, cameraWidth);
			logger.trace("{} = ({} / {}) * {}", projectionHeight, cameraFeedHeightMM, patternHeight, cameraHeight);
		}
	}

	/* The camera feed width and height (in px) */
	public void setCameraFeedSize(int width, int height) {
		if (logger.isTraceEnabled())
			logger.trace("camera feed w {} h {}", width, height);
		this.cameraHeight = height;
		this.cameraWidth = width;
	}

	private void setCameraFeedSize(Dimension2D resolution) {
		setCameraFeedSize((int) resolution.getWidth(), (int) resolution.getHeight());
	}

	/* Distance (in mm) camera to screen */
	public void setCameraDistance(int cameraDistance) {
		if (logger.isTraceEnabled())
			logger.trace("cameraDistance {}", cameraDistance);

		this.cameraDistance = cameraDistance;

		// TODO: Add logic to recalculate camera parameters if they were set via
		// calibration and not stored parameters
		if (!isInitialized()) {
			calculateUnknown();

			// This makes things work easier if the user doesn't really know to
			// set shooter distance
			// or the user starts an exercise that uses perspective but didn't
			// set shooter distance
			if (shooterDistance == -1)
				shooterDistance = cameraDistance;

		}
	}

	/* Distance (in mm) camera to shooter */
	public void setShooterDistance(int shooterDistance) {
		if (logger.isTraceEnabled())
			logger.trace("shooterDistance {}", shooterDistance);
		this.shooterDistance = shooterDistance;
	}

	public String getCalibratedCameraName() {
		return calibratedCameraName;
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
		if (logger.isTraceEnabled())
			logger.trace("projector res w {} h {}", width, height);
		this.projectorResWidth = width;
		this.projectorResHeight = height;
	}

	public void setProjectorResolution(Dimension2D dims) {
		setProjectorResolution((int) dims.getWidth(), (int) dims.getHeight());
	}

	public int getProjectionWidth() {
		return projectionWidth;
	}

	public int getProjectionHeight() {
		return projectionHeight;
	}

	protected double getFocalLength() {
		return focalLength;
	}

	protected double getSensorWidth() {
		return sensorWidth;
	}

	protected double getSensorHeight() {
		return sensorHeight;
	}
	
	protected int getUnknownCount()
	{
		int unknownCount = 0;

		final double wValues[] = { focalLength, patternWidth, cameraWidth, projectionWidth, sensorWidth, cameraDistance,
				projectorResWidth };

		for (int i = 0; i < wValues.length; i++) {
			if (wValues[i] == -1) {
				if (logger.isTraceEnabled())
					logger.trace("Unknown: {}", i);
				unknownCount++;
			}
		}
		
		return unknownCount;
	}

	protected void calculateUnknown() {
		int unknownCount = getUnknownCount();

		if (unknownCount > 1) {
			// We're okay with two unknowns if they're these two.
			if (!(focalLength == -1 && sensorWidth == -1 && unknownCount == 2)) {
				logger.error("More than one unknown");
				return;
			}
		} else if (unknownCount == 0) {
			// TODO: Update highest error unknown if this is the case
			return;
		}

		if (projectionWidth == -1) {
			projectionWidth = (int) (((double) cameraDistance * (double) patternWidth * sensorWidth)
					/ (focalLength * (double) cameraWidth));
			projectionHeight = (int) (((double) cameraDistance * (double) patternHeight * sensorHeight)
					/ (focalLength * (double) cameraHeight));

			if (logger.isTraceEnabled())
				logger.trace("({} *  {} * {}) / ({} * {})", cameraDistance, patternWidth, sensorWidth, focalLength,
						cameraWidth);

		} else if (sensorWidth == -1) {
			// Fix focalLength at 4 since we do not know it and we can only
			// calculate 1 unknown
			// 4 is an arbitrary selection
			if (focalLength == -1)
				focalLength = 4;

			sensorWidth = ((projectionWidth * focalLength * (double) cameraWidth)
					/ ((double) cameraDistance * (double) patternWidth));
			sensorHeight = ((projectionHeight * focalLength * (double) cameraHeight)
					/ ((double) cameraDistance * (double) patternHeight));

			if (logger.isTraceEnabled()) {
				logger.trace("({} *  {} * {}) / ({} * {})", projectionWidth, focalLength, cameraWidth, cameraDistance,
						patternWidth);
			}
			logger.info("New camera params: focalLength {} sensorWidth {} sensorHeight {} width {} height {}",
					focalLength, sensorWidth, sensorHeight, cameraWidth, cameraHeight);

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
		}
		
		calculateRealWorldSize();

		if (logger.isTraceEnabled())
			logger.trace("pW {} pH {} - pxW {} pxH {}", projectionWidth, projectionHeight, pxPerMMwide, pxPerMMhigh);
	}
	
	void calculateRealWorldSize()
	{
		if (projectorResWidth > -1 && projectionWidth > -1)
		{
			pxPerMMwide = ((double) projectorResWidth / (double) projectionWidth);
			pxPerMMhigh = ((double) projectorResHeight / (double) projectionHeight);
		}
	}
	

	/**
	 * Starting with a target's real world width and height in mm, as it appears
	 * on a projection, calculate a new width and height in pixels to resize the
	 * target to such that it appears to be a distance of
	 * <code>desiredDistance</code> in mm away. This calculation assumes that
	 * the current real world dimensions are the result of the target being
	 * <code>realDistance</code> away in mm.
	 * 
	 * To set the initial real word size of a target, call this method with the
	 * desired <code>realWidth</code> and <code>realHeight</code> with
	 * <code>realDistance</code> and <code>desiredDistance</code> equal to the
	 * target's initial real world distance.
	 * 
	 * @param realWidth
	 *            the current width of the target on the projection in mm
	 * @param realHeight
	 *            the current height of the target on the projection in mm
	 * @param realDistance
	 *            the current distance of the target in mm
	 * @param desiredDistance
	 *            the desired new distance of the target used to derive the new
	 *            target dimensions. Value must be > 0
	 * @return the new targets dimensions in pixels necessary to make it appear
	 *         <code>desiredDistance</code> away given its current real world
	 *         dimensions and distance
	 */
	public Optional<Dimension2D> calculateObjectSize(double realWidth, double realHeight, double desiredDistance) {
		if (projectionWidth == -1 || projectionHeight == -1 || shooterDistance == -1 || pxPerMMhigh == -1) {
			logger.error("projectionWidth, projectionHeight, shooterDistance, pxPerMMhigh unknown");
			return Optional.empty();
		}

		if (desiredDistance == 0) {
			throw new IllegalArgumentException("desiredDistance cannot be 0");
		}

		// Make it appropriate size for the desired distance
		// Should just cap the result dimensions at the size of the projector

		double distRatio = shooterDistance / desiredDistance;

		final double adjWidthmm = realWidth * distRatio;
		final double adjHeightmm = realHeight * distRatio;

		final double adjWidthpx = adjWidthmm * pxPerMMwide;
		final double adjHeightpx = adjHeightmm * pxPerMMhigh;

		if (logger.isTraceEnabled()) {
			logger.trace("real w {} h {} d {}", realWidth, realHeight, desiredDistance);
			logger.trace("sD {} dR {} - adjmm {} {} adjpx {} {}", shooterDistance, distRatio, adjWidthmm, adjHeightmm,
					adjWidthpx, adjHeightpx);

		}

		return Optional.of(new Dimension2D(adjWidthpx, adjHeightpx));
	}

	public boolean isInitialized() {
		return projectionWidth > -1 && projectionHeight > -1 && shooterDistance > -1 && pxPerMMhigh > -1;
	}

	public boolean isCameraParamsKnown() {
		return !(sensorWidth == -1 && focalLength == -1);
	}
}
