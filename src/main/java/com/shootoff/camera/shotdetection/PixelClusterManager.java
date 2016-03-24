/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

package com.shootoff.camera.shotdetection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelClusterManager {
	private static final Logger logger = LoggerFactory.getLogger(PixelClusterManager.class);

	private int numberOfRegions = -1;

	private final Set<Pixel> points;

	private final Map<Pixel, Integer> pixelMapping = new HashMap<Pixel, Integer>();

	private ShotDetectionManager shotDetectionManager;

	private final static double MINIMUM_CONNECTEDNESS = 3.66f;
	private final static double MAXIMUM_CONNECTEDNESS_SCALE = 6f;

	private final static double MINIMUM_CONNECTEDNESS_FACTOR = .018f;

	private final static double MINIMUM_DENSITY = .69f;

	private final static double MINIMUM_SHOT_RATIO = .5f;
	private final static double MAXIMUM_SHOT_RATIO = 1.4f;

	// Use different values if shot width + shot height <= 16 px
	private final static int SMALL_SHOT_THRESHOLD = 16;
	private final static double MINIMUM_SHOT_RATIO_SMALL = .5f;
	private final static double MAXIMUM_SHOT_RATIO_SMALL = 1.5f;

	private final static int EXCESSIVE_PIXEL_CUTOFF = 300;
	private final static int EXCESSIVE_PIXEL_REGION_COUNT = 1;

	protected PixelClusterManager(Set<Pixel> p, ShotDetectionManager shotDetectionManager) {
		points = p;
		this.shotDetectionManager = shotDetectionManager;
	}

	protected void clusterPixels() {
		final Stack<Pixel> mustExamine = new Stack<Pixel>();

		for (Pixel point : points) {
			if (!pixelMapping.containsKey(point)) {
				numberOfRegions++;
				mustExamine.add(point);
				pixelMapping.put(point, numberOfRegions);
			}

			if (numberOfRegions > EXCESSIVE_PIXEL_REGION_COUNT && points.size() > EXCESSIVE_PIXEL_CUTOFF) break;

			while (mustExamine.size() > 0) {
				final Pixel thisPoint = mustExamine.pop();

				int connectedness = 0;

				for (int h = -1; h <= 1; h++) {
					for (int w = -1; w <= 1; w++) {
						if (h == 0 && w == 0) continue;

						int rx = thisPoint.x + w;
						int ry = thisPoint.y + h;

						if (rx < 0 || ry < 0 || rx >= shotDetectionManager.getCameraManager().getFeedWidth()
								|| ry >= shotDetectionManager.getCameraManager().getFeedHeight())
							continue;

						final Pixel nearPoint = new Pixel(rx, ry);
						if (points.contains(nearPoint)) {
							if (!pixelMapping.containsKey(nearPoint)) {
								mustExamine.push(nearPoint);
								pixelMapping.put(nearPoint, numberOfRegions);
							}

							connectedness++;
						}
					}
				}

				thisPoint.setConnectedness(connectedness);
			}
		}
	}

	public Set<PixelCluster> dumpClusters() {
		Set<PixelCluster> clusters = new HashSet<PixelCluster>();

		for (int i = 0; i <= numberOfRegions; i++) {
			final PixelCluster cluster = new PixelCluster();

			double averageX = 0;
			double averageY = 0;

			int minX = shotDetectionManager.getCameraManager().getFeedWidth(),
					minY = shotDetectionManager.getCameraManager().getFeedHeight(), maxX = 0, maxY = 0;

			double avgconnectedness = 0;

			Iterator<Entry<Pixel, Integer>> it = pixelMapping.entrySet().iterator();
			while (it.hasNext()) {
				HashMap.Entry<Pixel, Integer> next = (Entry<Pixel, Integer>) it.next();
				if (next.getValue() == i) {
					Pixel nextPixel = next.getKey();

					if (nextPixel.x < minX)
						minX = nextPixel.x;
					else if (nextPixel.x > maxX) maxX = nextPixel.x;

					if (nextPixel.y < minY)
						minY = nextPixel.y;
					else if (nextPixel.y > maxY) maxY = nextPixel.y;

					cluster.add(nextPixel);

					final int connectedness = nextPixel.getConnectedness();

					averageX += nextPixel.x * connectedness;
					averageY += nextPixel.y * connectedness;

					avgconnectedness += connectedness;

					it.remove();
				}
			}

			final int clustersize = cluster.size();

			if (clustersize < shotDetectionManager.getMinimumShotDimension()) continue;

			averageX = (averageX / avgconnectedness);
			averageY = (averageY / avgconnectedness);

			avgconnectedness = avgconnectedness / clustersize;

			// We scale up the minimum in a linear scale as the cluster size
			// increases. This is an approximate density
			double scaled_minimum = Math.min(MINIMUM_CONNECTEDNESS
					+ ((clustersize - shotDetectionManager.getMinimumShotDimension()) * MINIMUM_CONNECTEDNESS_FACTOR),
					MAXIMUM_CONNECTEDNESS_SCALE);

			if (logger.isTraceEnabled()) logger.trace("Cluster {}: size {} connectedness {} scaled_minimum {} - {} {}",
					i, clustersize, avgconnectedness, scaled_minimum, averageX, averageY);

			if (avgconnectedness < scaled_minimum) continue;

			final int shotWidth = (maxX - minX) + 1;
			final int shotHeight = (maxY - minY) + 1;
			final double shotRatio = (double) shotWidth / (double) shotHeight;

			if (logger.isTraceEnabled()) logger.trace("Cluster {}: shotRatio {} {} - {} - {} {} {} {}", i, shotWidth,
					shotHeight, shotRatio, minX, minY, maxX, maxY);

			if ((shotWidth + shotHeight) > SMALL_SHOT_THRESHOLD
					&& (shotRatio < MINIMUM_SHOT_RATIO || shotRatio > MAXIMUM_SHOT_RATIO))
				continue;
			else if (shotRatio < MINIMUM_SHOT_RATIO_SMALL || shotRatio > MAXIMUM_SHOT_RATIO_SMALL) continue;

			final double r = (double) (shotWidth + shotHeight) / 4.0f;
			final double circleArea = Math.PI * Math.pow(r, 2);
			final double density = (double) (clustersize) / circleArea;

			if (logger.isTraceEnabled()) logger.trace("Cluster {}: density {} {} - {} {} - {}", i, shotWidth,
					shotHeight, circleArea, cluster.size(), density);

			if (density < MINIMUM_DENSITY) continue;

			cluster.centerPixelX = averageX;
			cluster.centerPixelY = averageY;

			clusters.add(cluster);
		}

		if (logger.isTraceEnabled())
			logger.trace("---- Detected {} shots from {} regions ------", clusters.size(), numberOfRegions + 1);

		return clusters;
	}
}
