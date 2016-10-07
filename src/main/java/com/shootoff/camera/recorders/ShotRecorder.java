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

package com.shootoff.camera.recorders;

import java.awt.image.BufferedImage;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.Closeable;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class ShotRecorder implements Closeable {
	// The number of milliseconds before and after a shot to record
	public static final long RECORD_LENGTH = 5000; // ms

	private static final Logger logger = LoggerFactory.getLogger(ShotRecorder.class);

	private final long startTime;
	private final long timeOffset;
	private final File relativeVideoFile;
	private final File videoFile;
	private final String cameraName;
	private final IMediaWriter videoWriter;
	private boolean isFirstShotFrame = true;

	public ShotRecorder(File relativeVideoFile, File videoFile, long cutDuration, IMediaWriter videoWriter,
			String cameraName) {
		this.relativeVideoFile = relativeVideoFile;
		this.videoFile = videoFile;
		this.videoWriter = videoWriter;
		this.cameraName = cameraName;

		startTime = System.currentTimeMillis();
		timeOffset = cutDuration;

		logger.debug("Started recording shot video: {}, cut duration = {} ms", videoFile.getName(), cutDuration);
	}

	public void recordFrame(BufferedImage frame) {
		final BufferedImage image = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		final IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

		final long timestamp = (System.currentTimeMillis() - startTime) + timeOffset;

		final IVideoPicture f = converter.toPicture(image, timestamp * 1000);
		f.setKeyFrame(isFirstShotFrame);
		f.setQuality(0);
		isFirstShotFrame = false;

		videoWriter.encodeVideo(0, f);
	}

	public File getRelativeVideoFile() {
		return relativeVideoFile;
	}

	public File getVideoFile() {
		return videoFile;
	}

	public String getCameraName() {
		return cameraName;
	}

	public boolean isComplete() {
		return System.currentTimeMillis() - startTime > RECORD_LENGTH;
	}

	@Override
	public void close() {
		videoWriter.close();

		logger.debug("Stopped recording shot video: {}, timeOffset = {}", relativeVideoFile.getPath(), timeOffset);
	}
}