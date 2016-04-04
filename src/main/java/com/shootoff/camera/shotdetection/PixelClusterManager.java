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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelClusterManager {
	private static final Logger logger = LoggerFactory.getLogger(PixelClusterManager.class);
	
	private final int feedWidth;
	private final int feedHeight;
	
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

	protected PixelClusterManager(int feedWidth, int feedHeight) {
		this.feedWidth = feedWidth;
		this.feedHeight = feedHeight;
	}

	private int preprocessClusterablePixels(Set<Pixel> clusterablePixels, Map<Pixel, Integer> pixelMapping) {
		final Stack<Pixel> mustExamine = new Stack<Pixel>();
		int numberOfRegions = -1;

		for (final Pixel pixel : clusterablePixels) {
			if (!pixelMapping.containsKey(pixel)) {
				numberOfRegions++;
				mustExamine.add(pixel);
				pixelMapping.put(pixel, numberOfRegions);
			}

			if (numberOfRegions > EXCESSIVE_PIXEL_REGION_COUNT && clusterablePixels.size() > EXCESSIVE_PIXEL_CUTOFF)
				break;

			while (!mustExamine.isEmpty()) {
				final Pixel thisPoint = mustExamine.pop();

				int connectedness = 0;

				for (int h = -1; h <= 1; h++) {
					for (int w = -1; w <= 1; w++) {
						if (h == 0 && w == 0) continue;

						final int rx = thisPoint.x + w;
						final int ry = thisPoint.y + h;

						if (rx < 0 || ry < 0 || rx >= feedWidth || ry >= feedHeight) continue;

						final Pixel nearPoint = new Pixel(rx, ry);
						if (clusterablePixels.contains(nearPoint)) {
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
		
		return numberOfRegions;
	}

	public Set<PixelCluster> clusterPixels(Set<Pixel> clusterablePixels, int minimumShotDimension) {
		final Map<Pixel, Integer> pixelMapping = new HashMap<Pixel, Integer>();
		
		final int numberOfRegions = preprocessClusterablePixels(clusterablePixels, pixelMapping);

		final Set<PixelCluster> clusters = new HashSet<PixelCluster>();

		for (int i = 0; i <= numberOfRegions; i++) {
			final PixelCluster cluster = new PixelCluster();

			double averageX = 0;
			double averageY = 0;

			int minX = feedWidth;
			int minY = feedHeight;
			int maxX = 0, maxY = 0;

			double avgconnectedness = 0;

			for (Entry<Pixel, Integer> pixelEntry : pixelMapping.entrySet()) {
				if (pixelEntry.getValue() == i) {
					final Pixel nextPixel = pixelEntry.getKey();

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
				}
			}

			final int clustersize = cluster.size();

			if (clustersize < minimumShotDimension) continue;

			averageX /= avgconnectedness;
			averageY /= avgconnectedness;

			avgconnectedness = avgconnectedness / clustersize;

			// We scale up the minimum in a linear scale as the cluster size
			// increases. This is an approximate density
			final double scaled_minimum = Math.min(
					MINIMUM_CONNECTEDNESS + ((clustersize - minimumShotDimension) * MINIMUM_CONNECTEDNESS_FACTOR),
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
			final double circleArea = Math.PI * r * r;
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
