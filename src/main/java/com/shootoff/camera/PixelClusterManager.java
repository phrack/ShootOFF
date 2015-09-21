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
	
	private ArrayList<Point> points;
	
	HashMap<Point, Integer> pixelMapping = new HashMap<Point, Integer>();

	PixelClusterManager(ArrayList<Point> p)
	{
			points = p;
	}
	
	void clusterPixels()
	{
		Stack<Point> mustExamine = new Stack<Point>();
		

		for (Point point : points)
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
						Point nearPoint = new Point(rx,ry);
						if (points.contains(nearPoint))
							logger.warn("{} {} - {} - {} {}", rx, ry, numberOfRegions, points.contains(nearPoint), !pixelMapping.containsKey(nearPoint));
						
						if (points.contains(nearPoint) && !pixelMapping.containsKey(nearPoint))
						{
							mustExamine.push(nearPoint);
							pixelMapping.put(nearPoint, numberOfRegions);
						}
					}
						
			}
		}
		
	}
	
	public ArrayList<Point> dumpClusters()
	{
		ArrayList<Point> centers = new ArrayList<Point>();
		
		for (int i = 0; i <= numberOfRegions; i++)
		{
			ArrayList<Point> cluster = new ArrayList<Point>();
			
			double averageX = 0;
			double averageY = 0;
			
			Iterator it = pixelMapping.entrySet().iterator();
			while (it.hasNext())
			{
				HashMap.Entry<Point, Integer> next = (Entry<Point, Integer>) it.next();
				if (next.getValue() == i)
				{
					cluster.add(next.getKey());
					logger.warn("Cluster {}: {}", i, next.getKey());
					averageX += next.getKey().x;
					averageY += next.getKey().y;
				}
				
			}
			averageX = averageX / cluster.size();
			averageY = averageY / cluster.size();
			
			
			logger.warn("Cluster {}: avg x {} avg y {}", i, averageX, averageY);
			
			if (cluster.size() > 9)
				centers.add(new Point((int)averageX,(int)averageY));
			
		}
		return centers;
	}
}
