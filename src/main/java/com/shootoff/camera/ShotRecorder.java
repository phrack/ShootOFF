package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class ShotRecorder {
	// The number of milliseconds before and after a shot to record
	private static final long RECORD_LENGTH = 5000; // ms
	
	private final Logger logger = LoggerFactory.getLogger(ShotRecorder.class);
	
	private final long startTime;
	private final File relativeVideoFile;
	private final File videoFile;
	private long shotTime;
	private final IMediaWriter videoWriter;
	private boolean hasShot = false;
	private boolean isFirstShotFrame = true;
	
	public ShotRecorder(ICodec.ID codec, String extension, String sessionName) {
		startTime = System.currentTimeMillis();
		relativeVideoFile =  new File(sessionName + File.separator + String.valueOf(System.nanoTime()) + extension);
		videoFile = new File(System.getProperty("shootoff.home") + File.separator + "sessions" + File.separator + 
				relativeVideoFile.getPath());
		
		System.out.println(relativeVideoFile.getPath());
		System.out.println(videoFile.getPath());
		
		videoWriter = ToolFactory.makeWriter(videoFile.getPath());
		videoWriter.addVideoStream(0, 0, codec, CameraManager.FEED_WIDTH, CameraManager.FEED_HEIGHT);
		
		logger.debug("Started recording new shot video: {}", videoFile.getName());
	}
	
	public void recordFrame(BufferedImage frame) {
		BufferedImage image = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

		IVideoPicture f = converter.toPicture(image,
				(System.currentTimeMillis() - startTime) * 1000);
		f.setKeyFrame(isFirstShotFrame);
		f.setQuality(0);
		isFirstShotFrame = false;

		videoWriter.encodeVideo(0, f);
	}
	
	public File getVideoFile() {
		return relativeVideoFile;
	}
	
	public boolean hasShot() {
		return this.hasShot;
	}
	
	public void recordShot() {
		shotTime = System.currentTimeMillis();
		hasShot = true;
		
		logger.debug("Recorded shot: {}", videoFile.getName());
	}
	
	public boolean isComplete() {
		boolean doneNoShot = !hasShot && System.currentTimeMillis() - startTime > RECORD_LENGTH;
		boolean doneShot = hasShot && System.currentTimeMillis() - shotTime > RECORD_LENGTH;
		
		return doneNoShot || doneShot;
	}
	
	public void close() {
		videoWriter.close();
		if (!hasShot) videoFile.delete();
		
		logger.debug("Stopped recording shot video: {}, hasShot = {}", videoFile.getName(), hasShot);
	}
}