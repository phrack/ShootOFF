package com.shootoff.camera.shotdetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelClusterManager {

	private static final Logger logger = LoggerFactory.getLogger(PixelClusterManager.class);

	private int numberOfRegions = -1;

	private ArrayList<Pixel> points;

	HashMap<Pixel, Integer> pixelMapping = new HashMap<Pixel, Integer>();

	private ShotDetectionManager shotDetectionManager;

	private final static double MINIMUM_CONNECTEDNESS = 3.66f;
	private final static double MAXIMUM_CONNECTEDNESS_SCALE = 6f;
	
	private final static double MINIMUM_CONNECTEDNESS_FACTOR = .018f;

	private final static double MINIMUM_DENSITY = .69f;

	private final static double MINIMUM_SHOT_RATIO = .5f;
	private final static double MAXIMUM_SHOT_RATIO = 1.5f;
	
	private final static int EXCESSIVE_PIXEL_CUTOFF = 300;
	private final static int EXCESSIVE_PIXEL_REGION_COUNT = 1;

	PixelClusterManager(ArrayList<Pixel> p, ShotDetectionManager shotDetectionManager) {
		points = p;
		this.shotDetectionManager = shotDetectionManager;
		
		 ((ch.qos.logback.classic.Logger)
		 logger).setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	void clusterPixels() {
		final Stack<Pixel> mustExamine = new Stack<Pixel>();

		for (Pixel point : points) {

			if (!pixelMapping.containsKey(point)) {
				numberOfRegions++;
				mustExamine.add(point);
				pixelMapping.put(point, numberOfRegions);
			}
			if (numberOfRegions > EXCESSIVE_PIXEL_REGION_COUNT && points.size() > EXCESSIVE_PIXEL_CUTOFF)
				break;
			

			
			while (mustExamine.size() > 0) {
				Pixel thisPoint = mustExamine.pop();

				int connectedness = 0;

				for (int h = -1; h <= 1; h++)
					for (int w = -1; w <= 1; w++) {
						if (h == 0 && w == 0) continue;

						int rx = thisPoint.x + w;
						int ry = thisPoint.y + h;

						if (rx < 0 || ry < 0 || rx >= shotDetectionManager.getCameraManager().getFeedWidth()
								|| ry >= shotDetectionManager.getCameraManager().getFeedHeight())
							continue;

						Pixel nearPoint = new Pixel(rx, ry);
						if (points.contains(nearPoint)) {
							if (!pixelMapping.containsKey(nearPoint)) {

								mustExamine.push(nearPoint);
								pixelMapping.put(nearPoint, numberOfRegions);

							}
							
							connectedness++;
						}
					}
				thisPoint.setConnectedness(connectedness);
			}
		}

	}

	public ArrayList<PixelCluster> dumpClusters() {
		ArrayList<PixelCluster> clusters = new ArrayList<PixelCluster>();

		for (int i = 0; i <= numberOfRegions; i++) {
			PixelCluster cluster = new PixelCluster();

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

					if (nextPixel.x < minX) minX = nextPixel.x;
					else if (nextPixel.x > maxX) maxX = nextPixel.x;
					if (nextPixel.y < minY) minY = nextPixel.y;
					else if (nextPixel.y > maxY) maxY = nextPixel.y;

					cluster.add(nextPixel);
					
					
					final int connectedness = nextPixel.getConnectedness();
					if (logger.isTraceEnabled()) {
						logger.trace("Cluster {}: {} {} - {}", i, nextPixel.x, nextPixel.y, connectedness);
					}
					averageX += nextPixel.x * connectedness;
					averageY += nextPixel.y * connectedness;

					avgconnectedness += connectedness;

					it.remove();
				}

			}

			if (cluster.size() < shotDetectionManager.getMinimumShotDimension()) continue;
			
			
			averageX = (averageX / avgconnectedness);
			averageY = (averageY / avgconnectedness);

			avgconnectedness = avgconnectedness / cluster.size();

			// We scale up the minimum in a linear scale as the cluster size
			// increases. This is an approximate density
			double scaled_minimum = Math.min(MINIMUM_CONNECTEDNESS
					+ ((cluster.size() - shotDetectionManager.getMinimumShotDimension())
							* MINIMUM_CONNECTEDNESS_FACTOR), MAXIMUM_CONNECTEDNESS_SCALE);

			logger.trace("Cluster {}: size {} connectedness {} scaled_minimum {} - {} {}", i,
					cluster.size(), avgconnectedness, scaled_minimum, averageX, averageY);
			
			if (avgconnectedness < scaled_minimum) continue;

			int shotWidth = (maxX - minX) + 1;
			int shotHeight = (maxY - minY) + 1;
			double shotRatio = (double) shotWidth / (double) shotHeight;

			logger.trace("Cluster {}: shotRatio {} {} - {} - {} {} {} {}", i, shotWidth, shotHeight, shotRatio, minX,
					minY, maxX, maxY);

			if (shotRatio < MINIMUM_SHOT_RATIO || shotRatio > MAXIMUM_SHOT_RATIO) continue;

			double r = (double) (shotWidth + shotHeight) / 4.0f;
			double circleArea = Math.PI * Math.pow(r, 2);
			double density = (double) cluster.size() / circleArea;

			logger.trace("Cluster {}: density {} {} - {} {} - {}", i, shotWidth, shotHeight, circleArea, cluster.size(),
					density);

			if (density < MINIMUM_DENSITY) continue;

			cluster.centerPixelX = averageX;
			cluster.centerPixelY = averageY;

			clusters.add(cluster);
		}

		logger.trace("---- Detected {} shots from {} regions ------", clusters.size(), numberOfRegions + 1);

		return clusters;
	}
}
