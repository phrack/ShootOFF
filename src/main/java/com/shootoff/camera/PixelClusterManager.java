package com.shootoff.camera;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelClusterManager {
	
	private final Logger logger = LoggerFactory.getLogger(PixelClusterManager.class);
	
	private int numberOfRegions = -1;
	
	private ArrayList<Pixel> points;
	
	HashMap<Pixel, Integer> pixelMapping = new HashMap<Pixel, Integer>();

	PixelClusterManager(ArrayList<Pixel> p)
	{
			points = p;
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
				Point thisPoint = mustExamine.pop();
				for(int h=-1;h<=1;h++)
					for(int w=-1;w<=1;w++) 
					{
						int rx = thisPoint.x+w; 
						int ry = thisPoint.y+h; 
						Pixel nearPoint = new Pixel(rx,ry);
						if (points.contains(nearPoint))
							logger.trace("{} {} - {} - {} {}", rx, ry, numberOfRegions, points.contains(nearPoint), !pixelMapping.containsKey(nearPoint));
						
						if (points.contains(nearPoint) && !pixelMapping.containsKey(nearPoint))
						{
							nearPoint = points.get(points.indexOf(nearPoint));
							
							mustExamine.push(nearPoint);
							pixelMapping.put(nearPoint, numberOfRegions);
						}
					}
						
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
			
			
			// FIX ME: We don't need to iterate over this more than once to get the pixel into the right cluster.
			Iterator it = pixelMapping.entrySet().iterator();
			while (it.hasNext())
			{
				HashMap.Entry<Pixel, Integer> next = (Entry<Pixel, Integer>) it.next();
				if (next.getValue() == i)
				{
					cluster.add(next.getKey());
					logger.trace("Cluster {}: {}", i, next.getKey());
					averageX += next.getKey().x;
					averageY += next.getKey().y;
				}
				
			}
			
			logger.warn("Cluster {} - {}", i, cluster.size());
			
			// It's too small, bail out early
			if (cluster.size() < 6)
				continue;
			
			
			averageX = averageX / cluster.size();
			averageY = averageY / cluster.size();

			
			cluster.setCenterPixel(new Pixel((int)averageX,(int)averageY));
			
			clusters.add(cluster);
		}
		return clusters;
	}
}
