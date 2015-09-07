package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class ShotRecorder {
	// The number of milliseconds before and after a shot to record
	public static final long RECORD_LENGTH = 5000; // ms
	
	private final Logger logger = LoggerFactory.getLogger(ShotRecorder.class);
	
	private final long startTime;
	private final long timeOffset;
	private final File relativeVideoFile;
	private final File videoFile;
	private final String cameraName;
	private final IMediaWriter videoWriter;
	private boolean isFirstShotFrame = true;
	
	public ShotRecorder(File relativeVideoFile, File videoFile, IMediaWriter videoWriter, String cameraName) {
		this.relativeVideoFile = relativeVideoFile;
		this.videoFile = videoFile;
		this.videoWriter = videoWriter;
		this.cameraName = cameraName;
		
		IMediaReader r = ToolFactory.makeReader(this.videoFile.getPath());
		r.open();
		
		startTime = System.currentTimeMillis();
		timeOffset = r.getContainer().getDuration();
		
		r.close();
		
		logger.debug("Started recording shot video: {}", videoFile.getName());
	}
	
	public void recordFrame(BufferedImage frame) {
		BufferedImage image = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

		long timestamp = (System.currentTimeMillis() - startTime) + timeOffset;
		
		IVideoPicture f = converter.toPicture(image,
				timestamp * 1000);
		f.setKeyFrame(isFirstShotFrame);
		f.setQuality(0);
		isFirstShotFrame = false;

		videoWriter.encodeVideo(0, f);
	}
	
	public File getVideoFile() {
		return relativeVideoFile;
	}
	
	public String getCameraName() {
		return cameraName;
	}
	
	public boolean isComplete() {
		return System.currentTimeMillis() - startTime > RECORD_LENGTH;
	}
	
	public void close() {
		videoWriter.close();
		
		logger.debug("Stopped recording shot video: {}, timeOffset = {}", videoFile.getName(), timeOffset);
	}
}