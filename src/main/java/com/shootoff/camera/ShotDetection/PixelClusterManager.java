package com.shootoff.camera.ShotDetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelClusterManager {
	
	private final Logger logger = LoggerFactory.getLogger(PixelClusterManager.class);
	
	private int numberOfRegions = -1;
	
	private ArrayList<Pixel> points;
	
	HashMap<Pixel, Integer> pixelMapping = new HashMap<Pixel, Integer>();

	private ShotDetectionManager shotDetectionManager;
	
	private final static double MINIMUM_CONNECTEDNESS = 6.72f;

	PixelClusterManager(ArrayList<Pixel> p, ShotDetectionManager shotDetectionManager)
	{
			points = p;
			this.shotDetectionManager = shotDetectionManager;
	}
	
	void clusterPixels()
	{
		Stack<Pixel> mustExamine = new Stack<Pixel>();
		

		for (Pixel point : points)
		{

			if (!pixelMapping.containsKey(point))
			{
				numberOfRegions++;
				mustExamine.add(point);
				pixelMapping.put(point, numberOfRegions);
			}
			while (mustExamine.size() > 0)
			{
				Pixel thisPoint = mustExamine.pop();
				
				int connectedness = 0;
				
				for(int h=-1;h<=1;h++)
					for(int w=-1;w<=1;w++) 
					{
						int rx = thisPoint.x+w; 
						int ry = thisPoint.y+h; 
						Pixel nearPoint = new Pixel(rx,ry);
						if (points.contains(nearPoint))
						{
							logger.trace("{} {} - {} - {} {}", rx, ry, numberOfRegions, points.contains(nearPoint), !pixelMapping.containsKey(nearPoint));
							
							if (pixelMapping.containsKey(nearPoint) && pixelMapping.get(nearPoint).intValue()==numberOfRegions)
							{
								connectedness++;
							}
							
							else if (!pixelMapping.containsKey(nearPoint))
							{
								
								connectedness++;
								
								nearPoint = points.get(points.indexOf(nearPoint));
								
								mustExamine.push(nearPoint);
								pixelMapping.put(nearPoint, numberOfRegions);
								
							}
						}
						

					}
				
				thisPoint.setConnectedness(connectedness);
			}
		}
		
	}
	
	
	public ArrayList<PixelCluster> dumpClusters()
	{
		ArrayList<PixelCluster> clusters = new ArrayList<PixelCluster>();
		
		for (int i = 0; i <= numberOfRegions; i++)
		{
			PixelCluster cluster = new PixelCluster();
			
			double averageX = 0;
			double averageY = 0;
			
			double avgconnectedness = 0;
			
			Iterator<Entry<Pixel, Integer>> it = pixelMapping.entrySet().iterator();
			while (it.hasNext())
			{
				HashMap.Entry<Pixel, Integer> next = (Entry<Pixel, Integer>) it.next();
				if (next.getValue() == i)
				{
					Pixel nextPixel = next.getKey();
					
					
					cluster.add(nextPixel);
					logger.trace("Cluster {}: {} {} - {}", i, nextPixel.x, nextPixel.y, nextPixel.getConnectedness());
					averageX += nextPixel.x * nextPixel.getConnectedness();
					averageY += nextPixel.y * nextPixel.getConnectedness();
					
					avgconnectedness += nextPixel.getConnectedness();
					
					pixelMapping.remove(next);
				}
				
			}
			
			averageX = (averageX / avgconnectedness);
			averageY = (averageY / avgconnectedness);

			avgconnectedness = avgconnectedness / cluster.size();
			
			logger.trace("Cluster {} - {} - connectedness {} - {} {}", i, cluster.size(), avgconnectedness, averageX, averageY);
			
			// It's too small or not well connected, bail out early
			if (cluster.size() < shotDetectionManager.getMinimumShotDimension() || avgconnectedness < MINIMUM_CONNECTEDNESS)
				continue;
			

			
			cluster.centerPixelX = averageX;
			cluster.centerPixelY = averageY;
			
			clusters.add(cluster);
		}
		return clusters;
	}
}
