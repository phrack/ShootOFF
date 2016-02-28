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

package com.shootoff.camera.arenamask;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class ArenaMaskManager implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ArenaMaskManager.class);

	private long delay = 0;

	private Mat mask = new Mat();
	private Size dsize = null;

	private int avgLums = 0;
	private int minLums;
	private int maxLums;

	private int[][] lumsMovingAverage;

	public volatile Mask maskFromArena = null;

	public Semaphore sem = new Semaphore(1);

	public AtomicBoolean isStreaming = new AtomicBoolean(true);

	private boolean recordMask = false;

	public ArenaMaskManager() {
		((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	@Override
	public void run() {
		if (logger.isDebugEnabled()) logger.debug("Starting arenaMaskManager thread");

		if (recordMask) startRecordingStream(new File("testingArenaMask.mp4"));

		while (isStreaming.get()) {
			try {
				sem.acquire();
			} catch (InterruptedException e) {
				continue;
			}
			if (maskFromArena == null) {
				sem.release();
				continue;
			}

			Mask nextMask = maskFromArena;
			long curDelay = System.currentTimeMillis() - nextMask.getTimestamp();

			if (curDelay > delay) {
				sem.release();
				continue;
			}

			handleMask(nextMask);
		}

		if (recordMask) stopRecordingStream();
	}

	public void handleMask(Mask nextMask) {
		Mat nextMat = nextMask.getLumMask(dsize);

		if (recordMask) recordMask(nextMask);

		int nextMaskAvgLum = nextMask.getAvgMaskLum();

		maskFromArena = null;
		sem.release();

		double scaledMaskAvgLum = (((double) (nextMaskAvgLum - 0) / (double) (255 * 255 - 0))
				* (double) (maxLums - minLums) + minLums);

		for (int y = 0; y < nextMat.rows(); y++) {
			for (int x = 0; x < nextMat.cols(); x++) {
				int[] maskpx = { 0 };
				mask.get(y, x, maskpx);

				int[] nextmatpx = { 0 };
				nextMat.get(y, x, nextmatpx);

				double scaler = (double) lumsMovingAverage[x][y] / scaledMaskAvgLum;

				// int newLum = (int) ((double) nextmatpx[0] * ((double)
				// lumsMovingAverage[x][y] / (double) nextMaskAvgLum));
				int scaledValue = (int) (((double) (nextmatpx[0] - 0) / (double) (255 * 255 - 0))
						* (double) (maxLums - minLums) + minLums);
				scaledValue *= scaler;

				// int normalized = (int)
				// ((double)(Math.min(Math.max(nextmatpx[0], minLums), maxLums)
				// - minLums) / (double)(maxLums - minLums));
				// int newLum = (int) ((double) normalized * ((double) avgLums /
				// (double) nextMaskAvgLum));

				// maskpx[0] = (((maskpx[0]) + scaledValue) / 2);
				maskpx[0] = scaledValue;

				/*
				 * if (scaledValue > lumsMovingAverage[x][y]) maskpx[0] =
				 * scaledValue; else maskpx[0] = (int) (.95 * maskpx[0]);
				 */
				/*
				 * if (x == 200 && y == 200 && nextmatpx[0] > 0) logger.warn(
				 * "pixel {} {} - min {} max {} - maskg {} scaledMaskAvgLum {} scaler {} scaledValue {} lumsMovingAverage[x][y] {} nmpx {} avgLums {} nmAvgLum {} mpx {}"
				 * , x, y, minLums, maxLums, mask.get(y, x)[0],
				 * scaledMaskAvgLum, scaler, scaledValue,
				 * lumsMovingAverage[x][y], nextmatpx[0], avgLums,
				 * nextMaskAvgLum, maskpx[0]);
				 */

				mask.put(y, x, maskpx);
			}
		}
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void start(int width, int height) {
		mask = new Mat(height, width, CvType.CV_32S);
		dsize = new Size(width, height);

		(new Thread(this)).start();
	}

	public Mat getMask() {
		return mask;
	}

	private boolean recordingStream = true;
	private boolean isFirstStreamFrame = true;
	private IMediaWriter videoWriterStream;
	private long recordingStartTime;

	private void recordMask(Mask mask) {
		if (recordingStream) {
			BufferedImage image = ConverterFactory.convertToType(mask.getMaskImage(), BufferedImage.TYPE_3BYTE_BGR);
			IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

			IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - recordingStartTime) * 1000);
			frame.setKeyFrame(isFirstStreamFrame);
			frame.setQuality(0);
			isFirstStreamFrame = false;

			videoWriterStream.encodeVideo(0, frame);
		}
	}

	public void startRecordingStream(File videoFile) {
		if (logger.isDebugEnabled()) logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());

		int width = (int) dsize.width;
		int height = (int) dsize.height;
		videoWriterStream = ToolFactory.makeWriter(videoFile.getName());
		videoWriterStream.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, width, height);
		recordingStartTime = System.currentTimeMillis();
		isFirstStreamFrame = true;

		recordingStream = true;
	}

	public void stopRecordingStream() {
		recordingStream = false;
		videoWriterStream.close();
	}

	public void setLumsMovingAverage(int[][] lumsMovingAverage) {
		this.lumsMovingAverage = lumsMovingAverage;
	}

	public void updateAvgLums(Mat frame) {
		long lumsCurrentAcrossFrame = 0;
		long lumsMinimumAcrossFrame = 255 * 255;
		long lumsMaximumAcrossFrame = 0;

		for (int y = 0; y < frame.rows(); y++) {
			for (int x = 0; x < frame.cols(); x++) {
				byte[] px = { 0, 0, 0 };
				frame.get(y, x, px);
				int matS = px[1] & 0xFF;
				int matV = px[2] & 0xFF;

				int curLum = ((255 - matS) * matV);

				lumsCurrentAcrossFrame += curLum;

				if (curLum > lumsMaximumAcrossFrame) {
					lumsMaximumAcrossFrame = curLum;
				} else if (curLum < lumsMinimumAcrossFrame) {
					lumsMinimumAcrossFrame = curLum;
				}
			}
		}

		lumsCurrentAcrossFrame /= frame.rows() * frame.cols();
		minLums = (int) lumsMinimumAcrossFrame;
		maxLums = (int) lumsMaximumAcrossFrame;
		avgLums = ((avgLums * 4) + (int) lumsCurrentAcrossFrame) / 5;
	}
}
