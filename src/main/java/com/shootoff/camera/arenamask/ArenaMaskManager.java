package com.shootoff.camera.arenamask;

import java.awt.image.BufferedImage;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArenaMaskManager implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ArenaMaskManager.class);

	
	private Buffer cBuffer;
	private long delay = 0;
	
	private Mat mask = new Mat();
	Size dsize = null;
	
	private final static int LUM_MA_LENGTH = 10;
	
	double avgLums = 0;
	

	@Override
	public void run() {
		while (true)
		{
			if (cBuffer.isEmpty()) try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			else
			{
				
				while (((Mask)cBuffer.get()).timestamp < System.currentTimeMillis()-(.5*delay))
				{
					
					Mask nextMask = ((Mask)cBuffer.remove());
					Mat nextMat = nextMask.getSplitMask(dsize);
	
					logger.warn("updatingMask {} {} {}", nextMask.timestamp, avgLums, nextMask.getAvgMaskLum());
	
					
					for (int y = 0; y < nextMat.rows(); y++)
					{
						for (int x = 0; x < nextMat.cols(); x++)
						{
							double newLum = mask.get(y, x)[0] * (avgLums/(nextMask.getAvgMaskLum()));
							
							if ((x*y)%1000==0)
								logger.warn("pixel {} {}", mask.get(y,x)[0], newLum);
							
							newLum = ((mask.get(y, x)[0] * (LUM_MA_LENGTH-1)) + nextMat.get(y,x)[0]) / LUM_MA_LENGTH;
							
							mask.put(y, x, newLum);
						}
					}
				}
			}			
		}
	}
	
	
	public void setDelay(long delay)
	{
		this.delay = delay;
		
		//this.delay = 150;
	}
	
	public ArenaMaskManager(int width, int height)
	{
		mask = new Mat(height, width, CvType.CV_8UC1);
		dsize = new Size(width, height);
		cBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(70));
		
		
	}
	
	public void start()
	{
		(new Thread(this)).start();
	}
	
	public Mat getMask()
	{
		return mask;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void insert(BufferedImage bImage, long timestamp)
	{
		cBuffer.add(new Mask(bImage, timestamp));
	}
	
	public synchronized Mat getMaskForTimestamp(Size dsize, long timestamp)
	{
		long delayedTimestamp = timestamp - delay;
		
		long peekTimestamp = peekTimestamp();

		if (cBuffer.isEmpty())
			return null;

		
		while (peekTimestamp < delayedTimestamp-3)
		{
			/*if ((delayedTimestamp%30)==0)
			{
				logger.warn("getArenaMask {} remove", peekTimestamp - delayedTimestamp);
			}*/
				
			
			if (cBuffer.isEmpty())
				return null;
			cBuffer.remove();
			peekTimestamp = peekTimestamp();
		}
		
		/*if (peekTimestamp > delayedTimestamp)
		{
			if ((delayedTimestamp%30)==0)
			{
				logger.warn("getArenaMask {} future", peekTimestamp - delayedTimestamp);
			}
			return null;
		}*/
		/*if ((delayedTimestamp%30)==0)
		{
			logger.warn("getArenaMask {} good", peekTimestamp - delayedTimestamp);
		}*/
		
		return ((Mask)cBuffer.get()).getMask(dsize);
	}
	
	private synchronized long peekTimestamp()
	{
		if (cBuffer.isEmpty())
			return -1;
		
		return ((Mask)cBuffer.get()).timestamp;
	}


	public void updateAvgLums(double lumsMovingAverageAcrossFrame, long currentFrameTimestamp) {
		avgLums = lumsMovingAverageAcrossFrame;
	}

	
}
