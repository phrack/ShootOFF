package com.shootoff.camera.ShotDetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.camera.CameraManager;

public class PixelClusterManager {
	
	private final Logger logger = LoggerFactory.getLogger(PixelClusterManager.class);
	
	private int numberOfRegions = -1;
	
	private ArrayList<Pixel> points;
	
	HashMap<Pixel, Integer> pixelMapping = new HashMap<Pixel, Integer>();

	private ShotDetectionManager shotDetectionManager;

	private final static double MINIMUM_CONNECTEDNESS = 4.44f;
	private final static double MINIMUM_CONNECTEDNESS_FACTOR = .018f;

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
						if (h==0 && w==0)
							continue;
						
						int rx = thisPoint.x+w; 
						int ry = thisPoint.y+h; 
						
						if (rx<0 || ry<0 || rx>=CameraManager.FEED_WIDTH || ry>=CameraManager.FEED_HEIGHT)
							continue;
						
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
				
				logger.trace("{} {} - {}", thisPoint.x, thisPoint.y, connectedness);
				
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

			// We scale up the minimum in a linear scale as the cluster size increases.  This is an approximate density
			double scaled_minimum = MINIMUM_CONNECTEDNESS+((cluster.size()-shotDetectionManager.getMinimumShotDimension())*MINIMUM_CONNECTEDNESS_FACTOR);
			
			logger.trace("Cluster {} - {} - connectedness {} scaled_minimum {} - {} {}", i, cluster.size(), avgconnectedness, scaled_minimum, averageX, averageY);
			
			// It's too small or not well connected, bail out early
			if (cluster.size() < shotDetectionManager.getMinimumShotDimension() || avgconnectedness < scaled_minimum)
				continue;
			

			
			cluster.centerPixelX = averageX;
			cluster.centerPixelY = averageY;
			
			clusters.add(cluster);
		}
		return clusters;
	}
}
